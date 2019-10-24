package org.acme.github.issues;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.acme.github.issues.client.SDMApiClient;
import org.acme.github.issues.model.AppConfig;
import org.acme.github.issues.model.SDMAuth;
import org.acme.github.issues.utils.GitHubIssueMapper;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/webhook")
public class WebhookResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookResource.class);

    @Inject
    ApiManager apiManager;

    @Inject
    @RestClient
    SDMApiClient sdmApiClient;

    @Inject
    ConfigManager configManager;

    private String createDataMutationQuery;
    private String updateDataMutationQuery;
    private String dataQuery;


    @POST
    public void onWebhook(@HeaderParam("X-GitHub-Event") String eventType, JsonObject payload) {
        Integer installationId = payload.getJsonObject("installation").getJsonNumber("id").intValue();
        Optional<String> account = lookupAccount(installationId);
        if (account.isPresent()) {
            executeCreate(payload, account.get());
            LOGGER.info("Injecting webhook event for account {}", account.get());
        } else {
            LOGGER.error("No account found for installationId {}", installationId);
        }
    }

    private void executeCreate(JsonObject payload, String account) {
        String key = GitHubIssueMapper.buildV3Key(payload);
        JsonObject data = GitHubIssueMapper.mapV3Issue(payload);
        JsonObject variables = Json.createObjectBuilder()
                .add("key", key)
                .add("data", data)
                .build();
        JsonObject query = createPayload(getCreateDataMutation(), variables);
        String token =  apiManager.createToken(account);
        try {
            JsonObject response = sdmApiClient.executeQuery(account, new SDMAuth(token), query);

            if (response.getJsonArray("errors").size() > 0) {
                String id = getDataId(account, key);

                if (id == null) {
                    LOGGER.error("Unable to create entity for key {}. {}", key, response.getJsonArray("errors"));
                    return;
                }

                executeUpdate(id, data, account);
                return;
            }

            LOGGER.info("Successfully created entity with key {}", key);
        } catch (Exception e) {
            LOGGER.error("Error executing response", e);
            throw e;
        }
    }

    private void executeUpdate(String id, JsonObject mappedData, String account) {
        JsonObject variables = Json.createObjectBuilder()
                .add("id", id)
                .add("data", mappedData)
                .build();
        JsonObject query = createPayload(getUpdateDataMutation(), variables);
        String token =  apiManager.createToken(account);
        try {
            JsonObject response = sdmApiClient.executeQuery(account, new SDMAuth(token), query);

            if (response.getJsonArray("errors").size() > 0) {
                LOGGER.error("Unable to update entity with id {}. {}", id, response.getJsonArray("errors"));
                return;
            }

            LOGGER.info("Successfully updated entity with id {}", id);
        } catch (Exception e) {
            LOGGER.error("Error executing UDPDATE response", e);
            throw e;
        }
    }

    private String getDataId(String account, String key) {
        JsonObject variables = Json.createObjectBuilder()
                .add("key", key)
                .build();

        JsonObject query = createPayload(getDataQuery(), variables);
        String token =  apiManager.createToken(account);
        try {
            JsonObject response = sdmApiClient.executeQuery(account, new SDMAuth(token), query);

            if (response.getJsonArray("errors").size() > 0) {
                return null;
            }

            JsonArray nodes = response.getJsonObject("data").getJsonObject("gitHubIssues").getJsonArray("nodes");
            if (nodes.size() > 0) {
                return nodes.get(0).asJsonObject().getString("id");
            }
        } catch (Exception e) {
            LOGGER.error("Error executing response", e);
            throw e;
        }

        return null;
    }

    private Optional<String> lookupAccount(Integer installationId) {
        Map<String, List<AppConfig>> installations = configManager.getInstallations();

        return installations.entrySet()
                .stream()
                .filter(e -> e.getValue().stream().anyMatch(appConfig -> appConfig.getInstallationId().equals(installationId)))
                .findFirst()
                .map(Map.Entry::getKey);
    }

    private JsonObject createPayload(String query, JsonObject variables) {
        return Json.createObjectBuilder()
                .add("operationName", "AddGitHubIssues")
                .add("query", query)
                .add("variables", variables)
                .build();
    }

    public String getUpdateDataMutation() {
        if (updateDataMutationQuery == null) {
            updateDataMutationQuery = getResourceQuery("update-data-mutation.graphql");
        }

        return updateDataMutationQuery;
    }

    public String getCreateDataMutation() {
        if (createDataMutationQuery == null) {
            createDataMutationQuery = getResourceQuery("create-data-mutation.graphql");
        }

        return createDataMutationQuery;
    }

    public String getDataQuery() {
        if (dataQuery == null) {
            dataQuery = getResourceQuery("get-data-query.graphql");
        }

        return dataQuery;
    }

    public String getResourceQuery(String resourceFile) {
        try (InputStream is = WebhookResource.class.getResourceAsStream(resourceFile)) {
            return IOUtils.toString(is, Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Error parsing data mutation query file", e);
        }

        return null;
    }
}
package org.acme.github.issues;

import io.quarkus.runtime.StartupEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.acme.github.issues.client.SDMApiClient;
import org.acme.github.issues.model.AppConfig;
import org.acme.github.issues.model.SDMAuth;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    @Inject
    @ConfigProperty(name = "sdm.app.id", defaultValue = "24")
    String appId;

    @Inject
    @RestClient
    SDMApiClient sdmApiClient;

    @Inject
    ApiManager apiManager;

    private JsonObject configQuery;
    private Map<String, List<AppConfig>> installations;

    public Map<String, List<AppConfig>> getInstallations() {
        if (installations == null) {
            SDMAuth auth = new SDMAuth(apiManager.createToken("acme-issues"));
            JsonObject response = sdmApiClient.executeQuery("acme-issues", auth, getConfigQuery());

            if (response.getJsonArray("errors").size() > 0) {
                LOGGER.error("Error fetching config: {}", response.getJsonArray("errors"));
                return Collections.emptyMap();
            }

            installations = new HashMap();
            response.getJsonObject("data").getJsonObject("configs").getJsonArray("nodes")
                    .stream()
                    .map(JsonValue::asJsonObject)
                    .forEach( e-> {
                        String accountName = e.getString("account");
                        List<AppConfig> ghAccounts = new ArrayList<>();
                        e.getJsonObject("config").getJsonArray("githubAccounts").stream()
                         .map(JsonValue::asJsonObject)
                         .forEach( en -> {
                                       ghAccounts.add(new AppConfig(en));
                                   });
                        installations.put(accountName, ghAccounts);
                    });

        }
        return installations;
    }

    private JsonObject getConfigQuery() {
        if (configQuery == null) {
            String query = null;
            try (InputStream is = WebhookResource.class.getResourceAsStream("config-query.graphql")) {
                query = IOUtils.toString(is, Charset.defaultCharset());
            } catch (IOException e) {
                LOGGER.error("Error parsing data mutation query file", e);
            }

            configQuery = Json.createObjectBuilder()
                    .add("query", query)
                    .add("variables", Json.createObjectBuilder()
                        .add("appId", appId)
                        .build())
                    .add("operationName", "getConfigs")
                    .build();
        }

        return configQuery;
    }
}

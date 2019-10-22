package org.acme.github.issues;

import javax.json.JsonObject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/webhook")
public class WebhookResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookResource.class);

    @POST
    public void onWebhook(@HeaderParam("X-GitHub-Event") String eventType, JsonObject payload) {
        LOGGER.info("Received webhook event: {} payload: {} ", eventType, payload);
    }
}

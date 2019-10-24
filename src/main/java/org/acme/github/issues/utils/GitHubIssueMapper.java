package org.acme.github.issues.utils;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class GitHubIssueMapper {

    public static String buildV3Key(JsonObject payload) {
        return String.format("%s:%s",
                payload.getJsonObject("installation").getJsonNumber("id"),
                payload.getJsonObject("issue").getString("node_id"));
    }

    public static JsonObject mapV3Issue(JsonObject payload) {
        JsonObject issue = payload.getJsonObject("issue");
        JsonObject repository = payload.getJsonObject("repository");
        JsonObject user = issue.getJsonObject("user");

        JsonArrayBuilder assignees = Json.createArrayBuilder();
        issue.getJsonArray("assignees").forEach(assignee -> {
            assignees.add(Json.createObjectBuilder()
                .add("id", assignee.asJsonObject().getJsonNumber("id"))
                .add("nodeId", assignee.asJsonObject().getString("node_id"))
                .build());
        });

        return Json.createObjectBuilder()
                .add("id", issue.getJsonNumber("id"))
                .add("nodeId", issue.getString("node_id"))
                .add("url", issue.getString("url"))
                .add("repository", Json.createObjectBuilder()
                    .add("id", repository.getJsonNumber("id"))
                    .add("nodeId", repository.getString("node_id"))
                    .build())
                .add("author", Json.createObjectBuilder()
                    .add("login", user.getString("login"))
                    .build())
                .add("assignees", assignees)
                .add("body", issue.getString("body"))
                .add("state", issue.getString("state").toUpperCase())
                .add("number", issue.getJsonNumber("number"))
                .add("title", issue.getString("title"))
                .add("updatedAt", issue.getString("updated_at"))
                .add("publishedAt", issue.getString("created_at"))
                .build();
    }
}

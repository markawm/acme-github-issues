package org.acme.github.issues.model;

import java.util.Objects;
import javax.json.JsonObject;

public class AppConfig {
    private String githubAccount;
    private Integer installationId;

    public AppConfig() {

    }

    public AppConfig(JsonObject config) {
        this.githubAccount = config.getString("account");
        this.installationId = config.getJsonNumber("installationId").intValue();
    }

    public AppConfig(String githubAccount, Integer installationId) {
        this.githubAccount = githubAccount;
        this.installationId = installationId;
    }

    public String getGithubAccount() {
        return githubAccount;
    }

    public void setGithubAccount(String githubAccount) {
        this.githubAccount = githubAccount;
    }

    public Integer getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Integer installationId) {
        this.installationId = installationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AppConfig appConfig = (AppConfig) o;
        return Objects.equals(githubAccount, appConfig.githubAccount) &&
                Objects.equals(installationId, appConfig.installationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubAccount, installationId);
    }

    @Override
    public String toString() {
        return "AppConfig{" +
                "githubAccount='" + githubAccount + '\'' +
                ", installationId='" + installationId + '\'' +
                '}';
    }
}

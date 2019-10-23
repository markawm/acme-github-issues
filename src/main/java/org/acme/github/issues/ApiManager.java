package org.acme.github.issues;

import com.cloudbees.sdm.api.AppTokens;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ApiManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiManager.class);

    @ConfigProperty(name = "app.private.key", defaultValue = "sdm-app-private-key.pem")
    String privateKey;

    @ConfigProperty(name = "sdm.api.url", defaultValue = "https://devoptics.devoptics-dev.beescloud.com")
    String sdmApiUrl;

    @ConfigProperty(name = "sdm.app.id", defaultValue = "24")
    String appId;

    public AppTokens appTokens;

    @PostConstruct
    public void setup() {
        String privateKeyValue = null;
        try (InputStream is = ApiManager.class.getResourceAsStream(privateKey)) {
            privateKeyValue = IOUtils.toString(is, Charset.defaultCharset());
        } catch (IOException e) {
            LOGGER.error("Error parsing private key file", e);
        }
        appTokens = new AppTokens(sdmApiUrl, privateKeyValue, appId);
    }

    String createToken(String account) {
        try {
            return appTokens.getAccountAccessToken(account);
        } catch (IOException e) {
            LOGGER.error("Error getting token", e);
        }

        return null;
    }
}

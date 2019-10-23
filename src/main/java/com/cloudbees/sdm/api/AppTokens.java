package com.cloudbees.sdm.api;

import com.damnhandy.uri.template.UriTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppTokens {
    private static final long ACCESS_TOKEN_VALIDITY_BUFFER_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final Logger LOGGER = LoggerFactory.getLogger(AppTokens.class);

    private transient PrivateKey privateKey;
    private transient AccessToken appAccessToken;
    private transient Map<String, AccessToken> accountAccessTokens = new HashMap<String, AccessToken>();

    private String platformEndpoint;
    private String privateKeyStr;
    private String appId;

    public AppTokens(String platformEndpoint, String privateKeyStr, String appId) {
        this.privateKeyStr = privateKeyStr;
        this.appId = appId;
        this.platformEndpoint = platformEndpoint;
    }

    public String getAccountAccessToken(String account) throws IOException {
        AccessToken accountAccessToken = accountAccessTokens.get(account);
        if (accountAccessToken != null && !accountAccessToken.isExpired()) {
            LOGGER.debug("Using cached token (account={}, expiresAt={})", account, new Date(accountAccessToken.getExpiresAt()));
            return accountAccessToken.getToken();
        }

        LOGGER.debug("Fetching app account token (account={})", account);
        String appAccessToken = appAccessToken();

        UriTemplate accountTokenTemplate = UriTemplate.buildFromTemplate(platformEndpoint)
                .literal("/platform/api/app/installations").path("account").literal("/accessToken")
                .build();

        URL accountTokenUrl = new URL(accountTokenTemplate.set("account", account).expand());

        HttpURLConnection connection = (HttpURLConnection)accountTokenUrl.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + appAccessToken);
        connection.connect();
        int status = connection.getResponseCode();
        if (status / 100 == 2) {
            try (InputStream in = connection.getInputStream()) {
                String accountBearerToken = IOUtils.toString(in, StandardCharsets.UTF_8.name());
                accountAccessTokens.put(account, new AccessToken(accountBearerToken));
                return accountBearerToken;
            }
        }
        else {
            throw new IOException("Could not retrieve access token (httpErrorCode:" + status + ")");
        }
    }

    public String appAccessToken() throws IOException {
        if (appAccessToken != null && !appAccessToken.isExpired()) {
            return appAccessToken.getToken();
        }
        long issuedAtMillis = System.currentTimeMillis();
        JwtClaims appToken = new JwtClaims();
        appToken.setGeneratedJwtId();
        appToken.setNotBefore(
                NumericDate.fromMilliseconds(issuedAtMillis - TimeUnit.MINUTES.toMillis(5)));
        appToken.setIssuedAt(NumericDate.fromMilliseconds(issuedAtMillis));
        appToken.setExpirationTime(
                NumericDate.fromMilliseconds(issuedAtMillis + TimeUnit.MINUTES.toMillis(5)));
        appToken.setSubject(appId);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(appToken.toJson());
        jws.setKey(getPrivateKey());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        String appBearerToken;
        try {
            appBearerToken = jws.getCompactSerialization();
        } catch (JoseException e) {
            throw new IOException("Could not sign Application JWT", e);
        }
        appAccessToken = new AccessToken(appBearerToken);
        return appBearerToken;
    }

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            try {
                privateKey = PrivateKeyUtils.readPrivateKey(privateKeyStr, null);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return privateKey;
    }

    private static class AccessToken {
        private final String token;
        private final long expiresMillis;

        public AccessToken(String token) {
            this.token = token;
            JwtConsumerBuilder builder = new JwtConsumerBuilder()
                    .setRelaxVerificationKeyValidation().setSkipSignatureVerification();
            JwtClaims claims;
            long expiresMillis;
            try {
                claims = builder.build().processToClaims(token);
                expiresMillis = claims.getExpirationTime().getValueInMillis();
            } catch (InvalidJwtException | MalformedClaimException e) {
                expiresMillis = System.currentTimeMillis() - 1;
            }
            this.expiresMillis = expiresMillis;
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return expiresMillis <= System.currentTimeMillis()
                    + ACCESS_TOKEN_VALIDITY_BUFFER_MILLIS;
        }

        public long getExpiresAt() {
            return expiresMillis;
        }
    }
}

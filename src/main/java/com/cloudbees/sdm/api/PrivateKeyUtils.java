/*
 * Copyright 2018 CloudBees, Inc.
 * All rights reserved.
 */

package com.cloudbees.sdm.api;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.Security;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * Utility to read the private key from a PEM file.
 */
public class PrivateKeyUtils {
    /**
     * The converter singleton.
     */
    private static final JcaPEMKeyConverter CONVERTER = newConverter();

    /**
     * Reads a private key from a file.
     *
     * @param file the file.
     * @param password the password or {@code null}.
     * @return the private key.
     * @throws IOException if things go wrong.
     */
    public static PrivateKey readPrivateKey(File file, char[] password) throws IOException {
        try (FileInputStream in = new FileInputStream(file)) {
            return readPrivateKey(in, password);
        }
    }

    /**
     * Reads a private key from a stream.
     *
     * @param stream the stream.
     * @param password the password or {@code null}.
     * @return the private key.
     * @throws IOException if things go wrong.
     */
    public static PrivateKey readPrivateKey(InputStream stream, char[] password)
            throws IOException {
        return readPrivateKey(stream, StandardCharsets.US_ASCII, password);
    }

    /**
     * Reads a private key from a stream.
     *
     * @param stream the stream.
     * @param charset the charset.
     * @param password the password or {@code null}.
     * @return the private key.
     * @throws IOException if things go wrong.
     */
    private static PrivateKey readPrivateKey(InputStream stream, Charset charset,
                                             char[] password) throws IOException {
        try (InputStreamReader in = new InputStreamReader(stream, charset);
                PEMParser parser = new PEMParser(in)) {
            Object o = parser.readObject();
            if (o instanceof PEMEncryptedKeyPair) {
                PEMEncryptedKeyPair kp = (PEMEncryptedKeyPair) o;
                return CONVERTER.getKeyPair(kp.decryptKeyPair(new BcPEMDecryptorProvider(password))).getPrivate();
            }
            if (o instanceof PEMKeyPair) {
                return CONVERTER.getKeyPair((PEMKeyPair) o).getPrivate();
            }
            if (o instanceof PrivateKeyInfo) {
                return CONVERTER.getPrivateKey((PrivateKeyInfo) o);
            }
            if (o instanceof KeyPair) {
                return ((KeyPair) o).getPrivate();
            }
            if (o instanceof PrivateKey) {
                return (PrivateKey) o;
            }
            throw new IOException("Unknown key format");
        } catch (PEMException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Returns a PrivateKey instance from its PEM format stored in a file or elsewhere.
     *
     * @param key String representing the PEM format for a key
     * @return associated PrivateKey instance
     * @throws IOException if the key cannot be read.
     */
    public static PrivateKey readPrivateKey(String key, char[] password) throws IOException {
        try (InputStream in = new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8))) {
            return readPrivateKey(in, StandardCharsets.UTF_8, password);
        }
    }

    /**
     * Reads a private key from an URL.
     *
     * @param url the url.
     * @param password the password or {@code null}.
     * @return the private key.
     * @throws IOException if things go wrong.
     */
    public static PrivateKey readPrivateKey(URL url, char[] password) throws IOException {
        return readPrivateKey(url.openStream(), StandardCharsets.US_ASCII, password);
    }

    private static JcaPEMKeyConverter newConverter() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        return new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
    }
}
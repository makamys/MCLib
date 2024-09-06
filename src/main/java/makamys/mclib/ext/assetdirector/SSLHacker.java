package makamys.mclib.ext.assetdirector;

import static makamys.mclib.core.MCLib.LOGGER;

import lombok.val;
import makamys.mclib.core.MCLib;
import org.apache.http.conn.ssl.SSLContexts;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;

/**
 * Hack Java's certificate handling to unconditionally accept Mojang's certificates.
 * Needed because java 1.8.0_51 doesn't support Mojang's new certificates, and many launchers bundle this version of Java.
 */
@SuppressWarnings("restriction")
public class SSLHacker {

    public static void hack() {
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.8.0")) {
            // check if patch < 75
            String patchVersion = javaVersion.split("_")[1].split("-")[0];
            if (Integer.parseInt(patchVersion) < 75) {
                LOGGER.warn("Your Java version (" + javaVersion + ") is out of date! Please consider updating to a newer version for improved stability and security.");
                LOGGER.warn("AssetDirector will replace the SSL trust manager to apply a compatibility hack.");
                try {
                    replaceSSLContext();
                } catch (Exception e) {
                    LOGGER.error("Failed to replace SSL trust manager! Asset downloads may fail.");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void replaceSSLContext() throws Exception {
        final CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);

        // from https://www.digicert.com/kb/digicert-root-certificates.htm
        try (InputStream is = SSLHacker.class.getClassLoader().getResourceAsStream("resources/mclib/" + MCLib.RESOURCES_VERSION + "/certs/DigiCertGlobalRootG3.crt.pem")) {
            final Certificate cert = cf.generateCertificate(is);
            keyStore.setCertificateEntry("digicertglobalrootg3", cert);
        }
        // TODO: consider adding other root certs added in https://www.oracle.com/java/technologies/javase/8u75-relnotes.html
        // and https://www.oracle.com/java/technologies/javase/8u101-relnotes.html (and probably some others)

        SSLContext fixed = SSLContexts.custom()
            .loadTrustMaterial(null, null) // load default ssl
            .loadTrustMaterial(keyStore, null) // load new certs
            .build();

        SSLContext.setDefault(fixed);
    }
}

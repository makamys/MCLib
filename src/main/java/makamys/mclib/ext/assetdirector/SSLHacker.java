package makamys.mclib.ext.assetdirector;

import static makamys.mclib.core.MCLib.LOGGER;

import sun.security.ssl.SSLContextImpl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Hack Java's certificate handling to unconditionally accept Mojang's certificates.
 * Needed because java 1.8.0_51 doesn't support Mojang's new certificates, and many launchers bundle this version of Java.
 */
@SuppressWarnings("restriction")
public class SSLHacker {

    private static final String TARGET_JAVA_VERSION = "1.8.0_51";
    private static final List<String> TRUSTED_HOSTS = Arrays.asList("minecraft.net", "mojang.com");
    
    private static boolean isEnabled;

    public static void hack() {
        if(System.getProperty("java.version").equals(TARGET_JAVA_VERSION)) {
            LOGGER.warn("Your Java version (" + TARGET_JAVA_VERSION + ") is out of date! Please consider updating to a newer version for improved stability and security.");
            LOGGER.warn("AssetDirector will replace the SSL trust manager to apply a compatibility hack.");
            try {
                replaceTrustManager();
            } catch(Exception e) {
                LOGGER.error("Failed to replace SSL trust manager! Asset downloads may fail.");
                e.printStackTrace();
            }
        }
    }

    private static void replaceTrustManager() throws Exception {
        SSLContext context = SSLContext.getDefault();
        
        Field contextSpiF = SSLContext.class.getDeclaredField("contextSpi");
        contextSpiF.setAccessible(true);
        SSLContextImpl contextImpl = (SSLContextImpl)contextSpiF.get(context);
        Field tmF = SSLContextImpl.class.getDeclaredField("trustManager");
        tmF.setAccessible(true);
        X509ExtendedTrustManager tm = (X509ExtendedTrustManager)tmF.get(contextImpl);
        
        tmF.set(contextImpl, new HackedX509TrustManager(tm));
    }

    private static class HackedX509TrustManager extends X509ExtendedTrustManager {

        private X509ExtendedTrustManager original;

        public HackedX509TrustManager(X509ExtendedTrustManager original) {
            this.original = original;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            original.checkClientTrusted(chain, authType, socket);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            SocketAddress sa = socket.getRemoteSocketAddress();
            if(isEnabled && sa instanceof InetSocketAddress) {
                String fullHost = ((InetSocketAddress)sa).getHostName();
                int lastDot = fullHost.lastIndexOf(".");
                int secondLastDot = fullHost.lastIndexOf(".", lastDot - 1);
                
                String host = secondLastDot == -1 ? fullHost : fullHost.substring(secondLastDot + 1);
                
                if(TRUSTED_HOSTS.contains(host)) {
                    return;
                }
            }
            original.checkServerTrusted(chain, authType, socket);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            original.checkClientTrusted(chain, authType, engine);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            original.checkServerTrusted(chain, authType, engine);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            original.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            original.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return original.getAcceptedIssuers();
        }
    }

    public static void enable() {
        isEnabled = true;
    }
    
    public static void disable() {
        isEnabled = false;
    }
}

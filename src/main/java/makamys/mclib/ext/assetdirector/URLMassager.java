package makamys.mclib.ext.assetdirector;

import static makamys.mclib.ext.assetdirector.AssetDirector.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLHandshakeException;

public class URLMassager {

    private Set<String> checkedHosts = new HashSet<>();
    private Set<String> httpHosts = new HashSet<>();
    
    public URL toURL(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        
        if(url.getProtocol().toLowerCase().equals("https") && useHTTPForURL(url)) {
            return new URL("http", url.getHost(), url.getPort(), url.getFile());
        } else {
            return url;
        }
    }
    
    private boolean useHTTPForURL(URL url) {
        String host = url.getHost();
        
        if(checkedHosts.contains(host)) {
            return httpHosts.contains(host);
        } else {
            checkedHosts.add(host);
            try(InputStream is = url.openStream()){
                // connection ok
            } catch (SSLHandshakeException e) {
                LOGGER.warn("SSL handshake failed for host " + host + " (" + e.getMessage() + "). Will attempt to use HTTP for this host. This is a workaround for old Java versions not supporting new TLS ciphers - please try updating your Java to fix this issue if possible.");
                silentlyPrintStackTrace(e);
                
                httpHosts.add(host);
                return true;
            } catch(IOException e) {
                LOGGER.warn("Failed to connect to host " + host + ".");
                e.printStackTrace();
            }
        }
        
        return false;
    }
    
    private static void silentlyPrintStackTrace(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter out = new PrintWriter(writer);
        e.printStackTrace(out);
        LOGGER.trace(writer.toString());
    }
    
}

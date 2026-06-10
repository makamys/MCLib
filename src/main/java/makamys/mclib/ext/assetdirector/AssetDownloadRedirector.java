package makamys.mclib.ext.assetdirector;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nullable;

public class AssetDownloadRedirector {

    public static final String REDIRECT_BASE_URL_PROPERTY = "assetDirector.downloadRedirectBaseUrl";

    private static final String LAUNCHER_META_HOST = "launchermeta.mojang.com";
    private static final String PISTON_META_HOST = "piston-meta.mojang.com";
    private static final String PISTON_DATA_HOST = "piston-data.mojang.com";
    private static final String RESOURCES_HOST = "resources.download.minecraft.net";

    private static String redirectBaseUrl = normalizeBaseUrl(System.getProperty(REDIRECT_BASE_URL_PROPERTY, ""));

    private AssetDownloadRedirector() {}

    public static void setRedirectBaseUrl(@Nullable String baseUrl) {
        redirectBaseUrl = normalizeBaseUrl(baseUrl);
    }

    public static String getRedirectBaseUrl() {
        return redirectBaseUrl;
    }

    public static void clearRedirectBaseUrl() {
        redirectBaseUrl = "";
    }

    public static String redirect(String source) {
        String baseUrl = redirectBaseUrl;
        if (baseUrl.isEmpty()) {
            return source;
        }

        try {
            URL url = new URL(source);
            String redirectedPath = getRedirectedPath(url);
            if (redirectedPath == null) {
                return source;
            }
            return baseUrl + redirectedPath;
        } catch (MalformedURLException e) {
            AssetDirector.LOGGER.warn("Ignoring malformed download URL " + source);
            return source;
        }
    }

    private static String normalizeBaseUrl(@Nullable String baseUrl) {
        if (baseUrl == null) {
            return "";
        }

        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return "";
        }

        try {
            new URL(trimmed);
            return trimmed;
        } catch (MalformedURLException e) {
            AssetDirector.LOGGER.warn("Ignoring malformed AssetDirector redirect base URL " + baseUrl);
            return "";
        }
    }

    @Nullable
    private static String getRedirectedPath(URL url) {
        String host = url.getHost();
        String path = url.getPath();
        String query = url.getQuery();

        if (RESOURCES_HOST.equalsIgnoreCase(host)) {
            return appendQuery("/assets" + path, query);
        }
        if (LAUNCHER_META_HOST.equalsIgnoreCase(host) || PISTON_META_HOST.equalsIgnoreCase(host)
            || PISTON_DATA_HOST.equalsIgnoreCase(host)) {
            return appendQuery(path, query);
        }
        return null;
    }

    private static String appendQuery(String path, @Nullable String query) {
        if (query == null || query.isEmpty()) {
            return path;
        }
        return path + "?" + query;
    }
}

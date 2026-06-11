package makamys.mclib.updatecheck;

/**
 * <p>Simulates a fake "internet" where you can upload text files to urls, and download them.
 * <p>This "internet" is addressed with the protocol mock://
 */
public class MockHelper {
    
    private static final boolean TEST_MODE = Boolean.parseBoolean(System.getProperty("updateCheckLib.test", "false"));
    private static final String MOCK_PROPERTY_PREFIX = "updatechecklib_mock_url_";
    
    public static final String MOCK_PREFIX = "mock://";
    
    public static boolean isTestMode() {
        return TEST_MODE;
    }

    public static boolean isMockUrl(String url) {
        return url.startsWith(MOCK_PREFIX);
    }
    
    public static String downloadMockText(String url) {
        if(!isMockUrl(url)) throw new IllegalArgumentException();
        
        return System.getProperty(MOCK_PROPERTY_PREFIX + url);
    }
    
    public static String uploadMockText(String url, String text) {
        if(!isMockUrl(url)) throw new IllegalArgumentException();
        
        System.setProperty(MOCK_PROPERTY_PREFIX + url, text);
        
        return url;
    }
    
}

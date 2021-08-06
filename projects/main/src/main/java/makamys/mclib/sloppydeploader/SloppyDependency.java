package makamys.mclib.sloppydeploader;

import java.util.Optional;

public class SloppyDependency {
    String testClass;
    String repo;
    String filename;
    Optional<String> dev;
    Optional<String> pattern;
    
    public SloppyDependency(String[] params) {
        this(params[0], params[1], params[2], params[3], params[4]);
    }
    
    public SloppyDependency(String repo, String filename, String testClass, String dev, String pattern) {
        this.testClass = testClass;
        this.repo = repo;
        this.filename = filename;
        this.dev = stringToOpt(dev);
        this.pattern = stringToOpt(pattern);
    }
    
    private static Optional<String> stringToOpt(String str) {
        return str == null || str.isEmpty() ? Optional.empty() : Optional.of(str);
    }
    
    public SloppyDependency(String repo, String filename, String testClass) {
        this(repo, filename, testClass, null, null);
    }
    
    public String serializeToString() {
        return String.join(",", new String[] {repo, filename, testClass, dev.orElse(""), pattern.orElse("")});
    }
}

package makamys.mclib.ext.assetdirector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import makamys.mclib.ext.assetdirector.ADConfig.VersionAssets.SoundEvent;

public class ADConfig {
    
    Map<String, VersionAssets> assets = new HashMap<>();
    
    private VersionAssets getOrCreateVersionAssets(String version) {
        VersionAssets va = assets.get(version);
        if(va == null) {
            assets.put(version, va = new VersionAssets());
        }
        return va;
    }
    
    public void addObject(String version, String path) {
        getOrCreateVersionAssets(version).objects.add(path);
    }
    
    public void addSoundEvent(String version, String name) {
        addSoundEvent(version, name, null);
    }
    
    public void addSoundEvent(String version, String name, String category) {
        getOrCreateVersionAssets(version).soundEvents.add(new SoundEvent(name, category));
    }
    
    public void addJar(String version) {
        getOrCreateVersionAssets(version).jar = true;
    }
    
    public static class VersionAssets {
        Set<String> objects = new HashSet<>();
        Set<SoundEvent> soundEvents = new HashSet<>();
        boolean jar;
        
        @Data
        @AllArgsConstructor
        public static class SoundEvent {
            public String name;
            public String category;
        }
    }
    
}

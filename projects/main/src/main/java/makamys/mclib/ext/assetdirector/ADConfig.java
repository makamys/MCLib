package makamys.mclib.ext.assetdirector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<String> objects = new ArrayList<>();
        List<SoundEvent> soundEvents = new ArrayList<>();
        boolean jar;
        
        public static class SoundEvent {
            public String name;
            public String category;
            
            public SoundEvent(String name, String category) {
                this.name = name;
                this.category = category;
            }
        }
    }
    
}

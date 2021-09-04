package makamys.mclib.core;

import makamys.mclib.core.sharedstate.SharedLibHelper;
import makamys.mclib.sloppydeploader.SloppyDepLoader;

class InternalModules {
    static { SharedLibHelper.shareifyClass(InternalModules.class); }
    
    public static SloppyDepLoader sloppyDepLoader;
}

# MCLib

This is a library that can be [shaded](http://web.archive.org/web/20150403035341/http://forgegradle.readthedocs.org/en/FG_1.2/user-guide/shading/) into Minecraft mods. It consists of the following subprojects. See their readmes for information about their licenses.

* [main](projects/main/README.md)
* [lesser](projects/lesser/README.md)

# What's inside?

* `SloppyDepLoader`: a dependency loader for optional dependencies, which won't fail if the dependency fails to be located. It *may or may not* load your dependencies *eventually*, hence its name.
* `UpdateCheckLibHelper`: helper for easily using [UpdateCheckLib](https://github.com/makamys/UpdateCheckLib).
* `InventoryUtils2`: helper for CodeChickenLib's `InventoryUtils` class
* `SharedReference`: helper for sharing state between multiple instances of the same shaded library (used by `SloppyDepLoader`)

# Usage

Releases are published on JitPack. To use this library in your mod, add this to your buildscript:

```gradle
repositories {
	maven { url 'https://jitpack.io' }
}

minecraft {
	srgExtra "PK: makamys/mclib <your mod's package>/lib/makamys/mclib"
}

dependencies {
	shade 'com.github.makamys:MCLib:0.1.3'
}
```

Refer to my mod [Satchels](https://github.com/makamys/Satchels) as an example of how it can be used in a project.
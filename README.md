# MCLib

This is a library that can be [shaded](http://web.archive.org/web/20150403035341/http://forgegradle.readthedocs.org/en/FG_1.2/user-guide/shading/) into Minecraft mods.

# What's inside?

## Shared modules

When multiple mods with MCLib embedded are present, calls to shared modules from all mods are redirected to whichever mod has the newest version of the given module.

* [`UpdateCheck`](https://github.com/makamys/MCLib/wiki/UpdateCheck): checks for updates and displays the results in a HTML file, with a notification button in the main menu.
	* Accessed via `MCLibModules.updateCheckAPI`
	* Example usage: [UCLTest.java](src/main/java/makamys/mclib/updatecheck/test/UCLTest.java)
* [`AssetDirector`](https://github.com/makamys/MCLib/wiki/AssetDirector): downloader for Mojang's assets and Minecraft jars directly off their servers, allowing use and redistribution of things like sounds without EULA worries.
	* Accessed via `AssetDirectorAPI`
	* Example usage: [ADTest.java](src/main/java/makamys/mclib/ext/assetdirector/test/ADTest.java)
* `SloppyDepLoader`: a dependency loader for optional dependencies, which won't fail if the dependency fails to be located. It makes no guarantee it will locate the requested dependencies, hence its name.
	* Accessed via `SloppyDepLoaderAPI`.
	* Example usage: [SDLTest.java](src/main/java/makamys/mclib/sloppydeploader/test/SDLTest.java).

## Helper classes
* `InventoryUtils2`: helper for CodeChickenLib's `InventoryUtils` class

# Usage

## Adding the dependency

Releases are published on JitPack. To use this library in your mod, add this to your buildscript:

```gradle
repositories {
	maven { url 'https://jitpack.io' }
}

minecraft {
	srgExtra "PK: makamys/mclib <YOUR/MOD/PACKAGE>/repackage/makamys/mclib"
}

dependencies {
	shade('com.github.makamys:MCLib:<VERSION>'){
		exclude group: "codechicken"
	}
}
```

In place of `<VERSION>`, use a release number (e.g. `0.3.4`).

> A commit hash can also be used, but doing this in production is discouraged as it may make MCLib unable to correctly select the highest version of the library.

### Shade configuration

The above snippet assumes there is a `shade` configuration in your build script. If there isn't, you can use the below one as an example (the `configurations` block has to go before the `dependencies` block). For additional explanation on what this does, see [ForgeGradle's shading tutorial](http://web.archive.org/web/20150403035341/http://forgegradle.readthedocs.org/en/FG_1.2/user-guide/shading/).

```gradle
configurations {
    shade
    compile.extendsFrom shade
}

jar {
    configurations.shade.each { dep ->
        from(project.zipTree(dep)){
            exclude 'META-INF', 'META-INF/**'
        }
    }
}
```

## Using the library

The library first has to be initialized by calling `MCLib.init()` in the mod construction phase. You can do this like this in your mod class:

```java
@EventHandler
public void onConstruction(FMLConstructionEvent event) {
	MCLib.init();
}
```

Static helper classes like `SloppyDepLoader` can be used by simply calling their static methods.

Shared modules like `UpdateCheck` require special setup via their respective `*API` classes (e.g. `UpdateCheckAPI`). See the example test files above to see how to use them.

Check the [wiki](https://github.com/makamys/MCLib/wiki) for more documentation.

If you want to see some examples of the library in action, see [Satchels](https://github.com/makamys/Satchels) and [Et Futurum Requiem](https://github.com/Roadhog360/Et-Futurum-Requiem).

# Contributing

The library has some test packages containing test mods that test its features. These packages are excluded from builds normally. To enable them, add `-Ptest_<library name>` to your Gradle command. See [project.gradle](project.gradle) for the available flags.

# License

See [LICENSE.md](LICENSE.md).

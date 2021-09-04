# MCLib

This is a library that can be [shaded](http://web.archive.org/web/20150403035341/http://forgegradle.readthedocs.org/en/FG_1.2/user-guide/shading/) into Minecraft mods. It consists of the following subprojects. See their readmes for information about their licenses.

* [main](projects/main/README.md)
* [lesser](projects/lesser/README.md)

# What's inside?

* [`UpdateCheck`](docs/UpdateCheck.md): checks for updates and displays the results in a HTML file, with a notification button in the main menu.
	* Accessed via `MCLibModules.updateCheckAPI`
* `SloppyDepLoader`: a dependency loader for optional dependencies, which won't fail if the dependency fails to be located. It *may or may not* load your dependencies *eventually*, hence its name.
	* Accessed via `SloppyDepLoaderAPI`
* `InventoryUtils2`: helper for CodeChickenLib's `InventoryUtils` class

# Usage

## Adding the dependency

Releases are published on JitPack. To use this library in your mod, add this to your buildscript:

```gradle
repositories {
	maven { url 'https://jitpack.io' }
}

minecraft {
	srgExtra "PK: makamys/mclib <your mod's package>/lib/makamys/mclib"
}

dependencies {
	shade 'com.github.makamys:MCLib:0.2'
}
```

## Using the library

The library first has to be initialized by calling `MCLib.init()` in the mod construction phase. You can do this by adding a static block like this to your mod class:

```java
static {
	MCLib.init();
}
```

Static helper classes like `SloppyDepLoader` can be used by simply calling their static methods.

Shared modules like `UpdateCheck` are exposed through the static fields of the `MCLibModules` class. These can be used starting from the pre-init phase, *but no earlier*. When multiple mods with MCLib embedded are present, calls to shared modules from all mods are redirected to a single mod which has the newest version of the library.

Refer to my mod [Satchels](https://github.com/makamys/Satchels) for an example of a mod using this library.

## More documentation

* [UpdateCheck](docs/UpdateCheck.md)

# Contributing

The library has some test packages containing test mods that test its features. These packages are excluded from builds normally. To enable them, add `-Ptest_foo` and such to your Gradle command.
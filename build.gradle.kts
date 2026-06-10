plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

sourceSets {
    main {
        if (!project.hasProperty("test_sdl")) {
            java.exclude("makamys/mclib/sloppydeploader/test/**")
        }
        if (!project.hasProperty("test_uc")) {
            java.exclude("makamys/mclib/updatecheck/test/**")
        }
        if (!project.hasProperty("test_ad")) {
            java.exclude("makamys/mclib/ext/assetdirector/test/**")
            resources.exclude("assets/adtest/**")
        }
    }
}

tasks.named<Jar>("jar") {
    val embedded = configurations.named("embedded")
    archiveBaseName.set("${project.property("modId")}-all-${project.property("minecraftVersion")}")
    from(embedded.map { configuration -> configuration.map { if (it.isDirectory) it else zipTree(it) } }) {
        exclude("**/LICENSE", "**/LICENSE.txt")
    }
}

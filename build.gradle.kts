plugins {
    id("idea")
    id("java")
    id("java-library")
    id("net.neoforged.moddev") version("2.0.78")
}

version = project.properties["mod_version"]!!
group = project.properties["mod_group"]!!

// Client-only regression mod; kept outside src/ because this project also includes src as a main source root.
val startupReferenceJar = providers.gradleProperty("startupReferenceJar").orNull?.let { file(it) }
val startupPackMods = providers.gradleProperty("startupPackMods").orNull?.let { file(it) }
val startupCheck = sourceSets.create("startupCheck") {
    java.setSrcDirs(listOf("tests/client/java"))
    resources.setSrcDirs(listOf("tests/client/resources"))
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath - sourceSets.main.get().output
}
if (startupPackMods != null) {
    require(startupPackMods.isDirectory) { "Missing startupPackMods directory" }
    // The selected pack supplies these runtime mods; compilation still uses the declared API versions.
    val replacedRuntimeMods = files(providers.provider { configurations.runtimeClasspath.get().incoming.artifacts.artifacts
        .filter {
            val id = it.id.componentIdentifier as? org.gradle.api.artifacts.component.ModuleComponentIdentifier
            id?.group in setOf("com.simibubi.create", "net.createmod.ponder", "dev.engine-room.flywheel",
                "com.tterrag.registrate", "curse.maven")
        }.map { it.file } })
    startupCheck.runtimeClasspath -= replacedRuntimeMods
}
val startupCheckName = providers.gradleProperty("startupCheckName").getOrElse("manual")
require(startupCheckName.matches(Regex("[A-Za-z0-9_-]+"))) { "Unsafe startupCheckName" }
val startupCheckDirectory = layout.buildDirectory.dir("startup-check/$startupCheckName")

repositories {
    mavenLocal()

    maven("https://maven.neoforged.net/#/releases/")
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven")
    maven("https://modmaven.dev")
    maven("https://maven.createmod.net") // Create, Ponder, Flywheel
    maven("https://maven.ithundxr.dev/snapshots") // Registrate
    maven("https://maven.blamejared.com") // JEI, Vazkii's Mods
}

dependencies {
    if (startupReferenceJar != null) {
        require(startupReferenceJar.isFile) { "Missing startupReferenceJar" }
        add(startupCheck.runtimeOnlyConfigurationName, files(startupReferenceJar))
    }
    implementation("com.simibubi.create:create-${property("minecraft_version")}:${property("create_version")}:slim") { isTransitive = false }
    implementation("net.createmod.ponder:Ponder-NeoForge-${property("minecraft_version")}:${property("ponder_version")}")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-${property("minecraft_version")}:${property("flywheel_version")}")
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-${property("minecraft_version")}:${property("flywheel_version")}")
    implementation("com.tterrag.registrate:Registrate:${property("registrate_version")}")

    // Dev QOL
    runtimeOnly("curse.maven:jei-238222:7270455")
}

neoForge {
    version = property("neo_version").toString()

    accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")

    parchment {
        mappingsVersion = property("parchment_mappings_version")!!.toString()
        minecraftVersion = property("parchment_minecraft_version")!!.toString()
    }

    addModdingDependenciesTo(startupCheck)
    val mainMod = mods.create(property("mod_id").toString()) {
        sourceSet(sourceSets["main"])
    }
    val checkMod = mods.create("cmverticaladditions_startup_check") {
        sourceSet(startupCheck)
    }

    runs {
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel.set(org.slf4j.event.Level.DEBUG)
            loadedMods.set(listOf(mainMod))
        }

        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id")!!.toString())
        }

        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", property("mod_id")!!.toString())
        }
        create("startupCheckClient") {
            client()
            sourceSet.set(startupCheck)
            loadedMods.set(if (startupReferenceJar == null) listOf(mainMod, checkMod) else listOf(checkMod))
            gameDirectory.set(startupCheckDirectory)
            programArguments.addAll("--width", "960", "--height", "540")
        }
    }
}

tasks.named("runStartupCheckClient") {
    notCompatibleWithConfigurationCache("Checks an isolated live-client report for each launch")
    doFirst {
        check(!startupCheckDirectory.get().file("startup-check.json").asFile.exists()) {
            "Use a fresh startupCheckName; existing evidence is preserved"
        }
        if (startupPackMods != null) {
            // Use normal mod discovery (including Sodium's bootstrap), not the development classpath.
            val destination = startupCheckDirectory.get().dir("mods").asFile.canonicalFile
            val source = startupPackMods.canonicalFile
            val buildRoot = layout.buildDirectory.get().asFile.canonicalFile.toPath()
            check(destination.toPath().startsWith(buildRoot)) { "Test mods must stay inside build/" }
            check(!source.toPath().startsWith(destination.toPath()) &&
                !destination.toPath().startsWith(source.toPath())) { "Source and destination must not overlap" }
            check(!destination.exists()) { "Use a fresh startupCheckName; existing test mods are preserved" }
            val jars = source.listFiles { entry -> entry.isFile && entry.extension == "jar" &&
                !entry.name.startsWith("cmverticaladditions-") }!!.toList()
            check(jars.isNotEmpty()) { "No external mod JARs found" }
            destination.mkdirs()
            jars.forEach { it.copyTo(destination.resolve(it.name)) }
        }
        val options = startupCheckDirectory.get().file("options.txt").asFile
        options.parentFile.mkdirs()
        if (!options.exists()) options.writeText("onboardAccessibility:false\nfullscreen:false\nenableVsync:false\nmaxFps:60\n")
    }
    doLast {
        val report = startupCheckDirectory.get().file("startup-check.json").asFile
        check(report.isFile && report.readText().contains("\"passed\": true")) {
            "Startup registration check failed or produced no report: $report"
        }
    }
}

tasks.processResources {
    val props = project.providers.gradlePropertiesPrefixedBy("").get()
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

tasks {
    jar {
        archiveBaseName.set("${rootProject.property("mod_id")}-neoforge")
    }
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
        resources {
            srcDir("src/generated/resources")
        }
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

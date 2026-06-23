plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.0"
    id("com.github.gmazzo.buildconfig") version "5.6.7"
}

buildscript {
    repositories.mavenCentral()
    dependencies.classpath("top.mrxiaom:LibrariesResolver-Gradle:1.7.27")
}
val base = top.mrxiaom.gradle.LibraryHelper(project)

group = "top.mrxiaom.sweetdata"
version = "1.0.3"
val targetJavaVersion = 8
val pluginBaseModules = base.modules.run { listOf(library, l10n, misc) }
val shadowGroup = "top.mrxiaom.sweetdata.libs"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly(base.depend.annotations)
    // compileOnly("org.spigotmc:spigot:1.20") // NMS

    compileOnly("me.clip:placeholderapi:2.12.2")

    base.library(LibraryHelper.adventure("4.25.0"))
    base.library(base.depend.HikariCP)
    base.collectPluginHolders()

    implementation("com.github.technicallycoded:FoliaLib:0.4.4") { isTransitive = false }
    for (artifact in pluginBaseModules) {
        implementation(artifact)
    }
    implementation(base.resolver.lite)
}

buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.sweetdata")

    base.doResolveLibraries()
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("java.time.Instant", "BUILD_TIME", "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)")
    buildConfigField("String[]", "RESOLVED_LIBRARIES", base.join())
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withJavadocJar()
    withSourcesJar()
}
tasks {
    shadowJar {
        configurations.add(project.configurations.runtimeClasspath.get())
        mapOf(
            "top.mrxiaom.pluginbase" to "base",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
    }
    val copyTask = register<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "SweetData-$version.jar" }
        into(rootProject.file("out"))
    }
    build {
        dependsOn(copyTask)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
    javadoc {
        (options as StandardJavadocDocletOptions).apply {
            locale("zh_CN")
            encoding("UTF-8")
            docEncoding("UTF-8")
            addBooleanOption("keywords", true)
            addBooleanOption("Xdoclint:none", true)
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()

            artifact(tasks["shadowJar"]).classifier = null
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}

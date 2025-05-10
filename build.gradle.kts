group = "net.highwayfrogs"
version = "1.0.0"
description = "An editor for Frogger: He's Back (1997)"
// To build a Windows exe, install the following things:
// Current Java Version: JDK 17 - https://www.oracle.com/java/technologies/downloads/#jdk17-windows
// Wix 3.xx (NOT 4.xx, since jpackage only supports 3.xx) binaries such as candle.exe should be added to the PATH environment variable.

// Mac & Linux currently require a JDK to be installed to run FrogLord. In that case, modules will need to be configured to be on the classpath when run.
// If anyone wants to submit a PR to streamline the ability to run FrogLord for these systems I'd be happy to take it.

// "--add-opens", "javafx.controls/javafx.scene.control.skin=ALL-UNNAMED" -> Adds reflection capability for modulename/package.
val sharedJvmArgs = listOf("--add-opens", "javafx.controls/javafx.scene.control.skin=ALL-UNNAMED")
val sharedPackageArgs = listOf("--copyright", "Kneesnap (Licensed w/MIT License)", "--description", description, "--vendor", "Highway Frogs")
val targetOs: org.gradle.internal.os.OperatingSystem = org.gradle.internal.os.OperatingSystem.current()
val enableStdOut = false

plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("io.freefair.lombok") version "8.6"
    id("org.beryx.runtime") version "1.13.1"
}

repositories {
    mavenCentral()
}

dependencies {
//    implementation("org.jfxtras:jmetro:11.6.14") // TODO: !

    // https://mvnrepository.com/artifact/org.lwjgl/lwjgl
    // https://docs.gradle.org/current/userguide/dependency_versions.html
    implementation("org.lwjgl:lwjgl-assimp:3.3.4") // Reference: https://lwjglgamedev.gitbooks.io/3d-game-development-with-lwjgl/content/chapter27/chapter27.html
}

java { // https://docs.gradle.org/current/userguide/java_plugin.html
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

javafx {
    version = "22.0.1"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

application { // https://docs.gradle.org/current/userguide/application_plugin.html#application_plugin (Executable JVM Application, easy to start locally and package)
    mainClass = "net.highwayfrogs.editor.FrogLordApplication"
    applicationDefaultJvmArgs = sharedJvmArgs

    tasks.run.get().workingDir = File(tasks.run.get().workingDir, "debug/")
    tasks.run.get().workingDir.mkdirs()
}

sourceSets {
    main {
        java {
            srcDirs("src")
        }
        resources {
            srcDirs("resources")
        }
    }
}

runtime { // https://badass-runtime-plugin.beryx.org/releases/latest/
    options = listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages", "--bind-services")
    launcher {
        noConsole = !enableStdOut
        jvmArgs = sharedJvmArgs + listOf("--module-path", "\$APPDIR", // Ensure modules get resolved at the correct path.
            "--add-modules=javafx.controls,javafx.fxml,javafx.swing", // Add the modules.
            "-splash:\$APPDIR/splash-screen.png" // Add the splash screen.
            )
    }

    // For this to work, we need to follow https://stackoverflow.com/questions/74498307/use-jpackage-with-wix-4
    jpackage {
        installerName = "FrogLord Installer"
        modules.addAll(listOf("jdk.unsupported", "java.scripting"))
        imageOptions = imageOptions + sharedPackageArgs
        installerOptions = installerOptions + sharedPackageArgs

        // Reference: https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html
        val resourceDir = "${projectDir}/resources"
        if (targetOs.isWindows) {
            installerOptions = installerOptions + listOf(
                "--win-dir-chooser", // lets the user specify an installation folder
                "--win-menu", // adds FrogLord to the start menu
                "--win-shortcut", "--win-shortcut-prompt", // creates a shortcut on the desktop.
                "--icon", "${resourceDir}/main-app-icon.ico", // sets the icon for the installer
                "--win-upgrade-uuid", "7c17a7a3-f112-4d52-a3b2-892a102852a8", // NOTE: If the user installs the same version as what's already installed, the installer will exit without warning. (Kill both the installer and msiexec.exe)
                "--about-url", "https://github.com/Kneesnap/FrogLord", // Includes information about FrogLord.
                "--win-help-url", "https://discord.gg/GSNCbCN") // Where to get help (discord)

            skipInstaller = true // We skip the application installer on Windows
            installerType = "exe"
            imageOptions = imageOptions + listOf("--icon", "${resourceDir}/main-app-icon.ico") // Sets the application icon.
            if (enableStdOut)
                imageOptions = imageOptions + listOf("--win-console")
        } else if (targetOs.isLinux) {
            installerType = "deb"
            installerOptions = installerOptions + listOf("--linux-shortcut")
            imageOptions = imageOptions + listOf("--icon", "${resourceDir}/graphics/icon.png")
        }
    }
}

tasks.register<Delete>("deletePackageIcon") { // Deletes the icon file in the release build.
    dependsOn("jpackageImage")
    if (runtime.getJpackageData().get().skipInstaller) // If we're making the installer, keep the icon.
        delete("${layout.buildDirectory.get()}/${runtime.getJpackageData().get().outputDir}/${project.getName()}/${project.getName()}.ico")
}

tasks.register("copySplashScreen") {
    dependsOn("jpackageImage")
    doFirst {
        copy {
            from("${projectDir}/resources/graphics")
            include("splash-screen.png")
            into("${layout.buildDirectory.get()}/${runtime.getJpackageData().get().outputDir}/${project.getName()}/app")
        }
    }
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = application.mainClass
        manifest.attributes["SplashScreen-Image"] = "graphics/splash-screen.png"
        // TODO: Faster run times.
    }

    jpackage {
        dependsOn("deletePackageIcon")
        dependsOn("copySplashScreen")
    }

    // This is unnecessary, and has been skipped.
    // id("edu.sc.seis.launch4j") version "3.0.5"
    /*launch4j { // https://github.com/TheBoegl/gradle-launch4j?tab=readme-ov-file#configurable-input-configuration
        // If we get a failure to launch, run with --l4j-debug-all, which creates launch4j.log in the same folder.
        mainClassName = application.mainClass
        println("Project Path: ".plus(projectDir))
        icon = "${projectDir}/resources/main-app-icon.ico"
        splashFileName = "${projectDir}/resources/graphics/splash-screen.bmp"
        supportUrl = "https://discord.gg/GSNCbCN"
        companyName = "Highway Frogs"
        copyright = "Kneesnap (Licensed w/MIT License)"
        jvmOptions = listOf("--module-path lib", "--add-modules=javafx.controls,javafx.fxml,javafx.swing", " ")
    }*/
}
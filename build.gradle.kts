plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test-junit"))

    intellijPlatform {
        val localIdePath = providers.gradleProperty("localIdePath").orNull
        if (localIdePath == null) {
            intellijIdea("2025.3.6")
        } else {
            local(localIdePath)
        }
        pluginVerifier()
    }
}

intellijPlatform {
    projectName = "MotionFlow"

    pluginConfiguration {
        changeNotes = """
            <h3>MotionFlow 0.1.1</h3>
            <ul>
              <li>Fixed live effects failing to start when IntelliJ invokes project startup on a background thread.</li>
              <li>Startup now marshals effect services to the EDT and expires safely with project disposal.</li>
            </ul>
        """.trimIndent()
        ideaVersion {
            sinceBuild = "253"
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            current()
            latest()
        }
    }
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    from(layout.projectDirectory.file("LICENSE")) {
        into("META-INF")
    }
    from(layout.projectDirectory.file("THIRD_PARTY_NOTICES.md")) {
        into("META-INF")
    }
}

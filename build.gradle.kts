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
            <h3>MotionFlow 0.1.0</h3>
            <ul>
              <li>Bounded caret trail, click ripple, and large-navigation pulse.</li>
              <li>Editor-context, tool-window, and optional inactive-frame cues.</li>
              <li>Four profiles, Reduced Motion, Power Save behavior, and live settings.</li>
              <li>One active-only motion clock, failure isolation, and redacted local diagnostics.</li>
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

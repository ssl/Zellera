plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    id("com.github.spotbugs") version "6.5.13"
    id("org.cyclonedx.bom") version "3.5.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("dev.detekt") version "2.0.0-alpha.6"
}

group = "zel"
version = file("../VERSION").readText().trim()
description = "Zellera server"

System.getenv("ZEL_BUILD_DIR")?.takeIf { it.isNotBlank() }?.let { layout.buildDirectory.set(file(it)) }

val nettyVersion = "4.2.18.Final"
val gsonVersion = "2.14.0"
val mariadbVersion = "3.5.10"
val hikariVersion = "7.1.0"
val dropwizardMetricsVersion = "4.2.40"
val commonsLang3Version = "3.20.0"
val commonsMath3Version = "3.6.1"
val slf4jVersion = "2.0.19"
val errorProneAnnotationsVersion = "2.50.0"
val logbackVersion = "1.6.3"
val flywayVersion = "13.7.0"
val fastutilVersion = "8.5.19"
val spotbugsVersion = "4.10.4"
val javaRelease = 25

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaRelease))
    }
}

kotlin {
    jvmToolchain(javaRelease)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        allWarningsAsErrors.set(true)
    }
}

listOf("compileClasspath", "runtimeClasspath").forEach { name ->
    configurations.named(name) { resolutionStrategy.activateDependencyLocking() }
}

dependencies {
    implementation(platform("io.netty:netty-bom:$nettyVersion"))
    implementation(platform("org.slf4j:slf4j-bom:$slf4jVersion"))
    constraints {
        implementation("com.google.errorprone:error_prone_annotations:$errorProneAnnotationsVersion")
    }

    implementation("io.netty:netty-common")
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-transport")
    implementation("io.netty:netty-codec-base")
    implementation("io.netty:netty-codec-http")
    implementation("io.netty:netty-handler")
    implementation("com.google.code.gson:gson:$gsonVersion")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:$mariadbVersion")
    implementation("it.unimi.dsi:fastutil:$fastutilVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("io.dropwizard.metrics:metrics-healthchecks:$dropwizardMetricsVersion") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    implementation("org.apache.commons:commons-lang3:$commonsLang3Version")
    implementation("org.apache.commons:commons-math3:$commonsMath3Version")
    implementation("org.slf4j:slf4j-api")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-mysql:$flywayVersion")
}

val buildHash: String =
    System.getenv("ZEL_BUILD_HASH")?.trim()?.takeIf { it.isNotEmpty() }
        ?: runCatching {
            providers
                .exec {
                    workingDir = projectDir
                    commandLine("git", "describe", "--always", "--dirty", "--abbrev=12")
                    isIgnoreExitValue = true
                }.standardOutput.asText
                .get()
                .trim()
        }.getOrDefault("")

val generateBuildInfo =
    tasks.register("generateBuildInfo") {
        val outputDir = layout.buildDirectory.dir("generated/resources/buildinfo")
        val projectName = "zellera-server"
        val projectVersion = project.version.toString()
        val projectHash = buildHash
        inputs.property("name", projectName)
        inputs.property("version", projectVersion)
        inputs.property("hash", projectHash)
        outputs.dir(outputDir)
        doLast {
            val file = outputDir.get().file("zel-build.properties").asFile
            file.parentFile.mkdirs()
            file.writeText("name=$projectName\nversion=$projectVersion\nhash=$projectHash\n")
        }
    }
sourceSets.main {
    resources.srcDir(generateBuildInfo)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.jar {
    archiveBaseName.set("zellera-server")
}

tasks.shadowJar {
    archiveBaseName.set("zellera-server")
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
    append("META-INF/io.netty.versions.properties")
    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/LICENSE*",
        "META-INF/license*",
        "META-INF/NOTICE*",
        "META-INF/DEPENDENCIES",
        "META-INF/versions/*/module-info.class",
        "module-info.class",
        "COPYRIGHT.txt",
        "LICENSE.txt",
    )
    manifest {
        attributes(
            "Main-Class" to "zel.Zellera",
            "Implementation-Title" to "zellera-server",
            "Implementation-Version" to project.version.toString(),
            "Java-Version" to javaRelease.toString(),
        )
    }
}
tasks.assemble { dependsOn(tasks.shadowJar) }

tasks.cyclonedxBom {
    schemaVersion = org.cyclonedx.Version.VERSION_16
    jsonOutput.set(layout.buildDirectory.file("libs/zellera-server-${project.version}-sbom.json"))
    xmlOutput.unsetConvention()
}
tasks.cyclonedxDirectBom {
    includeConfigs = listOf("runtimeClasspath")
}
tasks.assemble { dependsOn(tasks.cyclonedxBom) }

spotbugs {
    toolVersion.set(spotbugsVersion)
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.HIGH)
    excludeFilter.set(layout.projectDirectory.file("config/spotbugs-exclude.xml"))
    ignoreFailures.set(false)
}

ktlint {
    version.set("1.8.0")
    additionalEditorconfig.put("ktlint_standard_no-unused-imports", "enabled")
    filter { exclude { it.file.path.contains("/zel/protocol/generated/") } }
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
    exclude { it.file.path.contains("/zel/protocol/generated/") }
}
detekt {
    config.setFrom(layout.projectDirectory.file("config/detekt.yml"))
    buildUponDefaultConfig.set(true)
}
tasks.named("check") { dependsOn("detektMain", "detektTest") }

tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources") {
    val copied = destinationDir
    doFirst { copied.deleteRecursively() }
}

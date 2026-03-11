plugins {
    application
    java
    jacoco
}

group = "dev.springrad"
version = "0.1.0-SNAPSHOT"
val tambouiVersion = "0.2.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent {
            snapshotsOnly()
        }
    }
}

dependencies {
    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")

    implementation("org.apache.maven:maven-model:3.9.11")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.yaml:snakeyaml:2.3")

    // TamboUI dependencies
    implementation(platform("dev.tamboui:tamboui-bom:$tambouiVersion"))
    implementation("dev.tamboui:tamboui-toolkit")
    implementation("dev.tamboui:tamboui-panama-backend")
    annotationProcessor("dev.tamboui:tamboui-processor:$tambouiVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("dev.springrad.Main")
}

val nativeOutputDir = layout.buildDirectory.dir("native/nativeCompile")

tasks.register<Exec>("nativeCompile") {
    group = "build"
    description = "Builds a GraalVM native image for springrad in build/native/nativeCompile."
    dependsOn(tasks.jar)

    val jarTask = tasks.named<Jar>("jar")
    val runtimeClasspath = sourceSets.main.get().runtimeClasspath

    val classpath = files(jarTask.flatMap { it.archiveFile }, runtimeClasspath)
        .asPath
    val outputDir = nativeOutputDir.get().asFile
    val binaryName = "springrad"

    doFirst {
        outputDir.mkdirs()
        commandLine(
            "native-image",
            "--no-fallback",
            "-H:Name=$binaryName",
            "-cp",
            classpath,
            application.mainClass.get()
        )
        workingDir(outputDir)
    }
}

tasks.register<Exec>("nativeTest") {
    group = "verification"
    description = "Runs a smoke test for the native binary by printing --help."
    dependsOn("nativeCompile")

    val binary = nativeOutputDir.map { dir ->
        dir.file("springrad").asFile.absolutePath
    }

    doFirst {
        commandLine(binary.get(), "--help")
    }
}

tasks.processResources {
    filesMatching("version.properties") {
        expand("version" to project.version)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    doLast {
        val htmlIndex = reports.html.outputLocation.get().file("index.html").asFile
        println("Coverage report: ${htmlIndex.absolutePath}")
    }
}

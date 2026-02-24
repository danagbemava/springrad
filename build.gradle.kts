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

    // TamboUI dependencies
    implementation(platform("dev.tamboui:tamboui-bom:$tambouiVersion"))
    implementation("dev.tamboui:tamboui-toolkit")
    implementation("dev.tamboui:tamboui-panama-backend")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.yaml:snakeyaml:2.3")
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

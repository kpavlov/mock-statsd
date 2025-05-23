import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.dokka") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
    signing
    `maven-publish`

    // https://github.com/gradle-nexus/publish-plugin
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "com.github.kpavlov.mocks.statsd"

version = run {
    val ver = findProperty("version")
    if (ver != "unspecified" && ver != null) {
        ver
    } else {
        "0.0.1-SNAPSHOT"
    }
}

val projectName = ext.properties["name"].toString()
val projectDescription = ext.properties["projectDescription"].toString()
val projectRepository = ext.properties["repository"].toString()
val projectInceptionYear = ext.properties["inceptionYear"].toString()

description = projectDescription

repositories {
    mavenCentral()
}

dependencies {
    val awaitilityVersion = "4.3.0"
    val assertjVersion = "3.27.3"
    val slf4jVersion = "2.0.17"
    val junitVersion = "5.12.2"

    api("org.slf4j:slf4j-api:$slf4jVersion")
    api("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    runtimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.awaitility:awaitility-kotlin:$awaitilityVersion")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        )
    }
}

val dokkaHtml by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

val javadocJar: TaskProvider<Jar> by tasks.registering(Jar::class) {
    dependsOn(dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaHtml.outputDirectory)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xjsr305=strict",
            "-Xexplicit-api=strict"
        )
    }
}

kotlin {
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

tasks.assemble {
    dependsOn(javadocJar)
}

publishing {
    // https://docs.gradle.org/current/userguide/publishing_setup.html
    publications.create<MavenPublication>("maven") {
        pom {
            name.value(projectName)
            description.value(projectDescription)
            url.set("https://$projectRepository")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("kpavlov")
                    name.set("Konstantin Pavlov")
                    email.set("mail@KonstantinPavlov.net")
                    url.set("https://kpavlov.me?utm_source=mock-statsd")
                    roles.set(listOf("owner", "developer"))
                }
            }
            scm {
                connection.set("scm:git:git@$projectRepository.git")
                developerConnection.set("scm:git:git@$projectRepository.git")
                url.set(projectRepository)
                tag.set("HEAD")
            }
            inceptionYear.value(projectInceptionYear)
        }
        from(components["java"])
    }

    repositories {
        maven {
            name = "myRepo"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

nexusPublishing {
    repositories {
        // https://blog.solidsoft.pl/2015/09/08/deploy-to-maven-central-using-api-key-aka-auth-token/
        sonatype()
    }
}

signing {
    // https://docs.gradle.org/current/userguide/signing_plugin.html#sec:signatory_credentials
    sign(publishing.publications["maven"])
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

group = "no.nav.syfo"
version = "0.0.1"

val flyway = "10.15.2"
val hikari = "5.1.0"
val postgres = "42.7.3"
val postgresEmbedded = "2.0.7"
val logback = "1.5.6"
val logstashEncoder = "7.4"
val micrometerRegistry = "1.12.8"
val jacksonDatatype = "2.18.0"
val ktor = "3.0.2"
val spek = "2.0.19"
val mockk = "1.13.12"
val nimbusJoseJwt = "9.47"
val kafka = "3.9.0"
val kluent = "1.73"

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.2"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.ktor:ktor-client-apache:$ktor")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-jackson:$ktor")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor")
    implementation("io.ktor:ktor-server-call-id:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-status-pages:$ktor")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoder")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktor")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistry")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:$kafka", excludeLog4j)
    constraints {
        implementation("org.apache.zookeeper:zookeeper") {
            because("org.apache.kafka:kafka_2.13:$kafka -> https://www.cve.org/CVERecord?id=CVE-2023-44981")
            version {
                require("3.9.3")
            }
        }
        implementation("org.bitbucket.b_c:jose4j") {
            because("org.apache.kafka:kafka_2.13:$kafka -> https://github.com/advisories/GHSA-6qvw-249j-h44c")
            version {
                require("0.9.6")
            }
        }
        implementation("org.apache.commons:commons-compress") {
            because("org.apache.commons:commons-compress:1.22 -> https://www.cve.org/CVERecord?id=CVE-2012-2098")
            version {
                require("1.27.1")
            }
        }
    }

    // Database
    implementation("org.postgresql:postgresql:$postgres")
    implementation("com.zaxxer:HikariCP:$hikari")
    implementation("org.flywaydb:flyway-database-postgresql:$flyway")
    testImplementation("io.zonky.test:embedded-postgres:$postgresEmbedded")

    // (De-)serialization
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonDatatype")

    // Tests
    testImplementation("io.ktor:ktor-server-test-host:$ktor")
    testImplementation("io.mockk:mockk:$mockk")
    testImplementation("io.ktor:ktor-client-mock:$ktor")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusJoseJwt")
    testImplementation("org.amshove.kluent:kluent:$kluent")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}

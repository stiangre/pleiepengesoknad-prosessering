import org.gradle.internal.impldep.org.fusesource.jansi.AnsiRenderer.test
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = ext.get("ktorVersion").toString()
val dusseldorfKtorVersion = "1.2.1.65ce328"

val wiremockVersion = "2.19.0"
val openhtmltopdfVersion = "0.0.1-RC20"

val mainClass = "no.nav.helse.PleiepengesoknadProsesseringKt"

plugins {
    kotlin("jvm") version "1.3.31"
}

buildscript {
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/65ce328364975f63e111891794b954b892001c76/gradle/dusseldorf-ktor.gradle.kts")
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.31")
    }
}

dependencies {
    // Server
    compile ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    
    // Client
    compile ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    compile ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")

    // PDF
    compile ( "com.openhtmltopdf:openhtmltopdf-pdfbox:$openhtmltopdfVersion")
    compile ( "com.openhtmltopdf:openhtmltopdf-slf4j:$openhtmltopdfVersion")

    // Kafka
    compile("org.apache.kafka:kafka-clients:2.2.0")
    testCompile("org.testcontainers:kafka:1.11.2")

    // Test
    testCompile ("com.github.tomakehurst:wiremock:$wiremockVersion")
    testCompile("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testCompile ("com.nimbusds:oauth2-oidc-sdk:5.56")
    testCompile("org.skyscreamer:jsonassert:1.5.0")
}

repositories {
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("http://packages.confluent.io/maven/")

    jcenter()
    mavenLocal()
    mavenCentral()
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<Jar>("jar") {
    baseName = "app"

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations["compile"].map {
            it.name
        }.joinToString(separator = " ")
    }

    configurations["compile"].forEach {
        val file = File("$buildDir/libs/${it.name}")
        if (!file.exists())
            it.copyTo(file)
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.4.1"
}

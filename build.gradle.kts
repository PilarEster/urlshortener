import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.3" apply false
    id("io.spring.dependency-management") version "1.0.13.RELEASE" apply false
    kotlin("jvm") version "1.7.10" apply false
    kotlin("plugin.spring") version "1.7.10" apply false
    kotlin("plugin.jpa") version "1.7.10" apply false
    id("io.gitlab.arturbosch.detekt") version("1.21.0") apply true
}

group = "es.unizar"
version = "0.2022.1-SNAPSHOT"

var mockitoVersion = "4.0.0"
var bootstrapVersion = "3.4.0"
var jqueryVersion = "3.6.1"
var guavaVersion = "31.1-jre"
var commonsValidatorVersion = "1.6"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.gitlab.arturbosch.detekt")
    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
    }
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        "implementation"("org.springdoc:springdoc-openapi-data-rest:1.6.0")
        "implementation"("org.springdoc:springdoc-openapi-ui:1.6.0")
        "implementation"("org.springdoc:springdoc-openapi-kotlin:1.6.0")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.21.0")
    }

    detekt {
        autoCorrect = true
    }
}

project(":core") {
    dependencies{
        "implementation"("io.github.microutils:kotlin-logging-jvm:2.0.11")
    }
}

project(":repositories") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":delivery") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-hateoas")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("commons-validator:commons-validator:$commonsValidatorVersion")
        "implementation"("com.google.guava:guava:$guavaVersion")
        "implementation"("io.github.g0dkar:qrcode-kotlin-jvm:3.2.0")
        "implementation"("org.springframework.retry:spring-retry")
        "implementation"("org.springframework:spring-aspects")
        "implementation"("org.springframework.amqp:spring-rabbit:3.0.0")

        "testImplementation" ("org.springframework.amqp:spring-rabbit-test")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":app") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"(project(":delivery"))
        "implementation"(project(":repositories"))
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"( "org.webjars:bootstrap:$bootstrapVersion")
        "implementation"("org.webjars:jquery:$jqueryVersion")
        "implementation"("org.springframework.amqp:spring-rabbit:3.0.0")

        "runtimeOnly"("org.hsqldb:hsqldb")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.springframework.boot:spring-boot-starter-web")
        "testImplementation"("org.springframework.boot:spring-boot-starter-jdbc")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
        "testImplementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.apache.httpcomponents:httpclient")
        "testImplementation" ("org.testcontainers:testcontainers:1.17.6")
        "testImplementation" ("org.testcontainers:rabbitmq:1.17.6")
    }
}

plugins {
    kotlin("jvm") version "2.4.21"
    application
}

group = "io.paly"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.pdfbox:pdfbox:3.0.8")
    implementation("net.lingala.zip4j:zip4j:2.11.6")
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

val fatJar = tasks.register("fatJar", Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "io.paly.esetsalaryslipparser.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
}

tasks {
    jar {
        archiveClassifier.set("plain")
    }
    build {
        dependsOn(fatJar)
    }
}

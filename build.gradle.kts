plugins {
    kotlin("jvm") version "2.2.0"
    application
}

group = "io.paly"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.pdfbox", "pdfbox", "3.0.5")
    implementation("net.lingala.zip4j", "zip4j", "2.11.5")
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
    build {
        dependsOn(fatJar)
    }
}

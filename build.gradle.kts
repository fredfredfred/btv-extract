import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

group = "ah.pdf.extract"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("org.apache.pdfbox:pdfbox:3.0.2")
    implementation("org.bytedeco:tesseract-platform:5.3.4-1.5.10")
    implementation("net.sourceforge.tess4j:tess4j:5.11.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "19"
}

application {
    mainClass.set("ah.PDFProcessor")
    applicationDefaultJvmArgs = listOf("-Djna.library.path=/opt/homebrew/lib", "-Djna.debug_load=true", "-Dtesseract.datapath=/opt/homebrew/share/tessdata", "-Dtesseract.language=deu", "-Dtesseract.ocrEngineMode=1")
}


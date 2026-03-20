val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val zxing_version: String by project
val javacv_version: String by project
val opencv_version: String by project

plugins {
    kotlin("jvm") version "1.9.25"
    id("io.ktor.plugin") version "2.3.12"
    kotlin("plugin.serialization") version "1.9.25"
}

group = "com.qrscanner"
version = "1.0.0"

application {
    mainClass.set("com.qrscanner.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")

    // ZXing - Barcode/QR decoding
    implementation("com.google.zxing:core:$zxing_version")
    implementation("com.google.zxing:javase:$zxing_version")

    // OpenCV via ByteDeco JavaCV
    implementation("org.bytedeco:javacv:$javacv_version")
    implementation("org.bytedeco:opencv-platform:$opencv_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-websockets-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

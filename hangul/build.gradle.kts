import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
}

// 안드로이드 앱이 그대로 가져다 쓰므로 앱과 같은 Java 17 바이트코드로 만든다.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // 테스트 목록은 저장소 맨 위 testcases/에 둔다. 나중에 iOS 테스트도 같은 파일을 읽는다.
    val testcases = rootProject.layout.projectDirectory.dir("testcases")
    inputs.dir(testcases).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("testcases.dir", testcases.asFile.absolutePath)
    // CI 로그만 보고도 어느 줄이 틀렸는지 알 수 있게 실패 메시지를 다 찍는다.
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

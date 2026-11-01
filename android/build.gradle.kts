plugins {
    id("com.android.application")
}

// CI에서는 실행 번호와 커밋 해시로 버전을 매긴다. 폰에 어느 빌드가 깔렸는지 설정 화면에서 보기 위해서다.
val runNumber = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toInt() ?: 1
val commit = providers.environmentVariable("GITHUB_SHA").orNull?.take(7) ?: "local"

// 서명 키는 CI가 Secrets에서 꺼내 임시 파일로 만든다. 없으면 서명하지 않은 APK가 나온다.
val keystoreFile = providers.environmentVariable("SIGNING_KEYSTORE_FILE").orNull

android {
    namespace = "io.github.bedcoding.hangulime"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.bedcoding.hangulime"
        // 사용자 폰의 안드로이드 버전을 확인하기 전까지 쓰는 임시 값
        minSdk = 26
        targetSdk = 37
        versionCode = runNumber
        versionName = commit
    }

    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = providers.environmentVariable("SIGNING_STORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("SIGNING_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("SIGNING_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            // PC에서 만드는 개발판. 다른 앱으로 설치되어 매일 쓰는 키보드와 섞이지 않는다.
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

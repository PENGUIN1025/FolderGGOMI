plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.aggro1025.jeopda"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aggro1025.jeopda"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"

        // AdMob 기본값: 구글 공식 테스트 ID ("Test Ad" 표시, 수익 없음). 디버그 빌드는 항상 이 값
        manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "REWARDED_AD_UNIT", "\"ca-app-pub-3940256099942544/5224354917\"")
    }

    buildTypes {
        release {
            // 실제 AdMob 앱 ID (접다 - 폴더폰 모드)
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5192264798199858~4163105584"
            // 실제 보상형 광고 단위 (해제 보상형)
            buildConfigField("String", "REWARDED_AD_UNIT", "\"ca-app-pub-5192264798199858/8186111076\"")
            // 앱 크기 줄이기 (쓰지 않는 코드·리소스 제거)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    // 광고 SDK가 끌고 오는 오래된 Fragment를 권한 요청 API와 맞는 버전으로
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // 보상형 광고 (사용자 승인 2026-11-27): Google Mobile Ads SDK, 유럽·영국 개인정보 동의(UMP)
    implementation("com.google.android.gms:play-services-ads:25.5.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    // 500원 결제 (사용자 승인 2026-11-27): Google Play Billing
    implementation("com.android.billingclient:billing-ktx:8.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // 화면 스냅샷 (./gradlew testDebugUnitTest → app/build/screenshots/)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

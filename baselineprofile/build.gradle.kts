plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.flxrs.dankchat.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Benchmarks run on the managed device, absolute numbers are not representative
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
        buildConfigField("String", "TARGET_APP_ID", "\"io.github.c1trus01.lemchat\"")
    }

    buildFeatures {
        buildConfig = true
    }

    targetProjectPath = ":app"

    testOptions {
        managedDevices {
            localDevices {
                create("pixel10Api36") {
                    device = "Pixel 10"
                    apiLevel = 36
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel10Api36"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
}

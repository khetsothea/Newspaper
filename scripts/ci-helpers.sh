#!/bin/bash

function generateTestReports() {
    ./gradlew -i testDebugUnitTest -PfabricApiSecret=$FABRIC_API_SECRET -PfabricApiKey=$FABRIC_API_KEY jacocoTestReport uploadCoverageToCodacy --console=plain
    cp -r app/build/test-results/testDebugUnitTest/*.xml $CIRCLE_TEST_REPORTS && cp -r app/build/reports/tests/testDebugUnitTest $CIRCLE_TEST_REPORTS && cp -r app/build/reports/jacoco/jacocoTestReport $CIRCLE_TEST_REPORTS
}

function inspectCode() {
    ./gradlew -i testDebugUnitTest -PfabricApiSecret=$FABRIC_API_SECRET -PfabricApiKey=$FABRIC_API_KEY customFindBugs --console=plain
    cp -r app/build/outputs/findbugs $CIRCLE_TEST_REPORTS
}

function runInstrumentedTests() {
    # Run Android emulator
    nohup bash -c "emulator -avd phone-$1 -skin WVGA800 -no-boot-anim -no-window &"
    circle-android wait-for-boot

    if test $1 -ge 23
        then
            # Wait 4 minutes because circle-android is not reliable
            sleep 240
        else
            # Wait 2 minutes because circle-android is not reliable
            sleep 120
    fi

    unlockAndroidEmulator $1
    setUpAndroidEmulator $1

    # Run instrumented tests
    ./gradlew -i connectedDebugAndroidTest -PfabricApiSecret=$FABRIC_API_SECRET -PfabricApiKey=$FABRIC_API_KEY --console=plain

    # Stop the emulator
    adb emu kill

    mkdir -p $CIRCLE_TEST_REPORTS/phone-$1 && cp -r app/build/reports/androidTests/connected/* $CIRCLE_TEST_REPORTS/phone-$1
}

function buildRelease() {
    ./gradlew -PfabricApiSecret=$FABRIC_API_SECRET -PfabricApiKey=$FABRIC_API_KEY assembleRelease crashlyticsUploadDistributionRelease --console=plain && cp -r app/build/outputs/apk/app-release.apk $CIRCLE_ARTIFACTS && cp -r app/build/outputs/mapping/release/mapping.txt $CIRCLE_ARTIFACTS
}

function downloadAndroidSdk() {
    if [ ! -e $ANDROID_HOME/platforms/android-$1 ]; then echo y | android update sdk --no-ui --all --filter android-$1; fi

    if test $1 -eq 23
        then
            if [ ! -e $ANDROID_HOME/system-images/android-$1/google_apis ]; then echo y | android update sdk --no-ui --all --filter sys-img-armeabi-v7a-google_apis-$1; fi
        else
            if [ ! -e $ANDROID_HOME/system-images/android-$1/default ]; then echo y | android update sdk --no-ui --all --filter sys-img-armeabi-v7a-android-$1; fi
    fi
}

function createAndroidEmulator() {
    local id=$(android list targets | grep android-$1 | cut -d ' ' -f 2)

    if test $1 -eq 23
        then
            echo no | android create avd -n phone-$1 -t $id -b armeabi-v7a -g google_apis -c 128M
        else
            echo no | android create avd -n phone-$1 -t $id -b armeabi-v7a -c 128M
    fi

    echo "Original config"
    cat ~/.android/avd/phone-$1.avd/config.ini

    cp -rf avd-configs/config$1.ini ~/.android/avd/phone-$1.avd/config.ini

    echo "Updated config"
    cat ~/.android/avd/phone-$1.avd/config.ini
}

function unlockAndroidEmulator() {
    # Unlock device
    adb shell input keyevent 82

    if test $1 -ge 24
        then
            # Tap "Wait" to dismiss "Process System is not responding" dialog
            adb shell input tap 175 515
        else
            # Tap "Wait" to dismiss "Process System is not responding" dialog
            adb shell input tap 300 475
    fi

    if test $1 -ge 23
        then
            # Tap "OK" to dismiss welcome screen
            adb shell input tap 400 355
        else
            # Tap "OK" to dismiss welcome screen
            adb shell input tap 400 750
    fi
}

function setUpAndroidEmulator() {
    if test $1 -ge 17
        then
            adb shell settings put global animator_duration_scale 0
            adb shell settings put global transition_animation_scale 0
            adb shell settings put global window_animation_scale 0
    fi
}

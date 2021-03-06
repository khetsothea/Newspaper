package com.github.ayltai.newspaper.util;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TestUtils {
    private static AtomicBoolean isRunningUnitTest;
    private static AtomicBoolean isRunningInstrumentedTest;

    private TestUtils() {
    }

    public static synchronized boolean isRunningUnitTest() {
        if (TestUtils.isRunningUnitTest == null) {
            boolean isRunningUnitTest = false;

            try {
                Class.forName("org.robolectric.RobolectricTestRunner");
                isRunningUnitTest = true;
            } catch (final ClassNotFoundException e) {
                // Ignored
            }

            TestUtils.isRunningUnitTest = new AtomicBoolean(isRunningUnitTest);
        }

        return TestUtils.isRunningUnitTest.get();
    }

    public static synchronized boolean isRunningInstrumentedTest() {
        if (TestUtils.isRunningInstrumentedTest == null) {
            boolean isRunningInstrumentedTest = false;

            try {
                Class.forName("android.support.test.espresso.Espresso");
                isRunningInstrumentedTest = true;
            } catch (final ClassNotFoundException e) {
                // Ignored
            }

            TestUtils.isRunningInstrumentedTest = new AtomicBoolean(isRunningInstrumentedTest);
        }

        return TestUtils.isRunningInstrumentedTest.get();
    }
}

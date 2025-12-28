package com.ayushcodes.weatherapp // Defines the package for instrumented tests

import androidx.test.platform.app.InstrumentationRegistry // Imports InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4 // Imports AndroidJUnit4 runner

import org.junit.Test // Imports Test annotation
import org.junit.runner.RunWith // Imports RunWith annotation

import org.junit.Assert.* // Imports assertions

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class) // Specifies the test runner
// Example instrumented test class
class ExampleInstrumentedTest {
    // Test method to verify app context
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext // Get target context
        assertEquals("com.ayushcodes.weatherapp", appContext.packageName) // Assert package name
    }
}
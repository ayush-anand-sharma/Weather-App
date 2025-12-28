package com.ayushcodes.weatherapp // Defines the package name for this file

import android.annotation.SuppressLint // Imports SuppressLint annotation
import android.content.Intent // Imports Intent class for starting activities
import android.os.Bundle // Imports Bundle class for passing data between activities
import android.os.Handler // Imports Handler class for scheduling tasks
import android.os.Looper // Imports Looper class for managing thread message loops
import androidx.activity.enableEdgeToEdge // Imports enableEdgeToEdge for full-screen display
import androidx.appcompat.app.AppCompatActivity // Imports AppCompatActivity as the base class for activities

@SuppressLint("CustomSplashScreen") // Suppresses lint warning for custom splash screen
// Activity class for the splash screen displayed on app startup
class SplashScreen : AppCompatActivity() {
    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Call the superclass implementation
        enableEdgeToEdge() // Enable edge-to-edge display for immersive experience
        setContentView(R.layout.activity_splash_screen) // Set the content view to the splash screen layout
        
        // Use a Handler to delay the transition to the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java) // Create an intent to start MainActivity
            startActivity(intent) // Start the MainActivity
            finish() // Finish the SplashScreen activity so the user cannot go back to it
        },3000) // Delay execution for 3000 milliseconds (3 seconds)
    }
}
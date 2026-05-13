package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var database: FirebaseDatabase
    private lateinit var rootRef: DatabaseReference
    private var isPageLoaded = false

    // Cache latest values to update WebView once it's ready
    private var lastWaterValue = "0"
    private var lastDepth = "0"
    private var lastMode = "Auto-Adaptive"
    private var lastFishCount = "0"
    private var lastMovementStatus = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)

        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true

        // Initialize Firebase
        database = FirebaseDatabase.getInstance("https://aqualure-pro-default-rtdb.asia-southeast1.firebasedatabase.app")
        rootRef = database.getReference("AquaLurePro/device1")

        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                updateWebView()
            }
        }

        webView.loadUrl("file:///android_asset/index.html")

        // Real-time listener for sensor data
        val sensorRef = rootRef.child("sensorData")
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lastWaterValue = snapshot.child("waterValue").value?.toString() ?: "0"
                lastDepth = (snapshot.child("distance").value ?: snapshot.child("ultrasonicValue").value)?.toString() ?: "0"
                lastMode = snapshot.child("mode").value?.toString() ?: "Auto-Adaptive"
                lastFishCount = snapshot.child("fishCount").value?.toString() ?: "0"
                lastMovementStatus = (snapshot.child("fishDetected").value ?: snapshot.child("movementStatus").value)?.toString() ?: "Unknown"

                Log.d("FirebaseData", "Water: $lastWaterValue, Depth/Distance: $lastDepth, Mode: $lastMode, Fish: $lastFishCount, Status: $lastMovementStatus")

                if (isPageLoaded) {
                    updateWebView()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseData", "Error: ${error.message}")
            }
        })
    }

    private fun updateWebView() {
        webView.post {
            webView.evaluateJavascript(
                "updateFromFirebase('$lastWaterValue', '$lastDepth', '$lastMode', '$lastFishCount', '$lastMovementStatus')",
                null
            )
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun setMode(mode: String) {
            rootRef.child("controls/mode").setValue(mode)
        }

        @JavascriptInterface
        fun startAttraction() {
            rootRef.child("control/attraction").setValue(true)
        }

        @JavascriptInterface
        fun stopSystem() {
            rootRef.child("control/attraction").setValue(false)
        }

        @JavascriptInterface
        fun setLedColor(color: String) {
            rootRef.child("controls/ledColor").setValue(color)
        }

        @JavascriptInterface
        fun setVibration(value: Int) {
            rootRef.child("controls/vibrationIntensity").setValue(value)
        }

        @JavascriptInterface
        fun setLightIntensity(value: Int) {
            rootRef.child("controls/lightIntensity").setValue(value)
        }

        @JavascriptInterface
        fun setFrequency(value: Int) {
            rootRef.child("controls/frequency").setValue(value)
        }

        @JavascriptInterface
        fun setPulseInterval(value: String) {
            rootRef.child("controls/pulseInterval").setValue(value)
        }

        @JavascriptInterface
        fun startCapture() {
            rootRef.child("controls/capture").setValue(true)
        }

        @JavascriptInterface
        fun emergencyStop() {
            rootRef.child("controls/emergencyStop").setValue(true)
            rootRef.child("control/attraction").setValue(false)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
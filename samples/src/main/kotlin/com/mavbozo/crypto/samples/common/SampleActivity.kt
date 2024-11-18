package com.mavbozo.crypto.samples.common

import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

abstract class SampleActivity : AppCompatActivity() {
    protected val outputText: TextView by lazy {
        TextView(this).apply {
            setPadding(16, 16, 16, 16)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        scrollView.addView(outputText)
        setContentView(scrollView)

        lifecycleScope.launch {
            try {
                runSamples()
            } catch (e: Exception) {
                outputText.append("\nError: ${e.message}")
            }
        }
    }

    abstract suspend fun runSamples()

    protected fun appendLine(text: String) {
        outputText.append("$text\n")
    }
}
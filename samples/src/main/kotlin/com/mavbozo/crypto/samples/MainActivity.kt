package com.mavbozo.crypto.samples

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.mavbozo.crypto.samples.random.RandomActivity
import com.mavbozo.crypto.samples.cipher.CipherActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        Button(this).apply {
            text = "Random Number Generation Samples"
            setOnClickListener {
                startActivity(Intent(context, RandomActivity::class.java))
            }
            layout.addView(this)
        }

        Button(this).apply {
            text = "Cipher Samples"
            setOnClickListener {
                startActivity(Intent(context, CipherActivity::class.java))
            }
            layout.addView(this)
        }

        setContentView(layout)
    }
}
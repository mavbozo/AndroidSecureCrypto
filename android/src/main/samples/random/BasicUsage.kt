@file:JvmName("BasicUsage")
package com.mavbozo.androidsecurecrypto.samples.random

import com.mavbozo.androidsecurecrypto.Random
import com.mavbozo.androidsecurecrypto.Base64Flags
import kotlinx.coroutines.runBlocking

fun basicRandomExample() = runBlocking {
    Random.generateBytes(32).fold(
        onSuccess = { bytes ->
            try {
                println("Generated ${bytes.size} random bytes") 
                bytes.forEach { byte -> print("%02x".format(byte)) }
                println()
            } finally {
                bytes.fill(0)
            }
        },
        onFailure = { error -> 
            println("Generation failed: $error")
        }
    )
}

fun formatExample() = runBlocking {
    Random.generateBytesAsHex(16).fold(
        onSuccess = { hexString ->
            println("Hex: $hexString")
        },
        onFailure = { error ->
            println("Hex generation failed: $error") 
        }
    )
    
    Random.generateBytesAsBase64(
        size = 16,
        flags = Base64Flags.UrlSafe
    ).fold(
        onSuccess = { base64String ->
            println("Base64: $base64String")
        },
        onFailure = { error ->
            println("Base64 generation failed: $error")
        }
    )
}

fun main() {
    println("Running basic random example:")
    basicRandomExample()
    
    println("\nRunning format example:")
    formatExample()
}

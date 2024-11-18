package com.mavbozo.crypto.samples.random

import com.mavbozo.crypto.samples.common.SampleActivity

class RandomActivity : SampleActivity() {
    override suspend fun runSamples() {
        appendLine("Basic Random Bytes:")
        RandomSamples.basicRandomBytes().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nRandom Hex String:")
        RandomSamples.randomHexString().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nBase64 Examples:")
        RandomSamples.base64Examples().forEach { (name, result) ->
            appendLine("$name:")
            result.fold(
                onSuccess = { appendLine(it) },
                onFailure = { appendLine("Error: ${it.message}") }
            )
        }

        appendLine("\nEnhanced Random:")
        RandomSamples.enhancedRandom(this).fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )
    }
}
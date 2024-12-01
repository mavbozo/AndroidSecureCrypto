// KeyDerivationActivity.kt
package com.mavbozo.crypto.samples.keyderivation

import com.mavbozo.crypto.samples.common.SampleActivity
import com.mavbozo.androidsecurecrypto.Random

class KeyDerivationActivity : SampleActivity() {
    override suspend fun runSamples() {
        appendLine("Basic Key Derivation:")
        KeyDerivationSamples.basicKeyDerivation().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nDifferent Hash Algorithms:")
        KeyDerivationSamples.multipleAlgorithms().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nDifferent Key Sizes:")
        KeyDerivationSamples.differentKeySizes().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )

        appendLine("\nDomain Separation:")
        KeyDerivationSamples.domainSeparation().fold(
            onSuccess = { appendLine(it) },
            onFailure = { appendLine("Error: ${it.message}") }
        )
    }
}

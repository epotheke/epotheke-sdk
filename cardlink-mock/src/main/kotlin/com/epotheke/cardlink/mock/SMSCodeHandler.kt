/****************************************************************************
 * Copyright (C) 2024 ecsec GmbH.
 * All rights reserved.
 * Contact: ecsec GmbH (info@ecsec.de)
 *
 * This file is part of the epotheke SDK.
 *
 * GNU General Public License Usage
 * This file may be used under the terms of the GNU General Public
 * License version 3.0 as published by the Free Software Foundation
 * and appearing in the file LICENSE.GPL included in the packaging of
 * this file. Please review the following information to ensure the
 * GNU General Public License version 3.0 requirements will be met:
 * http://www.gnu.org/copyleft/gpl.html.
 *
 * Other Usage
 * Alternatively, this file may be used in accordance with the terms
 * and conditions contained in a signed written agreement between
 * you and ecsec GmbH.
 *
 ***************************************************************************/

package com.epotheke.cardlink.mock

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.security.SecureRandom
import java.util.Collections


private val logger = KotlinLogging.logger {}

@ApplicationScoped
class SMSCodeHandler {

    @ConfigProperty(name = "cardlink.mock.accept-all-tans")
    var acceptAllTans: Boolean = true

    private var smsCodes = mutableMapOf<String, String>()
    private var smsCodeRemainingTries = mutableMapOf<String, Int>()

    fun createSMSCode(webSocketId: String) : String {
        val smsCode = generateRandomString(ALPHABET_NUM, 8)
        smsCodes[webSocketId] = smsCode
        smsCodeRemainingTries[webSocketId] = DEFAULT_MAX_TRIES
        return smsCode
    }

    @Throws(MaxTriesReached::class)
    fun checkSMSCode(webSocketId: String, smsCode: String) : Boolean {
        if (acceptAllTans) {
            logger.debug { "Accepting all TANs because the 'cardlink.mock.accept-all-tans' property is set to: $acceptAllTans." }
            return true
        }

        val hasCodeForWebSocket = smsCodes.containsKey(webSocketId)

        if (! hasCodeForWebSocket) {
            return false
        }

        val webSocketTries = smsCodeRemainingTries[webSocketId]
            ?: throw IllegalStateException("Unable to determine counter for SMS-Code tries.")

        if (webSocketTries == 0) {
            throw MaxTriesReached("Max tries of $DEFAULT_MAX_TRIES reached.")
        } else {
            smsCodeRemainingTries[webSocketId] = webSocketTries - 1
        }

        val smsCodeMatch = smsCodes[webSocketId] == smsCode
        return smsCodeMatch
    }

    /**
     * Generates a random string containing only values from the given alphabet.
     *
     * @param alphabet The alphabet from which the individual characters are taken.
     * @param size Size of the target string.
     * @return The random string.
     * @throws IllegalArgumentException Thrown in case the alphabet or size contain invalid values.
     * @throws NullPointerException Thrown in case the alphabet is null.
     */
    private fun generateRandomString(alphabet: List<Char?>?, size: Int): String {
        require(size > 0) { "The parameter size is less than or equal to 0." }
        require(! alphabet.isNullOrEmpty()) { "The given alphabet is empty." }

        val rand = SecureRandom()
        val scaling = (alphabet.size - 1) / 255f
        val baseBytes = ByteArray(size)
        val out = StringBuilder(size)

        rand.nextBytes(baseBytes)
        for (i in baseBytes.indices) {
            var ord = baseBytes[i].toInt() and 0xFF // normalize number to positive value range
            ord = Math.round(scaling * ord)
            out.append(alphabet[ord])
        }

        return out.toString()
    }

    companion object {
        private const val DEFAULT_MAX_TRIES = 3

        @JvmField
        var ALPHABET_NUM: List<Char?>? = null

        init {
            val alphaNum = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            ALPHABET_NUM = Collections.unmodifiableList(listOf(*alphaNum))
        }
    }
}

class MaxTriesReached : Exception {
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}

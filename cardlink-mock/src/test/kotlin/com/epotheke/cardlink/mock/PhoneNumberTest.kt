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

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


@QuarkusTest
class PhoneNumberTest {

    @Inject
    lateinit var smsSender: SpryngsmsSender

    @ParameterizedTest
    @MethodSource("germanPhoneNumberProvider")
    fun testGermanNumbers(phoneNumber: String) {
        val isGermanNumber = smsSender.isGermanPhoneNumber(phoneNumber)
        Assertions.assertTrue(isGermanNumber)
    }

    @ParameterizedTest
    @MethodSource("nonGermanPhoneNumberProvider")
    fun testNonGermanNumbers(phoneNumber: String) {
        val isGermanNumber = smsSender.isGermanPhoneNumber(phoneNumber)
        Assertions.assertFalse(isGermanNumber)
    }

    companion object {
        @JvmStatic
        fun germanPhoneNumberProvider(): Stream<String> {
            return Stream.of(
                "+49 15172612345",
                "015172612345",
                "01736322621",
            )
        }
        @JvmStatic
        fun nonGermanPhoneNumberProvider(): Stream<String> {
            return Stream.of(
                "+41 44 668 18 00",
                "+40 12 638 68 00"
            )
        }
    }
}

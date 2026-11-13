package com.epotheke

import IosTerminals

fun Epotheke.setNFCDialogMessage(message: String) =
    (cardLinkAuthenticationProtocol.sal.terminals as IosTerminals)
        .iosTerminal
        .setAlertMessage(message)

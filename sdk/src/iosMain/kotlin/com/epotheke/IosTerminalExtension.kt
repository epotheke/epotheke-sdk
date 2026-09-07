package com.epotheke

import IosTerminals

fun Epotheke.setNFCDialogMessage(message: String) =
    (cardLinkAuthenticationProtocol.salSession.sal.terminals as IosTerminals)
        .iosTerminal
        .setAlertMessage(message)

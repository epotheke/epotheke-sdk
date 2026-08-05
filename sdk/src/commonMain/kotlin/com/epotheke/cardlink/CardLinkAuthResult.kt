package com.epotheke.cardlink

import com.epotheke.cardlink.data.AllgemeineVersicherungsdaten
import com.epotheke.cardlink.data.PersoenlicheVersichertendaten

class CardLinkAuthResult(
    var personalData: PersoenlicheVersichertendaten? = null,
    var insurerData: AllgemeineVersicherungsdaten? = null,
    var iccsn: String? = null,
    var iccsnReassignmentTimestamp: String? = null,
    var wsSessionId: String? = null,
)

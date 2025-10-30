package com.epotheke.cardlink

class CardLinkAuthResult(
    var personalData: PersoenlicheVersichertendaten? = null,
    var insurerData: AllgemeineVersicherungsdaten? = null,
    var iccsn: String? = null,
    var iccsnReassignmentTimestamp: String? = null,
    var wsSessionId: String? = null,
)

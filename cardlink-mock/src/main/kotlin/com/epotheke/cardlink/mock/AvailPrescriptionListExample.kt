package com.epotheke.cardlink.mock

val availablePrescriptionListsJsonExample = """
    {
      "messageId": "%s",
      "correlationId": "%s",
      "type": "availablePrescriptionLists",
      "availablePrescriptionLists": [
        {
          "type": "availablePrescriptionList",
          "ICCSN": "MTIzNDU2Nzg5",
          "prescriptionBundleList": [
            {
              "prescriptionId": "160.000.764.737.300.50",
              "erstellungszeitpunkt": "2024-06-30T09:30:00.000Z",
              "status": "final",
              "krankenversicherung": {
                "kostentraegertyp": "GKV",
                "ikKrankenkasse": "104212059",
                "kostentraeger": "AOK Baden-Württemberg",
                "wop": "52",
                "versichertenstatus": "1",
                "besonderePersonengruppe": "0",
                "dmpKz": "0"
              },
              "patient": {
                "gkvVersichertenId": "X234567891",
                "person": {
                  "vorname": "Tina",
                  "name": "Tester"
                },
                "geburtsdatum": "1985-05-05T00:00:00.000Z",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "42"
                }
              },
              "arzt": {
                "typ": "0",
                "berufsbezeichnung": "Hausarzt",
                "arztnummer": "838382202",
                "person": {
                  "vorname": "Hans",
                  "nachname": "Gesundmacher",
                  "titel": "Dr. med."
                }
              },
              "pruefnummer": "Y/400/1910/36/346",
              "organisation": {
                "bsnr": "31234567",
                "name": "Hausarztpraxis Dr. Gesundmacher",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "200"
                },
                "telefon": "+49 711 1234567",
                "fax": "+49 711 1234599",
                "eMail": "hans@praxis-gesundmacher.de"
              },
              "verordnung": {
                "type": "prescription",
                "ausstellungsdatum": "2024-06-30T00:00:00.000Z",
                "noctu": false,
                "bvg": false,
                "zuzahlungsstatus": "0",
                "autidem": true,
                "abgabehinweis": "Vor Zugriff durch Kinder schützen.",
                "anzahl": 1,
                "dosierung": true,
                "dosieranweisung": "1-0-1"
              },
              "arzneimittel": {
                "medicationItem": [
                  {
                      "type": "medicationPZN",
                      "kategorie": "0",
                      "impfstoff": false,
                      "normgroesse": "N3",
                      "pzn": "38706",
                      "handelsname": "Amoxicillin Al 1000",
                      "darreichungsform": "TAB",
                      "packungsgroesseNachMenge": "30",
                      "einheit": "TAB"
                  }
                ]
              }
            },
            {
              "prescriptionId": "160.100.000.000.012.06",
              "erstellungszeitpunkt": "2024-06-30T09:35:00.000Z",
              "status": "final",
              "krankenversicherung": {
                "kostentraegertyp": "GKV",
                "ikKrankenkasse": "104212059",
                "kostentraeger": "AOK Baden-Württemberg",
                "wop": "52",
                "versichertenstatus": "1",
                "besonderePersonengruppe": "0",
                "dmpKz": "0"
              },
              "patient": {
                "gkvVersichertenId": "X234567891",
                "person": {
                  "vorname": "Tina",
                  "name": "Tester"
                },
                "geburtsdatum": "1985-05-05T00:00:00.000Z",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "42"
                }
              },
              "arzt": {
                "typ": "0",
                "berufsbezeichnung": "Hausarzt",
                "arztnummer": "838382202",
                "person": {
                  "vorname": "Hans",
                  "nachname": "Gesundmacher",
                  "titel": "Dr. med."
                }
              },
              "pruefnummer": "Y/400/1910/36/346",
              "organisation": {
                "bsnr": "31234567",
                "name": "Hausarztpraxis Dr. Gesundmacher",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "200"
                },
                "telefon": "+49 711 1234567",
                "fax": "+49 711 1234599",
                "eMail": "hans@praxis-gesundmacher.de"
              },
              "verordnung": {
                "type": "prescription",
                "ausstellungsdatum": "2024-06-30T00:00:00.000Z",
                "noctu": false,
                "bvg": false,
                "zuzahlungsstatus": "0",
                "autidem": true,
                "abgabehinweis": "Vor Zugriff durch Kinder schützen.",
                "anzahl": 1,
                "dosierung": true,
                "dosieranweisung": "1-0-1"
              },
              "arzneimittel": {
                "medicationItem": [
                    {
                        "type": "medicationPZN",
                        "kategorie": "0",
                        "impfstoff": false,
                        "normgroesse": "N3",
                        "pzn": "232199",
                        "handelsname": "Simva Basics 10mg",
                        "darreichungsform": "TAB",
                        "packungsgroesseNachMenge": "100",
                        "einheit": "TAB"
                    }
                ]
              }
            },
            {
              "prescriptionId": "160.100.000.000.004.30",
              "erstellungszeitpunkt": "2024-06-30T09:40:00.000Z",
              "status": "final",
              "krankenversicherung": {
                "kostentraegertyp": "GKV",
                "ikKrankenkasse": "104212059",
                "kostentraeger": "AOK Baden-Württemberg",
                "wop": "52",
                "versichertenstatus": "1",
                "besonderePersonengruppe": "0",
                "dmpKz": "0"
              },
              "patient": {
                "gkvVersichertenId": "X234567891",
                "person": {
                  "vorname": "Tina",
                  "name": "Tester"
                },
                "geburtsdatum": "1985-05-05T00:00:00.000Z",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "42"
                }
              },
              "arzt": {
                "typ": "0",
                "berufsbezeichnung": "Hausarzt",
                "arztnummer": "838382202",
                "person": {
                  "vorname": "Hans",
                  "nachname": "Gesundmacher",
                  "titel": "Dr. med."
                }
              },
              "pruefnummer": "Y/400/1910/36/346",
              "organisation": {
                "bsnr": "31234567",
                "name": "Hausarztpraxis Dr. Gesundmacher",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "200"
                },
                "telefon": "+49 711 1234567",
                "fax": "+49 711 1234599",
                "eMail": "hans@praxis-gesundmacher.de"
              },
              "verordnung": {
                "type": "prescription",
                "ausstellungsdatum": "2024-06-30T00:00:00.000Z",
                "noctu": false,
                "bvg": false,
                "zuzahlungsstatus": "0",
                "autidem": true,
                "abgabehinweis": "Vor Zugriff durch Kinder schützen.",
                "anzahl": 1,
                "dosierung": true,
                "dosieranweisung": "1-0-0"
              },
              "arzneimittel": {
                "medicationItem": [
                    {
                        "type": "medicationIngredient",
                        "kategorie": "0",
                        "impfstoff": false,
                        "normgroesse": "N3",
                        "darreichungsform": "Tabletten",
                        "packungsgroesseNachMenge": "100",
                        "einheit": "Stück",
                        "listeBestandteilWirkstoffverordnung": [
                          {
                            "type": "bestandteilWirkstoffverordnung",
                            "wirkstoffnummer": "22686",
                            "wirkstoffname": "Ramipril",
                            "wirkstaerke": "5",
                            "wirkstaerkeneinheit": "mg"
                          }
                        ]                
                    }
                ]
              }
            },
            {
              "prescriptionId": "160.100.000.000.014.97",
              "erstellungszeitpunkt": "2024-06-30T09:45:00.000Z",
              "status": "final",
              "krankenversicherung": {
                "kostentraegertyp": "GKV",
                "ikKrankenkasse": "104212059",
                "kostentraeger": "AOK Baden-Württemberg",
                "wop": "52",
                "versichertenstatus": "1",
                "besonderePersonengruppe": "0",
                "dmpKz": "0"
              },
              "patient": {
                "gkvVersichertenId": "X234567891",
                "person": {
                  "vorname": "Tina",
                  "name": "Tester"
                },
                "geburtsdatum": "1985-05-05T00:00:00.000Z",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "42"
                }
              },
              "arzt": {
                "typ": "0",
                "berufsbezeichnung": "Hausarzt",
                "arztnummer": "838382202",
                "person": {
                  "vorname": "Hans",
                  "nachname": "Gesundmacher",
                  "titel": "Dr. med."
                }
              },
              "pruefnummer": "Y/400/1910/36/346",
              "organisation": {
                "bsnr": "31234567",
                "name": "Hausarztpraxis Dr. Gesundmacher",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "200"
                },
                "telefon": "+49 711 1234567",
                "fax": "+49 711 1234599",
                "eMail": "hans@praxis-gesundmacher.de"
              },
              "verordnung": {
                "type": "prescription",
                "ausstellungsdatum": "2024-06-30T00:00:00.000Z",
                "noctu": false,
                "bvg": false,
                "zuzahlungsstatus": "0",
                "autidem": true,
                "abgabehinweis": "Vor Zugriff durch Kinder schützen.",
                "anzahl": 1
              },
              "arzneimittel": {
                "medicationItem": [
                    {
                        "type": "medicationCompounding",
                        "kategorie": "0",
                        "impfstoff": false,
                        "herstellungsanweisung": "Lösung",
                        "verpackung": null,
                        "rezepturname": "Lösung",
                        "darreichungsform": null,
                        "gesamtmenge": "100",
                        "einheit": "ml",
                        "listeBestandteilRezepturverordnung": [
                          {
                            "type": "bestandteilRezepturverordnung",
                            "name": "Salicylsäure",
                            "menge": "5",
                            "einheit": "g"
                          },
                          {
                            "name": "2-propanol 70",
                            "mengeUndEinheit": "Ad 100 g"
                          }
                        ]
                    }
                ]
              }
            },
            {
              "prescriptionId": "160.100.000.000.006.24",
              "erstellungszeitpunkt": "2024-06-30T09:50:00.000Z",
              "status": "final",
              "krankenversicherung": {
                "kostentraegertyp": "GKV",
                "ikKrankenkasse": "104212059",
                "kostentraeger": "AOK Baden-Württemberg",
                "wop": "52",
                "versichertenstatus": "1",
                "besonderePersonengruppe": "0",
                "dmpKz": "0"
              },
              "patient": {
                "gkvVersichertenId": "X234567891",
                "person": {
                  "vorname": "Tina",
                  "name": "Tester"
                },
                "geburtsdatum": "1985-05-05T00:00:00.000Z",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "42"
                }
              },
              "arzt": {
                "typ": "0",
                "berufsbezeichnung": "Hausarzt",
                "arztnummer": "838382202",
                "person": {
                  "vorname": "Hans",
                  "nachname": "Gesundmacher",
                  "titel": "Dr. med."
                }
              },
              "pruefnummer": "Y/400/1910/36/346",
              "organisation": {
                "bsnr": "31234567",
                "name": "Hausarztpraxis Dr. Gesundmacher",
                "adresse": {
                  "type": "streetAddress",
                  "plz": "70372",
                  "ort": "Stuttgart",
                  "strasse": "Musterstrasse",
                  "hausnummer": "200"
                },
                "telefon": "+49 711 1234567",
                "fax": "+49 711 1234599",
                "eMail": "hans@praxis-gesundmacher.de"
              },
              "verordnung": {
                "type": "prescription",
                "ausstellungsdatum": "2024-06-30T00:00:00.000Z",
                "noctu": false,
                "bvg": false,
                "zuzahlungsstatus": "0",
                "autidem": true,
                "abgabehinweis": "Vor Zugriff durch Kinder schützen.",
                "anzahl": 1
              },
              "arzneimittel": {
                "medicationItem": [
                    {
                        "type": "medicationFreeText",
                        "kategorie": "0",
                        "impfstoff": false,
                        "freitextverordnung": "Metformin 850mg Tabletten N3"
                    }
                ]
              }
            }
          ]
        }
      ]
    }
""".trimIndent()

fun getAvailablePrescriptionListsExample(messageId: String, correlationId: String): AvailablePrescriptionLists {
    val prescriptionListExample = java.lang.String.format(availablePrescriptionListsJsonExample, messageId, correlationId)
    return prescriptionJsonFormatter.decodeFromString(prescriptionListExample)
}

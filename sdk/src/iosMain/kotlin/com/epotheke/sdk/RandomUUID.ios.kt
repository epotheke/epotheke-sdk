package com.epotheke.sdk

import platform.Foundation.NSUUID

actual fun randomUUID(): String = NSUUID().UUIDString()

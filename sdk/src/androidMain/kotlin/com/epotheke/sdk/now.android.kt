package com.epotheke.sdk

actual fun now(): Long {
    return System.currentTimeMillis() / 1000
}

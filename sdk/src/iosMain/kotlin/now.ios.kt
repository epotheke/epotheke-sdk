import platform.CoreFoundation.CFAbsoluteTimeGetCurrent

actual fun now(): Long {
    return CFAbsoluteTimeGetCurrent().toLong()
}

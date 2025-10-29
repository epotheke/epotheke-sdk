package com.epotheke.demo

/* Existing environments can be configured or added here.
*  If tokens are added ensure to not publish this file.
*  To ignore but keep updates:
*  $git update-index --skip-worktree demo-android-standalone/app/src/main/java/com/epotheke/demo/Service.kt
*  can be used.
*/
enum class Service(val url: String, val tenantToken: String?) {
    MOCK("https://mock.test.epotheke.com/cardlink", null ),
    DEV("https://service.dev.epotheke.com/cardlink", null),

    STAGE(
        "https://service.staging.epotheke.com/cardlink",
        "DUMMY"
    ),
    PROD(
        "https://service.epotheke.com/cardlink",
        "DUMMY"
    ),
}

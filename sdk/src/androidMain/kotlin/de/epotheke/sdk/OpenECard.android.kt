package de.epotheke.sdk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.oshai.kotlinlogging.KotlinLogging
import org.openecard.android.activation.AndroidContextManager
import org.openecard.android.activation.OpeneCard
import org.openecard.mobile.activation.ActivationSource
import org.openecard.mobile.activation.ServiceErrorResponse
import org.openecard.mobile.activation.StartServiceHandler
import org.openecard.mobile.activation.StopServiceHandler


private val logger = KotlinLogging.logger {}

abstract class OecActivity : Activity() {
    var oec: OpeneCard? = null
    var ctxManager: AndroidContextManager? = null
    var activationSource: ActivationSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initOecContext()
    }

    override fun onDestroy() {
        ctxManager?.terminateContext(object : StopServiceHandler {
            override fun onSuccess() {
                // do nothing
                logger.debug { "Open eCard stopped successfully." }
                cleanupOecInstances()
            }
            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to stop Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
        super.onDestroy()
    }

    protected fun initOecContext() {
        oec = OpeneCard.createInstance()
        ctxManager = oec?.context(this)
        ctxManager?.initializeContext(object : StartServiceHandler {
            override fun onSuccess(actSource: ActivationSource) {
                activationSource = actSource
            }
            override fun onFailure(ex: ServiceErrorResponse) {
                logger.error { "Failed to initialize Open eCard (code=${ex.statusCode}): ${ex.errorMessage}" }
                cleanupOecInstances()
            }
        })
    }

    protected fun cleanupOecInstances() {
        oec = null
        ctxManager = null
        activationSource = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        ctxManager?.onNewIntent(intent)
    }
}

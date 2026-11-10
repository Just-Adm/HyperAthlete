package com.hyperathlete.app.core.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.hyperathlete.app.HomeActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives `discord-<id>:/authorize/callback?code=…` from the Custom Tab, hands it to the
 * repository and jumps straight back to the app (no UI of its own).
 */
class DiscordCallbackActivity : Activity() {

    private val accounts: AccountRepository by lazy {
        EntryPointAccessors.fromApplication(applicationContext, com.hyperathlete.app.core.AppEntryPoint::class.java).accounts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        val uri = intent?.data
        if (uri != null) scope.launch { accounts.completeDiscord(uri) }
        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }

    private companion object {
        /** Outlives this short-lived activity so the token exchange finishes. */
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }
}

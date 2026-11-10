package com.hyperathlete.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle

/**
 * The launcher entry. It shows nothing: an app that is already open comes back exactly as it was,
 * otherwise [HomeActivity] starts. It keeps the original component name so home-screen icons survive updates.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!bringAppToFront()) {
            startActivity(
                Intent(this, HomeActivity::class.java).apply {
                    intent?.extras?.let { putExtras(it) }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
        finishQuietly()
    }

    /** Like a normal icon tap: returns false when the app is not open yet. */
    private fun bringAppToFront(): Boolean = runCatching {
        val task = getSystemService(ActivityManager::class.java).appTasks.firstOrNull {
            it.taskInfo?.baseActivity?.className == HomeActivity::class.java.name
        } ?: return false
        task.moveToFront()
        true
    }.getOrDefault(false)

    private fun finishQuietly() {
        finish()
        if (Build.VERSION.SDK_INT >= 34) overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        else @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }
}

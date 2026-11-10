package com.hyperathlete.app.core.workout

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.hyperathlete.app.R

/**
 * "How to" button in the workout player: opens a YouTube search for the exercise (as in the v0 app).
 * The English name is used whatever the app language, since that gives far better technique videos.
 */
object ExerciseVideo {
    const val YOUTUBE = "com.google.android.youtube"

    /** "Incline Dumbbell Press (30°)" → "Incline Dumbbell Press 30 exercise form". */
    fun query(englishName: String): String {
        val clean = englishName.replace(Regex("[()°+]"), " ").replace(Regex("\\s+"), " ").trim()
        return "$clean exercise form"
    }

    fun searchUrl(englishName: String): Uri = resultsUrl(query(englishName))

    fun open(context: Context, englishName: String) = search(context, query(englishName))

    fun resultsUrl(q: String): Uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))

    /**
     * Any YouTube search (also used by the measurement tests' "watch how" button).
     * A results link handed to the YouTube app is routed to the search page every time, even when
     * YouTube is already open in the background (ACTION_SEARCH only worked on a cold start: on a
     * running YouTube it just brought the app to the front without searching).
     */
    fun search(context: Context, q: String) {
        val url = resultsUrl(q)
        val app = Intent(Intent.ACTION_VIEW, url).setPackage(YOUTUBE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val appSearch = Intent(Intent.ACTION_SEARCH).setPackage(YOUTUBE)
            .putExtra(SearchManager.QUERY, q)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        for (intent in listOf(app, appSearch, web)) {
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        Toast.makeText(context, R.string.exercise_howto_none, Toast.LENGTH_SHORT).show()
    }
}

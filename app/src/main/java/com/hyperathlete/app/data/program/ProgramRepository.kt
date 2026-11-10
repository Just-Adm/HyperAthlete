package com.hyperathlete.app.data.program

import android.content.Context
import com.hyperathlete.app.domain.program.Program
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The program seed from assets, parsed once. */
@Singleton
class ProgramRepository @Inject constructor(@ApplicationContext private val context: Context) {
    val program: Program by lazy { load(context) }

    companion object {
        @Volatile private var cached: Program? = null

        fun load(context: Context): Program = cached ?: synchronized(this) {
            cached ?: context.applicationContext.assets.open("program/program.json").bufferedReader().use {
                Program.parse(it.readText())
            }.also { cached = it }
        }
    }
}

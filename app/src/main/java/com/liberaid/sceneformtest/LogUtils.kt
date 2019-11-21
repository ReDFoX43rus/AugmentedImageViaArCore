package com.liberaid.sceneformtest

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

interface ILogInterceptor {
    fun onLog(priority: Int, tag: String?, message: String, t: Throwable?)
}

class CustomTimberTree(private val interceptors: List<ILogInterceptor>) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        interceptors.forEach { it.onLog(priority, tag, message, t) }
        super.log(priority, tag, message, t)
    }
}

class LogIntoFileLogInterceptor(private val appContext: Context, private val minLogPriority: Int = Log.DEBUG) : ILogInterceptor {

    private val logDateTime: Date = Date()
    private var prevLogsCleared = false

    override fun onLog(priority: Int, tag: String?, message: String, t: Throwable?) {
        if(priority >= minLogPriority) {
            logIntoFile(tag, "$message ${t?.message ?: ""}")

            if(priority == Log.ERROR && t != null){
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                t.printStackTrace(pw)

                logIntoFile(tag, sw.toString())
            }
        }
    }

    private fun logIntoFile(tag: String?, message: String) = getLogFile(logDateTime)?.appendText("[$tag] $message\n")

    @SuppressLint("LogNotTimber")
    private fun getLogFile(date: Date): File? = runCatching {

        if(!prevLogsCleared){
            clearPreviousLogs()
            prevLogsCleared = true
        }

        val dirs = "${appContext.getExternalFilesDir(null)}/$LOG_PATH"
        File(dirs).apply {
            if(!exists()) {
                mkdirs()
            }
        }

        val filename = "$dirs/log.txt"

        File(filename)
    }.onFailure {
        Log.e(TAG, "Cannot create log file ${it.message}")
    }.getOrNull()

    private fun clearPreviousLogs() {
        val dirPath = "${appContext.getExternalFilesDir(null)}/$LOG_PATH"

        val files = File(dirPath).listFiles()
        if(files == null || files.isEmpty()) {
            return
        }

        val sortedLogFiles = files.filter { it.isFile }
            .map { it.name to it }
            .sortedByDescending { it.first }


        if(sortedLogFiles.size > MAX_LOGS_COUNT - 1)
            for(i in MAX_LOGS_COUNT - 1 until sortedLogFiles.size)
                sortedLogFiles[i].second.delete()
    }

    companion object {
        private const val TAG = "FileLogger"
        private const val LOG_PATH = "logs"
        private const val MAX_LOGS_COUNT = 5
    }
}

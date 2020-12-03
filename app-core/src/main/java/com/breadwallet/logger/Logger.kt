/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
@file:Suppress("NOTHING_TO_INLINE")

package com.breadwallet.logger

import android.os.Build
import android.util.Log
import com.breadwallet.logger.Logger.Companion
import com.breadwallet.logger.Logger.Companion.create
import com.breadwallet.logger.Logger.Companion.tag
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord

/**
 * A simple and instantiable logger with a default implementation
 * via [Companion].  Loggers can also be produced with a one-time
 * tag via [tag] or with a permanent manual tag via [create].
 *
 * When a tag is not specified, the calling class name will be used.
 */
interface Logger {
    companion object : Logger {
        private val defaultLogger = DefaultLogger()

        /** Acquire a default logger that will use [tag] for a single call. */
        fun tag(tag: String): Logger = defaultLogger.withTagForCall(tag)

        /** Create a new logger that will always use [tag]. */
        fun create(tag: String): Logger = DefaultLogger(tag)

        override fun verbose(message: String, vararg data: Any?) =
                defaultLogger.verbose(message, *data)

        override fun debug(message: String, vararg data: Any?) =
                defaultLogger.debug(message, *data)

        override fun info(message: String, vararg data: Any?) =
                defaultLogger.info(message, *data)

        override fun warning(message: String, vararg data: Any?) =
                defaultLogger.warning(message, *data)

        override fun error(message: String, vararg data: Any?) =
                defaultLogger.error(message, *data)

        override fun wtf(message: String, vararg data: Any?) =
                defaultLogger.wtf(message, *data)

        fun setJulLevel(level: Level) {
            defaultLogger.initialize(level)
        }
    }

    /** Log verbose [message] and any [data] objects. */
    fun verbose(message: String, vararg data: Any?)
    /** Log debug [message] and any [data] objects. */
    fun debug(message: String, vararg data: Any?)
    /** Log info [message] and any [data] objects. */
    fun info(message: String, vararg data: Any?)
    /** Log warning [message] and any [data] objects. */
    fun warning(message: String, vararg data: Any?)
    /** Log error [message] and any [data] objects. */
    fun error(message: String, vararg data: Any?)
    /** Log wtf [message] and any [data] objects. */
    fun wtf(message: String, vararg data: Any?)
}

/** Log verbose [message] and any [data] objects. */
inline fun logVerbose(message: String, vararg data: Any?) = Logger.verbose(message, *data)
/** Log debug [message] and any [data] objects. */
inline fun logDebug(message: String, vararg data: Any?) = Logger.debug(message, *data)
/** Log info [message] and any [data] objects. */
inline fun logInfo(message: String, vararg data: Any?) = Logger.info(message, *data)
/** Log warning [message] and any [data] objects. */
inline fun logWarning(message: String, vararg data: Any?) = Logger.warning(message, *data)
/** Log error [message] and any [data] objects. */
inline fun logError(message: String, vararg data: Any?) = Logger.error(message, *data)
/** Log wtf [message] and any [data] objects. */
inline fun logWtf(message: String, vararg data: Any?) = Logger.wtf(message, *data)

/** A default [Logger] implementation for use on Android. */
private class DefaultLogger(
        /** Used instead of looking up the calling element's name. */
        private val manualTag: String? = null
) : Handler(), Logger {

    companion object {
        /** Max character count for Logcat tags below Android N. */
        private const val MAX_TAG_LENGTH = 23
        /** Pattern matching anonymous class name. */
        private val ANONYMOUS_CLASS = "(\\$\\d+)+$".toPattern()
    }

    private val recordFormatter = object : Formatter() {
        override fun format(record: LogRecord): String = buildString {
            appendLine(record.message)
            record.thrown
                ?.run(Log::getStackTraceString)
                ?.run(::appendLine)
        }
    }

    init {
        formatter = recordFormatter
        initialize()
    }

    internal fun initialize(minimumLevel: Level = Level.INFO) {
        LogManager.getLogManager().reset()
        java.util.logging.Logger.getLogger("").apply {
            addHandler(this@DefaultLogger)
            level = minimumLevel
        }
    }

    /** Holds a tag that will be used once after [withTagForCall]. */
    private val overrideTag = ThreadLocal<String>()

    /** Get the expected tag name for a given log call. */
    private val nextTag: String?
        get() {
            val tag = overrideTag.get()
            if (tag != null) {
                overrideTag.remove()
                return tag
            }
            return manualTag ?: Throwable().stackTrace
                    .first { it.className !in ignoredStackClassNames }
                    .asTag()
        }

    /** A list of class names to filter when searching for the calling element name. */
    private val ignoredStackClassNames = listOf(
            DefaultLogger::class.java.name,
            Logger::class.java.name,
            Logger.Companion::class.java.name
    )

    /** Set the tag to use for the next log call. */
    fun withTagForCall(tag: String): DefaultLogger {
        overrideTag.set(tag)
        return this
    }

    override fun verbose(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.v(tag, _message)
                    else -> Log.v(tag, _message, error)
                }
            }, message, data)

    override fun debug(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.d(tag, _message)
                    else -> Log.d(tag, _message, error)
                }
            }, message, data)

    override fun info(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.i(tag, _message)
                    else -> Log.i(tag, _message, error)
                }
            }, message, data)

    override fun warning(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.w(tag, _message)
                    else -> Log.w(tag, _message, error)
                }
            }, message, data)

    override fun error(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.e(tag, _message)
                    else -> Log.e(tag, _message, error)
                }
            }, message, data)

    override fun wtf(message: String, vararg data: Any?) =
            logWith({ tag, _message, error ->
                when (error) {
                    null -> Log.wtf(tag, _message)
                    else -> Log.wtf(tag, _message, error)
                }
            }, message, data)

    /** Logs [message] and [data] objects using [logFunc]. */
    private inline fun logWith(
            crossinline logFunc: (
                    @ParameterName("tag") String?,
                    @ParameterName("_message") String,
                    @ParameterName("error") Throwable?
            ) -> Unit,
            message: String,
            data: Array<out Any?>
    ) {
        val tag = nextTag
        if (data.isEmpty())
            logFunc(tag, message, null)
        else {
            val hadException = when (val first = data.first()) {
                is Throwable -> {
                    logFunc(tag, message, first)
                    true
                }
                else -> {
                    logFunc(tag, message, null)
                    false
                }
            }

            data.drop(if (hadException) 1 else 0)
                    .forEachIndexed { index, obj ->
                        logFunc(tag, "\tData ${index + 1}:", null)
                        logFunc(tag, "\t\t$obj", null)
                    }
        }
    }

    /**
     * Returns a tag or null using [StackTraceElement.getClassName]
     * removing anonymous class name suffix and trimming to 23
     * characters when below Android N.
     */
    private fun StackTraceElement.asTag(): String? {
        return className.substringAfterLast('.')
                .let {
                    val tag = ANONYMOUS_CLASS.matcher(it)
                            .run { if (find()) replaceAll("") else it }
                    if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        tag
                    } else {
                        tag.take(MAX_TAG_LENGTH)
                    }
                }
    }

    override fun publish(record: LogRecord) {
        val julInt = level.intValue()
        val priority = when {
            julInt >= Level.SEVERE.intValue() -> Log.ERROR
            julInt >= Level.WARNING.intValue() -> Log.WARN
            julInt >= Level.INFO.intValue() -> Log.INFO
            else -> Log.DEBUG
        }
        val tag = record.loggerName
            .substringAfterLast('.')
            .takeLast(MAX_TAG_LENGTH)
        try {
            Log.println(priority, tag, formatter.format(record))
        } catch (e: RuntimeException) {
            error("Failed to print log record", e)
        }
    }

    override fun flush() = Unit

    override fun close() = Unit
}

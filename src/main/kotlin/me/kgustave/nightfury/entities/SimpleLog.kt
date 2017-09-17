/*
 * Copyright 2017 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("UNUSED")
package me.kgustave.nightfury.entities

import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

/*
 * Kotlin conversion of net.dv8tion.jda.core.utils.SimpleLog.
 *
 * LogListener has been removed because I won't ever use it anyways.
 */
class SimpleLog private constructor(val name: String)
{
    companion object
    {
        var DEFAULT_LEVEL = Level.INFO

        private val logMsgFormat : String                              = "[%time%] [%level%] [%name%]: %text%"
        private val dateFormat   : SimpleDateFormat                    = SimpleDateFormat("HH:mm:ss")
        private val logsMap      : MutableMap<String, SimpleLog>       = HashMap()
        private val fileLogs     : MutableMap<Level, MutableSet<File>> = HashMap()
        private var origStd      : PrintStream?                        = null
        private var origErr      : PrintStream?                        = null
        private var stdOut       : FileOutputStream?                   = null
        private var errOut       : FileOutputStream?                   = null

        @Throws(IOException::class)
        @SuppressWarnings("ResultOfMethodCallIgnored")
        fun addFileLogs(std: File?, err: File?)
        {
            if(std != null)
            {
                if(!std.absoluteFile.parentFile.exists())
                    std.absoluteFile.parentFile.mkdirs()
                if(!std.exists())
                    std.createNewFile()
                val fOut = FileOutputStream(std, true)
                System.setOut(PrintStream(object : OutputStream() {
                    @Throws(IOException::class)
                    override fun write(b: Int) {
                        (origStd?:System.out).write(b)
                        fOut.write(b)
                    }
                }))
                stdOut?.close()
                stdOut = fOut
            }
            else if(origStd != null)
            {
                System.setOut(origStd)
                stdOut?.close()
                origStd = null
            }
            if(err != null)
            {
                if(!err.absoluteFile.parentFile.exists())
                    err.absoluteFile.parentFile.mkdirs()
                if(!err.exists())
                    err.createNewFile()
                val fOut = FileOutputStream(err, true)
                System.setErr(PrintStream(object : OutputStream() {
                    @Throws(IOException::class)
                    override fun write(b: Int) {
                        (origErr?:System.err).write(b)
                        fOut.write(b)
                    }

                }))
                errOut?.close()
                errOut = fOut
            }
            else if(origErr != null)
            {
                System.setErr(origErr)
                errOut?.close()
                origErr = null
            }
        }

        @Throws(IOException::class)
        fun addFileLog(logLevel: Level, file: File)
        {
            (fileLogs[logLevel] ?: HashSet<File>().apply { fileLogs.put(logLevel, this) }).add(file.canonicalFile)
        }

        fun removeFileLog(logLevel: Level)
        {
            fileLogs.remove(logLevel)
        }

        @Throws(IOException::class)
        fun removeFileLog(file: File)
        {
            val setIterator = fileLogs.entries.iterator()
            while(setIterator.hasNext())
            {
                val set = setIterator.next()
                val fileIterator = set.value.iterator()
                while(fileIterator.hasNext())
                {
                    val logFile = fileIterator.next()
                    if(logFile == file.canonicalFile)
                    {
                        fileIterator.remove()
                        break
                    }
                }
                if(set.value.isEmpty()) setIterator.remove()
            }
        }

        private fun logToFiles(msg: String, level: Level)
        {
            for(file in collectFiles(level))
            {
                try {
                    if(!file.exists())
                        file.createNewFile()
                    file.appendText(msg + '\n', Charsets.UTF_8)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        private fun collectFiles(level: Level): Set<File>
        {
            val out = HashSet<File>()
            for((key, value) in fileLogs)
            {
                if(key.priority <= level.priority)
                    out.addAll(value)
            }
            return out
        }

        fun getLog(name: String) = logsMap[name.toLowerCase()]?: SimpleLog(name).apply {
            synchronized(logsMap) {
                logsMap.put(name.toLowerCase(), this)
            }
        }

        // Copied from org.apache.commons:commons-lang3:3.5 ExceptionsUtils.java
        fun getStackTrace(throwable: Throwable): String
        {
            val sw = StringWriter()
            val pw = PrintWriter(sw, true)
            throwable.printStackTrace(pw)
            return sw.buffer.toString()
        }
    }

    var level: Level? = null
    var effectiveLevel : Level
        get() = level ?: DEFAULT_LEVEL
        set(value) { level = value }

    fun trace(msg: Any?) = log(Level.TRACE, msg?:"")
    fun debug(msg: Any?) = log(Level.DEBUG, msg?:"")
    fun info(msg: Any?) = log(Level.INFO, msg?:"")
    fun warn(msg: Any?) = log(Level.WARNING, msg?:"")
    fun fatal(msg: Any?) = log(Level.FATAL, msg?:"")

    fun log(ex: Throwable)
    {
        log(Level.FATAL, "Encountered an exception:")
        log(Level.FATAL, getStackTrace(ex))
    }

    fun log(level: Level, msg: Any)
    {
        val message = logMsgFormat.replace("%time%", dateFormat.format(Date()))
                .replace("%level%", level.tag).replace("%name%", name)
                .replace("%text%", msg.toString())

        if(level == Level.OFF || level.priority < effectiveLevel.priority)
            logToFiles(message, level)
        else
            printOut(message, level)
    }

    private fun printOut(msg: String, level: Level) = if(level.isError) System.err.println(msg) else println(msg)

    enum class Level constructor(val tag: String, val priority: Int, val isError: Boolean)
    {
        ALL("Finest", 0, false),
        TRACE("Trace", 1, false),
        DEBUG("Debug", 2, false),
        INFO("Info", 3, false),
        WARNING("Warning", 4, true),
        FATAL("Fatal", 5, true),
        OFF("NO-LOGGING", 6, true)
    }
}
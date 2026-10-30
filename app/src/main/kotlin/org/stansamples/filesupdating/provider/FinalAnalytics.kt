package org.stansamples.filesupdating.provider

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.stansamples.filesupdating.entity.LogEvent
import org.stansamples.filesupdating.util.duration
import org.stansamples.filesupdating.util.formats
import org.stansamples.filesupdating.util.hex
import org.stansamples.filesupdating.util.string
import org.stansamples.filesupdating.util.toBytes
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

internal class FinalAnalytics(
    private val dirs: Dirs,
    private val locals: Locals,
    private val times: Times,
    contexts: Contexts,
    loggers: Loggers,
) : Analytics {
    private val coroutineScope = CoroutineScope(contexts.io + SupervisorJob())
    private val dirFormats = formats(pattern = "yyyyMMdd", locale = Locale.US, timeZone = TimeZone.getTimeZone("utc"))
    private val fileFormats = formats(pattern = "yyyyMMddHH", locale = Locale.US, timeZone = TimeZone.getTimeZone("utc"))
    private val indices = AtomicInteger(locals.launched.inWholeSeconds.toInt())
    private val locks = ReentrantReadWriteLock()
    private val logger = loggers.create("[Analytics]")

    override val events: List<LogEvent>
        get() {
            val purpose = Analytics.Purpose.Events
            val date = times.now()
            val fileName = "${purpose.name}_${fileFormats.string(date)}_${locals.launched.inWholeSeconds.toInt().toBytes().hex()}.jsonl"
            val file = dirs.docs
                .resolve(locals.packageName)
                .resolve(purpose.name)
                .resolve(dirFormats.string(date))
                .resolve(fileName)
            return locks.readLock().withLock {
                runCatching {
                    file.useLines { lines ->
                        lines.map {
                            JSONObject(it).toLogEvent()
                        }.toList()
                    }
                }.getOrNull().orEmpty()
            }
        }

    private fun JSONObject.toLogEvent(): LogEvent {
        val payload = mutableMapOf<String, String>()
        optJSONObject("payload")?.let {
            it.keys().forEach { key ->
                payload[key] = it.getString(key)
            }
        }
        return LogEvent(
            id = getString("id"),
            date = getLong("date").milliseconds,
            type = LogEvent.Type.valueOf(getString("type")),
            payload = payload,
        )
    }

    private fun LogEvent.toJSONObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("date", date.inWholeMilliseconds)
        obj.put("type", type.name)
        if (payload.isNotEmpty()) {
            val entries = JSONObject()
            payload.forEach { (key, value) ->
                entries.put(key, value)
            }
            obj.put("payload", entries)
        }
        return obj
    }

    private fun check(purpose: Analytics.Purpose, date: Duration) {
        for (dir in dirs.docs.resolve(locals.packageName).resolve(purpose.name).listFiles().orEmpty()) {
            if (!dir.exists()) continue
            if (!dir.isDirectory) continue
            val then = try {
                dirFormats.duration(dir.name)
            } catch (_: DateTimeParseException) {
                continue
            }
            if (date.minus(then) > 14.days) {
                dir.deleteRecursively()
                logger.debug("delete: $dir")
            }
        }
    }

    private fun report(purpose: Analytics.Purpose, event: LogEvent) {
        val fileName = "${purpose.name}_${fileFormats.string(event.date)}_${locals.launched.inWholeSeconds.toInt().toBytes().hex()}.jsonl"
        val dir = dirs.docs
            .resolve(locals.packageName)
            .resolve(purpose.name)
            .resolve(dirFormats.string(event.date))
        if (!dir.exists()) {
            if (!dir.mkdirs() || !dir.exists()) TODO()
        }
        val file = dir.resolve(fileName)
        logger.debug("file: ${file.absolutePath}")
        val text = StringBuilder(event.toJSONObject().toString()).append("\n")
        file.appendText(text.toString())
    }

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        payload: Map<String, String>,
    ) {
        val date = times.now()
        coroutineScope.launch {
            locks.writeLock().withLock {
                val event = LogEvent(
                    id = indices.incrementAndGet().toBytes().hex(),
                    date = date,
                    type = type,
                    payload = payload,
                )
                logger.debug("event: ${event.type.name}")
                runCatching {
                    check(purpose = purpose, date = date)
                    report(purpose = purpose, event = event)
                }.onFailure { error ->
                    logger.warning("report error: $error")
                }
            }
        }
    }

    override fun report(
        purpose: Analytics.Purpose,
        type: LogEvent.Type,
        error: Throwable,
    ) {
        val payload = mutableMapOf<String, String>()
        payload["error"] = error::class.java.name
        error.cause?.also {
            payload["cause"] = it::class.java.name
        }
        report(purpose = purpose, type = type, payload = payload)
    }
}

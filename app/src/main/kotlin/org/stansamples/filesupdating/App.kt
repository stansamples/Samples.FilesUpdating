package org.stansamples.filesupdating

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.stansamples.filesupdating.provider.Contexts
import org.stansamples.filesupdating.provider.FinalHeartbeats
import org.stansamples.filesupdating.provider.FinalLoggers
import org.stansamples.filesupdating.provider.Heartbeats
import org.stansamples.filesupdating.provider.Loggers
import kotlin.time.Duration.Companion.seconds

fun main() {
    val contexts = Contexts(
        main = Dispatchers.Default,
        default = Dispatchers.Default,
    )
    val loggers: Loggers = FinalLoggers()
    val heartbeats: Heartbeats = FinalHeartbeats(
        contexts = contexts,
        loggers = loggers,
    )
    runBlocking {
        withContext(contexts.default) {
            heartbeats.start()
            delay(30.seconds)
            heartbeats.stop()
        }
    }
}

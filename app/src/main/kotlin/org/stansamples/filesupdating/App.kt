package org.stansamples.filesupdating

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.stansamples.filesupdating.provider.Contexts
import org.stansamples.filesupdating.provider.Dirs
import org.stansamples.filesupdating.provider.FinalDirs
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
    val dirs: Dirs = FinalDirs()
    val loggers: Loggers = FinalLoggers()
    val heartbeats: Heartbeats = FinalHeartbeats(
        contexts = contexts,
        dirs = dirs,
        loggers = loggers,
    )
    runBlocking {
        withContext(contexts.default) {
            heartbeats.start()
            delay(20.seconds)
            heartbeats.stop()
        }
    }
}

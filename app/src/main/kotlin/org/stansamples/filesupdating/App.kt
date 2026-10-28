package org.stansamples.filesupdating

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.stansamples.filesupdating.provider.Contexts
import org.stansamples.filesupdating.provider.Dirs
import org.stansamples.filesupdating.provider.FinalDirs
import org.stansamples.filesupdating.provider.FinalHeartbeats
import org.stansamples.filesupdating.provider.FinalLocals
import org.stansamples.filesupdating.provider.FinalLoggers
import org.stansamples.filesupdating.provider.FinalPackages
import org.stansamples.filesupdating.provider.FinalRemotes
import org.stansamples.filesupdating.provider.FinalSecrets
import org.stansamples.filesupdating.provider.FinalTimes
import org.stansamples.filesupdating.provider.Heartbeats
import org.stansamples.filesupdating.provider.Locals
import org.stansamples.filesupdating.provider.Loggers
import org.stansamples.filesupdating.provider.Packages
import org.stansamples.filesupdating.provider.Remotes
import org.stansamples.filesupdating.provider.Secrets
import org.stansamples.filesupdating.provider.Times
import kotlin.time.Duration.Companion.seconds

fun main() {
    val contexts = Contexts(
        main = Dispatchers.Default,
        default = Dispatchers.Default,
    )
    val dirs: Dirs = FinalDirs()
    val locals: Locals = FinalLocals(
        dcTimeout = 32.seconds,
        serialNumber = "foobarbaz",
        ttl = 4.seconds,
    )
    val loggers: Loggers = FinalLoggers()
    val packages: Packages = FinalPackages()
    val remotes: Remotes = FinalRemotes()
    val secrets: Secrets = FinalSecrets()
    val times: Times = FinalTimes()
    val heartbeats: Heartbeats = FinalHeartbeats(
        contexts = contexts,
        dirs = dirs,
        locals = locals,
        loggers = loggers,
        packages = packages,
        remotes = remotes,
        secrets = secrets,
        times = times,
    )
    runBlocking {
        withContext(contexts.default) {
            heartbeats.start()
            delay(10.seconds)
            heartbeats.stop()
        }
    }
}

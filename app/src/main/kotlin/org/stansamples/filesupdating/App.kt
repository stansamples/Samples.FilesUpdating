package org.stansamples.filesupdating

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.stansamples.filesupdating.provider.Analytics
import org.stansamples.filesupdating.provider.Contexts
import org.stansamples.filesupdating.provider.Dirs
import org.stansamples.filesupdating.provider.FinalAnalytics
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
        io = Dispatchers.IO,
    )
    val dirs: Dirs = FinalDirs()
    val loggers: Loggers = FinalLoggers()
    val packages: Packages = FinalPackages()
    val remotes: Remotes = FinalRemotes()
    val secrets: Secrets = FinalSecrets()
    val times: Times = FinalTimes()
    val locals: Locals = FinalLocals(
        launched = times.now(),
    )
    val analytics: Analytics = FinalAnalytics(
        contexts = contexts,
        dirs = dirs,
        locals = locals,
        loggers = loggers,
        times = times,
    )
    val heartbeats: Heartbeats = FinalHeartbeats(
        analytics = analytics,
        contexts = contexts,
        dirs = dirs,
        locals = locals,
        loggers = loggers,
        packages = packages,
        remotes = remotes,
        secrets = secrets,
        times = times,
    )
    val logger = loggers.create("[App]")
    val job = SupervisorJob()
    val coroutineScope = CoroutineScope(contexts.main + job)
    coroutineScope.launch {
        withContext(contexts.default) {
            heartbeats.states.collect { state ->
                when (state) {
                    is Heartbeats.State.DownloadingPackage -> {
                        logger.debug("downloading ${state.packageName} ${state.read}/${state.fullLength}")
                    }
                    is Heartbeats.State.InstallingPackage -> {
                        logger.debug("installing ${state.packageName} ${state.read}/${state.fullLength}")
                    }
                    Heartbeats.State.Stopped -> { /* noop */ }
                    Heartbeats.State.Updating -> { /* noop */ }
                    is Heartbeats.State.UpdatingPackage -> { /* noop */ }
                    is Heartbeats.State.Waiting -> { /* noop */ }
                }
            }
        }
    }
    runBlocking {
        withContext(contexts.default) {
            heartbeats.start()
            delay(12.seconds)
            logger.debug("events: ${analytics.events.size}")
            delay(12.seconds)
            heartbeats.stop()
        }
        job.cancelAndJoin()
    }
}

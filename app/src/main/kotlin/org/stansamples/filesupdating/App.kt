package org.stansamples.filesupdating

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val locals: Locals = FinalLocals()
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
    val logger = loggers.create("[App]")
    val job = SupervisorJob()
    val coroutineScope = CoroutineScope(contexts.main + SupervisorJob())
    coroutineScope.launch {
        withContext(contexts.default) {
            heartbeats.states.collect { state ->
                when (state) {
                    is Heartbeats.State.DownloadingPackage -> {
                        logger.debug("${state.packageName}: ${state.read}/${state.fullLength}")
                    }
                    is Heartbeats.State.InstallingPackage -> { /* noop */ }
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
            delay(30.seconds)
            heartbeats.stop()
        }
        job.cancelAndJoin()
    }
}

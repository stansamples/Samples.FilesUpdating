package org.stansamples.filesupdating

import kotlinx.coroutines.Dispatchers
import org.stansamples.filesupdating.provider.Contexts
import org.stansamples.filesupdating.provider.FinalLoggers
import org.stansamples.filesupdating.provider.Loggers
import org.stansamples.filesupdating.provider.Providers

fun main() {
    val contexts = Contexts(
        main = Dispatchers.Main,
        default = Dispatchers.Default,
    )
    val loggers: Loggers = FinalLoggers()
    val providers = Providers(
        contexts = contexts,
        loggers = loggers,
    )
    val logger = loggers.create("[App]")
    logger.debug("Hello files updating!")
}

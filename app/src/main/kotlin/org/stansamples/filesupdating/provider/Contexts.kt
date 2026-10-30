package org.stansamples.filesupdating.provider

import kotlin.coroutines.CoroutineContext

internal class Contexts(
    val main: CoroutineContext,
    val default: CoroutineContext,
    val io: CoroutineContext,
)

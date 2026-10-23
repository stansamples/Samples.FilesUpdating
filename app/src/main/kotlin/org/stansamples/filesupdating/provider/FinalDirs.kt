package org.stansamples.filesupdating.provider

import java.io.File

internal class FinalDirs : Dirs {
    private val root = File(System.getProperty("user.home"))
    override val cache = root.resolve(".cache").also {
        if (!it.exists()) {
            if (!it.mkdirs()) TODO()
        }
    }
}

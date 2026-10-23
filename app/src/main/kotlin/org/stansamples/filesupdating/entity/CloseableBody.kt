package org.stansamples.filesupdating.entity

import java.io.Closeable
import java.io.InputStream

internal interface CloseableBody : Closeable {
    val contentLength: Long
    val src: InputStream
}

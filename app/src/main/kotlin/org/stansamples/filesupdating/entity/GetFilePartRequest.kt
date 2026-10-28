package org.stansamples.filesupdating.entity

internal data class GetFilePartRequest(
    val serialNumber: String,
    val id: String,
    val range: LongRange
)

package org.stansamples.filesupdating.entity

internal class TooManyRequestsException : Exception()

internal class RangeNotSatisfiableException : Exception()

internal class GetActualFilePartException(cause: Throwable) : Exception(cause)

internal class HeartbeatsStoppedException : Exception()

internal class HeartbeatsTTLException : Exception()

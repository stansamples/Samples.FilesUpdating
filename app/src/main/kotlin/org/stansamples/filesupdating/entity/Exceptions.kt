package org.stansamples.filesupdating.entity

internal class ConflictException : Exception()
internal class GetActualFilePartException(cause: Throwable) : Exception(cause)
internal class HeartbeatRequestException(cause: Throwable) : Exception(cause)
internal class HeartbeatsStoppedException : Exception()
internal class HeartbeatsTTLException : Exception()
internal class RangeNotSatisfiableException : Exception()
internal class TooManyRequestsException : Exception()

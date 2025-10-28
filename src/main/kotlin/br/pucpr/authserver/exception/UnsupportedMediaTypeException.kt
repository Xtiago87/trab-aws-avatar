package br.pucpr.authserver.exception

import org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(UNSUPPORTED_MEDIA_TYPE)
class UnsupportedMediaTypeException(
    message: String = "Unsupported media type",
    cause: Throwable? = null
) : IllegalArgumentException(message, cause) {
    constructor(vararg types: String, cause: Throwable? = null) : this(
        message = "Unsupported media type. Supported types: ${types.joinToString()}",
        cause = cause
    )
}

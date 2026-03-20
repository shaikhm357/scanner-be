package com.qrscanner.plugins

import com.qrscanner.config.configureCors
import com.qrscanner.exception.*
import com.qrscanner.models.response.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.event.Level
import java.time.Instant

fun Application.configureMonitoring() {
    install(CallLogging) {
        level = Level.INFO
    }

    configureCors()

    install(StatusPages) {
        exception<ImageTooLargeException> { call, cause ->
            call.respond(HttpStatusCode.PayloadTooLarge, cause.toErrorResponse())
        }
        exception<UnsupportedImageFormatException> { call, cause ->
            call.respond(HttpStatusCode.UnsupportedMediaType, cause.toErrorResponse())
        }
        exception<NoBarcodeFoundException> { call, cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, cause.toErrorResponse())
        }
        exception<CorruptedImageException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.toErrorResponse())
        }
        exception<InvalidBase64Exception> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.toErrorResponse())
        }
        exception<BatchLimitExceededException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.toErrorResponse())
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "Internal server error",
                    code = "INTERNAL_ERROR",
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}

private fun AppException.toErrorResponse() = ErrorResponse(
    error = message,
    code = code,
    timestamp = Instant.now().toString()
)

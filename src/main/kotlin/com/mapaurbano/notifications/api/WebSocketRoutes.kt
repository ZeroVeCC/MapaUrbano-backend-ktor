package com.mapaurbano.notifications.api

import com.mapaurbano.notifications.application.EventBus
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.webSocketRoutes() {
    val eventBus by inject<EventBus>()
    val json = Json { encodeDefaults = true }

    webSocket("/ws/public") {
        val subscription = eventBus.subscribePublic().onEach { event ->
            val eventText = json.encodeToString(event)
            send(Frame.Text(eventText))
        }.launchIn(this)

        try {
            for (frame in incoming) {
                // Not expecting client messages for now
            }
        } finally {
            subscription.cancelAndJoin()
        }
    }

    authenticate("admin-session") {
        webSocket("/ws/admin") {
            val subscription = eventBus.subscribeAdmin().onEach { event ->
                val eventText = json.encodeToString(event)
                send(Frame.Text(eventText))
            }.launchIn(this)

            try {
                for (frame in incoming) {
                    // Not expecting client messages for now
                }
            } finally {
                subscription.cancelAndJoin()
            }
        }
    }
}

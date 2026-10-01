package com.mapaurbano.notifications.application

import com.mapaurbano.notifications.dto.WsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

class EventBus {
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 100)
    
    suspend fun publish(event: WsEvent) {
        _events.emit(event)
    }

    fun subscribeAdmin(): Flow<WsEvent> = _events.asSharedFlow()

    fun subscribePublic(): Flow<WsEvent> = _events.asSharedFlow().filter { event ->
        // No enviamos eventos internos como asignaciones o notas privadas al canal público
        event.type != "report.assignment_changed" && event.type != "error"
    }
}

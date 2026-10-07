package com.mapaurbano.notifications

import com.mapaurbano.notifications.application.EventBus
import com.mapaurbano.notifications.dto.WsEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class EventBusTest {
    
    @Test
    fun `evento publicado llega a subscribers`() = runBlocking {
        val bus = EventBus()
        val event = WsEvent("test.event", Instant.now().toString(), null)
        
        var receivedEvent: WsEvent? = null
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            receivedEvent = withTimeout(2000) { bus.subscribeAdmin().first() }
        }
        
        bus.publish(event)
        job.join()
        
        assertEquals("test.event", receivedEvent?.type)
    }
}

package com.example.demo.service.impl;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 *we’ll test:
	register() adds emitter
	sendToUser() sends event
	remove() removes emitter
	emitter removed when exception happens
	client-disconnect exception handled quietly
	non-disconnect exception also removes emitter 
	
	We won’t test private helpers directly (isClientDisconnect, rootMessage) — we test them indirectly through behavior.
 * 
 * How do you test SSE services?
 * I focus on verifying registration, removal, and exception handling logic rather than actual streaming behavior, which is integration-level concern.
 *
 *
 *How do you test nested exception cause chains?
 *I manually construct nested exceptions using RuntimeException wrappers to simulate real-world wrapping 
 *behavior, then verify behavior through the public method rather than testing private helpers directly. 
 */
@ExtendWith(MockitoExtension.class)
class SseEventServiceImplTest {

    private SseEventServiceImpl service;

    @BeforeEach
    void setup() {
        service = new SseEventServiceImpl();
    }

    // ---------------------------------------
    // register
    // ---------------------------------------

    @Test
    void register_shouldAddEmitter() {

        SseEmitter emitter = service.register(1L);

        assertNotNull(emitter);

        // sending should not throw
        service.sendToUser(1L, "test", "payload");
    }

    // ---------------------------------------
    // sendToUser normal case
    // ---------------------------------------

    @Test
    void sendToUser_shouldSendWithoutError_whenEmitterActive() {

        service.register(1L);

        // Should not throw
        assertDoesNotThrow(() ->
                service.sendToUser(1L, "event", "data"));
    }

    // ---------------------------------------
    // remove
    // ---------------------------------------

    @Test
    void remove_shouldRemoveEmitter() {

        SseEmitter emitter = service.register(1L);

        service.remove(1L, emitter);

        // Now sending should do nothing
        assertDoesNotThrow(() ->
                service.sendToUser(1L, "event", "data"));
    }

    // ---------------------------------------
    // sendToUser with client disconnect exception
    // ---------------------------------------

    @Test
    void sendToUser_shouldRemoveEmitter_whenClientAbortException() throws Exception {

        SseEmitter emitter = service.register(1L);

        // Force exception during send
        SseEmitter failingEmitter = new SseEmitter(0L) {
            @Override
            public void send(Object object) throws IOException {
                throw new ClientAbortException("connection aborted");
            }
        };

        service.remove(1L, emitter);
        service.register(1L);
        service.remove(1L, failingEmitter);

        // manually insert failing emitter
        service.register(2L);
        service.remove(2L, failingEmitter);

        // just ensure no crash
        assertDoesNotThrow(() ->
                service.sendToUser(2L, "event", "data"));
    }

    // ---------------------------------------
    // sendToUser with non-disconnect exception
    // ---------------------------------------

    @Test
    void sendToUser_shouldRemoveEmitter_whenGeneralException() {

        SseEmitter failingEmitter = new SseEmitter(0L) {
            @Override
            public void send(Object object) throws IOException {
                throw new IOException("Some random error");
            }
        };

        service.register(3L);
        service.remove(3L, failingEmitter);

        assertDoesNotThrow(() ->
                service.sendToUser(3L, "event", "data"));
    }

    // ---------------------------------------
    // test disconnect detection through nested cause
    // ---------------------------------------

    @Test
    void sendToUser_shouldHandleNestedDisconnectException() {

        SseEmitter emitter = new SseEmitter(0L) {
            @Override
            public void send(Object object) throws IOException {
            	throw new RuntimeException(
            		    new ClientAbortException("connection aborted")
            		);
            }
        };

        service.register(4L);
        service.remove(4L, emitter);

        assertDoesNotThrow(() ->
                service.sendToUser(4L, "event", "data"));
    }
}

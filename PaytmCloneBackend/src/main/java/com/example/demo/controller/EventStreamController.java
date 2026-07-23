package com.example.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.security.JwtUtil;
import com.example.demo.service.SseEventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class EventStreamController {
	
	private final JwtUtil jwtUtil;
	private final SseEventService sseEventService;
	
	/**
	 *SSE is defined as the long-lived HTTP GET that returns a stream. The client subscribes to the resource.
	 *MediaType.TEXT_EVENT_STREAM_VALUE is a Spring constant whose value is "text/event-stream", so both are same only.
	 */
	
	// This avoids the “EventSource can’t send Authorization header” problem by sending token in the request param.
	@GetMapping(value = "/api/v1/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestParam("token") String token) {

	    SseEmitter emitter = new SseEmitter(0L); // emitter with 0 timeout, only used in the failure cases, just to return emitter.

	    try {
	        if (token == null || token.isBlank()) {
	            emitter.complete(); // complete() closes the stream cleanly.
	            return emitter; // then returning the emitter ends the request.
	        }

	        if (jwtUtil.isExpired(token)) {
	            emitter.complete();
	            return emitter;
	        }
	        
	        // service stores emitters keyed by userId.
	        Long userId = jwtUtil.extractUserId(token);

	        // register real emitter from service
	        // for valid token using registered and never using "emitter".
	        SseEmitter registered = sseEventService.register(userId);

	        // send initial ping (optional)
	        try {
	        	// just to check connection has been made.
	            registered.send(SseEmitter.event().name("connected").data("ok"));
	        } catch (Exception ignored) {}

	        return registered; // emitter that’s stored + cleaned up by your service, this is your system managed one in the service.

	    } catch (Exception e) {
	        // IMPORTANT: don't let it go to GlobalExceptionHandler
	    	// Because if an exception hits global handler in SSE context, it may try to write JSON, which breaks with text/event-stream
	    	// but again we have added the exception handler there itself to manage that JSON thing.
	        emitter.complete();
	        return emitter;
	    }
	}

}

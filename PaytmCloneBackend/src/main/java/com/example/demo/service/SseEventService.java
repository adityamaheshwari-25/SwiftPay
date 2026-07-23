package com.example.demo.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


public interface SseEventService {
	SseEmitter register(Long userId);
	
	// Object gives you flexibility that you can send any DTO, anything basically you don't have to override any method then.
	void sendToUser(Long userId, String eventName, Object payload);
	void remove(Long userId, SseEmitter emitter);
}

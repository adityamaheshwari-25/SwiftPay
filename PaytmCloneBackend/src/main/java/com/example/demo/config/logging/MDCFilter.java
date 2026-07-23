package com.example.demo.config.logging;

import java.io.IOException;
import java.util.UUID;

import org.jboss.logging.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 *MDC stands for Mapped Diagnostic Context.
It’s a feature provided by logging frameworks like Log4j and Logback that allows us to store contextual information in a thread-local map so that it gets automatically included in all log statements executed by that thread.

In Spring Boot applications, we commonly use MDC to store request-scoped data such as correlation IDs, request IDs, user IDs, or tenant information. This is especially important in microservices architectures where a single request may pass through multiple services. By including a correlation ID in logs, we can trace the entire flow of a request across systems.

Typically, we implement this using an MDCFilter or a OncePerRequestFilter. The filter intercepts the incoming HTTP request, extracts or generates a correlation ID, puts it into the MDC using MDC.put(), and ensures it’s cleared using MDC.clear() in a finally block to prevent memory leaks.

Overall, MDC improves observability, debugging, and log traceability in distributed systems. 

Thread-Local Storage is a mechanism that allows us to store data that is accessible only to the current thread.
Each thread has its own independent copy of the variable.
In Java, Thread-Local Storage is implemented using the ThreadLocal class.
It allows us to create variables that are isolated per thread, meaning changes made by one thread are not visible to other threads.

Internally, each thread maintains its own ThreadLocalMap, which stores values associated with ThreadLocal objects.
In Spring Boot, since servlet containers use thread pools, threads are reused.
That’s why we must call MDC.clear() after request completion, otherwise data may leak into the next request handled by the same thread.
 */
@Component
@Order(1) // ensures this runs before any filters like security filters so even auth errors get a Trace Id.
public class MDCFilter implements Filter {
	
	private static final String TRACE_ID = "traceId";

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			
			// generate a unique 8-character ID
			String id = UUID.randomUUID().toString().substring(0, 8);
			
			// put it in the ThreadLocal Map (The Sticky Note kinda)
			MDC.put(TRACE_ID, id);
			
			// adding it to the Response Header (helps debug in Browser Devtools)
			if (response instanceof HttpServletResponse httpResponse) {
				httpResponse.setHeader("X-Trace-ID", id);
			}
			
			// let the request continue to the controller/next filter.
			chain.doFilter(request, response);
			
		} finally {
			
			// critical: clearing the sticky note when the request is done.
			// this prevents the ID from "leaking" into the next request.
			MDC.remove(TRACE_ID);
		}
		
	}
	
	
}

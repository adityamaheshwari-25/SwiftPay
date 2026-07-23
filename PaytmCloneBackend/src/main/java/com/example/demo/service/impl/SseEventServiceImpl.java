package com.example.demo.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.catalina.connector.ClientAbortException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.demo.service.SseEventService;

import lombok.extern.slf4j.Slf4j;

/**
 * SSE connections are long-lived; client disconnects are normal.

When server writes after disconnect, exceptions are thrown.

In Spring/Tomcat, disconnect exceptions vary and are often wrapped.

We traverse Throwable.getCause() chain to detect disconnect signatures.

We match known message patterns for broad exceptions like IOException and IllegalStateException.

We remove and complete the emitter to avoid repeated failures and memory leaks.

We log disconnects as debug/warn, not critical.
 */
@Slf4j
@Service
public class SseEventServiceImpl implements SseEventService {

	// ConcurrentHashMap has thread safety.
  private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
  
  /**
   *creates a new SseEmitter

	stores it in emitters map

	attaches onCompletion, onTimeout, onError callbacks to remove it
	
	sendToUser() iterates over the list often (read-heavy)
	register() / remove() happen on connect/disconnect (write)
	
	connect and disconnect happens often but now that often as the sendToUser events, so CopyOnWriteArrayList 
	is a reasonable option.
	
	ConcurrentHashMap is thread-safe because reads are mostly lock-free using volatile visibility, 
	writes use CAS and bucket-level synchronization, and resizing is cooperative using forwarding nodes. 
	CopyOnWriteArrayList gives safe iteration because iterators use a snapshot of the internal array; 
	modifications copy the entire array and replace the reference, so iteration never sees concurrent 
	structural changes.
   */
  @Override
  public SseEmitter register(Long userId) {
	  // if you set some timeout time, then server will stop that connection after that timeout time of inactivity.
    SseEmitter emitter = new SseEmitter(0L); // no timeout, keep the SSE connection open indefinitely
    
    
    /**
     *computeIfAbsent prevents race conditions when multiple threads register simultaneously. 
     *CopyOnWriteArrayList allows safe iteration during broadcasting without 
     *ConcurrentModificationException, ArrayList is not thread-safe, if any concurrent thread option is performed, it
     *will throw ConcurrentModificationException.
     *
     *There are other alternatives as well that you can use, depends on the trade offs and all, basically
     *system design thing, we have used because it has more 
     *
     *It sort of depends on how big the application is and what it uses more, so based on that will use that
     *data structure.
     *
     * CopyOnWriteArrayList is optimized for read-heavy workloads
     * CopyOnWriteArrayList basically iterates over the snapshot of the internal array so if any modifications happens in the 
     * internal array this ConcurrentModificationException exception doesn't take place.
     */
    emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    
    /*
     * onCompletion runs when the SSE request is completed normally meaning, client closes the connection
     * server calls emitter.complete and request finishes
     * */

    emitter.onCompletion(() -> remove(userId, emitter));
    emitter.onTimeout(() -> remove(userId, emitter));
    emitter.onError(ex -> remove(userId, emitter));

    return emitter;
  }

  @Override
  public void sendToUser(Long userId, String eventName, Object payload) {
    List<SseEmitter> userEmitters = emitters.get(userId);
    if (userEmitters == null || userEmitters.isEmpty()) return;

    for (SseEmitter emitter : userEmitters) {
      try {
    	  // server keeps pushing event to the client via this.
        emitter.send(SseEmitter.event().name(eventName).data(payload));
      } catch (Exception ex) {
        if (isClientDisconnect(ex)) {
        	// here you get disconnected due to some reason.
          log.debug("SSE client disconnected for userId={} - removing emitter", userId);
        } else {
        	// here you have some bug.
          log.warn("SSE send failed for userId={} - removing emitter. cause={}",
              userId, rootMessage(ex), ex);
        }
        remove(userId, emitter);
      }
    }
  }

  @Override
  public void remove(Long userId, SseEmitter emitter) {
    List<SseEmitter> list = emitters.get(userId);
    if (list == null) return;
    
    // we are removing just that emitter because for that device only the network got disconnected, not for other devices that the same user is on.
    list.remove(emitter);
    if (list.isEmpty()) emitters.remove(userId);

    // Ensure container resources are released; safe if already completed
    try { emitter.complete(); } catch (Exception ignored) {}
  }

  // The disconnect with the client is surfaced as many exceptions as noted below. So instead of just IOException these are also the exceptions that occur during disconnect.
  /*
   * This method basically checks for the disconnect be the reason of the exception, and in spring/spring boot 
   * exceptions are wrapped up a lot, so the exception that you can be facing can be deep down that's why using while loop.
   * For Example: in the console I have, 
   * IllegalStateException: Failed to send ...

		caused by HttpMessageNotWritableException

			caused by JacksonIOException

				caused by AsyncRequestNotUsableException

					caused by ClientAbortException

						caused by IOException: connection aborted	
						
		So the exception was deep down.
		while loop until you hit the root cause.
   * */
  private boolean isClientDisconnect(Throwable ex) {
    Throwable cur = ex;
    while (cur != null) {
      if (cur instanceof ClientAbortException) return true;
      if (cur instanceof AsyncRequestNotUsableException) return true;
      if (cur instanceof IOException && msgContains(cur, "aborted")) return true;
      if (cur instanceof IllegalStateException && msgContains(cur, "failed to send")) return true;
      cur = cur.getCause();
    }
    return false;
  }

  /**
   * For checking the required message with some general phrases like "Aborted" without case sensitivity
   * and checking with the null pointer exception.
   */
  private boolean msgContains(Throwable t, String needle) {
    String m = t.getMessage();
    return m != null && m.toLowerCase().contains(needle);
  }
  
  /**
   *Actually it goes to the most root cause, usually its the most informative cause for that exception to occur. 
   */
  private String rootMessage(Throwable t) {
    Throwable cur = t;
    Throwable prev = null;
    
    //exceptions are nested that's why while loop.
    // cur != prev => prevents infinite loops if some weird exception has a circular cause chain (rare, but possible). It’s an extra safety net.
    while (cur != null && cur != prev) {
      prev = cur;
      cur = cur.getCause();
    }
    return (prev != null && prev.getMessage() != null) ? prev.getMessage() : String.valueOf(t);
  }
}

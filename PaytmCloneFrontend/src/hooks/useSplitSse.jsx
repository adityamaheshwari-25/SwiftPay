import { useEffect, useRef } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/hooks/queries/queryKeys";

/**
 * 
 * what we are doing is just pushing the event not the whole data or source of truth from the backend, because sometimes that data 
 * can just be dropped. so making rest request after getting the event.
 * SSE is built on http only.
 * This is perfect for notification. 
 * 
 * You could have used simple fetch as well, but I didn't go with that because there's lot of boilerplate code that I have to write.
 */

export const useSplitSse = ({ enabled }) => {

  // qc is the central React Query cache client for managing the cache.
  const qc = useQueryClient();

  // esRef stores the EventSource instance so that we can got it without re-rendering stuff.
  // we cannot use useState because it causes re-renders, a ref is mutable and doesn't trigger UI updates.
  /**
   * useRef is used to store the mutable value without causing any re-render, we don't want multiple connections to be created that's
   * why not using useState(), as because on every re-render it can make new connection and attach events which will create a lot of bugs.
   * 
   * Only one connection is required for all of this, and EventSource is not the UI that we want to update that's also why.
   * 
   * Basically:
   * Also, refs help prevent this common bug:

      effect re-runs and creates a new EventSource

      you lose the reference to the old EventSource instance

      old one continues running in the background

      now you have multiple open connections


    Using a ref gives you a stable “handle” to the resource.  

    useRef is basically for the external resources(connections, timers, websockets, EventSource) that should not cause UI re-renders, like if the connection closes or opens it should 
    cause UI re-renders.

    useState is for UI state that should trigger rendering when it changes.


   */
  const esRef = useRef(null);

  
  useEffect(() => {

    // http only cookie works naturally with the Event source, no need of the token, that's the best option for that actually.
    const token = localStorage.getItem("authToken");
    if (!enabled || !token) return;

    // encodeURIComponent(token) prevents URL breaking and injection issues if token contains special chars.
    const base = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
    const sseUrl = `${base}/events/stream?token=${encodeURIComponent(token)}`;

    /**
     * Event source basically creates a GET request to the SSE endpoint, makes the connection open, establishes a network
     * it automatically reconnects if disconnected.
     */
    const es = new EventSource(sseUrl); // EventSource is the native library for the stream events in the browser.
    esRef.current = es;

    /**
     * 
     * The invalidation performs like that -> marks that data as stale, and then when next time its needed, it refeches it, or immediately if in use, 
     * depending on the config like the common ones refetchOnMount, refetchOnWindowFocus, refetchOnReconnect
     * 
     * We have manual cache mutation that is faster but its more complex, more prone to bugs and all, so invalidating is simple, correct
     * and fewer bugs.
     * 
     * So basically SSE sends some event as in the code below that something is changed, we use the invalidation strategy.
     */
    const invalidate = (splitId) => {
      qc.invalidateQueries({ queryKey: queryKeys.splits.created() });
      qc.invalidateQueries({ queryKey: queryKeys.splits.involved() });

      // currrently don't have the split specific page, so no need of this currently.
      // if (splitId) {
      //   qc.invalidateQueries({ queryKey: queryKeys.splits.details(splitId) });
      // }
    };

    // we are parsing because sse event data is always a string.
    const parseSplitId = (event) => {
      // If event data isn’t JSON or malformed, it prevents the hook from crashing the componen, that's why try/catch block.
      try {
        // console.log("Parsing split Id: ", event.type, event.data);

        // payload may be { splitId: 1 } OR { data: { splitId: 1 } }
        const payload = JSON.parse(event.data);
        return payload?.splitId ?? payload?.data?.splitId ?? null;
        // console.log(payload?.splitId)

        // optional chaining so it should not crash if the payload.data is undefined, that is there when the response is coming from the backend, as its async request.
        // return payload?.splitId ?? null;
      } catch {
        return null;
      }
    };

    // for visibility
    // onopen fires when connection established/re-established.
    es.onopen = () => console.log("SSE connected");
    // onerror fires when connection fails, EventSource auto-retries by default. So an “error” doesn’t mean it’s dead forever. It may reconnect.
    es.onerror = (e) => console.log("SSE error (auto-retry)", e);

    // my backend sends named events
    // these are basically called the named event listeners.
    // and this is just for like server has started sending events properly.
    es.addEventListener("connected", (e) => console.log("SSE connected payload:", e.data));

    // named event, this event has a name "split.created"
    es.addEventListener("split.created", (e) => {
      /**
       * e is the event object provided by the browsers for sse events, its usually a MessageEvent containing e.type...e.data and all. 
       */
      const splitId = parseSplitId(e);
      invalidate(splitId);
    });

    es.addEventListener("split.updated", (e) => {
      // e.data contains the json string.
      const splitId = parseSplitId(e);
      invalidate(splitId);
    });

    // fallback (in case any default events are sent)
    // this catches events without a name.
    es.onmessage = (e) => {
      console.log("SSE default message:", e.data);
    };

    /**
     *when dependency changes, it cleans up the things, we need cleanup because don't want to fill up the spaces that we don't want
     that can cause
     * */ 
    
    return () => {
      es.close(); // clean up
      esRef.current = null; // clean up
    };
  }, [enabled, qc]);
};

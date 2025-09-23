(ns ring.sse.protocols)


(defprotocol Listener
  "A protocol for handling SSE responses. The second argument is an object that 
   satisfies the SSESender protocol."
  (on-open [listener sse-sender] "Called when the SSE response is opened and ready."))


(defprotocol Sender
  "A protocol for sending SSE responses."
  (-send [sender message] "Sends a SSE message"))

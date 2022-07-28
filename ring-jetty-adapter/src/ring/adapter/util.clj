(ns ring.adapter.util
  ;; KeyStoreScanner has `LOG` which loads logging during compilation time.
  ;; This interrupts GraalVM compilation
  (:import org.eclipse.jetty.util.ssl.KeyStoreScanner))

(defn create-key-store-scanner [ssl-context scan-interval]
  (doto (KeyStoreScanner. ssl-context)
    (.setScanInterval scan-interval)))


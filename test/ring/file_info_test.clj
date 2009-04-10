(ns ring.file-info-test
  (:use clj-unit.core ring.file-info)
  (:import java.io.File))

(def non-file-app (wrap (constantly {:headers {} :body "body"})))

(def known-file (File. "test/ring/assets/plain.txt"))
(def known-file-app (wrap (constantly {:headers {} :body known-file})))

(def unknown-file (File. "test/ring/assets/random.xyz"))
(def unknown-file-app (wrap (constantly {:headers {} :body unknown-file})))

(def custom-type-app (wrap {"txt" "custom/type"}
                           (constantly {:headers {} :body known-file})))

(deftest "wrap: non-file response"
  (assert= {:headers {} :body "body"} (non-file-app {})))

(deftest "wrap: known file response"
  (assert=
    {:headers {"Content-Type" "text/plain" "Content-Length" "6"}
     :body    known-file}
    (known-file-app {})))

(deftest "wrap: unknown file resonse"
  (assert=
    {:headers {"Content-Type" "application/octet-stream" "Content-Length" "7"}
     :body    unknown-file}
   (unknown-file-app {})))

(deftest "wrap: custom mime types"
  (assert=
    {:headers {"Content-Type" "custom/type" "Content-Length" "6"}
     :body known-file}
    (custom-type-app {})))

(ns ring.util.test.response
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [ring.util.response :refer :all])
  (:import [java.io File InputStream]
           [org.apache.commons io.FileUtils]))

(deftest test-redirect
  (is (= {:status 302 :headers {"Location" "http://google.com"} :body ""}
         (redirect "http://google.com")))
  (are [x y] (= (->> x
                     (redirect "/foo")
                     :status)
                y)
       :moved-permanently 301
       :found 302
       :see-other 303
       :temporary-redirect 307
       :permanent-redirect 308
       300 300))

(deftest test-redirect-after-post
  (is (= {:status 303 :headers {"Location" "http://example.com"} :body ""}
         (redirect-after-post "http://example.com"))))

(deftest test-bad-request
  (is (= {:status 400 :headers {} :body "Bad Request"}
         (bad-request "Bad Request"))))

(deftest test-not-found
  (is (= {:status 404 :headers {} :body "Not found"}
         (not-found "Not found"))))

(deftest test-created
  (testing "with location and without body"
    (is (= {:status 201 :headers {"Location" "foobar/location"} :body nil}
           (created "foobar/location"))))
  (testing "with body and with location"
    (is (= {:status 201 :headers {"Location" "foobar/location"} :body "foobar"}
           (created "foobar/location" "foobar")))))

(deftest test-response
  (is (= {:status 200 :headers {} :body "foobar"}
         (response "foobar"))))

(deftest test-status
  (is (= {:status 200 :body ""} (status {:status nil :body ""} 200))))

(deftest test-content-type
  (is (= {:status 200 :headers {"Content-Type" "text/html" "Content-Length" "10"}}
         (content-type {:status 200 :headers {"Content-Length" "10"}}
                       "text/html"))))

(deftest test-charset
  (testing "add charset"
    (is (= (charset {:status 200 :headers {"Content-Type" "text/html"}} "UTF-8")
           {:status 200 :headers {"Content-Type" "text/html; charset=UTF-8"}})))
  (testing "replace existing charset"
    (is (= (charset {:status 200 :headers {"Content-Type" "text/html; charset=UTF-16"}}
                    "UTF-8")
           {:status 200 :headers {"Content-Type" "text/html; charset=UTF-8"}})))
  (testing "default content-type"
    (is (= (charset {:status 200 :headers {}} "UTF-8")
           {:status 200 :headers {"Content-Type" "text/plain; charset=UTF-8"}})))
  (testing "case insensitive"
    (is (= (charset {:status 200 :headers {"content-type" "text/html"}} "UTF-8")
           {:status 200 :headers {"content-type" "text/html; charset=UTF-8"}}))))

(deftest test-get-charset
  (testing "simple charset"
    (is (= (get-charset {:headers {"Content-Type" "text/plain; charset=UTF-8"}})
           "UTF-8")))
  (testing "case insensitive"
    (is (= (get-charset {:headers {"content-type" "text/plain; charset=UTF-16"}})
           "UTF-16")))
  (testing "missing charset"
    (is (nil? (get-charset {:headers {"Content-Type" "text/plain"}}))))
  (testing "missing content-type"
    (is (nil? (get-charset {:headers {}})))))

(deftest test-header
  (is (= {:status 200 :headers {"X-Foo" "Bar"}}
         (header {:status 200 :headers {}} "X-Foo" "Bar"))))

(deftest test-response?
  (is (response? {:status 200, :headers {}}))
  (is (response? {:status 200, :headers {}, :body "Foo"}))
  (is (not (response? {})))
  (is (not (response? {:users []}))))

(defmacro with-classloader
  "Temporarily replaces the current context classloader with one that
   includes everything in dir"
  [[dir] & forms]
  `(let [current-thread# (Thread/currentThread)
         original-loader# (.getContextClassLoader current-thread#)
         new-loader# (java.net.URLClassLoader. (into-array [(.toURL ~dir)])
                                               original-loader#)]
     (try
        (.setContextClassLoader current-thread# new-loader#)
        ~@forms
        (finally
          (.setContextClassLoader current-thread# original-loader#)))))

(defn- make-jar-url [jar-path res-path]
  (io/as-url (str "jar:file:" jar-path "!/" res-path)))

(deftest test-url-response
  (testing "resources from a jar file"
    (let [base-path (.getPath (io/resource "ring/assets/test-resource.jar"))
          root-url (make-jar-url base-path "public/")
          empty-resource (make-jar-url base-path "public/empty-resource")
          non-empty-resource (make-jar-url base-path "public/hi-resource")]
      (is (nil? (url-response root-url)))
      (is (slurp (:body (url-response empty-resource))) "")
      (is (slurp (:body (url-response non-empty-resource))) "hi"))))

(deftest test-resource-response
  (testing "response map"
    (let [resp (resource-response "/ring/assets/foo.html")]
      (is (= (resp :status) 200))
      (is (= (into #{} (keys (resp :headers))) #{"Content-Length" "Last-Modified"}))
      (is (= (slurp (resp :body)) "foo"))))

  (testing "with root option"
    (let [resp (resource-response "/foo.html" {:root "/ring/assets"})]
      (is (= (slurp (resp :body)) "foo"))))

  (testing "with child class-loader"
    (let [resource (File/createTempFile "response_test" nil)]
      (FileUtils/writeStringToFile resource "just testing")
      (with-classloader [(.getParentFile resource)]
        (let [resp (resource-response (.getName resource))]
          (is (= (slurp (resp :body))
                 "just testing"))))))

  (testing "missing resource"
    (is (nil? (resource-response "/missing/resource.clj"))))

  (testing "response body type"
    (let [body (:body (resource-response "ring/util/response.clj"))]
      (is (instance? File body))
      (is (.startsWith (slurp body) "(ns ring.util.response")))
    (let [body (:body (resource-response "clojure/java/io.clj"))]
      (is (instance? InputStream body))
      (is (.contains (slurp body) "clojure.java.io"))))

  (testing "resource is a directory"
    (is (nil? (resource-response "/ring/assets"))))

  (testing "resource is a directory in a jar file"
    (is (nil? (resource-response "/clojure/lang"))))

  (testing "resource is a directory in a jar file with a trailing slash"
    (is (nil? (resource-response "/clojure/lang/"))))

  (testing "resource is a file with spaces in path"
    (let [resp (resource-response "/ring/assets/hello world.txt")]
      (is (= (:body resp)
             (.getAbsoluteFile (File. "test/ring/assets/hello world.txt"))))
      (is (= (slurp (:body resp))
             "Hello World\n"))))

  (testing "nil resource-data values"
    (defmethod resource-data :http [_] {})
    (with-redefs [io/resource (constantly (java.net.URL. "http://foo"))]
      (is (= (response nil) (resource-response "whatever")))))

  (testing "resource path cannot contain '..'"
    (is (nil? (resource-response "../util/response.clj" {:root "ring/assets"})))
    (is (nil? (resource-response "../../util/response.clj"
                                 {:root "ring/assets/bars"
                                  :allow-symlinks? true}))))

  (testing "resource response doesn't follow symlinks by default"
    (is (nil? (resource-response "backlink/foo.html" {:root "ring/assets/bars"})))
    (is (nil? (resource-response "backlink/bars.txt" {:root "ring/assets/bars"}))))

  (testing "resource response can optionally follows symlinks"
    (let [resp (resource-response "backlink/foo.html"
                                  {:root "ring/assets/bars"
                                   :allow-symlinks? true})]
      (is (= (resp :status) 200))
      (is (= (into #{} (keys (resp :headers))) #{"Content-Length" "Last-Modified"}))
      (is (= (get-in resp [:headers "Content-Length"]) "3"))
      (is (= (slurp (resp :body)) "foo"))))

  (comment
    ;; This test requires the ability to have file names in the source
    ;; tree with non-ASCII characters in them encoded as UTF-8.  That
    ;; may be platform-specific.  Comment out for now.

    ;; If this fails on Mac OS X, try again with the command line:
    ;; LC_CTYPE="UTF-8" lein test
    (testing "resource is a file with UTF-8 characters in path"
      (let [resp (resource-response "/ring/assets/abcíe.txt")]
        (is (= (:body resp)
               (.getAbsoluteFile (File. "test/ring/assets/abcíe.txt"))))
        (is (.contains (slurp (:body resp)) "UTF-8"))))))

(deftest test-file-response
  (testing "response map"
    (let [resp (file-response "foo.html" {:root "test/ring/assets"})]
      (is (= (resp :status) 200))
      (is (= (into #{} (keys (resp :headers))) #{"Content-Length" "Last-Modified"}))
      (is (= (get-in resp [:headers "Content-Length"]) "3"))
      (is (= (slurp (resp :body)) "foo"))))

  (testing "file path cannot contain '..' "
    (is (nil? (file-response "../../../project.clj" {:root "test/ring/assets"})))
    (is (nil? (file-response "../../../project.clj" {:root "test/ring/assets/bars" :allow-symlinks? true}))))

  (testing "file response optionally follows symlinks"
    (let [resp (file-response "backlink/foo.html" {:root "test/ring/assets/bars" :allow-symlinks? true})]
      (is (= (resp :status) 200))
      (is (= (into #{} (keys (resp :headers))) #{"Content-Length" "Last-Modified"}))
      (is (= (get-in resp [:headers "Content-Length"]) "3"))
      (is (= (slurp (resp :body)) "foo")))

    (is (nil? (file-response "backlink/foo.html" {:root "test/ring/assets/bars"})))
    (is (nil? (file-response "backlink/bars.txt" {:root "test/ring/assets/bars"})))))

(deftest test-set-cookie
  (is (= {:status 200 :headers {} :cookies {"Foo" {:value "Bar"}}}
         (set-cookie {:status 200 :headers {}}
                     "Foo" "Bar")))
  (is (= {:status 200 :headers {} :cookies {"Foo" {:value "Bar" :http-only true}}}
         (set-cookie {:status 200 :headers {}}
                     "Foo" "Bar" {:http-only true}))))

(deftest test-find-header
  (is (= (find-header {:headers {"Content-Type" "text/plain"}} "Content-Type")
         ["Content-Type" "text/plain"]))
  (is (= (find-header {:headers {"content-type" "text/plain"}} "Content-Type")
         ["content-type" "text/plain"]))
  (is (= (find-header {:headers {"Content-typE" "text/plain"}} "content-type")
         ["Content-typE" "text/plain"]))
  (is (nil? (find-header {:headers {"Content-Type" "text/plain"}} "content-length"))))

(deftest test-get-header
  (is (= (get-header {:headers {"Content-Type" "text/plain"}} "Content-Type")
         "text/plain"))
  (is (= (get-header {:headers {"content-type" "text/plain"}} "Content-Type")
         "text/plain"))
  (is (= (get-header {:headers {"Content-typE" "text/plain"}} "content-type")
         "text/plain"))
  (is (nil? (get-header {:headers {"Content-Type" "text/plain"}} "content-length"))))

(deftest test-update-header
  (is (= (update-header {:headers {"Content-Type" "text/plain"}}
                        "content-type"
                        str "; charset=UTF-8")
         {:headers {"Content-Type" "text/plain; charset=UTF-8"}}))
  (is (= (update-header {:headers {}}
                        "content-type"
                        str "; charset=UTF-8")
         {:headers {"content-type" "; charset=UTF-8"}})))

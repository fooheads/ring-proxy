(ns fooheads.ring-proxy
  (:require
    [clj-http.client :as client]
    [clj-http.conn-mgr :as conn]
    [clj-http.core :as core]
    [clojure.string :as str]
    [ring.util.request :refer [body-string]])
  (:import
      [java.net URI]))


(defn- map-keys
  "Apply f to all keys in m"
  [f m]
  (reduce-kv (fn [m k v] (assoc m (f k) v)) {} m))


(defn prepare-cookies
  "Removes the :domain and :secure keys and converts the :expires key (a Date)
  to a string in the ring response map resp. Returns resp with cookies properly
  munged."
  [resp]
  (let [prepare #(-> (update-in % [1 :expires] str)
                     (update-in [1] dissoc :domain :secure))]
    (assoc resp :cookies (into {} (map prepare (:cookies resp))))))


(defn- fix-headers
  "Remove the content-length header from the response.
  That header seems to be broken depending on whether the
  response is chunked or not."
  [resp]
  (update resp :headers (fn [headers]
                          (->
                            (map-keys str/lower-case headers)
                            (dissoc "content-length"
                                    "transfer-encoding")))))


(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))


(defn proxy-handler [remote-base-uri conn-opts & [http-opts]]
  (let [cm (conn/make-reusable-conn-manager conn-opts)
        hclient (core/build-http-client
                  {:max-redirects 0 :redirect-strategy :none}
                  false cm)]

    (fn [req]
      (let [rmt-full   (URI. (str remote-base-uri "/"))
            rmt-path   (URI. (.getScheme    rmt-full)
                             (.getAuthority rmt-full)
                             (.getPath      rmt-full) nil nil)
            lcl-path   (:uri req)
            remote-uri (str (.resolve ^java.net.URI rmt-path
                                      ^String lcl-path))
            query-string (:query-string req)
            url (if query-string (str remote-uri "?" query-string) remote-uri)

            body (body-string req)

            request
            (merge
              {:method (:request-method req)
               :url url

               :headers
               (merge
                 (dissoc (map-keys str/lower-case (:headers req))
                         "host"
                         "content-length"
                         "transfer-encoding")
                 {"referer" remote-uri})

               :throw-exceptions false
               :body body
               :as :stream
               :connection-manager cm
               :http-client hclient} http-opts)]
        (->
          request
          client/request
          ;prepare-cookies
          fix-headers)))))

(comment

  (fix-headers {:body "hej" :headers {"CONTENT-LENGTH" 497
                                      "Content-Type" "text/html"}}))


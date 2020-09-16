(ns fooheads.ring-proxy
  (:require
    [clj-http.client :as client]
    [clj-http.conn-mgr :as conn]
    [clj-http.core :as core])
  (:import
      [java.net URI]))

(defn prepare-cookies
  "Removes the :domain and :secure keys and converts the :expires key (a Date)
  to a string in the ring response map resp. Returns resp with cookies properly
  munged."
  [resp]
  (let [prepare #(-> (update-in % [1 :expires] str)
                     (update-in [1] dissoc :domain :secure))]
    (assoc resp :cookies (into {} (map prepare (:cookies resp))))))

(defn fix-headers [resp]
  (update resp :headers dissoc "Transfer-Encoding"))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn proxy-handler [remote-base-uri & [http-opts]]
  (let [cm (conn/make-reusable-conn-manager {:threads 10 :default-per-route 10})
        hclient (core/build-http-client {} false cm)]

    (fn [req]
      (let [rmt-full   (URI. (str remote-base-uri "/"))
            rmt-path   (URI. (.getScheme    rmt-full)
                             (.getAuthority rmt-full)
                             (.getPath      rmt-full) nil nil)
            lcl-path   (:uri req)
            remote-uri (str (.resolve rmt-path lcl-path))
            query-string (:query-string req)
            url (if query-string (str remote-uri "?" query-string) remote-uri)
            request (merge {:method (:request-method req)
                            :url url
                            :headers (merge (dissoc (:headers req) "host" "content-length") {"referer" remote-uri})
                            ;:body (if-let [len (get-in req [:headers "content-length"])]
                            ;        (slurp-binary (:body req) (Integer/parseInt len))
                            :body (:body req)
                            :follow-redirects true
                            :throw-exceptions false
                            :as :stream
                            :connection-manager cm
                            :http-client hclient} http-opts)]
        (->
          request
          client/request
          ;prepare-cookies
          fix-headers)))))



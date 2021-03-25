(ns fooheads.proxy
  (:require
    [clojure.string :as str]
    [fooheads.ring-proxy :refer [proxy-handler]]
    [org.httpkit.server :as http]))


(defn- args->map [args]
  (->>
    args
    (map read-string)
    (partition 2)
    (map vec)
    (map
      (fn [[k v]]
        [(-> k (str/replace #"^--" "") (keyword))
         v]))
    (into {})))


(defn logger
  ([typ message]
   (println typ ": " message))
  ([typ message ex]
   (println typ ": " message "\n" (str ex))))


(defn -main [& args]
  (prn (args->map args))
  (let [args (args->map args)
        conn-threads (or (:conn-threads args) 200)
        conn-opts {:threads conn-threads :default-per-route conn-threads}

        handler (proxy-handler (:remote-base-uri args) conn-opts)

        server-port (or (:port args) 8888)
        server-threads (or (:threads args) 200)
        server-opts {:port server-port :thread server-threads}

        server-opts (if (:logging args)
                      (merge {:error-logger (partial logger "ERROR")
                              :warn-logger  (partial logger "WARN")
                              :event-logger (partial logger "EVENT")}
                             server-opts)
                      server-opts)]

    (prn "server-opts" server-opts)
    (http/run-server handler server-opts)))

(comment
  (def stop (-main "--port" "8888"
                   "--threads" "200"
                   "--remote-base-uri" "http://localhost:7777"
                   "--logging" "true"))
  (stop))



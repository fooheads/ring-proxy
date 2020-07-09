(ns user 
  (:require
    [fooheads.ring-proxy :refer [proxy-handler]]
    [org.httpkit.server :as http]))

#_(def handler (proxy-handler "https://www.digirop.com"))
(def handler (proxy-handler "https://winrop.com"))
(def handler (proxy-handler "https://p95mwukoya.execute-api.eu-west-1.amazonaws.com"))

(def stop (http/run-server handler {:port 9999}))

(stop)

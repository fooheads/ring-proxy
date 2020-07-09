(ns user 
  (:require
    [fooheads.ring-proxy :refer [proxy-handler]]
    [org.httpkit.server :as http]))

(def handler (proxy-handler "https://gp.se"))

(def stop (http/run-server handler {:port 9999}))

(stop)

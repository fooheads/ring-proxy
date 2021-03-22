(ns user
  (:require
    [fooheads.ring-proxy :refer [proxy-handler]]
    [org.httpkit.server :as http]))


; :threads and :default-per-route (num connections per route)
; should be configured for the proxy-handler.
(def handler (proxy-handler "http://localhost:7777"
                            {:threads 200 :default-per-route 200}))


(comment
  ; Don't forget to think through num threads (:thread in singular
  ; in httpkit's case) for the http server running the proxy.

  (def stop (http/run-server handler {:port 9999 :thread 200}))

  (stop))

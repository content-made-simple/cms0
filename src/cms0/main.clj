(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]))

(defn ->content []
  (-> (response/file-response "resources/public/2025-01-06-20-11-06.mp4")
      (response/content-type "video/mp4")))

(defn show-content []
  (-> "<!doctype html><html><body><video src = \"/content/1\" controls=\"true\" preload=\"metadata\"></video></body></html>"
      (response/response)
      (response/content-type "text/html")))

(defn list-content []
  (-> "<!doctype html><html><body><ul><li><a href=\"/show-content/1\">First video</a></li></ul></body></html>"
      (response/response)
      (response/content-type "text/html")))

(defn handler [{:keys [uri] :as req}]
  (cond
    (= "/content/1" uri) (->content)
    (= "/show-content/1" uri) (show-content)
    :else (list-content)))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)

  (def server (start-server 3000 ))
  (stop-server server)
  )

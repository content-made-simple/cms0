(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]))

(defn ->content [{:keys [content-type filename] :as _foo}]
  (-> (response/file-response filename)
      (response/content-type content-type)))

(defn render [h]
  (-> h
      (hiccup/html)
      (str)
      (response/response)
      (response/content-type "text/html")))

(defn show-content [{:keys [id] :as _foo}]
  (-> [:html [:body [:video {:src (str "/content/" id)
                             :controls true
                             :preload "metadata"}]]]
      (render)))

(defn bar [{:keys [id title] :as _foo}]
  [:li [:a {:href (str "/show-content/" id)} title]])

(defn list-content [foos]
  (-> [:html [:body [:ul (map bar foos)]]]
      (render)))

(defonce db (atom [{:content-type "video/mp4"
                    :filename "resources/public/2025-01-06-20-11-06.mp4"
                    :title "Episode 1"
                    :id 1}]))

(defn handler [{:keys [uri] :as req}]
  (cond
    (= "/content/1" uri) (->content (first @db))
    (= "/show-content/1" uri) (show-content (first @db))
    (= "/content/2" uri) (->content (second @db))
    (= "/show-content/2" uri) (show-content (second @db))

    :else (list-content @db)))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)

  (def server (start-server 3000))
  (stop-server server)
  (swap! db conj {:content-type "video/mp4"
                  :filename "resources/public/episode-2.mp4"
                  :title "Episode 2"
                  :id 2})

  ,)

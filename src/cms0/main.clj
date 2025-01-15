(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]
            [ring.middleware.multipart-params :as multipart]
            [clojure.java.io :as io]))

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
  [:html [:body [:video {:src (str "/content/" id)
                         :controls true
                         :preload "metadata"}]]])

(defn bar [{:keys [id title] :as _foo}]
  [:li [:a {:href (str "/show-content/" id)} title]])

(defn list-content [foos]
  [:html [:body [:ul (map bar foos)]]])

(def upload-form
  [:html
   [:body
    [:form {:method "post"
            :action "/upload-content"
            :enctype "multipart/form-data"}
     [:input {:type "file"
              :name "file"}]
     [:br]
     [:input {:type "text"
              :name "title"}]
     [:br]
     [:input {:type "submit" :name "Upload"}]
     ]]])

(defonce db (atom [{:content-type "video/mp4"
                    :filename "resources/public/2025-01-06-20-11-06.mp4"
                    :title "Episode 1"
                    :id 1}]))

(defn handle-upload [req]
  (let [{:keys [multipart-params] :as req}
        (multipart/multipart-params-request req {})
        file (get multipart-params "file")
        id (->> @db (map :id) (apply max) inc)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)]
    (io/copy (:tempfile file) destination)
    (swap! db conj
           {:content-type (:content-type file)
            :title (get multipart-params "title")
            :filename path
            :id id})))

(defn handler [{:keys [uri] :as req}]
  (cond
    (= "/content/1" uri) (->content (first @db))
    (= "/show-content/1" uri) (render (show-content (first @db)))
    (= "/content/2" uri) (->content (second @db))
    (= "/show-content/2" uri) (render (show-content (second @db)))
    (= "/content/3" uri) (->content (nth @db 2))
    (= "/show-content/3" uri) (render (show-content (nth @db 2)))

    (= "/upload-form" uri) (render upload-form)
 #_#_   (= "/upload-content" uri) (do (handle-upload req)
                                  (response/redirect "/"))

    :else (render (list-content @db))))

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

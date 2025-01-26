(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]
            [ring.middleware.multipart-params :as multipart]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [duratom.core :as duratom]))

(defn ->content [{:keys [content-type filename] :as _foo}]
  (-> (response/file-response filename)
      (response/content-type content-type)))

(defn render [h]
  (-> h
      (hiccup/html)
      (str)
      (response/response)
      (response/content-type "text/html")))

(defn video [{:keys [id] :as _foo}]
  [:video {:src (str "/content/" id)
           :controls true
           :preload "metadata"}])

(defn page [body]
  [:html
   [:body
    body]])

(defn bar [{:keys [id title] :as _foo}]
  [:li [:a {:href (str "/video/" id)} title]])

(defn home [foos]
  [:ul (map bar foos)])

(def upload-form
  [:form {:method "post"
          :action "/upload-content"
          :enctype "multipart/form-data"}
   [:input {:type "file"
            :name "file"}]
   [:br]
   [:input {:type "text"
            :name "title"}]
   [:br]
   [:input {:type "submit" :name "Upload"}]])

(defonce db (duratom/duratom :local-file
                             :file-path "resources/db"
                             :init []))

(defn next-id [db]
  (->> db (map :id) (apply max) inc))

(defn handle-upload [req]
  (let [{:keys [multipart-params] :as req}
        (multipart/multipart-params-request req {})
        file (get multipart-params "file")
        id (next-id @db)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)]
    (io/copy (:tempfile file) destination)
    (swap! db conj
           {:content-type (:content-type file)
            :title (get multipart-params "title")
            :filename path
            :id id})))

(defn ->id [uri]
  (-> uri (str/split #"/") last parse-long))

(defn by-id [db id]
  (nth db (dec id)))

(defn handler [{:keys [uri] :as req}]
  (cond
    (str/starts-with? uri "/content/") (->content (by-id @db (->id uri)))
    (str/starts-with? uri "/video/") (render (page (video (by-id @db (->id uri)))))

    (= "/upload-form" uri) (render (page upload-form))
    #_#_(= "/upload-content" uri) (do (handle-upload req)
                                      (response/redirect "/"))

    :else (render (page (home @db)))))

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

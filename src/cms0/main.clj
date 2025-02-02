(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.params :as params]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [duratom.core :as duratom])
  (:import (java.security MessageDigest)
           (java.util HexFormat)))

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
           :width "100%"
           :controls true
           :preload "metadata"}])

(defn page [body]
  [:html
   [:head]
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

(def signup-form
  [:form {:method "post"
          :action "/signup"}
   [:br]
   [:input {:type "text"
            :name "email"}]
   [:br]
   [:input {:type "submit" :name "Signup"}]])

(defn token [{:keys [email token] :as _user}]
  [:div
   (str "Your token is " token " put it somewhere safe")])

(defonce db (duratom/duratom :local-file
                             :file-path "resources/db"
                             :init []))


(defn next-id [db collection]
  (or (some->> db collection (map :id) seq (apply max) inc)
      0))

(defn handle-upload [req]
  (let [{:keys [multipart-params] :as req}
        (multipart/multipart-params-request req {})
        file (get multipart-params "file")
        id (next-id @db :content)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)]
    (io/copy (:tempfile file) destination)
    (swap! db update :content conj
           {:content-type (:content-type file)
            :title (get multipart-params "title")
            :filename path
            :id id})))

(defn ->id [uri]
  (-> uri (str/split #"/") last parse-long))

(defn bytes->hexstring [^bytes ba]
  (-> (HexFormat/of)
      .withUpperCase
      (.formatHex ba)))

(defn string->digest [^String s]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (->> s .getBytes (.digest digest))))

(defn ->token [edn]
  (-> edn
      pr-str
      string->digest
      bytes->hexstring))

(defn handle-signup [req]
  (let [{:keys [form-params]} (params/assoc-form-params req "UTF-8")
        email (get form-params "email")
        id (next-id @db :user)
        timestamp (inst-ms (java.util.Date.))
        user {:id id
              :email email
              :token (->token [email timestamp])}]
    (swap! db update :user conj user)
    user))

(defn content-by-id [db id]
  (nth (:content db) (dec id)))

(defn handler [{:keys [uri] :as req}]
  (cond
    (str/starts-with? uri "/content/") (->content (content-by-id @db  (->id uri)))
    (str/starts-with? uri "/video/") (render (page (video (content-by-id @db  (->id uri)))))

    (= "/upload-form" uri) (render (page upload-form))
    (= "/signup-form" uri) (render (page signup-form))
    (= "/signup" uri)  (render (page (token (handle-signup req))))

    #_#_(= "/upload-content" uri) (do (handle-upload req)
                                      (response/redirect "/"))
    :else (render (page (home (:content @db))))))

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

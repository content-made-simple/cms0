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
   [:div "title" [:input {:type "text"
                          :name "title"}]]

   [:div "email" [:input {:type "text"
                          :name "email"}]]

   [:div "token" [:input {:type "text"
                          :name "token"}]]
   [:br]
   [:input {:type "submit" :value "Upload"}]])

(def signup-form
  [:form {:method "post"
          :action "/signup"}
   [:div "email"
    [:input {:type "text"
             :name "email"}]]
   [:br]
   [:input {:type "submit" :name "Signup"}]])

(def reset-token-form
  [:form {:method "post"
          :action "/reset-token"}
   [:div "email" [:input {:type "text"
                          :name "email"}]]
   [:div "token" [:input {:type "text"
                          :name "token"}]]
   [:br]
   [:input {:type "submit" :name "Signup"}]])

(defn token [{:keys [email token] :as _user}]
  [:div
   (str "Your token is " token " put it somewhere safe")])

(defn reset-token [user]
  (if user
    [:div
     (str "Your token is " (:token user) " put it somewhere safe")]
    [:div
     "You're cheating"]))

(defonce db (duratom/duratom :local-file
                             :file-path "resources/db"
                             :init []))

(defn next-id [db collection]
  (or (some->> @db collection (map :id) seq (apply max) inc)
      0))

(defn by-email [email user]
  (= (:email user) email))

(defn by-token [token user]
  (= (:token user) token))

(defn by-id [id x]
  (= (:id x) id))

(defn by-token-and-email [token email user]
  (and (by-token token user)
       (by-email email user)))

(defn query [db collection query-fn]
  (->> @db
       collection
       (filter query-fn)))

(defn query-one [db collection query-fn]
  (first (query db collection query-fn)))

(defn content-by-id [db id]
  (query-one db :content (partial by-id id)))

(defn transact! [db collection f x]
  (swap! db update collection f x))

(defn handle-upload [req]
  (let [{:keys [multipart-params] :as req} (multipart/multipart-params-request req)
        {:strs [file email token]} multipart-params
        id (next-id db :content)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)
        user (query-one @db :user (partial by-token-and-email token email))]
    (if user
      (do (io/copy (:tempfile file) destination)
          (transact! db :content conj {:content-type (:content-type file)
                                       :title (get multipart-params "title")
                                       :filename path
                                       :user-id (:id user)
                                       :id id}))
      {::errors {:status 403
                 :body "Unknown user"}})))

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

(defn handle-signup [{:keys [db timestamp] :as req}]
  (let [{:keys [form-params]} (params/assoc-form-params req "UTF-8")
        email (get form-params "email")
        id (next-id db :user)
        user {:id id
              :email email
              :token (->token [email timestamp])}]
    (transact! db :user user conj)
    user))

(defn replace [xs to-replace]
  (mapv (fn [x]
          (if (= (:id x) (:id to-replace))
            to-replace
            x)) xs))

(defn handle-reset-token [{:keys [db timestamp] :as req}]
  (let [{:keys [form-params]} (params/assoc-form-params req "UTF-8")
        email (get form-params "email")
        token (get form-params "token")]
    (when-let [user (query-one db :user (partial by-token-and-email token email))]
      (let [new-user (assoc user :token (->token [email timestamp]))]
        (transact! db :user replace new-user)
        new-user))))


(defn handle-errors [errors]
  errors)

(defn error [{:keys [query-string] :as req}]
  [:h1 query-string])

(defn handler [{:keys [uri] :as req}]
  (let [req (assoc req
                   :timestamp (inst-ms (java.util.Date.))
                   :db db)]
    (cond
      (str/starts-with? uri "/content/") (->content (content-by-id db (->id uri)))
      (str/starts-with? uri "/video/") (render (page (video (content-by-id db (->id uri)))))

      (= "/upload-form" uri) (render (page upload-form))
      (= "/signup-form" uri) (render (page signup-form))
      (= "/signup" uri)  (render (page (token (handle-signup req))))
      (= "/reset-token-form" uri) (render (page reset-token-form))
      (= "/reset-token" uri) (render (page (reset-token (handle-reset-token req))))

      (= "/upload-content" uri) (let [upload-result (handle-upload req)]
                                  (if-let [errors  (::errors upload-result)]
                                    (response/redirect-after-post "/error?Unauthenticated")
                                    (response/redirect-after-post "/")))
      (= "/error" uri) (render (page (error req)))
      :else (render (page (home (:content @db)))))))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)
  (:user @db)

  (def server (start-server 3000))
  (stop-server server)
  (swap! db conj {:content-type "video/mp4"
                  :filename "resources/public/episode-2.mp4"
                  :Title "Episode 2"
                  :id 2})
  (swap! db assoc-in [:content 5 :content-type] "video/mp4"))

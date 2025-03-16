(ns cms0.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.util.response :as response]
            [hiccup2.core :as hiccup]
            [ring.middleware.multipart-params :as multipart]
            [ring.middleware.params :as params]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cms0.ui :as ui]
            [cms0.db :as db])
  (:import (java.security MessageDigest)
           (java.util HexFormat)))

(defn ->content [{:keys [content-type filename]}]
  (-> (response/file-response filename)
      (response/content-type content-type)))

(defn render [h]
  (-> h
      (hiccup/html)
      (str)
      (response/response)
      (response/content-type "text/html")))

(defn by-email [email user]
  (= (:email user) email))

(defn by-token [token user]
  (= (:token user) token))

(defn by-id [id x]
  (= (:id x) id))

(def all (constantly true))

(defn by-token-and-email [token email user]
  (and (by-token token user)
       (by-email email user)))

(defn content-by-id [db id]
  (db/query-one db :content (partial by-id id)))

(defn handle-upload [{:keys [user] :as req}]
  (let [{:keys [multipart-params db] :as _req} (multipart/multipart-params-request req)
        {:strs [file]} multipart-params
        id (db/next-id db :content)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)]
    (if user
      (do (io/copy (:tempfile file) destination)
          (db/transact! db :content conj {:content-type (:content-type file)
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
        id (db/next-id db :user)
        user {:id id
              :email email
              :token (->token [email timestamp])}]
    (db/transact! db :user user conj)
    user))

(defn user-authenticate [db email token]
  (db/query-one db :user (partial by-token-and-email token email)))

(defn handle-reset-token [{:keys [db timestamp user] :as req}]
  (let [{:keys [form-params]} (params/assoc-form-params req "UTF-8")
        email (get form-params "email")]
    (when user
      (let [new-user (assoc user :token (->token [email timestamp]))]
        (db/transact! db :user db/replace new-user)
        new-user))))

(defn handle-errors [errors]
  errors)


;; TODO
;; - review the handlers
;; - basic testing
(defn handler* [{:keys [uri db] :as req}]
  (let [{:keys [form-params]} (params/assoc-form-params req "UTF-8")
        email (get form-params "email")
        token (get form-params "token")
        user (user-authenticate db email token)
        req (assoc req :user user)]
    (cond
      (str/starts-with? uri "/content/") (->content (content-by-id db (->id uri)))
      (str/starts-with? uri "/video/") (render (ui/page (ui/video (content-by-id db (->id uri)))))

      (= "/upload-form" uri) (render (ui/page ui/upload-form))
      (= "/signup-form" uri) (render (ui/page ui/this-is-a-beta #_signup-form))
      (= "/signup" uri)  (render (ui/page ui/this-is-a-beta #_(token-message (handle-signup req))))
      (= "/reset-token-form" uri) (render (ui/page ui/reset-token-form))
      (= "/reset-token" uri) (render (ui/page (ui/reset-token-message (handle-reset-token req))))

      (= "/upload-content" uri) (let [upload-result (handle-upload req)]
                                  (if-let [errors  (::errors upload-result)]
                                    (response/redirect-after-post "/error?Unauthenticated")
                                    (response/redirect-after-post "/")))
      (= "/error" uri) (render (ui/page (ui/error req)))
      :else (render (ui/page (ui/home (db/query db :content all)))))))

(defn handler [req]
  (let [req (assoc req
                   :timestamp (inst-ms (java.util.Date.))
                   :db db/db)]
    (handler* req)))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)
  (:user @db/db)

  (def server (start-server 3000))
  (stop-server server)
  (swap! db/db update :content conj {:content-type "video/mp4"
                                  :filename "resources/public/episode-7.mp4"
                                  :title "Episode 7"
                                  :id 7})
  

  (:user @db/db)


  )



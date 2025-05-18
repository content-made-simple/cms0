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

(defn render [{:keys [data content-type redirect] :or {content-type "text/html"}}]
  (cond
    redirect
    (response/redirect-after-post redirect)
    
    (= content-type "text/html")
    (-> data
        (hiccup/html)
        (str)
        (response/response)
        (response/content-type "text/html"))
    
    :else (->content data)))

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

(defn handle-upload [{:keys [user multipart-params db] :as req}]
  (let [{:strs [file]} multipart-params
        id (db/next-id db :content)
        path (str "resources/public/content-" id)
        destination (java.io.File. path)]
    (io/copy (:tempfile file) destination)
    (db/transact! db :content conj {:content-type (:content-type file)
                                    :title (get multipart-params "title")
                                    :filename path
                                    :user-id (:id user)
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

(defn handle-signup [{:keys [db timestamp form-params] :as req}]
  (let [email (get form-params "email")
        id (db/next-id db :user)
        user {:id id
              :email email
              :token (->token [email timestamp])}]
    (db/transact! db :user user conj)
    user))

(defn user-authenticate [db email token]
  (db/query-one db :user (partial by-token-and-email token email)))

(defn handle-reset-token [{:keys [db timestamp user]}]
  (let [email (:email user)
        new-user (assoc user :token (->token [email timestamp]))]
    (db/transact! db :user db/replace new-user)
    new-user))

(def commands {:reset-token {:id :reset-token
                             :authenticated? true
                             :page ui/reset-token-message
                             :handler handle-reset-token
                             :parameters [:email :token]}
               :upload-content {:id :upload-content
                                :authenticated? true
                                :redirect "/"
                                :handler handle-upload}
               :signup {:id :signup
                        :handler (constantly "NOOOOOO!")
                        :page ui/this-is-a-beta}})

;; TODO
;; - test "/content"
;; - test render
;; - rewrite database to assosiative

(defn evaluate-command! [{:keys [id page handler redirect] :as command} {:keys [user] :as request}]
  (cond
    (and (:authenticated? command)
         (not user))
    {:data (ui/page (ui/error "Unauthenticated"))}

    (and id redirect)
    (do (handler request)
        {:data [] :redirect redirect})

    id
    {:data (ui/page (page (handler request)))}

    :else (ui/under-construction :unknown-command)))

(defn handler* [{:keys [uri db form-params multipart-params] :as req}]
  (let [email (get form-params "email")
        token (get form-params "token")
        command (keyword (get form-params "command" (get multipart-params "command")))
        user (user-authenticate db email token)
        req (assoc req :user user)]
    (cond
      (str/starts-with? uri "/content/") {:data (content-by-id db (->id uri)) :content-type "video"}
      (str/starts-with? uri "/video/") {:data (ui/page (ui/video (content-by-id db (->id uri))))}

      (= "/upload-form" uri) {:data (ui/page ui/upload-form)}
      (= "/signup-form" uri) {:data (ui/page (ui/this-is-a-beta "no") #_signup-form)}
      (= "/reset-token-form" uri) {:data (ui/page ui/reset-token-form)}
      (= "/command" uri) (evaluate-command! (command commands) req)
      (= "/error" uri) {:data (ui/page (ui/error (:query-string req)))}
      :else {:data (ui/page (ui/home (db/query db :content all)))})))

(defn handler [req]
  (let [req (-> req
                (params/assoc-form-params "UTF-8")
                (multipart/multipart-params-request)
                (assoc :timestamp (inst-ms (java.util.Date.))
                       :db db/db))]
    (render (handler* req))))

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
  (let [i 11]
    (swap! db/db update :content conj {:content-type "video/mp4"
                                       :filename (str "resources/public/episode-" i ".mp4")
                                       :title (str "Episode " i)
                                       :id i}))

  (:content @db/db))




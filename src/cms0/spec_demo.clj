(ns cms0.spec-demo
  "Using spec with orchestra to verify that things are right at runtime"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [hiccup2.core :as hiccup]
            [orchestra.spec.test :as st]
            [ring.adapter.jetty :as jetty]
            [ring.util.response :as response])
  (:import (java.io File)))

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

(s/def ::uri string?)
(s/def ::ring-request
  (s/keys :req-un [::uri]))

(s/def ::status pos-int?)
(s/def ::body any?)
(s/def ::headers map?)
(s/def ::ring-response
  (s/keys :req-un [::status
                   ::body
                   ::headers]))

;; Hack around the fact that specs cannot have
;; variable conforming values depending on context :(
(s/def ::html-body
  (s/and string? #(string/starts-with? % "<html>")))

(defn verify-html-body
  [{:keys [body]}]
  (s/valid? ::html-body body))

(s/def ::file-body
  #(instance? File %))

(defn verify-http-body
  [{:keys [body]}]
  (s/valid? ::file-body body))

(s/fdef handler
        :args (s/cat :request ::ring-request)
        :ret ::ring-response
        :fn #(if (-> % :args :request :uri (string/starts-with? "/content"))
               (verify-http-body (:ret %))
               (verify-html-body (:ret %))))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

;; WILD IDEA ... Run instrumentation in production
(st/instrument)

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)

  (def server (start-server 3000))
  (stop-server server)
  (swap! db conj {:content-type "video/mp4"
                  :filename "resources/public/episode-2.mp4"
                  :title "Episode 2"
                  :id 2}),)

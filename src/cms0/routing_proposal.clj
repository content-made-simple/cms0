(ns cms0.routing-proposal
  "Using spec with orchestra to verify that things are right at runtime.
  This time with simpler routing functions."
  (:require [clojure.spec.alpha :as s]
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

(defn id->content [{:keys [id] :as _foo}]
  (-> [:html [:body [:video {:src (str "/content/" id)
                             :controls true
                             :preload "metadata"}]]]
      (render)))

(defn bar [{:keys [id title] :as _foo}]
  [:li [:a {:href (str "/show-content/" id)} title]])

(defn default-page [foos]
  (-> [:html [:body [:ul (map bar foos)]]]
      (render)))

(defonce db (atom [{:content-type "video/mp4"
                    :filename "resources/public/2025-01-06-20-11-06.mp4"
                    :title "Episode 1"
                    :id 1}]))

(defn not-found-response [uri]
  (ring.util.response/not-found (str "Not found " uri)))

(defn default-page? [uri]
  (= "/" uri))

(defn uri->parts [rx uri]
  (re-find rx uri))

(def content-uri-regex #"/ui/(\d+)")
(def server-uri-regex #"/content/(\d+)")

(defn ui-request? [uri]
  (= 2 (count (uri->parts content-uri-regex uri))))

(defn server-request? [uri]
  (= 2 (count (uri->parts server-uri-regex uri))))

(defn uri->default-page-response []
  (default-page @db))

(defn uri->ui-content-response [uri]
  (let [id (last (uri->parts content-uri-regex uri))]
    (condp = id
      "1" (id->content (first @db))
      "2" (id->content (second @db))
      (not-found-response uri))))

(defn uri->content-response [uri]
  (let [id (last (uri->parts server-uri-regex uri))]
    (condp = id
      "1" (->content (first @db))
      "2" (->content (second @db))
      (not-found-response uri))))

(defn handler [{:keys [uri] :as req}]
  (cond
    (default-page? uri) (uri->default-page-response)
    (ui-request? uri) (uri->ui-content-response uri)
    (server-request? uri) (uri->content-response uri)
    :else (not-found-response uri)))

(s/def ::uri string?)
(s/def ::ring-request
  (s/keys :req-un [::uri]))

(def html-min-regex
  #"<html><body>.*</body></html>")

(s/def ::html-body
  (s/and string? #(re-matches html-min-regex %)))

(def not-found #{404})
(s/def ::not-found-status not-found)
(s/def ::not-found-body string?)                        ;; Can make this more specific

(def ok #{200})
(s/def ::ok-status ok)

(def error #{500})
(s/def ::error-status error)

(s/def ::file-body
  #(instance? File %))

(s/def ::status pos-int?)
(s/def ::body any?)
(s/def ::headers map?)
(s/def ::ring-response
  (s/keys :req-un [::status
                   ::body
                   ::headers]))

(def dev-mode? true)                                        ;; REPL / env var to flip this

;; Use this to recover spec data from failing fdef functions
(s/check-asserts dev-mode?)

(defn verify-html-response
  [{:keys [status body]}]
  (s/assert ::ok-status status)
  (s/assert ::html-body body))

(defn verify-http-response
  [{:keys [status body]}]
  (s/assert ::ok-status status)
  ;; We are serving file responses exclusively at the moment
  (s/assert ::file-body body))

(defn verify-not-found
  [{:keys [status body]}]
  (s/assert ::not-found-status status)
  (s/assert ::not-found-body body))

(defn verify-error-body
  [{:keys [status body]}]
  (s/assert ::error-status status)
  (s/assert ::body body))

(s/fdef handler
  :args (s/cat :request ::ring-request)
  :ret ::ring-response
  :fn (fn input-output-check [{:keys [ret]
                               {:keys [status]} :ret
                               {:keys [request]} :args}]
        (prn :ret ret)
        ;; Ensure we apply the correct spec, not found is checked first.
        ;; Having simple specs that are made more specific here makes this part simpler
        ;; to reason about: s/or's make it tricky to pick apart contents of :ret & :arg
        (cond
          (not-found status) (verify-not-found ret)

          (and (ok status)
               (-> request :uri server-request?)) (verify-http-response ret)

          (ok status) (verify-html-response ret)

          :else (verify-error-body ret))))

(defn start-server [port]
  (jetty/run-jetty #'handler {:port port :join? false}))

(defn stop-server [server]
  (.stop server))

;; WILD IDEA ... Run instrumentation in production
(when dev-mode?
  (st/instrument))

(comment
  (require '[clojure.repl.deps :as deps])
  (deps/sync-deps)

  (def server (start-server 3000))
  (stop-server server)
  (swap! db conj {:content-type "video/mp4"
                  :filename "resources/public/episode-2.mp4"
                  :title "Episode 2"
                  :id 2})
  ;; this comment avoids re-format, then EMACS can eval the last form correctly
  )

(ns cms0.main-test
  (:require [cms0.main :as sut]
            [cms0.db :as db]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]))

(defn page [body]
  {:data [:html [:head] [:body body]]})

(def users  [{:email "erik@assum.net" :token "3" :id 1}])

(def unauthenticated (page [:h1 "Unauthenticated"]))

(deftest test-base-case
  (testing "Home page with empty database"
    (let [db (atom {:content []})]
      (is (= (page [:ul ()])
             (sut/handler* {:uri "" :db db})))))
  (testing "Home page with some content "
    (let [db (atom {:content [{:id 1 :title "foo"}]})]
      (is (= (page [:ul [[:li [:a {:href "/video/1"} "foo"]]]])
             (sut/handler* {:uri "" :db db}))))))

(deftest test-reset-token
  (testing "No authenticated user"
    (let [db (atom {:user []})]
      (is (= unauthenticated
             (sut/handler* {:uri "/command" :db db :form-params {"command" "reset-token"}})))))
  (testing "No authenticated user"
    (let [db (atom {:user users})]
      (is (= unauthenticated
             (sut/handler* {:uri "/command" :db db :form-params {"command" "reset-token"}})))))
  (testing "Authenticated user"
    (let [db (atom {:user users})]
      (is (= (page [:div
                    "Your token is F53574B0FE662F7A3011F35F6CD162D945D0AD301F0A7C05FF07F7A137AB37DF put it somewhere safe"])
             (sut/handler* {:uri "/command" :db db :timestamp 3 :form-params {"command" "reset-token" "email" "erik@assum.net" "token" "3"}}))))))

(deftest test-handle-upload
  (let [db (atom {:user users
                  :content []})]
    (testing "Unauthenticated"
      (is (= unauthenticated
             (sut/handler* {:uri "/command"
                            :db db
                            :timestamp 3
                            :multipart-params {"command" "upload-content"}}))))
    (testing "Authenticated"
      (let [fs (atom {})]
        (with-redefs [io/copy (fn [tmp-file dest]
                                (reset! fs {:tmp-file tmp-file :dest dest}))]
          (is (= {:data [] :redirect "/"}
                 (sut/handler* {:uri "/command"
                                :db db
                                :timestamp 3
                                :form-params {"email" "erik@assum.net" "token" "3"}
                                :multipart-params {"command" "upload-content"
                                                   "file" {:tempfile "lol"
                                                           :content-type "text/html"}
                                                   "title" "Title"}})))
          (is (= [{:content-type "text/html",
                   :title "Title",
                   :filename "resources/public/content-0",
                   :user-id 1,
                   :id 0}] (:content @db)))
          (is (= {:tmp-file "lol",
                  :dest (java.io.File. "resources/public/content-0")} @fs)))))))


(deftest test-handler-base-case
  (with-redefs [db/db (atom {})]
    (is (= {:status 200,	  
	    :headers {"Content-Type" "text/html"},
	    :body "<html><head></head><body><ul></ul></body></html>"}
           (sut/handler {:uri ""})))))


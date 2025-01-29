(ns cms0.user
  (:require
    [cms0.id-key :refer [edn->hex-digest]])
  (:import (java.util Date)))

(defn ->key
  "Create a new key for a user"
  [user-name timestamp]
  (edn->hex-digest [user-name timestamp]))



(comment

  ;; Get a key for a user
  (let [timestamp (inst-ms (Date.))]
    (->key "cms" timestamp))

  ;; End
  )


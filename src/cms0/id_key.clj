(ns cms0.id-key
  (:import (java.security MessageDigest)
           (java.util HexFormat)))

(set! *warn-on-reflection* true)

(defn bytes->hex-string
  "As of JDK 17 we can use HexFormat"
  [^bytes ba]
  (-> (HexFormat/of)
      .withUpperCase
      (.formatHex ba)))

(def supported-digests
  (set ["SHA-1" "SHA-256"]))

(def default-digest "SHA-1")

(defn string->digest
  "Produce a digest of the given string. A digest can be specified via the
  :digest-name key. Must be a supported digest. Defaults to the default-digest"
  [^String s & {:keys [digest-name]
                :or {digest-name default-digest}}]
  {:pre [(contains? supported-digests digest-name)]}
  (let [digest (MessageDigest/getInstance digest-name)]
    (some->> s .getBytes (.digest digest))))

(defn edn->hex-digest
  "Returns a hex formatted string of the digest produced from the given EDN"
  [edn-data & {:keys [digest-name]
               :or {digest-name default-digest}}]
  (-> edn-data
      pr-str
      (string->digest {:digest-name digest-name})
      bytes->hex-string))

(comment

  ;; Get a key for a user
  (let [user "cms"
        timestamp (inst-ms (java.util.Date.))]
    (edn->hex-digest [user timestamp]))

  ;; End
  )
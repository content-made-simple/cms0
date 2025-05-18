(ns cms0.ui)

(defn video [{:keys [id] :as _foo}]
  [:video {:src (str "/content/" id)
           :width "100%"
           :controls true
           :preload "metadata"}])

(defn make-input [{:keys [id label value]
                   input-type :type
                   input-name :name
                   :or {input-type "text"}}]
  (let [id (or id input-name)]
    (list (when label
            [:label {:for id} label])
          [:input (cond-> {:type (name input-type)
                           :id id}
                    input-name (assoc :name input-name)
                    value (assoc :value value))])))

(def upload-form
  [:form {:method "post"
          :action "/command"
          :enctype "multipart/form-data"}
   [:input {:type "hidden"
            :name "command"
            :value "upload-content"}]
   (make-input {:name "file"
                :type :file})
   [:br]
   (make-input {:name "title"
                :label "Title"})
   [:br]
   (make-input {:name "email"
                :type :email
                :label "Email"})
   [:br]
   (make-input {:name "token"
                :label "Token"})

   [:br]
   (make-input {:value "Upload"
                :type :submit})])

(defn video-link [{:keys [id title] :as _video}]
  [:li [:a {:href (str "/video/" id)} title]])

(defn home [content]
  [:ul (map video-link content)])

(defn page [body]
  [:html
   [:head]
   [:body
    body]])

(defn this-is-a-beta [_]
  [:div
   "No signups at this time, we're still in pre-alpha"])

(def signup-form
  [:form {:method "post"
          :action "/signup"}
   (make-input {:name "email"
                :title "Email"
                :type :email})
   [:br]
   (make-input {:type :submit :value "Sign up"})])

(def reset-token-form
  [:form {:method "post"
          :action "/command"}
   [:input {:type "hidden" :name "command" :value "reset-token"}]
   (make-input {:name "email"
                :title "Email"
                :type :email})
   [:br]
   (make-input {:name "token"
                :title "Token"})
   [:br]
   (make-input {:type :submit :value "Reset"})])

(defn token-message [{:keys [token] :as _user}]
  [:div
   (str "Your token is " token " put it somewhere safe")])

(defn reset-token-message [user]
  [:div
   (str "Your token is " (:token user) " put it somewhere safe")])

(defn error [msg]
  [:h1 msg])

(defn under-construction [command]
  [:h1 (str "Under construction" (name command))])

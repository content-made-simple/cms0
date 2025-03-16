(ns cms0.ui)

(defn video [{:keys [id] :as _foo}]
  [:video {:src (str "/content/" id)
           :width "100%"
           :controls true
           :preload "metadata"}])

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

(defn video-link [{:keys [id title] :as _video}]
  [:li [:a {:href (str "/video/" id)} title]])

(defn home [content]
  [:ul (map video-link content)])

(defn page [body]
  [:html
   [:head]
   [:body
    body]])

(def this-is-a-beta
  [:div
   "No signups at this time, we're still in pre-alpha"])

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

(defn token-message [{:keys [token] :as _user}]
  [:div
   (str "Your token is " token " put it somewhere safe")])

(defn reset-token-message [user]
  (if user
    [:div
     (str "Your token is " (:token user) " put it somewhere safe")]
    [:div
     "You're cheating"]))

(defn error [{:keys [query-string] :as req}]
  [:h1 query-string])

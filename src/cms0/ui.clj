(ns cms0.ui)

(defn video [{:keys [id] :as _foo}]
  [:video {:src (str "/content/" id)
           :width "100%"
           :controls true
           :preload "metadata"}])

(def upload-form
  [:form {:method "post"
          :action "/command"
          :enctype "multipart/form-data"}
   [:input {:type "hidden"
            :name "command"
            :value "upload-content"}]
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

(defn this-is-a-beta [_]
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
          :action "/command"}
   [:input {:type "hidden" :name "command" :value "reset-token"}]
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
  [:div
   (str "Your token is " (:token user) " put it somewhere safe")])

(defn error [msg]
  [:h1 msg])

(defn under-construction [command]
  [:h1 (str "Under construction" (name command))])

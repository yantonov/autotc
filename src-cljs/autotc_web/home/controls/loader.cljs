(ns autotc-web.home.controls.loader)

(defn loader [{:keys [visible]} data]
  (if visible
    [:div nil
     [:img {:src "/img/facebook.svg"
            :alt "loader"
            :class "facebook-loader"}]]))

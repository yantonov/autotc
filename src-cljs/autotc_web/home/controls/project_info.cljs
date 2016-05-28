(ns autotc-web.home.controls.project-info
  (:require [clojure.string :as string]))

(defn project-info [project branches]
  [:div nil
   (let [attrs (:attrs project :attrs)]
     [:a {:href (:webUrl attrs)
          :target "_blank"
          :class-name "project-link"} (:name attrs)])
   [:span {:class-name "branches"}
    (string/join "," branches)]])

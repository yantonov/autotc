(ns autotc-web.home.home
  (:require
   [reagent.core :as r]))

(defn home-root-component []
  [:div
   [:div "test component"]])

(defn ^:export init []
  (println "hello home page")
  (r/render-component (home-root-component)
                            (js/document.getElementById "main-content")))


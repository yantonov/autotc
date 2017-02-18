(ns autotc-web.home.controls.server-list
  (:require [reagent.core :as r]
            [cljsjs.react-bootstrap]))

(def Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(def NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))

(defn server-list [servers
                   selected-server
                   on-server-select]
  [:div
   nil
   [Nav {:bs-style "tabs"
         :active-key (second (first (filter (fn [[s index]]
                                              (= (:id selected-server)
                                                 (:id s)))
                                            (map vector servers (iterate inc 0)))))
         :on-select (fn [index] (on-server-select (get servers index)))}
    (for [[server index] (map vector servers (iterate inc 0))]
      [NavItem {:key index
                :event-key index
                :href "#" }
       (:alias server)])]])

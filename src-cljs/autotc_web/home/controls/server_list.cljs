(ns autotc-web.home.controls.server-list
  (:require [reagent.core :as r]
            [cljsjs.react-bootstrap]))

(def Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(def NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))

(defn server-list [servers
                   selected-server-index
                   on-server-select]
  [:div
   nil
   [Nav {:bs-style "tabs"
         :active-key selected-server-index
         :on-select on-server-select}
    (for [[server index] (map vector servers (iterate inc 0))]
      [NavItem {:key index
                :event-key index
                :href "#"}
       (:alias server)])]])

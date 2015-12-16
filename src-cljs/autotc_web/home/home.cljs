(ns autotc-web.home.home
  (:require
   [cljsjs.react-bootstrap]
   [reagent.core :as r]))

(defonce Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(defonce NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))

(defn info-message []
  (r/create-class
   {:reagent-render
    (fn [message]
      (if (nil? message)
        nil
        [:div {:class-name "static-modal"}
         [:div {:tab-index "-1"
                :role "dialog"
                :style {:display "block"}
                :class-name "modal in"}
          [:div {:class-name "modal-dialog"}
           [:div {:class-name "modal-content"}
            [:div {:class-name "modal-header"
                   :style {:border-bottom-width "0px"}}
             [:button {:type "button"
                       :class-name "close"
                       :aria-hidden "true"}
              "x"]
             [:h4 {:class-name "modal-title"} message]]]]]]))}))

(defn server-list [servers
                   selected-server-index
                   on-server-select]
  (r/create-class
   {:render
    (fn [this]
      [:div
       nil
       [Nav {:bs-style "tabs"
             :active-key selected-server-index
             :on-select on-server-select
             }
        (for [[server index] (map (fn [a b] [a b]) servers (iterate inc 1))]
          [NavItem {:key index
                    :event-key index
                    :href "#"}
           (:alias server)])]])}))

(defn home-page []
  (r/create-class
   {:get-initial-state
    (fn [_]
      {:servers [{:alias "japan"}
                 {:alias "metal"}]
       :selected-server-index nil
       :agents []
       :manually-selected-agents []
       :message nil
       :show-agent-list-loader false})
    :render
    (fn [this]
      (let [state (r/state this)]
        [:div
         [info-message (:message state)]
         [server-list
          (:servers state)
          (:selected-server-index state)
          (fn [_] nil)]]))}))

(defn ^:export init []
  (println "init home page")
  (r/render-component [home-page]
                      (js/document.getElementById "main-content")))


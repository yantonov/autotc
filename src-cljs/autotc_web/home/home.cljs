(ns autotc-web.home.home
  (:require
   [reagent.core :as r]))

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

(defn home-page []
  (r/create-class
   {:get-initial-state
    (fn [_]
      {:servers []
       :selected-server-index nil
       :agents []
       :manually-selected-agents []
       :message nil
       :show-agent-list-loader false})
    :render
    (fn [this]
      (let [state (r/state this)
            message (:message state)]
        [:div
         [:div "test component content"]
         [info-message message]]))}))

(defn ^:export init []
  (println "hello home page")
  (r/render-component [home-page]
                      (js/document.getElementById "main-content")))


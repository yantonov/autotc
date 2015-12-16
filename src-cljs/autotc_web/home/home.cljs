(ns autotc-web.home.home
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]))

(defonce Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(defonce NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))
(defonce Grid (r/adapt-react-class js/ReactBootstrap.Grid))
(defonce Row (r/adapt-react-class js/ReactBootstrap.Row))
(defonce Col (r/adapt-react-class js/ReactBootstrap.Col))

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
             :on-select on-server-select}
        (for [[server index] (map vector servers (iterate inc 1))]
          [NavItem {:key index
                    :event-key index
                    :href "#"}
           (:alias server)])]])}))

(defn multi-action-toolbar [{enabled :enabled
                             visible :visible
                             on-start :on-start
                             on-stop :on-stop
                             on-reboot :on-reboot
                             on-run-custom-build :on-run-custom-build} data]
  (r/create-class
   {:render
    (fn [this]
      nil)}))

(defn agent-list [{agents :agents
                   selected-agents :selected-agents
                   handle-select-agent :on-select-agent
                   handle-select-all :on-select-all
                   show-loader :show-loader} data]
  (r/create-class
   {:render
    (fn [this]
      nil)}))

(defn home-page []
  (r/create-class
   {:get-initial-state
    (fn [_]
      {:servers []
       :selected-server-index nil
       :agents []
       :selected-agents []
       :manually-selected-agents []
       :message nil
       :show-agent-list-loader false})
    :reset-timer
    (fn [this timer]
      (let [state (r/state this)]
        (when-let [timer (:poll-agent-timer state)]
          (stop timer))))
    :load-agents
    (fn [this server]
      ;; todo
      nil)
    :get-server-list
    (fn [this]
      (ajax/GET
       "/servers/list"
       {:params {}
        :response-format (ajax/json-response-format {:keywords? true})
        :handler
        (fn [response]
          (let [servers (:servers response)]
            (do
              (r/set-state this {:servers servers
                                 :agents []
                                 :selected-agents []
                                 :manually-selected-agents []
                                 }))

            ))}))
    :component-did-mount
    (fn [this]
      (.getServerList this))
    :component-unmount
    (fn [this]
      (reset-timer this)
      nil)
    :on-server-select
    (fn [this server-index]
      (let [state (r/state this)]
        (if (= server-index
               (:selected-server-index state))
          nil
          (do
            (.resetTimer this)
            (let [current-server (get (:servers state) server-index)]
              (r/set-state this
                           {:show-agent-list-loader true
                            :selected-server-index server-index
                            :selected-agents []
                            :manually-selected-agents []
                            :agents []
                            :poll-agent-timer (BasedOnActivityIntervalTimer.
                                               (partial loadAgents this (current-server))
                                               5000
                                               60000)}))))))
    :handle-select-agent
    (fn [this]
      nil)
    :handle-select-all
    (fn [this]
      nil)
    :render
    (fn [this]
      (let [state (r/state this)]
        [:div
         [info-message (:message state)]
         [server-list
          (:servers state)
          (:selected-server-index state)
          (partial onServerSelect this)]
         [Grid nil
          [Row {:className "show-grid"}
           [Col {:xs 12
                 :md 6}
            [:br]
            [multi-action-toolbar {:enabled (> (count (:selected-agents state)) 0)
                                   :visible (> (count (:agents state)) 0)
                                   :on-start (partial handleStartBuild this)
                                   :on-stop (partial handleStoBuild this)
                                   :on-reboot (partial handleRebootAgent this)
                                   :on-run-custom-build (partial handleRunCustomBuild this)}]
            [agent-list {:agents (:agents state)
                         :selected-agents (:selected-agents state)
                         :on-select-agent (partial handleSelectAgent this)
                         :on-select-all (partial handleSelectAll this)
                         :show-loader (:show-agent-list-loader state)}]]]]]))}))

(defn ^:export init []
  (println "init home page")
  (r/render-component [home-page]
                      (js/document.getElementById "main-content")))


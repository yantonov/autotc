(ns autotc-web.home.home
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]
            [goog.string :as gstring]))

(defonce Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(defonce NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))
(defonce Grid (r/adapt-react-class js/ReactBootstrap.Grid))
(defonce Row (r/adapt-react-class js/ReactBootstrap.Row))
(defonce Col (r/adapt-react-class js/ReactBootstrap.Col))
(defonce ListGroup (r/adapt-react-class js/ReactBootstrap.ListGroup))
(defonce ListGroupItem (r/adapt-react-class js/ReactBootstrap.ListGroupItem))

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

(defn loader []
  [:div {:color "#ddd"
         :size "16px"
         :margin "4px"}
   "todo: change to halogen loader"])

(defn select-all-element [{visible :visible
                           on-select :on-select
                           checked :checked} data]
  (if (not visible)
    nil
    [ListGroupItem
     [:input {:type "checkbox"
              :on-click on-select
              :checked checked}]
     (gstring/unescapeEntities "&nbsp;")
     "All agents"]))

(defn- get-agent-status [agent]
  (let [status (:statusText agent)]
    (if (nil? status)
      "Unknown status. Check agent configuration."
      status)))

(defn- get-image [status running]
  (cond
    (= status "Failure")
    (if running
      "running_red.gif"
      "stopped_red.gif")

    (= status "Success")
    (if running
      "running_green.gif"
      "stopped_green.gif")

    true
    "unknown.png"))

(defn agent-status [{running :running
                     status :status} data]
  [:img {:src (str "/img/statuses/" (get-image status running))
         :alt (str status (if running
                            "in progress"
                            "completed"))}])

(defn agent-list-item [{key :key
                        agent :agent
                        selected :selected
                        on-select :on-select} data]
  [ListGroupItem
   {:key key}
   [:input {:type "checkbox"
            :checked selected
            :on-click on-select
            :class-name "agent__checkbox"}]
   (gstring/unescapeEntities "&nbsp;")
   [agent-status {:running (:running agent)
                  :status (:status agent)}]
   (gstring/unescapeEntities "&nbsp;")
   [:span {:class-name "agent__text agent__name"}
    [:a {:href (:webUrl agent)
         :target "_blank"}
     (:name agent)]]
   (gstring/unescapeEntities "&nbsp;")
   [:span {:class-name "agent__text agent__status"}
    (str "["(get-agent-status agent) "]")]])

(defn agent-list [{agents :agents
                   selected-agents :selected-agents
                   handle-select-agent :on-select-agent
                   handle-select-all :on-select-all
                   show-loader :show-loader} data]
  (if show-loader
    [:div
     nil
     [loader]]
    [:div
     nil
     [:br]
     [ListGroup]
     [select-all-element {:visible (> (count agents) 0)
                          :on-select on-select-all
                          :checked select-all-checked}]
     (for [[a i] (map vector agents (iterate inc 0))]
       [agent-list-item {:key i
                         :agent a
                         :selected false
                         :on-select on-select-agent}])]))

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
          (js/clearInterval timer))))
    :load-agents
    (fn [this server]
      (let [state (r/state this)
            selected-server-index (:selected-server-index state)
            selected-server (get (:servers state) selected-server-index)]
        (if (or (nil? server)
                (and (not (nil? selected-server))
                     (not (= (:id selected-server)
                             (:id server)))))
          nil
          (do
            (let [url (str "/agents/list/" (:id server))]
              (ajax/GET
               url
               {:response-format (ajax/json-response-format {:keywords? true})
                :handler (fn [response]
                           (let [state (r/state this)
                                 selected-server-index (:selected-server-index state)
                                 selected-server (get (:servers state) selected-server-index)]
                             (if (and (not (nil? selected-server))
                                      (not (= (:id selected-server)
                                              (:id server))))
                               nil
                               (do
                                 (r/set-state this {:agents (:agents response)
                                                    :show-agent-list-loader false})))))
                :error-handler (fn [response]
                                 (r/set-state this {:agents []
                                                    :show-agent-list-loader false
                                                    :selected-agents []
                                                    :manually-selected-agents []}))}))))))
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
              (if (and (not (nil? servers))
                       (> (count servers) 0))
                (.loadAgents this))
              (r/set-state this {:servers servers
                                 :agents []
                                 :selected-agents []
                                 :manually-selected-agents []})
              (when-let [default-server-index (if (and (not (nil? servers))
                                                       (> (count servers) 0))
                                                0
                                                nil)]
                (.onServerSelect this default-server-index)))))}))
    :component-did-mount
    (fn [this]
      (this.getServerList))
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
            (r/set-state this
                         {:show-agent-list-loader true
                          :selected-server-index server-index
                          :selected-agents []
                          :manually-selected-agents []
                          :agents []})
            (.resetTimer this)
            (let [current-server (get (:servers state) server-index)]
              (r/set-state this
                           {:poll-agent-timer (js/setInterval
                                               (fn [] (.loadAgents this current-server))
                                               5000)}))))))
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
          this.onServerSelect]
         [Grid nil
          [Row {:className "show-grid"}
           [Col {:xs 12
                 :md 6}
            [:br]
            [multi-action-toolbar {:enabled (> (count (:selected-agents state)) 0)
                                   :visible (> (count (:agents state)) 0)
                                   ;; :on-start (partial handleStartBuild this)
                                   ;; :on-stop (partial handleStoBuild this)
                                   ;; :on-reboot (partial handleRebootAgent this)
                                   ;; :on-run-custom-build (partial handleRunCustomBuild this)
                                   }]
            [agent-list {:agents (:agents state)
                         ;; :selected-agents (:selected-agents state)
                         ;; :on-select-agent (partial handleSelectAgent this)
                         ;; :on-select-all (partial handleSelectAll this)
                         :show-loader (get state :show-agent-list-loader false)}]]]]]))}))

(defn ^:export init []
  (println "init home page")
  (r/render-component [home-page]
                      (js/document.getElementById "main-content")))


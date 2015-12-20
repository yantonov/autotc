(ns autotc-web.home.home
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [autotc-web.util.poller :as plr]))

(def Nav (r/adapt-react-class js/ReactBootstrap.Nav))
(def NavItem (r/adapt-react-class js/ReactBootstrap.NavItem))
(def Grid (r/adapt-react-class js/ReactBootstrap.Grid))
(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))
(def ListGroup (r/adapt-react-class js/ReactBootstrap.ListGroup))
(def ListGroupItem (r/adapt-react-class js/ReactBootstrap.ListGroupItem))
(def ButtonToolbar (r/adapt-react-class js/ReactBootstrap.ButtonToolbar))
(def Button (r/adapt-react-class js/ReactBootstrap.Button))
(def Glyphicon (r/adapt-react-class js/ReactBootstrap.Glyphicon))
(def Loader (r/adapt-react-class js/Halogen.ScaleLoader))

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

(defn multi-action-toolbar [{:keys [enabled
                                    visible
                                    on-start
                                    on-stop
                                    on-reboot
                                    on-run-custom-build]} data]
  (let [disabled (not enabled)]
    (if (and visible enabled)
      [:div {:class-name "multi-action-toolbar-container"}
       [:div {:class-name "navbar-default multi-action-panel"}
        [:div {:class-name "container"
               :style {}}
         [ButtonToolbar
          [Button {:disabled disabled
                   :on-click on-start}
           [Glyphicon {:glyph "play"}]
           " Start"]
          [Button {:disabled disabled
                   :on-click on-stop}
           [Glyphicon {:glyph "stop"}]
           " Stop"]
          [Button {:disabled disabled
                   :on-click on-reboot}
           [Glyphicon {:glyph "eject"}]
           " Reboot"]
          [Button {:disabled disabled
                   :on-click on-run-custom-build}
           [Glyphicon {:glyph "th"}]
           (str " Clean" (gstring/unescapeEntities "&amp;") "Build")]]]]])))

(defn loader [{:keys [visible]} data]
  (if visible
    [Loader {:color "#ddd"
             :size "16px"
             :margin "4px"}]))

(defn select-all-element [{:keys [visible
                                  on-change
                                  checked]} data]
  (if visible
    [ListGroupItem
     [:input {:type "checkbox"
              :on-change (fn [event] (on-change event.target.checked))
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

(defn agent-status [{:keys [running
                            status]} data]
  [:img {:src (str "/img/statuses/" (get-image status running))
         :alt (str status (if running "in progress" "completed"))}])

(defn agent-list-item [{:keys [key
                               agent
                               selected
                               on-change]} data]
  [ListGroupItem
   {:key key}
   [:input {:type "checkbox"
            :checked selected
            :on-change (fn [event] (on-change event.target.checked))
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

(defn is-agent-selected? [selected-agents agent]
  (contains? selected-agents (:id agent)))

(defn update-agent-selection [set agent selected?]
  ((if selected? conj disj) set (:id agent)))

(defn agent-list [{:keys [agents
                          selected-agents
                          on-select-agent
                          on-select-all
                          show-loader]} data]
  [:div
   [loader {:visible show-loader}]
   [:br]
   [ListGroup
    [select-all-element {:visible (> (count agents) 0)
                         :on-change on-select-all
                         :checked (= (count agents) (count selected-agents))}]
    (for [[a i] (map vector agents (iterate inc 0))]
      [agent-list-item {:key i
                        :agent a
                        :selected (is-agent-selected? selected-agents a)
                        :on-change (fn [checked] (on-select-agent a checked))}])]])

(defn other-server-selected? [state load-agents-for-server]
  (if (nil? load-agents-for-server)
    true
    (let [selected-server-index (:selected-server-index state)
          selected-server (get (:servers state) selected-server-index)]
      (and (not (nil? selected-server))
           (not (= (:id selected-server)
                   (:id load-agents-for-server)))))))

(defn home-page []
  (r/create-class
   {:get-initial-state
    (fn [_]
      {:servers []
       :selected-server-index nil
       :agents []
       :selected-agents #{}
       :manually-selected-agents #{}
       :message nil
       :show-agent-list-loader false})
    :reset-timer
    (fn [this]
      (when-let [timer (:poll-agent-timer (r/state this))]
        (plr/stop timer)))
    :load-agents
    (fn [this server]
      (if (other-server-selected? (r/state this) server)
        nil
        (let [url (str "/agents/list/" (:id server))]
          (ajax/GET
           url
           {:response-format (ajax/json-response-format {:keywords? true})
            :handler (fn [response]
                       (if (other-server-selected? (r/state this) server)
                         nil
                         (do
                           (r/set-state this {:agents (:agents response)
                                              :show-agent-list-loader false}))))
            :error-handler (fn [response]
                             (r/set-state this {:agents []
                                                :show-agent-list-loader false
                                                :selected-agents #{}
                                                :manually-selected-agents #{}}))}))))
    :get-server-list
    (fn [this]
      (ajax/GET
       "/servers/list"
       {:params {}
        :response-format (ajax/json-response-format {:keywords? true})
        :handler
        (fn [response]
          (let [servers (:servers response)
                has-any-server? (and (not (nil? servers))
                                     (> (count servers)))]
            (do
              (r/set-state this {:servers servers
                                 :agents []
                                 :selected-agents #{}
                                 :manually-selected-agents #{}})
              (if has-any-server?
                (.onServerSelect this 0)))))}))
    :component-did-mount
    (fn [this]
      (this.getServerList))
    :component-unmount
    (fn [this]
      (this.resetTimer)
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
                          :selected-agents #{}
                          :manually-selected-agents #{}
                          :agents []})
            (.resetTimer this)
            (let [current-server (get (:servers state) server-index)
                  p (plr/create-poller (fn []
                                         (.loadAgents this current-server))
                                       3000
                                       60000)]
              (do
                (plr/start p)
                (r/set-state this
                             {:poll-agent-timer p})))))))
    :handle-select-agent
    (fn [this agent selected?]
      (let [s (r/state this)
            selected-agents (:selected-agents s)
            manually-selected-agents (:manually-selected-agents s)]
        (r/set-state this {:selected-agents (update-agent-selection selected-agents
                                                                    agent
                                                                    selected?)
                           :manually-selected-agents (update-agent-selection manually-selected-agents
                                                                             agent
                                                                             selected?)})))
    :handle-select-all
    (fn [this checked?]
      (let [{agents :agents
             selected-agents :selected-agents
             manually-selected-agents :manually-selected-agents} (r/state this)

            new-selected-agents
            (if (empty? selected-agents)
              (if (empty? manually-selected-agents)
                (apply hash-set (map :id agents))
                (apply hash-set manually-selected-agents))
              (if (< (count selected-agents) (count agents))
                (apply hash-set (map :id agents))
                #{}))]
        (r/set-state this {:selected-agents new-selected-agents})))
    :handle-start-build
    (fn [this]
      (this.execActionForAgents
       "/agents/startBuild"
       "request to trigger build was sent"
       "build triggered"))
    :handle-stop-build
    (fn [this]
      (this.execActionForAgents
       "/agents/stopBuild"
       "request to stop build was sent"
       "build stopped"))
    :handle-reboot-agent
    (fn [this]
      (this.execActionForAgents
       "/agents/rebootAgent"
       "request to reboot agent was sent"
       "reboot triggered"))
    :handle-run-custom-build
    (fn [this]
      (this.execActionForAgents
       "/agents/runCustomBuild"
       "request to run custom build was sent"
       "custom build has triggered"))
    :show-message
    (fn [this message]
      (when-let [message-timer (:message-timer (r/state this))]
        (js/clearTimeout message-timer))
      (r/set-state this {:message message
                         :message-timer (js/setTimeout this.closeMessage 5000)}))
    :close-message
    (fn [this]
      (r/set-state this {:message nil}))
    :exec-action-for-agents
    (fn [this url trigger-message completed-message]
      (let [s (r/state this)
            current-server-id (:id (get (:servers s) (:selected-server-index s)))
            agent-ids (clj->js (map identity (:selected-agents s)))]
        (do
          (this.showMessage trigger-message)
          (ajax/POST url
                     {:params {"serverId" current-server-id
                               "agentIds[]" agent-ids}
                      :format (ajax/url-request-format)
                      :handler (fn [response]
                                 (println response)
                                 (this.showMessage completed-message))
                      :error-handler (fn [response] (println response))}))))
    :render
    (fn [this]
      (let [{:keys [servers
                    selected-server-index
                    message
                    selected-agents
                    agents
                    show-agent-list-loader] :or {:show-agent-list-loader false}}
            (r/state this)]
        [:div
         [info-message message]
         [server-list
          servers
          selected-server-index
          this.onServerSelect]
         [Grid nil
          [Row {:className "show-grid"}
           [Col {:xs 12
                 :md 6}
            [:br]
            [multi-action-toolbar {:enabled (not (empty? selected-agents))
                                   :visible (not (empty? agents))
                                   :on-start this.handleStartBuild
                                   :on-stop this.handleStopBuild
                                   :on-reboot this.handleRebootAgent
                                   :on-run-custom-build this.handleRunCustomBuild}]
            [agent-list {:agents agents
                         :selected-agents selected-agents
                         :on-select-agent this.handleSelectAgent
                         :on-select-all this.handleSelectAll
                         :show-loader show-agent-list-loader}]]]]]))}))

(defn ^:export init []
  (r/render-component [home-page]
                      (js/document.getElementById "main-content")))

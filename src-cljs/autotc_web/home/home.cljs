(ns autotc-web.home.home
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]
            [goog.string :as gstring]
            [autotc-web.util.poller :as plr]
            [rex.core :as rcore]
            [rex.ext.cursor :as rcur]
            [rex.reducer :as rr]
            [rex.watcher :as rwt]
            [rex.middleware :as rmw]
            [rex.ext.action-creator :as acm]
            [rex.ext.reducer-for-type :as rrtype]
            [autotc-web.util.reducer-helpers :as rhp]))

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

(defn is-agent-selected? [selected-agents agent]
  (contains? selected-agents (:id agent)))

(defn update-agent-selection [set agent selected?]
  ((if selected? conj disj) set (:id agent)))

(defn- define-reducers []
  (rrtype/reducer-for-type :init-page
                           (fn [state action]
                             (assoc state :page {})))

  (rrtype/reducer-for-type :new-server-list
                           (fn [state action]
                             (rcur/update-state (:cursor action)
                                                state
                                                {:servers (:servers action)
                                                 :agents []
                                                 :selected-agents #{}
                                                 :manually-selected-agents #{}})))

  (rrtype/reducer-for-type :new-agent-list
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:agents (:agents action)
                                               :show-agent-list-loader false})))

  (rrtype/reducer-for-type :reset-agent-list
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:agents []
                                               :show-agent-list-loader false
                                               :selected-agents #{}
                                               :manually-selected-agents #{}})))

  (rrtype/reducer-for-type :init-load-agent-list
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:show-agent-list-loader true
                                               :selected-server-index (:server-index action)
                                               :selected-agents #{}
                                               :manually-selected-agents #{}
                                               :agents []})))

  (rrtype/reducer-for-type :attach-poll-agent-timer
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:poll-agent-timer (:poll-agent-timer action)})))

  (rrtype/reducer-for-type :agent-selected
                           (fn [state action]
                             (let [cursor (:cursor action)
                                   {:keys [selected-agents
                                           manually-selected-agents]} (rcur/get-state cursor state)
                                   {:keys [agent
                                           selected?]} action]
                               (rhp/merge-state state
                                                cursor
                                                {:selected-agents (update-agent-selection selected-agents
                                                                                          agent
                                                                                          selected?)
                                                 :manually-selected-agents (update-agent-selection manually-selected-agents
                                                                                                   agent
                                                                                                   selected?)}))))

  (rrtype/reducer-for-type :select-all-agents
                           (fn [state action]
                             (let [cursor (:cursor action)
                                   old-state (rcur/get-state cursor state)
                                   {agents :agents
                                    selected-agents :selected-agents
                                    manually-selected-agents :manually-selected-agents} old-state
                                   new-selected-agents
                                   (if (empty? selected-agents)
                                     (if (empty? manually-selected-agents)
                                       (apply hash-set (map :id agents))
                                       (apply hash-set manually-selected-agents))
                                     (if (< (count selected-agents) (count agents))
                                       (apply hash-set (map :id agents))
                                       #{}))]
                               (rhp/merge-state state
                                                cursor
                                                {:selected-agents new-selected-agents}))))

  (rrtype/reducer-for-type :show-message
                           (fn [state action]
                             (let [{:keys [message
                                           message-timer]} action]
                               (rhp/merge-state state
                                                (:cursor action)
                                                {:message message
                                                 :message-timer message-timer}))))

  (rrtype/reducer-for-type :hide-message
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:message nil})))
  )

(defn hide-message-action-creator [cursor]
  (fn [dispatch get-store]
    (dispatch {:type :hide-message
               :cursor cursor})))

(defn show-message-action-creator [message cursor]
  (fn [dispatch get-state]
    (let [state (rcur/get-state cursor (get-state))]
      (when-let [message-timer (:message-timer state)]
        (js/clearTimeout message-timer))
      (dispatch {:type :show-message
                 :cursor cursor
                 :message message
                 :message-timer (js/setTimeout (fn [] ((hide-message-action-creator cursor) dispatch get-state))
                                               5000)}))))

(defn reset-timer-action-creator [cursor]
  (fn [dispatch get-store]
    (when-let [timer (:poll-agent-timer (rcur/get-state cursor (get-store)))]
      (plr/stop timer))
    nil))

(defn other-server-selected? [state load-agents-for-server]
  (if (nil? load-agents-for-server)
    true
    (let [selected-server-index (:selected-server-index state)
          selected-server (get (:servers state) selected-server-index)]
      (and (not (nil? selected-server))
           (not (= (:id selected-server)
                   (:id load-agents-for-server)))))))

(defn load-agents-action-creator [server cursor]
  (fn [dispatch get-state]
    (if (other-server-selected? (rcur/get-state cursor
                                                (get-state))
                                server)
      nil
      (do
        (let [url (str "/agents/list/" (:id server))]
          (ajax/GET
           url
           {:response-format (ajax/json-response-format {:keywords? true})
            :handler (fn [response]
                       (if (other-server-selected? (rcur/get-state cursor
                                                                   (get-state))
                                                   server)
                         nil
                         (dispatch {:type :new-agent-list
                                    :cursor cursor
                                    :agents (:agents response)})))
            :error-handler (fn [response]
                             (dispatch {:type :reset-agent-list
                                        :cursor cursor}))}))))))

(defn select-server-action-creator [server-index cursor]
  (fn [dispatch get-state]
    (let [state (rcur/get-state cursor (get-state))]
      (if (= server-index
             (:selected-server-index state))
        nil
        (do
          (dispatch {:type :init-load-agent-list
                     :cursor cursor
                     :server-index server-index})
          (dispatch (reset-timer-action-creator cursor))
          (let [current-server (get (:servers state) server-index)
                p (plr/create-poller (fn []
                                       (dispatch (load-agents-action-creator current-server cursor)))
                                     3000
                                     60000)]
            (do
              (plr/start p)
              (dispatch {:type :attach-poll-agent-timer
                         :cursor cursor
                         :poll-agent-timer p}))))))))

(defn get-server-list-action-creator [cursor]
  (fn [dispatch get-state]
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
            (dispatch {:type :new-server-list
                       :cursor cursor
                       :servers servers})
            (if has-any-server?
              (dispatch (select-server-action-creator 0 cursor))))))})))

(defn exec-action-for-agents [cursor url trigger-message completed-message]
  (fn [dispatch get-store]
    (let [s (rcur/get-state cursor (get-store))
          current-server-id (:id (get (:servers s) (:selected-server-index s)))
          agent-ids (clj->js (map identity (:selected-agents s)))]
      (do
        ((show-message-action-creator trigger-message cursor) dispatch get-store)
        (ajax/POST url
                   {:params {"serverId" current-server-id
                             "agentIds" agent-ids}
                    :format (ajax/json-request-format)
                    :handler (fn [response]
                               ((show-message-action-creator completed-message cursor) dispatch get-store))
                    :error-handler (fn [response] (println response))})))))

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
   {:key key
    :style {:padding "0"}}
   [:div {:on-click (fn [] (on-change (not selected)))
          :class-name "agent__row"}
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
          :target "_blank"
          :on-click (fn [event]
                      (.stopPropagation event)
                      true)}
      (:name agent)]]
    (gstring/unescapeEntities "&nbsp;")
    [:span {:class-name "agent__text agent__status"}
     (str "["(get-agent-status agent) "]")]]])

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

(defn home-page []
  (let [page-cursor (rcur/nest (rcur/make-cursor)
                               :page)]
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
        (rcore/dispatch (reset-timer-action-creator page-cursor)))
      :component-did-mount
      (fn [this]

        (rwt/defwatcher
          (fn [old-state new-state]
            (r/set-state this (rcur/get-state page-cursor new-state))))

        (rcore/dispatch {:type :init-page})
        (rcore/dispatch (get-server-list-action-creator page-cursor)))
      :component-unmount
      (fn [this]
        (this.resetTimer)
        nil)
      :on-server-select
      (fn [this server-index]
        (rcore/dispatch (select-server-action-creator server-index page-cursor)))
      :handle-select-agent
      (fn [this agent selected?]
        (rcore/dispatch {:type :agent-selected
                         :cursor page-cursor
                         :agent agent
                         :selected? selected?}))
      :handle-select-all
      (fn [this checked?]
        (rcore/dispatch {:type :select-all-agents
                         :cursor page-cursor}))
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
        (rcore/dispatch (show-message-action-creator message page-cursor)))
      :close-message
      (fn [this]
        (rcore/dispatch (hide-message-action-creator page-cursor)))
      :exec-action-for-agents
      (fn [this url trigger-message completed-message]
        (rcore/dispatch (exec-action-for-agents page-cursor
                                                url
                                                trigger-message
                                                completed-message)))
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
            [Row {:class-name "agent-list"}
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
                           :show-loader show-agent-list-loader}]]]]]))})))

(defn ^:export init []
  (let [page (home-page)]
    (do
      (rmw/defmiddleware acm/action-creator-middleware)
      (define-reducers)
      (r/render-component [page]
                          (js/document.getElementById "main-content")))))

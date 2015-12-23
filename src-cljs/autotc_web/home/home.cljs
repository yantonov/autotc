(ns autotc-web.home.home
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [goog.string :as gstring]
            [rex.core :as rcore]
            [rex.ext.cursor :as rcur]
            [rex.watcher :as rwt]
            [rex.middleware :as rmw]
            [rex.ext.action-creator :as acm]
            [autotc-web.home.reducers :as reducers]
            [autotc-web.home.actions :as actions]))

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
  (letfn [(is-agent-selected? [selected-agents agent]
            (contains? selected-agents (:id agent)))]
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
                          :on-change (fn [checked] (on-select-agent a checked))}])]]))

(defn home-page []
  (let [cursor (rcur/nest (rcur/make-cursor)
                          :page)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (rwt/defwatcher
          (fn [old-state new-state]
            (r/set-state this (rcur/get-state cursor new-state))))
        (actions/init-page cursor)
        (actions/load-server-list cursor))
      :component-unmount
      (fn [this]
        (actions/reset-timer cursor)
        nil)
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
            (fn [server-index] (actions/on-server-selected server-index cursor))]
           [Grid nil
            [Row {:class-name "agent-list"}
             [Col {:xs 12
                   :md 6}
              [:br]
              [multi-action-toolbar {:enabled (not (empty? selected-agents))
                                     :visible (not (empty? agents))
                                     :on-start (fn [] (actions/start-build cursor))
                                     :on-stop (fn [] (actions/stop-build cursor))
                                     :on-reboot (fn [] (actions/reboot-agent cursor))
                                     :on-run-custom-build (fn [] (actions/run-custom-build cursor))}]
              [agent-list {:agents agents
                           :selected-agents selected-agents
                           :on-select-agent (fn [agent selected?] (actions/on-agent-selected agent selected? cursor))
                           :on-select-all (fn [selected?] (actions/all-agents-selected selected? cursor))
                           :show-loader show-agent-list-loader}]]]]]))})))

(defn ^:export init []
  (let [page (home-page)]
    (do
      (rmw/defmiddleware acm/action-creator-middleware)
      (reducers/define-reducers)
      (r/render-component [page]
                          (js/document.getElementById "main-content")))))

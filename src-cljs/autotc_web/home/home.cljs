(ns autotc-web.home.home
  (:require [clojure.string :as string]
            [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [goog.string :as gstring]
            [goog.string.format]
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

(defn filter-link [visible enabled text on-click]
  (if visible
    (let [css {:class-name "filter-selection-link"}]
      (if enabled
        [:span css text]
        [:a (-> css
                (assoc :href "#" text)
                (assoc :on-click on-click))
         text]))))

(defn multi-action-toolbar [{:keys [enabled
                                    visible
                                    all-agents-count
                                    selected-agents-count
                                    on-start
                                    on-stop
                                    on-restart
                                    on-reboot
                                    on-run-custom-build
                                    filter-value
                                    filter-all
                                    filter-selected
                                    filter-not-selected]} data]
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
                   :on-click on-restart}
           [Glyphicon {:glyph "repeat"}]
           " Restart"]
          [Button {:disabled disabled
                   :on-click on-reboot}
           [Glyphicon {:glyph "eject"}]
           " Reboot"]]
         [:div {:class-name "filter-panel"}

          [filter-link
           (< selected-agents-count all-agents-count)
           (or (= filter-value :all)
               (nil? filter-value))
           "All"
           filter-all]

          [filter-link
           true
           (or (= filter-value :selected)
               (= all-agents-count selected-agents-count))
           (str "Selected (" selected-agents-count ")")
           filter-selected]

          [filter-link
           (< selected-agents-count all-agents-count)
           (= filter-value :not-selected)
           (str "Not selected (" (- all-agents-count selected-agents-count) ")")
           filter-not-selected]]]]])))

(defn loader [{:keys [visible]} data]
  (if visible
    [:div nil
     [:img {:src "/img/facebook.svg"
            :alt "loader"
            :class "facebook-loader"}]]))

(defn select-all-element [{:keys [visible
                                  on-change
                                  checked]} data]
  (if visible
    [ListGroupItem
     {:class-name "agent-container"}
     [:div {:on-click (fn [event] (on-change (not checked)))
            :class-name "agent__row"}
      [:input {:type "checkbox"
               :checked checked
               :class-name "agent__checkbox"}]
      (gstring/unescapeEntities "&nbsp;")
      "All agents"]]))

(defn- get-agent-status [agent]
  (let [status (:statusText agent)]
    (if (nil? status)
      "Unknown status. Check agent configuration."
      status)))

(defn- get-image [status running]
  (cond
    (= status "FAILURE")
    (if running
      "running_red.gif"
      "stopped_red.gif")

    (= status "SUCCESS")
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
    :class-name "agent-container"}
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
     [:a {:href (:last-build-webUrl agent)
          :target "_blank"
          :on-click (fn [event]
                      (.stopPropagation event)
                      true)}
      (str "["(get-agent-status agent) "]")]]
    (if-let [queue-url (:queue-webUrl agent)]
      [:a {:href queue-url
           :target "_blank"
           :on-click (fn [event]
                       (.stopPropagation event)
                       true)} "queued"])]])

(defn agent-list [{:keys [agents
                          selected-agents
                          on-select-agent
                          on-select-all
                          show-loader
                          filter-value]} data]
  (letfn [(is-agent-selected? [selected-agents agent]
            (contains? selected-agents (:id agent)))]
    [:div
     [loader {:visible show-loader}]
     [ListGroup
      [select-all-element {:visible (> (count agents) 0)
                           :on-change on-select-all
                           :checked (= (count agents) (count selected-agents))}]
      (for [[a i] (map vector
                       (filter (fn [a]
                                 (cond
                                   (nil? filter-value) true

                                   (= filter-value :all) true

                                   (= filter-value :selected) (is-agent-selected? selected-agents a)

                                   :else (not (is-agent-selected? selected-agents a))))
                               agents)
                       (iterate inc 0))]
        [agent-list-item {:key i
                          :agent a
                          :selected (is-agent-selected? selected-agents a)
                          :on-change (fn [checked] (on-select-agent a checked))}])]]))

(defn copy [element text]
  (let [element-id "elbaId"
        text-node (or (.getElementById js/document element-id)
                      (.createElement js/document "span"))]
    (set! (.-id text-node) element-id)
    (set! (.-innerText text-node) text)
    (set! (.-style text-node) "display: none:")
    (.appendChild (.-body js/document) text-node)
    (let [sel (.getSelection js/window)
          range (.createRange js/document)]
      (.selectNodeContents range text-node)
      (.removeAllRanges sel)
      (.addRange sel range)
      (.execCommand js/document "copy")
      (.selectNodeContents range element)
      (.removeAllRanges sel)
      (.addRange sel range)
      (.removeChild (.-body js/document) text-node))))

(defn current-problems-list [problems]
  [:div
   nil
   (map (fn [problem]
          [:div
           {:key (:name problem)}
           [Row nil
            [Col {:xs 2
                  :md 1}
             [:img {:src "/img/copy.png"
                    :class-name "copy_test_name_icon"
                    :alt "copy"
                    :on-click (fn [event]
                                (copy (.-target event) (:name problem))
                                (.stopPropagation event)
                                false)}]]
            [Col {:xs 18
                  :md 9}
             [:a {:href (:webUrl problem)
                  :target "_blank"
                  :class-name "current_problem_item"}
              (:name problem)]]
            [Col {:xs 4
                  :md 2}
             [:a {:href (->> problem
                             :build
                             :webUrl)
                  :target "_blank"}
              (->> problem
                   :build
                   :name)]]]])
        problems)])

(defn home-page []
  (let [cursor (rcur/nest (rcur/make-cursor)
                          :page)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (rwt/defwatcher
          (fn [old-state action new-state]
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
                      branches
                      agents
                      show-agent-list-loader
                      filter-value
                      current-problems]
               :or {:show-agent-list-loader false}}
              (r/state this)]
          [:div
           [info-message message]
           [server-list
            servers
            selected-server-index
            (fn [server-index] (actions/on-server-selected server-index cursor))]
           [:div nil
            [Col {:xs 12
                  :md 6}
             (string/join "," branches)]
            [Col {:xs 12
                  :md 6}
             (if (empty? current-problems)
               (gstring/unescapeEntities "&nbsp;")
               (gstring/format "Current problems: %d" (count current-problems)))]]
           [:div {:style {:padding-left "0px"}}
            [Row {:class-name "agent-list"}
             [Col {:xs 12
                   :md 6}
              [multi-action-toolbar {:enabled (not (empty? selected-agents))
                                     :visible (not (empty? agents))
                                     :all-agents-count (count agents)
                                     :selected-agents-count (count selected-agents)
                                     :on-start (fn [] (actions/start-build cursor))
                                     :on-stop (fn [] (actions/stop-build cursor))
                                     :on-restart (fn [] (actions/restart-build cursor))
                                     :on-reboot (fn [] (actions/reboot-agent cursor))
                                     :on-change-show-selected-only (fn [] (actions/change-show-selected-only cursor))
                                     :filter-value filter-value
                                     :filter-all (fn [] (actions/filter-show-all cursor))
                                     :filter-selected (fn [] (actions/filter-show-selected cursor))
                                     :filter-not-selected (fn [] (actions/filter-show-not-selected cursor))
                                     }]
              [agent-list {:agents agents
                           :selected-agents selected-agents
                           :filter-value filter-value
                           :on-select-agent (fn [agent selected?] (actions/on-agent-selected agent selected? cursor))
                           :on-select-all (fn [selected?] (actions/on-all-agents-selected selected? cursor))
                           :show-loader show-agent-list-loader}]]
             [Col {:xs 12
                   :md 6}
              [current-problems-list current-problems]]]]]))})))

(defn ^:export init []
  (let [page (home-page)]
    (do
      (rmw/defmiddleware acm/action-creator-middleware)
      (reducers/define-reducers)
      (r/render-component [page]
                          (js/document.getElementById "main-content")))))

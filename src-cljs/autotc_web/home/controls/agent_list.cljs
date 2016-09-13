(ns autotc-web.home.controls.agent-list
  (:require [reagent.core :as r]
            [goog.string :as gstring]))

(def ListGroup (r/adapt-react-class js/ReactBootstrap.ListGroup))
(def ListGroupItem (r/adapt-react-class js/ReactBootstrap.ListGroupItem))

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

(defn agent-list-item [{:keys [agent
                               selected
                               on-change]} data]
  [ListGroupItem
   {:key (:name agent)
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
     (let [status-text (str "["(get-agent-status agent) "]")]
       (if (:last-build-webUrl agent)
         [:a {:href (:last-build-webUrl agent)
              :target "_blank"
              :on-click (fn [event]
                          (.stopPropagation event)
                          true)}
          status-text]
         [:span {} status-text]))]
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
     [autotc-web.home.controls.loader/loader {:visible show-loader}]
     [ListGroup
      [select-all-element {:visible (> (count agents) 0)
                           :on-change on-select-all
                           :checked (= (count agents) (count selected-agents))}]
      (for [agent (filter (fn [a]
                            (cond
                              (nil? filter-value)
                              true

                              (= filter-value :all)
                              true

                              (= filter-value :selected)
                              (is-agent-selected? selected-agents a)

                              :else
                              (not (is-agent-selected? selected-agents
                                                       a))))
                          agents)]
        [agent-list-item {:agent
                          agent

                          :selected
                          (is-agent-selected? selected-agents agent)

                          :on-change
                          (fn [checked] (on-select-agent agent checked))}])]]))

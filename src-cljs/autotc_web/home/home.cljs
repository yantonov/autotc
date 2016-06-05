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
            [autotc-web.home.actions :as actions]
            [autotc-web.home.controls.info-message]
            [autotc-web.home.controls.multi-action-toolbar]
            [autotc-web.home.controls.server-list]
            [autotc-web.home.controls.project-info]
            [autotc-web.home.controls.current-problems]
            [autotc-web.home.controls.current-problems-pages]
            [autotc-web.home.controls.current-problems-stats]
            [autotc-web.home.controls.agent-list]))

(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))

(defn home-page []
  (let [cursor (rcur/nest (rcur/make-cursor) :page)]
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
                      project
                      branches
                      agents
                      show-agent-list-loader
                      filter-value
                      current-problems
                      tests-with-expanded-stack-traces]
               :or {:show-agent-list-loader false}}
              (r/state this)
              selected-server (get servers selected-server-index)]
          [:div
           [autotc-web.home.controls.info-message/info-message message]
           [autotc-web.home.controls.server-list/server-list
            servers
            selected-server-index
            (fn [server-index] (actions/on-server-selected server-index cursor))]
           [:div {:style {:padding-left "0px"}}
            [Row {:class-name "agent-list"}
             [Col {:xs 12
                   :md 6}
              [autotc-web.home.controls.multi-action-toolbar/multi-action-toolbar
               {:enabled (not (empty? selected-agents))
                :visible (not (empty? agents))
                :all-agents-count (count agents)
                :selected-agents-count (count selected-agents)
                :on-start (fn [] (actions/start-build cursor))
                :on-stop (fn [] (actions/stop-build cursor))
                :on-restart (fn [] (actions/restart-build cursor))
                :on-reboot (fn [] (actions/reboot-agent cursor))

                :on-change-show-selected-only
                (fn [] (actions/change-show-selected-only cursor))

                :filter-value filter-value
                :filter-all (fn [] (actions/filter-show-all cursor))
                :filter-selected (fn [] (actions/filter-show-selected cursor))

                :filter-not-selected
                (fn [] (actions/filter-show-not-selected cursor))}]
              [autotc-web.home.controls.project-info/project-info
               project
               branches]
              [autotc-web.home.controls.agent-list/agent-list
               {:agents
                agents

                :selected-agents
                selected-agents

                :filter-value
                filter-value

                :on-select-agent
                (fn [agent selected?] (actions/on-agent-selected agent
                                                                 selected?
                                                                 cursor))
                :on-select-all
                (fn [selected?] (actions/on-all-agents-selected selected?
                                                                cursor))
                :show-loader
                show-agent-list-loader}]]
             [Col {:xs 12
                   :md 6}
              [autotc-web.home.controls.current-problems-stats/current-problems-stats
               (:id selected-server)
               current-problems
               (fn [] (actions/toggle-stack-traces selected-server
                                                   cursor
                                                   true))
               (fn [] (actions/toggle-stack-traces selected-server
                                                   cursor
                                                   false))]
              [autotc-web.home.controls.current-problems-pages/current-problems-pages
               current-problems
               (fn [page] (actions/select-current-problems-page selected-server
                                                                page
                                                                cursor))]
              [autotc-web.home.controls.current-problems/current-problems-list
               selected-server
               current-problems
               tests-with-expanded-stack-traces
               cursor
               (fn [test-name]
                 (actions/expand-stack-trace test-name cursor))]]]]]))})))

(defn ^:export init []
  (let [page (home-page)]
    (do
      (rmw/defmiddleware acm/action-creator-middleware)
      (reducers/define-reducers)
      (r/render-component [page]
                          (js/document.getElementById "main-content")))))

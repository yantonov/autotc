(ns autotc-web.home.controls.current-problems
  (:require [reagent.core :as r]
            [autotc-web.util.copy :as copy]
            [goog.string :as gstring]
            [autotc-web.util.html_helpers :as html]))

(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))

(defn current-problems-list [server problems tests-with-stack-traces cursor expand-stack-trace-fn]
  [:div
   nil
   (map
    (fn [problem]
      (let [stack-trace (or (:details problem) "")
            test-name (:name problem)]
        [:div {:key test-name}
         [Row nil
          [Col {:xs 6
                :md 3
                :class-name "single_problem"}
           [:a {:href "#"
                :on-click (fn [event]
                            (copy/copy test-name))
                :title "copy test name"}
            [:img {:src "/img/copy.png"
                   :class-name "copy_icon"
                   :alt "test name"}]]
           [:a {:href (:webUrl problem)
                :target "_blank"
                :title "test history"}
            [:img {:src "/img/clock.png"
                   :class-name "copy_icon"
                   :alt "test history"}]]
           [:a {:href (-> problem :build :webUrl)
                :target "_blank"}
            (->> problem :build :name)]]
          [Col {:xs 18
                :md 9}
           [:a {:href "#"
                :on-click (fn [event]
                            (copy/copy test-name)
                            (expand-stack-trace-fn test-name))
                :class-name "current_problem_item"}
            test-name]]]
         (if (or (:show-stacktraces problems)
                 (contains? tests-with-stack-traces test-name))
           [Row nil
            [:a {:href "#"
                 :on-click (fn [event]
                             (copy/copy stack-trace)
                             (.stopPropagation event))
                 :title "copy stack trace"
                 :class-name "stacktrace_link"}
             [:span {:class-name "stacktrace"}
              (gstring/unescapeEntities
               (html/html-escape stack-trace))]]]
           nil)]))
    (:problems problems))])

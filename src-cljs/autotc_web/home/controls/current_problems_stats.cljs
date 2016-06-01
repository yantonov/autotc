(ns autotc-web.home.controls.current-problems-stats
  (:require [goog.string :as gstring]))

(defn- current-problems-stats [current-problems change-show-stack-trace]
  (let [problems-count (get current-problems :problems-count 0)]
    [:div nil
     (if (zero? problems-count)
       "No failed tests. Congratulations!"
       [:div nil
        (gstring/format "Current problems: %d" problems-count)
        (gstring/unescapeEntities "&nbsp;")
        [:a {:href "#"
             :on-click (fn [event]
                         (change-show-stack-trace))}
         (if (:show-stacktraces current-problems)
           "hide stack"
           "show stack")]])]))

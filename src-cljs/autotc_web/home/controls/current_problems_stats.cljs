(ns autotc-web.home.controls.current-problems-stats
  (:require [goog.string :as gstring]))

(defn- current-problems-stats [server-id current-problems change-show-stack-trace]
  (let [problems-count (get current-problems :problems-count nil)]
    [:div nil
     (when-not (nil? problems-count)
       (if (zero? problems-count)
         [:span nil "No failed tests. Congratulations!"]
         [:div nil
          (gstring/format "Current problems: %d" problems-count)
          (gstring/unescapeEntities "&nbsp;")
          [:a {:href ""
               :on-click (fn [event]
                           (change-show-stack-trace)
                           (.preventDefault event))}
           (if (:show-stacktraces current-problems)
             "hide stack"
             "show stack")]
          (gstring/unescapeEntities "&nbsp;")
          [:a {:href (str "/download-current-problems?"
                          "id="
                          server-id)}
           "download failed tests"]]))]))

(ns autotc-web.home.controls.current-problems-stats
  (:require [goog.string :as gstring]))

(defn- current-problems-stats [server-id
                               current-problems
                               expand-stack-traces
                               collapse-stack-traces]
  (let [problems-count (get current-problems :problems-count)]
    [:div nil
     (when-not (nil? problems-count)
       (if (zero? problems-count)
         [:span nil "No failed tests. Congratulations!"]
         [:div nil
          (gstring/format "Current problems: %d" problems-count)
          (gstring/unescapeEntities "&nbsp;")
          [:a {:href ""
               :title "collapse stack traces"
               :on-click (fn [event]
                           (collapse-stack-traces)
                           (.preventDefault event))}
           [:img {:src "/img/collapse.png"
                  :alt "expand stack traces"} nil]]
          (gstring/unescapeEntities "&nbsp;")
          [:a {:href ""
               :title "expand stack traces"
               :on-click (fn [event]
                           (expand-stack-traces)
                           (.preventDefault event))}
           [:img {:src "/img/expand.png"
                  :alt "collapse stack traces"} nil]]
          (gstring/unescapeEntities "&nbsp;")
          [:a {:href (str "/download-current-problems?"
                          "id="
                          server-id)}
           [:img {:src "/img/save.png"
                  :alt "save"}
            nil]]]))]))

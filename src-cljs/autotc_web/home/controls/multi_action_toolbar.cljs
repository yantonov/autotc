(ns autotc-web.home.controls.multi-action-toolbar
  (:require [reagent.core :as r]))

(def ButtonToolbar (r/adapt-react-class js/ReactBootstrap.ButtonToolbar))
(def Button (r/adapt-react-class js/ReactBootstrap.Button))
(def Glyphicon (r/adapt-react-class js/ReactBootstrap.Glyphicon))

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


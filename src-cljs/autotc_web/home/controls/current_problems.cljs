(ns autotc-web.home.controls.current-problems
  (:require [reagent.core :as r]
            [autotc-web.util.copy :as copy]
            [goog.string :as gstring]
            [autotc-web.util.html_helpers :as html]))

(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))

(defn current-problems-list [server problems cursor]
  [:div
   nil
   (map (fn [problem]
          [:div
           {:key (:name problem)}
           [Row nil
            [Col {:xs 6
                  :md 3
                  :class-name "single_problem"}
             [:a {:href "#"
                  :on-click (fn [event]
                              (copy/copy (:name problem))
                              (.stopPropagation event))
                  :title "copy test name"}
              [:img {:src "/img/copy.png"
                     :class-name "copy_icon"
                     :alt "test name"}]]
             [:a {:href "#"
                  :on-click (fn [event]
                              (copy/copy (:details problem))
                              (.stopPropagation event))
                  :title "copy stack trace"}
              [:img {:src "/img/stack.png"
                     :class-name "copy_icon"
                     :alt "stack trace"}]]
             [:a {:href (->> problem
                             :build
                             :webUrl)
                  :target "_blank"}
              (->> problem
                   :build
                   :name)]]
            [Col {:xs 18
                  :md 9}
             [:a {:href (:webUrl problem)
                  :target "_blank"
                  :class-name "current_problem_item"}
              (:name problem)]]]
           (if (:show-stacktraces problems)
             [Row nil
              [:div {:class-name "stacktrace"}
               (gstring/unescapeEntities
                (html/html-escape (:details problem)))]]
             nil)])
        (:problems problems))])

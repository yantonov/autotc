(ns autotc-web.home.controls.current-problems
  (:require [reagent.core :as r]
            [autotc-web.util.copy :as copy]
            [goog.string :as gstring]
            [autotc-web.util.html_helpers :as html]))

(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))

(defn copy-test-name-icon [test-name
                           mark-test-name-as-copied
                           on-mouseover-test-name
                           on-mouseout-test-name
                           tests-with-copy-hint
                           test-names-inside-clipboard]
  [:div {:class-name "test-action"}
   [:a {:on-click (fn [event]
                    (mark-test-name-as-copied test-name)
                    (copy/copy test-name))
        :on-mouse-over (fn [event]
                         (on-mouseover-test-name test-name))
        :on-mouse-out (fn [event]
                        (on-mouseout-test-name test-name))}
    [:div {:class-name "hint-placeholder copy-test-name-icon"}
     (if (contains? tests-with-copy-hint test-name)
       [:span {:class-name "hint icon-hint"}
        (if (contains? test-names-inside-clipboard test-name)
          "Copied"
          "Copy test name to clipboard")])]]])

(defn copy-stack-icon [test-name
                       stack-trace
                       mark-test-name-as-copied
                       on-mouseover-stack
                       on-mouseout-stack
                       tests-with-copy-stack-hint
                       test-names-inside-clipboard]
  [:div {:class-name "test-action"}
   [:a {:on-click (fn [event]
                    (mark-test-name-as-copied test-name)
                    (copy/copy stack-trace))
        :on-mouse-over (fn [event]
                         (on-mouseover-stack test-name))
        :on-mouse-out (fn [event]
                        (on-mouseout-stack test-name))}
    [:div {:class-name "hint-placeholder copy-stack-icon"}
     (if (contains? tests-with-copy-stack-hint test-name)
       [:span {:class-name "hint icon-hint"}
        (if (contains? test-names-inside-clipboard test-name)
          "Copied"
          "Copy stack to clipboard")])]]])

(defn test-history-icon [problem]
  [:div {:class-name "test-action"}
   [:a {:href (:webUrl problem)
        :target "_blank"
        :title "test history"}
    [:div {:class-name "hint-placeholder test-history-icon"}]]])

(defn build-link [problem]
  [:div {:class-name "build-link"}
   [:a {:href (-> problem :build :webUrl)
        :target "_blank"
        :title "build"}
    (->> problem :build :name)]])

(defn test-name-link [test-name
                      expand-stack-trace-fn]
  [:a {:on-click (fn [event]
                   (copy/copy test-name)
                   (expand-stack-trace-fn test-name))
       :class-name "pointer"}
   test-name])

(defn stack-trace-control [test-name
                           stack-trace
                           problems
                           tests-with-stack-traces]
  (letfn [(xor [a b] (not (= a b)))]
    (if (xor (:show-stacktraces problems)
             (contains? tests-with-stack-traces test-name))
      [Row nil
       [:span {:class-name "stacktrace"}
        (gstring/unescapeEntities
         (html/html-escape stack-trace))]]
      nil)))

(defn current-problems-list [server
                             problems
                             tests-with-stack-traces
                             tests-with-copy-hint
                             tests-with-copy-stack-hint
                             test-names-inside-clipboard
                             cursor
                             expand-stack-trace-fn
                             on-mouseover-test-name
                             on-mouseout-test-name
                             mark-test-name-as-copied
                             on-mouseover-stack
                             on-mouseout-stack]
  [:div nil
   (map
    (fn [problem]
      (let [stack-trace (or (:details problem) "")
            test-name (:name problem)]
        [:div {:key test-name}
         [Row nil
          [Col {:xs 6
                :md 3
                :class-name "test-actions"}
           [copy-test-name-icon
            test-name
            mark-test-name-as-copied
            on-mouseover-test-name
            on-mouseout-test-name
            tests-with-copy-hint
            test-names-inside-clipboard]

           [copy-stack-icon
            test-name
            stack-trace
            mark-test-name-as-copied
            on-mouseover-stack
            on-mouseout-stack
            tests-with-copy-stack-hint
            test-names-inside-clipboard]

           [test-history-icon problem]

           [build-link problem]]

          [Col {:xs 18
                :md 9
                :class-name "current_problem_item"}
           [test-name-link
            test-name
            expand-stack-trace-fn]]]

         [stack-trace-control
          test-name
          stack-trace
          problems
          tests-with-stack-traces]]))
    (:problems problems))])

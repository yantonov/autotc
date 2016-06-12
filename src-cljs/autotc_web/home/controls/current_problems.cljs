(ns autotc-web.home.controls.current-problems
  (:require [reagent.core :as r]
            [autotc-web.util.copy :as copy]
            [goog.string :as gstring]
            [autotc-web.util.html_helpers :as html]))

(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))

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
            test-name (:name problem)
            xor (fn [a b] (not (= a b)))]
        [:div {:key test-name}
         [Row nil
          [Col {:xs 6
                :md 3
                :class-name "test-actions"}
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
                   "Copy test name to clipboard")])]]]
           [:div {:class-name "test-action"}
            [:a {:on-click (fn [event]
                             (mark-test-name-as-copied test-name)
                             (copy/copy stack-trace)
                             (print stack-trace))
                 :on-mouse-over (fn [event]
                                  (on-mouseover-stack test-name))
                 :on-mouse-out (fn [event]
                                 (on-mouseout-stack test-name))}
             [:div {:class-name "hint-placeholder copy-stack-icon"}
              (if (contains? tests-with-copy-stack-hint test-name)
                [:span {:class-name "hint icon-hint"}
                 (if (contains? test-names-inside-clipboard test-name)
                   "Copied"
                   "Copy stack to clipboard")])]]]
           [:div {:class-name "test-action"}
            [:a {:href (:webUrl problem)
                 :target "_blank"
                 :title "test history"}
             [:div {:class-name "hint-placeholder test-history-icon"}]]]
           [:div {:class-name "build-link"}
            [:a {:href (-> problem :build :webUrl)
                 :target "_blank"
                 :title "build"}
             (->> problem :build :name)]]]
          [Col {:xs 18
                :md 9}
           [:a {:on-click (fn [event]
                            (copy/copy test-name)
                            (expand-stack-trace-fn test-name))
                :class-name "current_problem_item pointer"}
            test-name]]]
         (if (xor (:show-stacktraces problems)
                  (contains? tests-with-stack-traces test-name))
           [Row nil
            [:span {:class-name "stacktrace pointer"}
             (gstring/unescapeEntities
              (html/html-escape stack-trace))]]
           nil)]))
    (:problems problems))])

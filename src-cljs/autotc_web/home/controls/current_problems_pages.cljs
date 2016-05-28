(ns autotc-web.home.controls.current-problems-pages)

(defn current-problems-pages [current-problems select-page]
  (let [page-count (:page-count current-problems)
        current-page (:current-page current-problems)
        x (do (print page-count) 1)]
    (if (<= page-count 1)
      nil
      [:div {:class-name "current_problems_pages"}
       (map (fn [[tag attrs content] index]
              [tag (assoc attrs :key (str "current-problems-page-" index)) content])
            (interpose [:span {} " "]
                       (map (fn [i]
                              (let [default-attrs {:class-name "current_problems_page"}]
                                (if (= i current-page)
                                  [:span default-attrs (str i)]
                                  [:a (-> default-attrs
                                          (assoc :on-click (fn [event] (select-page i)))
                                          (assoc :href "#"))
                                   (str i)])))
                            (range 1 (inc page-count))))
            (iterate inc 1))])))

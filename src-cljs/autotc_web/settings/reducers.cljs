(ns autotc-web.settings.reducers
  (:require [rex.ext.reducer-for-type :as r]
            [rex.ext.cursor :as c]
            [autotc-web.util.reducer-helpers :as h]))

(defn define-reducers []
  (r/reducer-for-type :init-page
                      (fn [state action]
                        (c/set-state (:cursor action)
                                     state
                                     {:show-list true
                                      :servers []})))

  (r/reducer-for-type :server-list-loaded
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:servers (:servers action)})))

  (r/reducer-for-type :toggle-list
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:show-list (:visible action)})))

  (r/reducer-for-type :confirm-delete-server
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:server-to-delete (:server action)})))

  (r/reducer-for-type :text-input-changed
                      (fn [state action]
                        (let [cursor (:cursor action)
                              value (:value action) ]
                          (c/set-state cursor
                                       state
                                       value)))))

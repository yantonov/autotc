(ns autotc-web.util.reducer-helpers
  (:require [rex.ext.cursor :as rcur]))

(defn merge-state [state cursor local-update-data]
  (let [old-local-state (rcur/get-state cursor state)
        new-local-state (merge old-local-state
                               local-update-data)]
    (rcur/set-state cursor
                    state
                    new-local-state)))

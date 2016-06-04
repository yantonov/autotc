(ns autotc-web.util.poller)

(defonce poller-events ["mousemove"
                        "mouseup"
                        "keyup"
                        "keydown"])

(defprotocol IPoller
  (start [this])
  (stop [this]))

(deftype Poller [activity-timeout
                 idle-timeout
                 state-atom]
  IPoller
  (start [this]
    (let [{:keys [activity-listener
                  activity-timer-fn
                  idle-timer-fn]} @state-atom]
      (do
        (doseq [e poller-events]
          (.addEventListener js/document e activity-listener))

        (swap! state-atom
               (fn [a]
                 (assoc a
                        :activity-timer
                        (js/setInterval activity-timer-fn
                                        activity-timeout))))
        (swap! state-atom
               (fn [a]
                 (assoc a
                        :idle-timer
                        (js/setInterval idle-timer-fn
                                        idle-timeout)))))))

  (stop [this]
    (let [{:keys [activity-listener
                  activity-timer
                  idle-timer]} @state-atom]
      (do
        (doseq [e poller-events]
          (js/document.removeEventListener e activity-listener))

        (when-let [timer activity-timer]
          (js/clearInterval timer))
        (when-let [timer idle-timer]
          (js/clearInterval timer))))))

(defn create-poller [func
                     activity-timeout
                     idle-timeout]
  (let [timer-fn (fn [get-state-fn
                      allow-fire
                      callback-fn]
                   (let [state (get-state-fn)
                         now (js/Date.)
                         last-activity-time (:last-activity-time state)
                         diff (.abs js/Math (- (.getTime now)
                                               (.getTime last-activity-time)))]
                     (do

                       (if (allow-fire diff)
                         (do
                           (callback-fn))))))
        state (atom {})
        activity-listener #(swap! state assoc :last-activity-time (js/Date.))]
    (do
      (swap! state
             (fn [s]
               (-> s
                   (assoc :last-activity-time
                          (js/Date.))
                   (assoc :activity-listener
                          activity-listener)
                   (assoc :activity-timer-fn
                          (fn []
                            (timer-fn (fn [] @state)
                                      (fn [time]
                                        (< time idle-timeout))
                                      func)))
                   (assoc :idle-timer-fn
                          (fn []
                            (timer-fn (fn [] @state)
                                      (fn [time]
                                        (> time idle-timeout))
                                      func))))))
      (Poller. activity-timeout
               idle-timeout
               state))))

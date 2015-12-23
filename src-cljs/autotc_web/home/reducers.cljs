(ns autotc-web.home.reducers
  (:require [rex.ext.cursor :as c]
            [rex.ext.reducer-for-type :as r]
            [autotc-web.util.reducer-helpers :as h]))

(defn update-agent-selection [set agent selected?]
  ((if selected? conj disj) set (:id agent)))

(defn- define-reducers []
  (r/reducer-for-type :init-page
                      (fn [state action]
                        (c/update-state (:cursor action) state {})))

  (r/reducer-for-type :new-server-list
                      (fn [state action]
                        (c/update-state (:cursor action)
                                        state
                                        {:servers (:servers action)
                                         :agents []
                                         :selected-agents #{}
                                         :manually-selected-agents #{}})))

  (r/reducer-for-type :new-agent-list
                      (fn [state action]
                        (h/merge-state state
                                         (:cursor action)
                                         {:agents (:agents action)
                                          :show-agent-list-loader false})))

  (r/reducer-for-type :reset-agent-list
                      (fn [state action]
                        (h/merge-state state
                                         (:cursor action)
                                         {:agents []
                                          :show-agent-list-loader false
                                          :selected-agents #{}
                                          :manually-selected-agents #{}})))

  (r/reducer-for-type :init-load-agent-list
                      (fn [state action]
                        (h/merge-state state
                                         (:cursor action)
                                         {:show-agent-list-loader true
                                          :selected-server-index (:server-index action)
                                          :selected-agents #{}
                                          :manually-selected-agents #{}
                                          :agents []})))

  (r/reducer-for-type :attach-poll-agent-timer
                      (fn [state action]
                        (h/merge-state state
                                         (:cursor action)
                                         {:poll-agent-timer (:poll-agent-timer action)})))

  (r/reducer-for-type :agent-selected
                      (fn [state action]
                        (let [cursor (:cursor action)
                              {:keys [selected-agents
                                      manually-selected-agents]} (c/get-state cursor state)
                              {:keys [agent
                                      selected?]} action]
                          (h/merge-state state
                                           cursor
                                           {:selected-agents (update-agent-selection selected-agents
                                                                                     agent
                                                                                     selected?)
                                            :manually-selected-agents (update-agent-selection manually-selected-agents
                                                                                              agent
                                                                                              selected?)}))))

  (r/reducer-for-type :select-all-agents
                      (fn [state action]
                        (let [cursor (:cursor action)
                              old-state (c/get-state cursor state)
                              {agents :agents
                               selected-agents :selected-agents
                               manually-selected-agents :manually-selected-agents} old-state
                              new-selected-agents
                              (if (empty? selected-agents)
                                (if (empty? manually-selected-agents)
                                  (apply hash-set (map :id agents))
                                  (apply hash-set manually-selected-agents))
                                (if (< (count selected-agents) (count agents))
                                  (apply hash-set (map :id agents))
                                  #{}))]
                          (h/merge-state state
                                           cursor
                                           {:selected-agents new-selected-agents}))))

  (r/reducer-for-type :show-message
                      (fn [state action]
                        (let [{:keys [message
                                      message-timer]} action]
                          (h/merge-state state
                                           (:cursor action)
                                           {:message message
                                            :message-timer message-timer}))))

  (r/reducer-for-type :hide-message
                      (fn [state action]
                        (h/merge-state state
                                         (:cursor action)
                                         {:message nil})))
  )

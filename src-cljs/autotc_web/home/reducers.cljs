(ns autotc-web.home.reducers
  (:require [rex.ext.cursor :as c]
            [rex.ext.reducer-for-type :as r]
            [autotc-web.util.reducer-helpers :as h]))

(defn update-agent-selection [set agent selected?]
  ((if selected? conj disj) set (:id agent)))

(defn adjust-filter-value [old-filter-value all-agents new-selected-agents]
  (if (or (= (count new-selected-agents)
             (count all-agents))
          (= 0 (count new-selected-agents)))
    :all
    old-filter-value))

(defn- define-reducers []
  (r/reducer-for-type :init-page
                      (fn [state action]
                        (c/update-state (:cursor action) state {})))

  (r/reducer-for-type :on-server-list-loaded
                      (fn [state action]
                        (c/update-state (:cursor action)
                                        state
                                        {:servers (:servers action)
                                         :agents []
                                         :branches []
                                         :selected-agents #{}
                                         :manually-selected-agents #{}
                                         :current-problems []})))

  (r/reducer-for-type :on-agents-list-loaded
                      (fn [state action]
                        (let [{:keys [cursor
                                      agents]}
                              action

                              {:keys [selected-agents
                                      manually-selected-agents]}
                              (c/get-state cursor state)

                              agents-ids (vec (map :id agents))]
                          (h/merge-state state
                                         (:cursor action)
                                         {:agents
                                          (:agents action)

                                          :branches
                                          (:branches action)

                                          :show-agent-list-loader
                                          false

                                          :selected-agents
                                          (apply hash-set (filter (fn [a] (contains? selected-agents a))
                                                                  agents-ids))
                                          :manually-selected-agents
                                          (apply hash-set (filter (fn [a] (contains? manually-selected-agents a))
                                                                  agents-ids))}))))

  (r/reducer-for-type :agent-list-is-loading
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:show-agent-list-loader true
                                        :agents []
                                        :branches []})))

  (r/reducer-for-type :init-load-agent-list
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:show-agent-list-loader true
                                        :selected-server-index (:server-index action)
                                        :selected-agents #{}
                                        :manually-selected-agents #{}
                                        :agents []
                                        :branches []
                                        :filter-value nil
                                        :current-problems []})))

  (r/reducer-for-type :attach-poll-agent-timer
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:poll-agent-timer (:poll-agent-timer action)})))

  (r/reducer-for-type :agent-selected
                      (fn [state action]
                        (let [cursor (:cursor action)
                              {:keys [agents
                                      selected-agents
                                      manually-selected-agents
                                      filter-value]} (c/get-state cursor state)
                              {:keys [agent
                                      selected?]} action
                              new-selected-agents (update-agent-selection selected-agents
                                                                          agent
                                                                          selected?)]
                          (h/merge-state state
                                         cursor
                                         {:selected-agents new-selected-agents
                                          :manually-selected-agents (update-agent-selection manually-selected-agents
                                                                                            agent
                                                                                            selected?)
                                          :filter-value (adjust-filter-value filter-value
                                                                             agents
                                                                             new-selected-agents)}))))

  (r/reducer-for-type :select-all-agents
                      (fn [state action]
                        (let [cursor (:cursor action)
                              old-state (c/get-state cursor state)
                              {:keys [agents
                                      selected-agents
                                      manually-selected-agents
                                      filter-value]} old-state
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
                                         {:selected-agents new-selected-agents
                                          :filter-value (adjust-filter-value filter-value
                                                                             agents
                                                                             new-selected-agents)}))))

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

  (r/reducer-for-type :filter-changed
                      (fn [state action]
                        (let [cursor (:cursor action)
                              old-state (c/get-state cursor state)
                              {:keys [selected-agents
                                      agents
                                      filter-value]} old-state

                              new-filter-value
                              (cond
                                (empty? selected-agents)
                                :all

                                (= (count selected-agents)
                                   (count agents))
                                :all

                                true
                                (:value action))]
                          (h/merge-state state
                                         (:cursor action)
                                         {:filter-value new-filter-value}))))

  (r/reducer-for-type :on-current-problems-list-loaded
                      (fn [state action]
                        (h/merge-state state
                                       (:cursor action)
                                       {:current-problems
                                        (:current-problems action)})))
  )

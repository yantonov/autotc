(ns autotc-web.home.actions
  (:require [autotc-web.util.poller :as poller]
            [ajax.core :as ajax]
            [rex.ext.cursor :as cur]
            [rex.core :as r]))

(defn hide-message-action-creator [cursor]
  (fn [dispatch get-store]
    (dispatch {:type :hide-message
               :cursor cursor})))

(defn show-message-action-creator [message cursor]
  (fn [dispatch get-state]
    (let [state (cur/get-state cursor (get-state))]
      (when-let [message-timer (:message-timer state)]
        (js/clearTimeout message-timer))
      (dispatch {:type :show-message
                 :cursor cursor
                 :message message
                 :message-timer (js/setTimeout (fn [] (dispatch (hide-message-action-creator cursor)))
                                               5000)}))))

(defn reset-timer-action-creator [cursor]
  (fn [dispatch get-store]
    (when-let [timer (:poll-agent-timer (cur/get-state cursor (get-store)))]
      (poller/stop timer))
    nil))

(defn other-server-selected? [state load-agents-for-server]
  (if (nil? load-agents-for-server)
    true
    (let [selected-server-index (:selected-server-index state)
          selected-server (get (:servers state) selected-server-index)]
      (and (not (nil? selected-server))
           (not (= (:id selected-server)
                   (:id load-agents-for-server)))))))

(defn load-agents-action-creator [server cursor]
  (fn [dispatch get-state]
    (if (other-server-selected? (cur/get-state cursor
                                               (get-state))
                                server)
      nil
      (do
        (let [url (str "/agents/list/" (:id server))]
          (ajax/GET
           url
           {:response-format (ajax/json-response-format {:keywords? true})
            :handler (fn [response]
                       (if (other-server-selected? (cur/get-state cursor
                                                                  (get-state))
                                                   server)
                         nil
                         (dispatch {:type :new-agent-list
                                    :cursor cursor
                                    :agents (:agents response)})))
            :error-handler (fn [response]
                             (dispatch {:type :reset-agent-list
                                        :cursor cursor}))}))))))

(defn select-server-action-creator [server-index cursor]
  (fn [dispatch get-state]
    (let [state (cur/get-state cursor (get-state))]
      (if (= server-index
             (:selected-server-index state))
        nil
        (do
          (dispatch {:type :init-load-agent-list
                     :cursor cursor
                     :server-index server-index})
          (dispatch (reset-timer-action-creator cursor))
          (let [current-server (get (:servers state) server-index)
                p (poller/create-poller (fn []
                                          (dispatch (load-agents-action-creator current-server cursor)))
                                        3000
                                        60000)]
            (do
              (poller/start p)
              (dispatch {:type :attach-poll-agent-timer
                         :cursor cursor
                         :poll-agent-timer p}))))))))

(defn get-server-list-action-creator [cursor]
  (fn [dispatch get-state]
    (ajax/GET
     "/servers/list"
     {:params {}
      :response-format (ajax/json-response-format {:keywords? true})
      :handler
      (fn [response]
        (let [servers (:servers response)
              has-any-server? (and (not (nil? servers))
                                   (> (count servers)))]
          (do
            (dispatch {:type :new-server-list
                       :cursor cursor
                       :servers servers})
            (if has-any-server?
              (dispatch (select-server-action-creator 0 cursor))))))})))

(defn exec-action-for-agents-action-creator [cursor url trigger-message completed-message]
  (fn [dispatch get-store]
    (let [s (cur/get-state cursor (get-store))
          current-server-id (:id (get (:servers s) (:selected-server-index s)))
          agent-ids (clj->js (map identity (:selected-agents s)))]
      (do
        (dispatch (show-message-action-creator trigger-message cursor))
        (ajax/POST url
                   {:params {"serverId" current-server-id
                             "agentIds" agent-ids}
                    :format (ajax/json-request-format)
                    :handler (fn [response]
                               (dispatch (show-message-action-creator completed-message cursor)))
                    :error-handler (fn [response] (println response))})))))


(defn reset-timer [cursor]
  (r/dispatch (reset-timer-action-creator cursor)))

(defn init-page [cursor]
  (r/dispatch {:type :init-page
               :cursor cursor}))

(defn load-server-list [cursor]
  (r/dispatch (get-server-list-action-creator cursor)))

(defn on-server-selected [server-index cursor]
  (r/dispatch (select-server-action-creator server-index cursor)))

(defn on-agent-selected [agent selected? cursor]
  (r/dispatch {:type :agent-selected
               :cursor cursor
               :agent agent
               :selected? selected?}))

(defn all-agents-selected [selected? cursor]
  (r/dispatch {:type :select-all-agents
               :cursor cursor}))

(defn exec-action-for-agents [cursor url trigger-message completed-message]
  (r/dispatch (exec-action-for-agents-action-creator cursor
                                                     url
                                                     trigger-message
                                                     completed-message)))

(defn start-build [cursor]
  (exec-action-for-agents cursor
                          "/agents/startBuild"
                          "request to trigger build was sent"
                          "build triggered"))

(defn stop-build [cursor]
  (exec-action-for-agents cursor
                          "/agents/stopBuild"
                          "request to stop build was sent"
                          "build stopped"))

(defn reboot-agent [cursor]
  (exec-action-for-agents cursor
                          "/agents/rebootAgent"
                          "request to reboot agent was sent"
                          "reboot triggered"))

(defn run-custom-build [cursor]
  (exec-action-for-agents cursor
                          "/agents/runCustomBuild"
                          "request to run custom build was sent"
                          "custom build has triggered"))

(defn show-message-dialog [cursor message]
  (r/dispatch (show-message-action-creator message cursor)))

(defn hide-message-dialog [cursor]
  (r/dispatch (hide-message-action-creator cursor)))
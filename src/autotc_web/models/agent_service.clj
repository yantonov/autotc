(ns autotc-web.models.agent-service
  (:require [autotc-web.models.db :as db])
  (:import api.http.teamcity.io.TeamCitySession))

(def CACHED_TIME_IN_SECONDS 3)

(def cache (ref {}))

(defprotocol IAgentInfo
  (get-value [this])

  (wait-value [this]))

(defrecord AgentInfo [agent]
  IAgentInfo
  (get-value [_]
    @agent)

  (wait-value [_]
    (await agent)))

(defn reset-cache []
  (dosync
   (ref-set cache {})))

(defn get-memoized-agents [server-id
                           get-agents-info
                           get-now
                           get-initial-last-updated
                           update-needed?]
  (dosync
   (let [server-info (get @cache server-id)
         now (get-now)]
     (if (nil? server-info)
       (alter cache assoc server-id {:agent (agent {})
                                     :last-updated (get-initial-last-updated now)}))
     (let [{last-updated :last-updated
            a :agent}
           (get-in @cache [server-id])]
       (if (update-needed? last-updated now)
         (do
           (alter cache assoc-in [server-id :last-updated] now)
           (send a (fn [_]
                     (try
                       {:agents (get-agents-info server-id)}
                       (catch Exception e
                         {:error e}))))))
       (AgentInfo. a)))))

(defn get-now []
  (java.time.LocalDateTime/now))

(defn get-initial-last-updated [now]
  (.plusSeconds
   now
   (- (* 2 CACHED_TIME_IN_SECONDS))))

(defn update-needed? [last-updated now]
  (> (.between
      java.time.temporal.ChronoUnit/SECONDS
      last-updated
      now)
     CACHED_TIME_IN_SECONDS))

(defn request-agents-from-teamcity [server-id]
  (let [server (db/get-server-by-id (Long/parseLong (str server-id)))
        session (TeamCitySession/create server)]
    (-> session
        .getProject
        .getConfigurations)))

(defn get-agents [server-id]
  (get-memoized-agents server-id
                       request-agents-from-teamcity
                       get-now
                       get-initial-last-updated
                       update-needed?))

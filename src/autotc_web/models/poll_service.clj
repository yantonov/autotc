(ns autotc-web.models.poll-service
  (:require [autotc-web.models.exception :as exception]))

(def CACHED_TIME_IN_SECONDS 3)

(def cache (ref {}))

(defprotocol IProjectInfo
  (get-value [this])

  (wait-value [this]))

(defrecord ProjectInfo [agent]
  IProjectInfo
  (get-value [_]
    @agent)

  (wait-value [_]
    (await agent)))

(defn reset-cache []
  (dosync
   (ref-set cache {})))

(defn get-memoized-info [server-id
                         get-project-info
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
       (when (update-needed? last-updated now)
         (alter cache assoc-in [server-id :last-updated] now)
         (send a (fn [_]
                   (try
                     {:info (get-project-info server-id)}
                     (catch Exception e
                       {:error (exception/pretty-print-exception e)})))))
       (ProjectInfo. a)))))

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

(defn cached [server-id retrieve-data-fn]
  (get-memoized-info server-id
                     retrieve-data-fn
                     get-now
                     get-initial-last-updated
                     update-needed?))

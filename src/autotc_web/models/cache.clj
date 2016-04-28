(ns autotc-web.models.cache
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

(defn get-memoized-info [cache-key
                         retrieve-data-fn
                         get-now
                         get-initial-last-updated
                         update-needed?]
  (dosync
   (let [server-info (get @cache cache-key)
         now (get-now)]
     (if (nil? server-info)
       (alter cache assoc cache-key {:agent (agent {})
                                     :last-updated (get-initial-last-updated now)}))
     (let [{last-updated :last-updated
            a :agent}
           (get-in @cache [cache-key])]
       (when (update-needed? last-updated now)
         (alter cache assoc-in [cache-key :last-updated] now)
         (send a (fn [_]
                   (try
                     {:info (retrieve-data-fn)}
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

(defn cached [cache-key retrieve-data-fn]
  (get-memoized-info cache-key
                     retrieve-data-fn
                     get-now
                     get-initial-last-updated
                     update-needed?))

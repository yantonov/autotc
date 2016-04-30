(ns autotc-web.routes.home
  (:require [autotc-web.log :as log]
            [autotc-web.models.cache :as chc]
            [autotc-web.models.db :as db]
            [autotc-web.models.tc :as tc]
            [autotc-web.models.exception :as exception]
            [autotc-web.views.layout :as layout]
            [clojure.pprint :as pprint]
            [compojure.core :refer :all]
            [ring.util.response :as rur]))

(defn- home []
  (layout/common [:script {:type "text/javascript"
                           :src "/cljs/dev/autotc-web.js"}]
                 [:script {:type "text/javascript"
                           :src "/cljs/home/home.js"}]))

(defn- tc-server-to-json [server]
  (select-keys server [:alias :id]))

(defn- build-type-info-to-json [build-type-info]
  (let [build-type (:build-type build-type-info)
        last-build (:last-build build-type-info)
        queue (:queue build-type-info)]
    (hash-map :id (:id build-type)
              :name (:name build-type)
              :webUrl (:webUrl build-type)
              :running (:running last-build)
              :status (:status last-build)
              :statusText (get-in build-type-info [:last-build-details :status-text])
              :queue-webUrl (:webUrl queue)
              :last-build-webUrl (:webUrl last-build))))

(defn- get-servers []
  (rur/response {:servers (map tc-server-to-json
                               (db/read-servers))}))

(defn- request-project-info-from-teamcity [server-id]
  (let [server (db/get-server-by-id (Long/parseLong (str server-id)))]
    (tc/project-info (:host server)
                     (:port server)
                     (:project server)
                     (:username server)
                     (:password server))))

(defn- request-current-problems [server-id]
  (let [server (db/get-server-by-id (Long/parseLong (str server-id)))]
    (tc/current-problems (:host server)
                         (:port server)
                         (:project server)
                         (:username server)
                         (:password server))))

;; TODO: not agent but build types
(defn- agents-for-server [server-id]
  (let [{:keys [info error]}
        (.get-value (chc/cached (keyword (str "project-info-" server-id))
                                (fn []
                                  (request-project-info-from-teamcity server-id))))
        build-types (:build-types info)
        branches (:branches info)]
    (rur/response {:branches
                   branches

                   :agents
                   (if (not (nil? build-types))
                     (map build-type-info-to-json build-types)
                     nil)

                   :error
                   error})))

(defn- current-problems [server-id]
  (let [{:keys [info error]}
        (.get-value (chc/cached (keyword (str "current-problems-" server-id))
                                (fn []
                                  (request-current-problems server-id))
                                :cache-seconds 30))]
    (rur/response {:current-problems
                   (get info :current-problems [])

                   :error
                   error})))

(defn- exec-action-for-agents [server-id build-type-ids action]
  ;; holy shit
  ;; TODO: decompose this method
  (try
    (let [{:keys [host port username password]}
          (db/get-server-by-id (Long/parseLong (str server-id)))]
      (rur/response
       (reduce (fn [result build-type-id]
                 (try
                   (do
                     (action host port username password build-type-id)
                     (update-in result [:count] inc))
                   (catch Exception e
                     (do
                       (log/error e
                                  (str "cant exec action for server:"
                                       server-id
                                       " build type id: "
                                       build-type-id))
                       (assoc-in result [:error] (exception/pretty-print-exception e))))))
               {:count 0
                :error ""}
               build-type-ids)))
    (catch Exception e
      (let [error (exception/pretty-print-exception e)]
        (log/error e (str "cant exec action for server:" server-id " agents: " build-type-ids))
        (rur/response {:error error})))))

(defn- start-build [server-id build-type-ids]
  (exec-action-for-agents server-id
                          build-type-ids
                          tc/trigger-build))

(defn- stop-build [server-id build-type-ids]
  (exec-action-for-agents server-id
                          build-type-ids
                          tc/cancel-build))

(defn- restart-build [server-id build-type-ids]
  (exec-action-for-agents server-id
                          build-type-ids
                          (fn [host port user pass build-type-id]
                            (try (tc/cancel-build host port user pass build-type-id)
                                 (catch Exception e
                                   (log/error e)))
                            (try (tc/trigger-build host port user pass build-type-id)
                                 (catch Exception e
                                   (log/error e))))))

(defn- reboot-agent [server-id build-type-ids]
  (exec-action-for-agents server-id
                          build-type-ids
                          tc/reboot-agent))

(defroutes home-routes
  (GET "/" [] (home))
  (GET "/servers/list" [] (get-servers))
  (GET "/agents/list/:id" [id] (agents-for-server id))
  (GET "/current-problems/:id" [id] (current-problems id))
  (POST "/agents/startBuild"
      request
    (fn [request]
      (let [{serverId "serverId"
             agentIds "agentIds"} (:params request)]
        (start-build serverId agentIds))))
  (POST "/agents/stopBuild"
      request
    (fn [request]
      (let [{serverId "serverId"
             agentIds "agentIds"} (:params request)]
        (stop-build serverId agentIds))))
  (POST "/agents/restartBuild"
      request
    (fn [request]
      (let [{serverId "serverId"
             agentIds "agentIds"} (:params request)]
        (restart-build serverId agentIds))))
  (POST "/agents/rebootAgent"
      request
    (fn [request]
      (let [{serverId "serverId"
             agentIds "agentIds"} (:params request)]
        (reboot-agent serverId agentIds)))))

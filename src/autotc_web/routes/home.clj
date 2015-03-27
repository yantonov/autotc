(ns autotc-web.routes.home
  (:require [compojure.core :refer :all]
            [autotc-web.views.layout :as layout]
            [autotc-web.models.db :as db]
            [ring.util.response :as rur])
  (:import api.http.teamcity.domain.TeamCityServer)
  (:import api.http.teamcity.io.TeamCityProxy)
  (:import api.http.teamcity.io.TeamCitySession))

(defn- home []
  (layout/common [:script {:type "text/javascript"
                           :src "/js/combined/home/home.js"}]))

(defn- tc-server-to-json [^TeamCityServer server]
  (hash-map :alias (. server getAlias)
            :id (. server getId)))

(defn- pretty-print-exception [e]
  (clojure.string/join
   "\n"
   (concat [(.getMessage e)]
           (map (fn [item]
                  (format "%s %s.%s:%d"
                          (.getFileName item)
                          (.getClassName item)
                          (.getMethodName item)
                          (.getLineNumber item)))
                (.getStackTrace e)))))

(defn- tc-agent-to-json [agent]
  (let [build (. agent getLastBuild)]
    (hash-map :id (. agent getId)
              :name (. agent toString)
              :running (. build isRunning)
              :status (-> build
                          .getStatus
                          .toString)
              :statusText (. build getStatusText))))

(defn- get-servers []
  (rur/response {:servers (map tc-server-to-json
                               (db/read-servers))}))

(defn- agents-for-server [server-id]
  (let [server (db/get-server-by-id server-id)
        session (TeamCitySession/create server)]
    (rur/response {:agents (map tc-agent-to-json
                                (-> session
                                    .getProject
                                    .getConfigurations))})))

(defn- exec-action-for-agents [server-id agent-ids session-action]
  (try
    (let [server (db/get-server-by-id server-id)
          session (TeamCitySession/create server)
          ids-set (set agent-ids)
          agents (filter #(contains? ids-set (. % getId))
                         (-> session
                             .getProject
                             .getConfigurations))]
      (rur/response
       (reduce (fn [result agent]
                 (try
                   (do
                     (session-action session agent)
                     (assoc result :count (inc (:count result))))
                   (catch Exception e
                     (do
                       (. (System/err) println (.getMessage e))
                       (assoc result :error
                              (str (:error result)
                                   " "
                                   (pretty-print-exception e)))))))
               {:count 0
                :error ""}
               agents)))
    (catch Exception e
      (let [error (pretty-print-exception e)]
        (. (System/err) println error)
        (rur/response {:error error})))))

(defn- start-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (. session start agent))))

(defn- stop-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (. session stop agent))))

(defn- reboot-agent [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (. session rebootMachine agent))))

(defn- run-custom-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (. session runCustomBuild agent))))

(defroutes home-routes
  (GET "/" [] (home))
  (GET "/servers/list" [] (get-servers))
  (GET "/agents/list/:id" [id] (agents-for-server id))
  (POST "/agents/startBuild"
        [serverId agentIds]
        (start-build serverId agentIds))
  (POST "/agents/stopBuild"
        [serverId agentIds]
        (stop-build serverId agentIds))
  (POST "/agents/rebootAgent"
        [serverId agentIds]
        (reboot-agent serverId agentIds))
  (POST "/agents/runCustomBuild"
        [serverId agentIds]
        (run-custom-build serverId agentIds)))

(ns autotc-web.routes.home
  (:require [compojure.core :refer :all]
            [autotc-web.views.layout :as layout]
            [autotc-web.models.db :as db]
            [ring.util.response :as rur])
  (:import api.http.teamcity.domain.TeamCityServer)
  (:import api.http.teamcity.io.TeamCityProxy)
  (:import api.http.teamcity.io.TeamCitySession))

(defn- home []
  (layout/common [:script {:type "text/jsx" :src "/js/home/home.jsx"}]))

(defn- tc-server-to-json [^TeamCityServer server]
  (hash-map :alias (. server getAlias)
            :id (. server getId)))

(defn- tc-agent-to-json [agent]
  (hash-map :id (. agent getId)
            :name (. agent toString)))

(defn- get-servers []
  (rur/response {:servers (map tc-server-to-json
                               (db/read-servers))}))

(defn- get-agents [server-id]
  (let [server (db/get-server-by-id server-id)
        session (TeamCitySession/create server)]
    (rur/response {:agents (map tc-agent-to-json (-> session
                                                     .getProject
                                                     .getConfigurations))})))

(defn- exec-action-for-agents [server-id agent-ids session-action]
  (let [server (db/get-server-by-id server-id)
        session (TeamCitySession/create server)
        ids-set (set agent-ids)
        agents (filter #(contains? ids-set (. % getId))
                       (-> session
                           .getProject
                           .getConfigurations))]
    (doseq [agent agents]
      (session-action session agent))
    (rur/response {:result (count agents)})))

(defn start-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent] (. session start agent))))

(defn- stop-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent] (. session stop agent))))

(defn- reboot-agent [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent] (. session rebootMachine agent))))

(defroutes home-routes
  (GET "/" [] (home))
  (GET "/servers/list" [] (get-servers))
  (GET "/agents/list/:id" [id] (get-agents id))
  (POST "/agents/startBuild" [serverId agentIds] (start-build serverId agentIds))
  (POST "/agents/stopBuild" [serverId agentIds] (stop-build serverId, agentIds))
  (POST "/agents/rebootAgent" [serverId agentIds] (reboot-agent serverId, agentIds)))

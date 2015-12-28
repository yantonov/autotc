(ns autotc-web.routes.home
  (:require [compojure.core :refer :all]
            [autotc-web.views.layout :as layout]
            [autotc-web.models.db :as db]
            [ring.util.response :as rur]
            [autotc-web.log :as log]
            [clojure.pprint :as pprint])
  (:import api.http.teamcity.domain.TeamCityServer)
  (:import api.http.teamcity.io.TeamCityProxy)
  (:import api.http.teamcity.io.TeamCitySession))

(defn- home []
  (layout/common [:script {:type "text/javascript"
                           :src "/cljs/dev/autotc-web.js"}]
                 [:script {:type "text/javascript"
                           :src "/cljs/home/home.js"}]))

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
              :webUrl (. agent getWebUrl)
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
    (let [server (db/get-server-by-id (Long/parseLong (str server-id)))
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
                       (log/error e (str "cant exec action for server:" server-id " agent: " (. agent getId)))
                       (assoc result :error
                              (str (:error result)
                                   " "
                                   (pretty-print-exception e)))))))
               {:count 0
                :error ""}
               agents)))
    (catch Exception e
      (let [error (pretty-print-exception e)]
        (log/error e (str "cant exec action for server:" server-id " agents: " agent-ids))
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

(defn- restart-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]

                            (try (. session stop agent)
                                 (catch Exception e))
                            (try (. session start agent)
                                 (catch Exception e)))))

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
            (reboot-agent serverId agentIds))))
  (POST "/agents/runCustomBuild"
        request
        (fn [request]
          (let [{serverId "serverId"
                 agentIds "agentIds"} (:params request)]
            (run-custom-build serverId agentIds)))))

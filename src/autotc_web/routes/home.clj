(ns autotc-web.routes.home
  (:require [compojure.core :refer :all]
            [autotc-web.views.layout :as layout]
            [autotc-web.models.db :as db]
            [autotc-web.models.agent-service :as agent-service]
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

(defn- tc-server-to-json [server]
  (select-keys server [:alias :id]))

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
  (let [build (.getLastBuild agent)]
    (hash-map :id (.getId agent )
              :name (str agent)
              :webUrl (.getWebUrl agent)
              :running (.isRunning build)
              :status (-> build
                          .getStatus
                          .toString)
              :statusText (.getStatusText build))))

(defn- get-servers []
  (rur/response {:servers (map tc-server-to-json
                               (db/read-servers))}))

(defn- agents-for-server [server-id]
  (let [agents (:agents (.get-value (agent-service/get-agents server-id)))]
    (rur/response {:agents (if (not (nil? agents))
                             (map tc-agent-to-json agents)
                             nil)})))

(defn- exec-action-for-agents [server-id agent-ids session-action]
  ;; holy shit
  ;; TODO: decompose this method
  (try
    (let [server-data (db/get-server-by-id (Long/parseLong (str server-id)))
          server (TeamCityServer. (:id server-data)
                                  (:alias server-data)
                                  (:host server-data)
                                  (:port server-data)
                                  (:project server-data)
                                  (:username server-data)
                                  (:password server-data))
          session (TeamCitySession/create server)
          ids-set (set agent-ids)
          agents (filter #(contains? ids-set (.getId %))
                         (-> session
                             .getProject
                             .getConfigurations))]
      (rur/response
       (reduce (fn [result agent]
                 (try
                   (do
                     (session-action session agent)
                     (assoc-in result [:count] inc))
                   (catch Exception e
                     (do
                       (log/error e (str "cant exec action for server:" server-id " agent: " (.getId agent)))
                       (assoc-in result  [:error] str " " (pretty-print-exception e))))))
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
                            (.start session agent))))

(defn- stop-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (.stop session agent))))

(defn- restart-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]

                            (try (.stop session agent)
                                 (catch Exception e
                                   (log/error e)))
                            (try (.start session agent)
                                 (catch Exception e
                                   (log/error e))))))

(defn- reboot-agent [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (.rebootMachine session agent))))

(defn- run-custom-build [server-id agent-ids]
  (exec-action-for-agents server-id
                          agent-ids
                          (fn [session agent]
                            (.runCustomBuild session agent))))

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

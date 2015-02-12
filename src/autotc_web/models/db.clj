(ns autotc-web.models.db
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.DriverManager)
  (:import api.http.teamcity.domain.TeamCityServer))

(def ^:private db {:classname "org.sqlite.JDBC",
                   :subprotocol "sqlite",
                   :subname "db.sq3"})

(defn create-servers-table []
  (sql/with-connection
    db
    (sql/create-table
     :servers
     [:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
     [:alias "TEXT"]
     [:host "TEXT"]
     [:port "INTEGER"]
     [:project "TEXT"]
     [:username "TEXT"]
     [:password "TEXT"])))

(defn read-servers-internal []
  (sql/with-connection
    db
    (sql/with-query-results res
      ["SELECT * FROM servers ORDER BY alias ASC"]
      (doall res))))

(defn to-tc-server [row]
  (new TeamCityServer
       (:id row)
       (:alias row)
       (:host row)
       (:port row)
       (:project row)
       (:username row)
       (:password row)))

(defn read-servers []
  (map to-tc-server (read-servers-internal)))

(defn get-server-by-id [id]
  (sql/with-connection
    db
    (sql/with-query-results res
      ["SELECT * FROM servers where id = ?" id]
      (to-tc-server (first res)))))

(defn add-server [alias host port project username password]
  (sql/with-connection
    db
    (sql/insert-values
     :servers
     [:alias :host :port :project :username :password]
     [alias host (Integer/parseInt port) project username password])))

(defn delete-server [id]
  (sql/with-connection
    db
    (sql/delete-rows
     :servers
     ["id = ?" id])))

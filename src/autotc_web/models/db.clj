(ns autotc-web.models.db
  (:require [clojure.java.jdbc :as sql])
  (:import java.sql.DriverManager))

(def ^:private db {:classname "org.sqlite.JDBC",
                   :subprotocol "sqlite",
                   :subname "db.sq3"})

(defn create-servers-table []
  (sql/with-db-transaction [t-conn db]
    (sql/db-do-commands
     t-conn
     (sql/create-table-ddl :servers
                           [[:id "INTEGER PRIMARY KEY AUTOINCREMENT"]
                            [:alias "TEXT"]
                            [:host "TEXT"]
                            [:port "INTEGER"]
                            [:project "TEXT"]
                            [:username "TEXT"]
                            [:password "TEXT"]]))))

(defn read-servers-internal []
  (sql/with-db-transaction [t-conn db]
    (sql/query t-conn
               ["SELECT * FROM servers ORDER BY alias ASC"]
               {:result-set-fn doall})))

(defn to-tc-server [row]
  {:id (:id row)
   :alias (:alias row)
   :host (:host row)
   :port (:port row)
   :project (:project row)
   :username (:username row)
   :password (:password row)})

(defn read-servers []
  (map to-tc-server (read-servers-internal)))

(defn get-server-by-id [id]
  (sql/with-db-transaction [t-conn db]
    (sql/query t-conn
               ["SELECT * FROM servers where id = ?" id]
               {:result-set-fn (fn [res] (to-tc-server (first res)))})))

(defn add-server [alias host port project username password]
  (sql/with-db-transaction [t-conn db]
    (sql/insert!
     t-conn
     :servers
     [:alias :host :port :project :username :password]
     [alias host (Integer/parseInt port) project username password])))

(defn delete-server [id]
  (sql/with-db-transaction [t-conn db]
    (sql/delete! t-conn
                 :servers
                 ["id = ?" id])))

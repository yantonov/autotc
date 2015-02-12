(ns autotc-web.handler
  (:require [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [autotc-web.routes.home :refer [home-routes]]
            [autotc-web.routes.settings :refer [settings-routes]]
            [autotc-web.models.db :as db]
            [ring.middleware.json :as rmj]))

(defn init []
  (println "autotc-web is starting")
  (if-not (.exists (java.io.File. "./db.sq3"))
    (db/create-servers-table)))

(defn destroy []
  (println "autotc-web is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (-> (routes home-routes
              settings-routes
              app-routes)
      (handler/site)
      (wrap-base-url)
      (rmj/wrap-json-response)))

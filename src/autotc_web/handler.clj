(ns autotc-web.handler
  (:require [compojure.core :refer [defroutes routes]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.defaults :as ringdefaults]
            [hiccup.middleware :refer [wrap-base-url]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [autotc-web.routes.home :refer [home-routes]]
            [autotc-web.routes.settings :refer [settings-routes]]
            [autotc-web.models.db :as db]
            [ring.middleware.json :as rmj]
            [autotc-web.log :as log]
            [clojure.pprint :as pprint]))

(defn init []
  (println "autotc-web is starting")
  (if-not (.exists (java.io.File. "./db.sq3"))
    (db/create-servers-table)))

(defn destroy []
  (println "autotc-web is shutting down"))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn log-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e (log/error e (str "Error occured for request :" (pprint/pprint request)))))))

(def app
  (-> (routes home-routes
              settings-routes
              app-routes)
      (rmj/wrap-json-params)
      (rmj/wrap-json-response)
      (wrap-base-url)
      (ringdefaults/wrap-defaults (-> ringdefaults/site-defaults
                                      (assoc-in [:security :anti-forgery] false )
                                      (assoc-in [:params :urlencoded] true)))
      (log-exception)))

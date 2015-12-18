(ns autotc-web.routes.settings
  (:require [compojure.core :refer :all]
            [autotc-web.views.layout :as layout]
            [autotc-web.models.db :as db]
            [ring.util.response :as rur])
  (:import api.http.teamcity.domain.TeamCityServer))

(defn settings []
  (layout/common [:script {:type "text/javascript"
                           :src "/js/combined/settings/settings.js"}]))

(defn settings-cljs []
  (layout/common [:script {:type "text/javascript"
                           :src "/cljs/dev/autotc-web.js"}]
                 [:script {:type "text/javascript"
                           :src "/cljs/settings/settings.js"}]))

(defn- tc-server-to-json [^TeamCityServer server]
  (hash-map :alias (. server getAlias)
            :host (. server getHost)
            :port (. server getPort)
            :project (. server getProject)
            :id (. server getId)))

(defn- get-servers []
  (rur/response {:servers (map tc-server-to-json
                               (db/read-servers))}))

(defroutes settings-routes
  (GET "/settings" [] (settings))
  (GET "/settings-cljs" [] (settings-cljs))
  (GET "/settings/servers/list" [] (get-servers))
  (POST "/settings/servers/add"
        [alias host port project username password]
        (db/add-server alias host port project username password))
  (POST "/settings/servers/delete" [id]
        (db/delete-server id)))

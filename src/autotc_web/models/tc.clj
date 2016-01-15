(ns autotc-web.models.tc
  (:require [clj-teamcity-api.core :as tc]
            [autotc-web.log :as log]
            [autotc-web.models.exception :as exception])
  (:import clj_teamcity_api.core.TeamCityServer)
  (:import clj_teamcity_api.core.Credentials))

(defn build-types [host port project-name user pass]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)
        project-id (:id (tc/find-project server credentials project-name))
        project (tc/project server credentials project-id)
        build-type-ids (:build-type-ids project)
        build-types (pmap (fn [build-type-id]
                            (try (let [last-build (->> build-type-id
                                                       (tc/last-builds server credentials)
                                                       first)
                                       build-type (->> build-type-id
                                                       (tc/build-type server credentials)
                                                       :attrs)
                                       last-build-details (->> last-build
                                                               :id
                                                               (tc/build server credentials))]
                                   {:last-build last-build
                                    :build-type build-type
                                    :last-build-details last-build-details})
                                 (catch Exception e
                                   (log/error e (format "cant get info for build type id=[%s]" build-type-id))
                                   {:error (exception/pretty-print-exception e)})))
                          build-type-ids)]
    build-types))

(defn trigger-build [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)]
    (tc/trigger-build server credentials build-type-id)))

(defn cancel-build [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)]
    (tc/cancel-build server credentials build-type-id)))

(defn reboot-agent [host port user pass build-type-id]
  (let [server (TeamCityServer. host port)
        credentials (Credentials. user pass)]
    (tc/reboot-agent server credentials build-type-id)))


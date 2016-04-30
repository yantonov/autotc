(ns autotc-web.models.tc
  (:require [clj-teamcity-api.api :as tc]
            [clj-teamcity-api.net :as tcn]
            [autotc-web.log :as log]
            [autotc-web.models.exception :as exception]))

(defn- tag? [tag-name]
  #(= tag-name (:tag %)))

(defn- empty-list-if-nil [x]
  (if (nil? x) '() x))

(defn- project-by-name [projects name]
  (->> projects
       :content
       (filter (fn [project]
                 (= name (get-in project [:attrs :name]))))
       first
       :attrs))

(defn- project-build-type-ids [project]
  (let [build-types-tag (->> project
                             :content
                             (filter (tag? :buildTypes))
                             first)
        build-type-ids (->> build-types-tag
                            :content
                            (map #(get-in % [:attrs :id])))]
    build-type-ids))

(defn- last-builds-view [last-builds]
  (->> last-builds
       :content
       (map :attrs)))

(defn- build-view [build]
  (let [info
        (:content build)

        web-url
        (get-in build [:attrs :webUrl])

        test-occurences
        (->> info
             (filter (tag? :testOccurrences))
             first
             :attrs)

        changes
        (->> info
             (filter (tag? :lastChanges))
             first
             :content
             (map :attrs))

        status-text
        (->> info
             (filter (tag? :statusText))
             first
             :content
             first)

        agent
        (->> info
             (filter (tag? :agent))
             first
             :attrs)]
    {:test-occurences (empty-list-if-nil test-occurences)
     :changes (empty-list-if-nil changes)
     :status-text status-text
     :agent agent
     :webUrl web-url}))

(defn vcs-roots-view [roots]
  (->> roots
       :content
       (map :attrs)))

(defn vcs-root-view [root]
  (->> root
       :content
       (filter (tag? :properties))
       first
       :content
       (filter (tag? :property))
       (map (fn [tag]
              (let [attrs (:attrs tag)]
                (vector (keyword (:name attrs))
                        (:value attrs)))))
       (apply concat)
       (apply hash-map)))

(defn project-queue-view [queue]
  (->> queue
       :content
       (map :attrs)))

(defn project-info [host port project-name user pass]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        project-id
        (:id (project-by-name (tc/projects server credentials) project-name))

        project
        (tc/project server credentials project-id)

        build-type-ids
        (project-build-type-ids project)

        build-types
        (doall (map (fn [build-type-id]
                      (try (let [last-build (->> build-type-id
                                                 (tc/last-builds server credentials)
                                                 last-builds-view
                                                 first)
                                 build-type (->> build-type-id
                                                 (tc/build-type server credentials)
                                                 :attrs)
                                 last-build-details (->> last-build
                                                         :id
                                                         (tc/build server credentials)
                                                         build-view)]
                             {:last-build last-build
                              :build-type build-type
                              :last-build-details last-build-details})
                           (catch Exception e
                             (log/error e (format "cant get info for build type id=[%s]" build-type-id))
                             {:error (exception/pretty-print-exception e)})))
                    build-type-ids))

        vcs-roots-ids
        (map :id
             (vcs-roots-view
              (try (tc/vcs-roots server credentials project-id)
                   (catch Exception e
                     (log/error e (format "cant get vcs roots for project id=[%s]" project-id))
                     {}))))

        branches
        (map (comp :branchName vcs-root-view)
             (doall (map #(try (tc/vcs-root server credentials %)
                               (catch Exception e
                                 (log/error e (format "cant get vcs root id=[%s]" %))
                                 {}))
                         vcs-roots-ids)))

        project-queue
        (reduce (fn [m item]
                  (assoc m (:buildTypeId item) item))
                {}
                (try (project-queue-view
                      (tc/project-queue server credentials project-id))
                     (catch Exception e
                       (log/error e (format "cant get build queue for project id=[%s]" project-id))
                       [])))

        build-types-with-queue
        (doall
         (map (fn [build-type]
                (let [build-type-id (get-in build-type [:build-type :id])
                      queue (get project-queue build-type-id)]
                  (assoc build-type :queue queue)))
              build-types))]
    {:branches branches
     :build-types build-types-with-queue}))

(defn- test-occurences-view [response]
  (->> response
       :content
       (map :attrs)))

(defn- test-occurence-detail-view [project-web-url
                                   project-id
                                   build
                                   test-details-response]
  (let [attrs (:attrs test-details-response)
        test-tag (->> test-details-response
                      :content
                      (filter (tag? :test))
                      first
                      :attrs)
        test-id (:id test-tag)
        project-parsed-url (java.net.URL. project-web-url)
        host (.getHost project-parsed-url)
        port (let [port-number (.getPort project-parsed-url)]
               (if (= -1 port-number)
                 80
                 port-number))
        web-url (format "http://%s:%d/project.html?projectId=%s&testNameId=%s&tab=testDetails" host port project-id test-id)
        name (:name attrs)
        pattern-matches  (re-matches #"(.*)\.([^.]+\.[^.]+)" name)]
    (let [result {:webUrl web-url
                  :build build}]
      (-> result
          (assoc :name (if (not (nil? pattern-matches))
                         (nth pattern-matches 2)
                         name))
          (assoc :namespace (if (not (nil? pattern-matches))
                              (nth pattern-matches 1)
                              nil))))))

(defn- test-failed? [t]
  (and (not (get t :ignored false))
       (not (= "SUCCESS" (:status t)))))

(defn attach-build-info-for-each-test [f builds]
  (letfn [(extend-test-info [build]
            (map #(assoc % :build (:build build))
                 (:tests build)))]
    (apply concat (map #(-> %
                            :builds
                            f
                            extend-test-info)
                       builds))))

(defn combine-latest-builds [builds]
  (let [butlast-build (attach-build-info-for-each-test second builds)
        last-build (attach-build-info-for-each-test first builds)]
    (if (empty? butlast-build)
      (filter test-failed? last-build)
      (concat (filter test-failed? last-build)
              (filter #(and (test-failed? %)
                            (empty? (filter (fn [x]
                                              (and (= (:name %)
                                                      (:name x))
                                                   (not (test-failed? x))))
                                            last-build)))
                      butlast-build)))))

(defn parse-project-domain [project]
  (let [project-web-url (:webUrl project)
        project-parsed-url (java.net.URL. project-web-url)
        host (.getHost project-parsed-url)
        port (let [port-number (.getPort project-parsed-url)]
               (if (= -1 port-number)
                 80
                 port-number))]
    (format "%s:%d" host port)))

(defn current-problems [host port project-name user pass]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        project
        (project-by-name (tc/projects server credentials) project-name)

        project-id (:id project)
        project-domain (parse-project-domain project)

        project
        (tc/project server credentials project-id)

        build-type-ids
        (project-build-type-ids project)

        parse-test-details-response
        (fn [test-details-response]
          (->> test-details-response
               :content
               (filter (tag? :test))
               first
               :attrs))

        get-test-details
        (fn [test-id]
          (->> test-id
               (tc/test-occurences server credentials)
               parse-test-details-response))

        parse-build-response
        (fn [build-response]
          (let [build-type-info (->> build-response
                                     :content
                                     (filter (tag? :buildType))
                                     first
                                     :attrs)]
            (assoc build-type-info :webUrl (->> build-response
                                                :attrs
                                                :webUrl))))

        get-tests-from-latest-builds
        (fn [build-type-ids]
          (map (fn [build-type-id]
                 {:build-type-id build-type-id
                  :builds (doall (map (fn [build]
                                        {:build (->> build
                                                     :id
                                                     (tc/build server credentials)
                                                     parse-build-response)
                                         :tests (->> build
                                                     :id
                                                     (tc/tests-occurences server credentials)
                                                     test-occurences-view)})
                                      (->> build-type-id
                                           (tc/last-builds server credentials)
                                           last-builds-view
                                           (take 2))))})
               build-type-ids))

        test-name-pattern
        #"(.*)\.([^.]+\.[^.]+)"

        patch-test-info
        (fn [test-handle]
          (let [name (:name test-handle)
                pattern-matches (re-matches test-name-pattern name)
                test-details (get-test-details (:id test-handle))]
            (-> test-details
                (assoc :webUrl (format "http://%s/project.html?projectId=%s&testNameId=%s&tab=testDetails" project-domain project-id (:id test-details)))
                (assoc :name (if (not (nil? pattern-matches))
                               (nth pattern-matches 2)
                               name))
                (assoc :namespace (if (not (nil? pattern-matches))
                                    (nth pattern-matches 1)
                                    nil))
                (assoc :build (:build test-handle)))))

        problems
        (->> build-type-ids
             get-tests-from-latest-builds
             combine-latest-builds
             (map patch-test-info))

        ;; current-problems
        ;; (sort-by
        ;;  (fn [item] (str "%s:%s"
        ;;                  (-> item
        ;;                      :build
        ;;                      :build-type
        ;;                      :name)
        ;;                  (-> item
        ;;                      :name)))
        ;;  (map (fn [test-handle]
        ;;         (->> test-handle
        ;;              :test-id
        ;;              (tc/test-occurences server credentials)
        ;;              (test-occurence-detail-view project-web-url
        ;;                                          project-id
        ;;                                          (:build test-handle))))
        ;;       (apply concat
        ;;              (map (fn [build-type-id]
        ;;                     (try
        ;;                       (let [last-build
        ;;                             (->> build-type-id
        ;;                                  (tc/last-builds server credentials)
        ;;                                  last-builds-view
        ;;                                  first)

        ;;                             last-build-details
        ;;                             (->> last-build
        ;;                                  :id
        ;;                                  (tc/build server credentials))

        ;;                             build-type-problems
        ;;                             (->> last-build
        ;;                                  :id
        ;;                                  (tc/tests-occurences server credentials)
        ;;                                  test-occurences-view
        ;;                                  (filter test-failed?)
        ;;                                  (map (fn [content]
        ;;                                         (let [attrs (:attrs content)]
        ;;                                           {:test-id (:id attrs)
        ;;                                            :build {:attrs (:attrs last-build-details)
        ;;                                                    :build-type (->> last-build-details
        ;;                                                                     :content
        ;;                                                                     (filter (tag? :buildType))
        ;;                                                                     first
        ;;                                                                     :attrs)}}))))]
        ;;                         build-type-problems)
        ;;                       (catch Exception e
        ;;                         (log/error e (format "cant get test occurences for type id=[%s]" build-type-id))
        ;;                         {:error (exception/pretty-print-exception e)})))
        ;;                   build-type-ids))))
        ]
    {:current-problems problems}))

(defn trigger-build [host port user pass build-type-id]
  (let [server (tcn/make-server host :port port)
        credentials (tcn/make-credentials user pass)]
    (tc/trigger-build server credentials build-type-id)))

(defn cancel-build [host port user pass build-type-id]
  (let [server (tcn/make-server host :port port)
        credentials (tcn/make-credentials user pass)
        running-build-id (->> (tc/running-build server credentials build-type-id)
                              last-builds-view
                              first
                              :id)]
    (if (not (nil? running-build-id))
      (tc/cancel-build server credentials running-build-id))))

(defn reboot-agent [host port user pass build-type-id]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        ;; TODO: think to replace this heuristics to agent requirement parameters
        last-build-id
        (->> (tc/last-builds server credentials build-type-id)
             last-builds-view
             first
             :id)

        last-build
        (tc/build server credentials last-build-id)

        agent-id
        (->> last-build
             :content
             (filter (tag? :agent))
             first
             :attrs
             :id)]
    (tc/reboot-agent server credentials agent-id)))

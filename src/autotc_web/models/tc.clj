(ns autotc-web.models.tc
  (:require [clj-teamcity-api.api :as tc]
            [clj-teamcity-api.net :as tcn]
            [autotc-web.log :as log]
            [autotc-web.models.exception :as exception]
            [autotc-web.models.tc-response-parser :as parser]))

(defn project-info [host port project-name user pass]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        project-id
        (->> project-name
             (parser/project-by-name (tc/projects server credentials))
             :id)

        project
        (tc/project server credentials project-id)

        build-type-ids
        (parser/build-type-ids project)

        build-types
        (doall (map (fn [build-type-id]
                      (let [last-build
                            (->> build-type-id
                                 (tc/last-builds server credentials)
                                 parser/parse-last-builds
                                 first)

                            build-type
                            (->> build-type-id
                                 (tc/build-type server credentials)
                                 :attrs)

                            last-build-details
                            (->> last-build
                                 :id
                                 (tc/build server credentials)
                                 parser/parse-build-response)]
                        {:last-build last-build
                         :build-type build-type
                         :last-build-details last-build-details}))
                    build-type-ids))

        vcs-roots-ids
        (map :id
             (parser/parse-vcs-roots
              (tc/vcs-roots server credentials project-id)))

        branches
        (distinct (map (comp :branchName parser/parse-vcs-root)
                       (doall (map #(tc/vcs-root server credentials %)
                                   vcs-roots-ids))))

        project-queue
        (reduce (fn [m item]
                  (assoc m (:buildTypeId item) item))
                {}
                (parser/parse-project-queue
                 (tc/project-queue server credentials project-id)))

        build-types-with-queue
        (doall
         (map (fn [build-type]
                (let [build-type-id (get-in build-type [:build-type :id])
                      queue (get project-queue build-type-id)]
                  (assoc build-type :queue queue)))
              build-types))]
    {:project project
     :branches branches
     :build-types build-types-with-queue}))

(defn- test-failed? [t]
  (and (not (get t :ignored false))
       (not= "SUCCESS" (:status t))))

(defn attach-build-info-for-each-test [f builds]
  (letfn [(extend-test-info [build]
            (map #(assoc % :build (:build build))
                 (:tests build)))]
    (mapcat #(-> %
                 :builds
                 f
                 extend-test-info)
            builds)))

(defn combine-latest-builds [builds]
  (let [butlast-build (attach-build-info-for-each-test second builds)
        last-build (attach-build-info-for-each-test first builds)
        last-build-completed? (every? identity
                                      (map (fn [b] (-> b
                                                       first
                                                       :build
                                                       :running
                                                       not))
                                           builds))]
    (if (empty? butlast-build)
      (filter test-failed? last-build)
      (concat (filter test-failed? last-build)
              (filter #(and (not last-build-completed?)
                            (test-failed? %)
                            (empty? (filter (fn [x]
                                              (and (= (:name %)
                                                      (:name x))
                                                   (not (test-failed? x))))
                                            last-build)))
                      butlast-build)))))

(defn distinct-by
  [f coll]
  (letfn [(step [xs seen]
            (lazy-seq
             ((fn [[x :as xs] seen]
                (when-let [s (seq xs)]
                  (let [fx (f x)]
                    (if (contains? seen fx)
                      (recur (rest s) seen)
                      (cons x (step (rest s) (conj seen fx)))))))
              xs seen)))]
    (step coll #{})))

(defn current-problems [host port project-name user pass]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        project
        (parser/project-by-name (tc/projects server credentials)
                                project-name)

        project-id (:id project)
        project-domain (parser/parse-project-domain project)

        project
        (tc/project server credentials project-id)

        build-type-ids
        (parser/build-type-ids project)

        get-test-details
        (fn [test-id]
          (->> test-id
               (tc/test-occurences server credentials)
               parser/parse-test-occurences))

        get-tests-from-latest-builds
        (fn [build-type-ids]
          (map (fn [build-type-id]
                 {:build-type-id
                  build-type-id

                  :builds
                  (doall
                   (map (fn [build]
                          {:build (->> build
                                       :id
                                       (tc/build server credentials)
                                       parser/parse-build)
                           :tests (->> build
                                       :id
                                       (tc/tests-occurences server credentials)
                                       parser/parse-tests-occurences)})
                        (->> build-type-id
                             (tc/last-builds server credentials)
                             parser/parse-last-builds
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
                (assoc :webUrl (format (str "http://%s/project.html"
                                            "?projectId=%s"
                                            "&testNameId=%s"
                                            "&tab=testDetails")
                                       project-domain
                                       project-id
                                       (:id test-details)))
                (assoc :name (if-not (nil? pattern-matches)
                               (nth pattern-matches 2)
                               name))
                (assoc :namespace (when-not (nil? pattern-matches)
                                    (nth pattern-matches 1)))
                (assoc :build (:build test-handle)))))

        problems
        (->> build-type-ids
             get-tests-from-latest-builds
             combine-latest-builds
             (map patch-test-info)
             (distinct-by :name)
             (sort-by #(str "%s:%s"
                            (get-in % [:build :name])
                            (:name %))))]
    {:current-problems problems}))

(defn trigger-build [host port user pass build-type-id]
  (let [server (tcn/make-server host :port port)
        credentials (tcn/make-credentials user pass)]
    (tc/trigger-build server credentials build-type-id)))

(defn cancel-build [host port user pass build-type-id]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        running-build-id
        (->> (tc/running-build server credentials build-type-id)
             parser/parse-last-builds
             first
             :id)]
    (if (not (nil? running-build-id))
      (tc/cancel-build server credentials running-build-id))))

(defn reboot-agent [host port user pass build-type-id]
  (let [server
        (tcn/make-server host :port port)

        credentials
        (tcn/make-credentials user pass)

        ;; TODO: think to replace this heuristics
        ;; to agent requirement parameters
        agent-id
        (->> (tc/last-builds server credentials build-type-id)
             parser/parse-last-builds
             first
             :id
             (tc/build server credentials)
             (parser/parse-agent-id-from-build))]
    (tc/reboot-agent server credentials agent-id)))

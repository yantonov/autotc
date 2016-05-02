(ns autotc-web.models.tc
  (:require [clj-teamcity-api.api :as tc]
            [clj-teamcity-api.net :as tcn]
            [autotc-web.log :as log]
            [autotc-web.models.exception :as exception]
            [autotc-web.models.tc-response-parser :as parser]))

(defn- tag? [tag-name]
  #(= tag-name (:tag %)))

(defn- empty-list-if-nil [x]
  (if (nil? x) '() x))

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
                      (try (let [last-build (->> build-type-id
                                                 (tc/last-builds server credentials)
                                                 parser/parse-last-builds
                                                 first)
                                 build-type (->> build-type-id
                                                 (tc/build-type server credentials)
                                                 :attrs)
                                 last-build-details (->> last-build
                                                         :id
                                                         (tc/build server credentials)
                                                         parser/parse-build-response)]
                             {:last-build last-build
                              :build-type build-type
                              :last-build-details last-build-details})
                           (catch Exception e
                             (log/error e (format "cant get info for build type id=[%s]" build-type-id))
                             {:error (exception/pretty-print-exception e)})))
                    build-type-ids))

        vcs-roots-ids
        (map :id
             (parser/parse-vcs-roots
              (try (tc/vcs-roots server credentials project-id)
                   (catch Exception e
                     (log/error e (format "cant get vcs roots for project id=[%s]" project-id))
                     {}))))

        branches
        (map (comp :branchName parser/parse-vcs-root)
             (doall (map #(try (tc/vcs-root server credentials %)
                               (catch Exception e
                                 (log/error e (format "cant get vcs root id=[%s]" %))
                                 {}))
                         vcs-roots-ids)))

        project-queue
        (reduce (fn [m item]
                  (assoc m (:buildTypeId item) item))
                {}
                (try (parser/parse-project-queue
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

(defn distinct-by
  "Returns a lazy sequence of the elements of coll, removing any elements that
  return duplicate values when passed to a function f."
  [f coll]
  (let [step (fn step [xs seen]
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
        (parser/project-by-name (tc/projects server credentials) project-name)

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
                 {:build-type-id build-type-id
                  :builds (doall (map (fn [build]
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
             (map patch-test-info)
             (distinct-by :name)
             (sort-by #(str "%s:%s"
                            (-> % :build :name)
                            (-> % :name))))]
    {:current-problems problems}))

(defn trigger-build [host port user pass build-type-id]
  (let [server (tcn/make-server host :port port)
        credentials (tcn/make-credentials user pass)]
    (tc/trigger-build server credentials build-type-id)))

(defn cancel-build [host port user pass build-type-id]
  (let [server (tcn/make-server host :port port)
        credentials (tcn/make-credentials user pass)
        running-build-id (->> (tc/running-build server credentials build-type-id)
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

        ;; TODO: think to replace this heuristics to agent requirement parameters
        last-build-id
        (->> (tc/last-builds server credentials build-type-id)
             parser/parse-last-builds
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

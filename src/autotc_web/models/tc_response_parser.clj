(ns autotc-web.models.tc-response-parser
  (:require [autotc-web.log :as log]))

(defn- tag? [tag-name]
  #(= tag-name (:tag %)))

(defn- empty-list-if-nil [x]
  (if (nil? x) '() x))

(defn project-by-name [projects name]
  (->> projects
       :content
       (filter (fn [project]
                 (.equalsIgnoreCase name (get-in project [:attrs :name]))))
       first
       :attrs))

(defn build-type-ids [project]
  (->> project
       :content
       (filter (tag? :buildTypes))
       first
       :content
       (map #(get-in % [:attrs :id]))))

(defn parse-project-domain [project]
  (let [project-web-url (:webUrl project)
        project-parsed-url
        (try (java.net.URL. project-web-url)
             (catch Exception e
               (log/error e (str "cant parse url = " project-web-url))))
        host (.getHost project-parsed-url)
        port (let [port-number (.getPort project-parsed-url)]
               (if (= -1 port-number)
                 80
                 port-number))]
    (format "%s:%d" host port)))

(defn parse-build-response [build]
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
             :attrs)

        branchName
        (nth (re-matches #"\d+\s+(.*)+" (-> build
                                            :attrs
                                            :number)) 1)]
    {:test-occurences (empty-list-if-nil test-occurences)
     :changes (empty-list-if-nil changes)
     :status-text status-text
     :agent agent
     :webUrl web-url
     :branchName branchName}))

(defn parse-last-builds [last-builds]
  (->> last-builds
       :content
       (map :attrs)))

(defn parse-vcs-roots [roots]
  (->> roots
       :content
       (map :attrs)))

(defn parse-vcs-root [root]
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

(defn parse-project-queue [queue]
  (->> queue
       :content
       (map :attrs)))

(defn parse-test-occurences [test-details-response]
  (let [test-attrs (->> test-details-response
                        :content
                        (filter (tag? :test))
                        first
                        :attrs)]
    (assoc test-attrs :details (->> test-details-response
                                    :content
                                    (filter (tag? :details))
                                    first
                                    :content
                                    first))))

(defn parse-build [build-response]
  (let [build-type-info (->> build-response
                             :content
                             (filter (tag? :buildType))
                             first
                             :attrs)]
    (-> build-type-info
        (assoc :webUrl (->> build-response
                            :attrs
                            :webUrl))
        (assoc :running (->> build-response
                             :attrs
                             :running)))))

(defn parse-tests-occurences [response]
  (->> response
       :content
       (map :attrs)))

(defn parse-agent-id-from-build [last-build]
  (->> last-build
       :content
       (filter (tag? :agent))
       first
       :attrs
       :id))

(defn parse-build-change-response [response]
  (->> response
       :content
       (filter (tag? :change))
       first
       :attrs
       :id))

(defn parse-single-change-response [response]
  (->> response
       :content
       (filter (tag? :vcsRootInstance))
       first
       :attrs
       :id))

(defn branch-name-from-resulting-properties [response]
  (->> response
       :content
       (filter (tag? :property))
       (filter (fn [p] (-> p
                           :attrs
                           :name
                           (.contains ".vcsroot.branchName"))))
       first
       :attrs
       :value))



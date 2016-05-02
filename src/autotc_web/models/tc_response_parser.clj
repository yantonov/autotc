(ns autotc-web.models.tc-response-parser)

(defn- tag? [tag-name]
  #(= tag-name (:tag %)))

(defn- empty-list-if-nil [x]
  (if (nil? x) '() x))

(defn project-by-name [projects name]
  (->> projects
       :content
       (filter (fn [project]
                 (= name (get-in project [:attrs :name]))))
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
        project-parsed-url (java.net.URL. project-web-url)
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
             :attrs)]
    {:test-occurences (empty-list-if-nil test-occurences)
     :changes (empty-list-if-nil changes)
     :status-text status-text
     :agent agent
     :webUrl web-url}))

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
  (->> test-details-response
       :content
       (filter (tag? :test))
       first
       :attrs))

(defn parse-build [build-response]
  (let [build-type-info (->> build-response
                             :content
                             (filter (tag? :buildType))
                             first
                             :attrs)]
    (assoc build-type-info :webUrl (->> build-response
                                        :attrs
                                        :webUrl))))


(defn parse-tests-occurences [response]
  (->> response
       :content
       (map :attrs)))

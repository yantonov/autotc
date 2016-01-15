(ns autotc-web.models.exception)

(defn pretty-print-exception [e]
  (clojure.string/join
   "\n"
   (concat [(.getMessage e)]
           (map (fn [item]
                  (format "%s %s.%s:%d"
                          (.getFileName item)
                          (.getClassName item)
                          (.getMethodName item)
                          (.getLineNumber item)))
                (.getStackTrace e)))))

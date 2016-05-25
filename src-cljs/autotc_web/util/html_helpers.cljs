(ns autotc-web.util.html-helpers
  (:require [clojure.string :as string]))

(defn html-escape [s]
  (string/escape s
                 {\< "&lt;"
                  \> "&gt;"
                  \& "&amp;"
                  \" "&quot;"}))

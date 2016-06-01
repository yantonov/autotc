(ns autotc-web.repl
  (:require  [autotc-web.handler :as autotc]
             [ring.server.standalone :as rings]
             [ring.middleware.content-type :refer [wrap-content-type]]
             [ring.middleware.not-modified :refer [wrap-not-modified]]
             [ring.middleware.file :refer [wrap-file]]))

(defonce server (atom nil))

(defn get-handler []
  ;; #'app expands to (var app) so that when we reload our code,
  ;; the server is forced to re-resolve the symbol in the var
  ;; rather than having its own copy. When the root binding
  ;; changes, the server picks it up without having to restart.
  (-> #'autotc/app
      ;; Makes static assets in $PROJECT_DIR/resources/public/ available.
      (wrap-file "resources")
      (wrap-content-type) ; Content-type header
      (wrap-not-modified) ;If-Modified-Since header
      ))

(defn start-server
  "used for starting the server in development mode from REPL"
  [& [port]]
  (let [port (if port (Integer/parseInt port) 8080)]
    (reset! server
            (rings/serve (get-handler)
                         {:port port
                          :init autotc/init
                          :auto-reload? true
                          :destroy autotc/destroy
                          :join true}))
    (println (str "You can view the site at http://localhost:" port))))

(defn stop-server []
  (.stop @server)
  (reset! server nil))

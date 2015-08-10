(defproject autotc-web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.4.0"]
                 [hiccup "1.0.5"]
                 [ring-server "0.4.0"]
                 [api.http/teamcity "0.0.1"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.xerial/sqlite-jdbc "3.8.11.1"]
                 [ring/ring-json "0.4.0"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler autotc-web.handler/app
         :init autotc-web.handler/init
         :destroy autotc-web.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false,
     :stacktraces? false,
     :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"]
                   [ring/ring-devel "1.4.0"]]}})

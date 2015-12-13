(defproject autotc-web "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [commons-codec/commons-codec "1.10"] ; it seems its bug in ring-defaults (required dependency)
                 [hiccup "1.0.5"]
                 [api.http/teamcity "0.0.1"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [ring/ring-json "0.4.0"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler autotc-web.handler/app
         :init autotc-web.handler/init
         :destroy autotc-web.handler/destroy}
  :profiles {:uberjar {:aot :all}
             :production
             {:ring {:open-browser? false,
                     :stacktraces? false,
                     :auto-reload? false}}
             :dev {:dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.4.0"]]}})

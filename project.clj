(defproject autotc-web "0.1.0-SNAPSHOT"
  :description "apply action to multiple teamcity agents"
  :url "https://github.com/yantonov/autotc"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/java.jdbc "0.5.8"]
                 [org.xerial/sqlite-jdbc "3.8.11.2"]
                 [ring/ring-json "0.4.0"]
                 [ch.qos.logback/logback-classic "1.1.7"] ;logging
                 ;; cljs
                 [org.clojure/clojurescript "1.8.51"]
                 [reagent "0.6.0-alpha" :exclusions [cljsjs/react]]
                 [cljsjs/react-bootstrap "0.28.1-0" :exclusions
                  [[org.webjars.bower/jquery]
                   [cljsjs/react-bootstrap]]]
                 [cljs-ajax "0.5.4"]
                 ;; libs
                 [rex "0.1.0-SNAPSHOT"]
                 [clj-teamcity-api "0.1.0-SNAPSHOT"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-cljsbuild "1.1.3"]]
  :ring {:handler autotc-web.handler/app
         :init autotc-web.handler/init
         :destroy autotc-web.handler/destroy}
  :profiles {:uberjar {:aot :all}
             :production
             {:ring {:open-browser? false
                     :stacktraces? false
                     :auto-reload? false}}
             :dev {:dependencies [[ring-mock "0.1.5"]
                                  [ring/ring-devel "1.4.0"]]}}
  :cljsbuild
  {:builds
   {:production
    {:source-paths ["src-cljs"]
     :compiler {:output-to "resources/public/cljs/dev/autotc-web.js"
                :optimizations :advanced
                :pretty-print false}}
    :development
    {:source-paths ["src-cljs"]
     :compiler {:output-dir "resources/public/cljs/dev"
                :output-to "resources/public/cljs/dev/autotc-web.js"
                :source-map "resources/public/cljs/dev/autotc-web.js.map"
                :externs ["externs/externs.js"]
                :optimizations :whitespace
                :pretty-print true}}}})

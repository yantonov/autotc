(ns autotc-web.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn common [& body]
  (html5
   [:head
    [:title "Welcome to autotc-web"]
    (include-css "/css/bootstrap.min.css")
    (include-js "/js/lib/jquery-2.1.3.min.js")
    (include-js "/js/common/String.js")
    (include-js "/js/common/Console.js")
    (include-js "/js/common/Poller.js")
    (include-js "/js/lib/bootstrap.3.3.2.min.js")
    (include-js "/js/lib/JSXTransformer-0.12.2.js")
    (include-js "/js/lib/react-0.12.2.min.js")
    (include-js "/js/lib/react-bootstrap.min.js")
    [:link {:rel "shortcut icon"
            :href "/img/favicon.ico"}]]
   [:body
    [:nav {:class "navbar navbar-default"}
     [:div {:class "container"}
      [:div {:class "nav-header"}
       [:a {:class "navbar-brand" :href "/"} "autotc"]]
      [:div {:class "collapse navbar-collapse"}
       [:p {:class "navbar-text navbar-right"}
        [:a {:class "navbar-link"
             :href "/settings"} "Settings"]]]]]
    [:div {:class "container"
           :id "main-content"} body]]))

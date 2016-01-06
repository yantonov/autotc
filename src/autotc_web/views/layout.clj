(ns autotc-web.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]))

(defn common [& body]
  (html5
   [:head
    [:title "Welcome to autotc-web"]
    (include-css "/css/bootstrap.min.css")
    (include-css "/css/gh-fork-ribbon.css")
    (include-css "/css/gh-fork-ribbon.ie.css")
    (include-css "/css/style.css")
    (include-js "/js/lib/jquery-2.1.3.min.js")
    (include-js "/js/lib/react-0.14.5.min.js")
    (include-js "/js/lib/react-dom-0.14.5.min.js")
    (include-js "/js/lib/react-bootstrap-0.28.1.min.js")
    (include-js "/js/lib/halogen.0.1.2.min.js")
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
           :id "main-content"} body]
    [:div {:class "github-fork-ribbon-wrapper right-bottom"}
     [:div {:class "github-fork-ribbon github-fork-custom-styles"}
      [:a {:href "https://github.com/yantonov/autotc-web"}
       "Fork me on GitHub"]]]]))

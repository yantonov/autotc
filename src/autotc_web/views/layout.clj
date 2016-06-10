(ns autotc-web.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [clojure.string :as cljstr]))

(defn common [& body]
  (html5
   [:head
    [:title "autotc"]
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
      [:a {:href "https://github.com/yantonov/autotc"
           :target "_blank"}
       "Fork me on GitHub"]]]]))

(defn current-problems-file [problems]
  (html5
   [:head
    [:title "autotc"]
    (include-js "https://ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js")
    [:meta {:charset "UTF-8"}
     nil]
    [:style nil
     (cljstr/join "\n"
                  ["<!--"
                   "body {"
                   "font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif"
                   "}"
                   ""
                   ".fixed {"
                   "text-decoration: line-through;"
                   "}"
                   ""
                   "a, a:hover, a:visited {"
                   "color: #337ab7;"
                   "}"
                   ""
                   "a.copystack, a.copystack:hover, a.copystack:visited {"
                   "color: black;"
                   "text-decoration: none;"
                   "}"
                   "-->"])]]
   [:body
    [:div nil
     [:ol nil
      (map (fn [problem]
             [:li nil
              [:div nil
               [:input {:type "checkbox"
                        :class "markasfixed"}
                nil]
               [:a {:href "#"
                    :class "test"
                    :title "expand stack trace"
                    :style "padding-left: 8px"}
                (:name problem)]
               [:span {:style "padding-left:8px;"}
                "["]
               [:a {:href "#"
                    :class "copy"}
                "copy test name"]
               "]"]
              [:div {:style "white-space: pre; display: none"
                     :class "stack"}
               [:a {:href "#"
                    :class "copystack"
                    :title "copy stack trace"}
                (:details problem)]]])
           problems)]]
    [:script {:type "text/javascript"}
     "
    function copy (element) {
    var elementId = 'copyTextPlaceHolderId';

    var selection = window.getSelection();
    selection.removeAllRanges();

    var range = document.createRange();
    range.selectNodeContents($(element).get(0));

    selection.removeAllRanges();
    selection.addRange(range);

    var status = document.execCommand('copy');

    selection.removeAllRanges();
}

$(document).ready(function () {
    $('.test').click(function(e) {
        var testLink = $(e.target);
        testLink.parent().parent().find('.stack').toggle();
        copy(testLink.get());
        return false;
    });
    $('.markasfixed').change(function(e) {
       var fixed = $(this).is(':checked');
       $(e.target).parent().find('.test').toggleClass('fixed', fixed);
    });
    $('.copy').click(function(e) {
        var testLink = $(e.target).siblings('.test').get(0);
        copy(testLink);
        return false;
    });
    $('.copystack').click(function(e) {
        copy(e.target);
        return false;
    });
});
"]]))

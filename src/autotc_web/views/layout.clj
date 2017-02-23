(ns autotc-web.views.layout
  (:require [hiccup.page :refer [html5 include-css include-js]]
            [clojure.string :as cljstr]))

;; TODO: cljc (reuse from clj, cljs)
(defn html-escape [s]
  (cljstr/escape s
                 {\< "&lt;"
                  \> "&gt;"
                  \& "&amp;"
                  \" "&quot;"}))

(defn common [& body]
  (html5
   [:head
    [:title "autotc"]
    (include-css "/css/bootstrap.min.css")
    (include-css "/css/gh-fork-ribbon.css")
    (include-css "/css/gh-fork-ribbon.ie.css")
    (include-css "/css/style.css")
    (include-js "/js/lib/jquery-2.1.3.min.js")
    (include-js "/js/lib/react-15.3.2.min.js")
    (include-js "/js/lib/react-dom-15.3.2.min.js")
    (include-js "/js/lib/react-bootstrap-0.30.5.min.js")
    (include-js "/js/lib/halogen.0.1.2.min.js")
    [:link {:rel "shortcut icon"
            :href "/img/favicon.ico"}]]
   [:body
    [:nav {:class "navbar navbar-default"}
     [:div {:class "container-fluid"}
      [:div {:class "nav-header"}
       [:a {:class "navbar-brand" :href "/"} "autotc"]]
      [:div {:class "collapse navbar-collapse"}
       [:p {:class "navbar-text navbar-right"}
        [:a {:class "navbar-link"
             :href "/settings"} "Settings"]]]]]
    [:div {:class "container-fluid"
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
                   ".hidden {"
                   "display: none;"
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
                "["
                [:a {:href "#"
                     :class "copy-test-name"}
                 "copy test name"]
                "]"]
               [:span {:style "padding-left:8px;"}
                "["
                [:a {:href "#"
                     :class "copy-stack"}
                 "copy stack trace"]
                "]"]
               [:div {:style "white-space: pre; display: none"
                      :class "stack"}
                (html-escape (:details problem))]]])
           problems)]]
    [:span {:class "hidden lskey"}
     (format "autotc-checked-%s" (str (java.util.UUID/randomUUID)))]
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

function toggleItem(arr, targetItem, add) {
   var patched = $.grep(arr, function (item, index) {
     return item != targetItem;
   });
   if (add)
     patched.push(targetItem);
   return patched;
}

function getFixedFromLocalStorage(key) {
   var result = localStorage.getItem(key);
   if (result === undefined || result === null)
     return [];
   return $.parseJSON(result);
}

function updateFixedInLocalStorage(key, data) {
   localStorage.setItem(key, JSON.stringify(data));
}

$(document).ready(function () {
    var localStorageKey = $('.lskey').text();

    var already_fixed = getFixedFromLocalStorage(localStorageKey);
    for (var i = 0; i < already_fixed.length; ++i) {
        var index = already_fixed[i];
        $('.markasfixed:eq(' + index + ')').each(function (i, element) {
          $(element).click();
        });
    }

    $('.test').click(function(e) {
        var testLink = $(e.target);
        testLink.parent().find('.stack').toggle();
        copy(testLink.get(0));
        return false;
    });
    $('.markasfixed').each(function(index) {
       var el = $(this);
       el.change(function(e) {
          var fixed = $(this).is(':checked');
          $(e.target).parent().find('.test').toggleClass('fixed', fixed);
          var oldFixed = getFixedFromLocalStorage(localStorageKey);
          var newFixed = toggleItem(oldFixed, index, fixed);
          updateFixedInLocalStorage(localStorageKey, newFixed);
       });
    });
    $('.copy-test-name').click(function(e) {
        var testLink = $(e.target).parent().siblings('.test').get(0);
        copy(testLink);
        return false;
    });
    $('.copy-stack').click(function(e) {
        var copyLink = $(e.target);
        var stackElement = copyLink.parent().siblings('.stack');
        stackElement.show();
        copy(stackElement.get(0));
        stackElement.hide();
        return false;
    });
});
"]]))

(ns autotc-web.settings.settings
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [rex.ext.cursor :as rcur]
            [rex.watcher :as rwt]
            [rex.middleware :as rmw]
            [rex.ext.action-creator :as acm]
            [autotc-web.settings.reducers :as reducers]
            [autotc-web.settings.actions :as a]))

(def Table (r/adapt-react-class js/ReactBootstrap.Table))
(def Button (r/adapt-react-class js/ReactBootstrap.Button))
(def ButtonToolbar (r/adapt-react-class js/ReactBootstrap.ButtonToolbar))
(def Input (r/adapt-react-class js/ReactBootstrap.Input))
(def Grid (r/adapt-react-class js/ReactBootstrap.Grid))
(def Row (r/adapt-react-class js/ReactBootstrap.Row))
(def Col (r/adapt-react-class js/ReactBootstrap.Col))
(def ModalBody (r/adapt-react-class js/ReactBootstrap.Modal.Body))
(def ModalHeader (r/adapt-react-class js/ReactBootstrap.Modal.Header))
(def ModalTitle (r/adapt-react-class js/ReactBootstrap.Modal.Title))
(def ModalFooter (r/adapt-react-class js/ReactBootstrap.Modal.Footer))
(def Modal (r/adapt-react-class js/ReactBootstrap.Modal.Dialog))

(defn server-element [{:keys [key
                              index
                              server
                              on-delete]} data]
  [:tr
   {:key index}
   [:td nil (inc index)]
   [:td nil (:alias server)]
   [:td nil (str (:host server) ":" (:port server))]
   [:td nil (:project server)]
   [:td
    nil
    [ButtonToolbar
     [Button {:bs-style "danger"
              :on-click on-delete}
      "Delete"]]]])

(defn server-list [{:keys [servers
                           on-delete]} data]
  [Table {:striped true
          :bordered true
          :condensed true
          :hover true}
   [:thead
    [:colgroup
     [:col {:span "1" :width "5%"}]
     [:col {:span "1" :width "35%"}]
     [:col {:span "1" :width "20%"}]
     [:col {:span "1" :width "20%"}]
     [:col {:span "1" :width "20%"}]]
    [:tr
     [:th "#"]
     [:th "Alias"]
     [:th "Host:Port"]
     [:th "Project"]
     [:th "Actions"]]]
   [:tbody
    (for [[svr index] (map vector servers (iterate inc 0))]
      [server-element {:key index
                       :index index
                       :server svr
                       :on-delete (fn [] (on-delete svr))}])]])

(defn add-server-form [on-add]
  [:div
   [Button {:on-click on-add} "Add"]])

(defn delete-confirmation-dialog [{:keys [server
                                          on-ok
                                          on-cancel]} data]
  [Modal {:show true
          :backgrop false
          :animation false
          :on-hide on-cancel}
   [ModalHeader {:closebutton true}
    [ModalTitle (str "Do you really want to delete '" (:alias server) "'?")]]
   [ModalBody
    (str (:alias server) " at " (:host server) ":" (:port server))]
   [ModalFooter
    [Button {:on-click on-cancel} "Cancel"]
    [Button {:on-click on-ok
             :bs-style "danger"} "Delete"]]])

(defn edit-server-form [{:keys [form-cursor
                                on-save
                                on-cancel]} data]
  (let [update-fn (fn [field-key]
                    (fn [e]
                      (a/text-input-changed (rcur/nest form-cursor
                                                       field-key)
                                            e)))]
    [:form {:action ""
            :method "POST"
            :on-submit (fn [e]
                         (.preventDefault e)
                         (on-save))}
     [Grid
      [Row
       [Col {:xs 12
             :md 6}
        [Input {:type "text"
                :label "Alias"
                :placeholder "Enter text"
                :on-change (update-fn :alias)}]
        [Input {:type "text"
                :label "Host"
                :placeholder "Enter text"
                :on-change (update-fn :host)}]
        [Input {:type "text"
                :label "Port"
                :placeholder "Enter text"
                :on-change (update-fn :port)}]
        [Input {:type "text"
                :label "Project"
                :placeholder "Enter text"
                :on-change (update-fn :project)}]
        [Input {:type "text"
                :label "Username"
                :placeholder "Enter text"
                :on-change (update-fn :username)}]
        [Input {:type "password"
                :label "Password"
                :placeholder "Enter text"
                :on-change (update-fn :password)}]
        [ButtonToolbar
         [Button {:type "submit"
                  :bs-style "success"}
          "Save"]
         [Button {:type "button"
                  :on-click on-cancel}
          "Cancel"]]]]]]))

(defn settings-page []
  (let [cursor (rcur/nest (rcur/make-cursor)
                          :page)
        edit-server-form-cursor (rcur/nest cursor
                                           :edit-server-form)]
    (r/create-class
     {
      :component-did-mount
      (fn [this]
        (rwt/defwatcher
          (fn [old-state action new-state]
            (r/set-state this (rcur/get-state cursor new-state))))
        (a/init-page cursor)
        (a/load-server-list cursor))
      :render
      (fn [this]
        (let [{:keys [show-list
                      servers
                      server-to-delete]} (r/state this)
              show-confirm-delete-dialog? (not (nil? server-to-delete))]
          (if (not show-list)
            [:div nil
             [add-server-form (fn [] (a/begin-add-server cursor))]
             [:br]
             [server-list {:servers servers
                           :on-delete (fn [server] (a/confirm-delete-server cursor server))}]
             (if show-confirm-delete-dialog?
               [delete-confirmation-dialog {:server server-to-delete
                                            :on-ok (fn [] (a/delete-server cursor server-to-delete))
                                            :on-cancel (fn [] (a/cancel-delete-server cursor))}])]
            [edit-server-form {:form-cursor edit-server-form-cursor
                               :on-save (fn [] (a/save-server cursor edit-server-form-cursor))
                               :on-cancel (fn [] (a/cancel-edit-server cursor))}])))})))

(defn ^:export init []
  (rmw/defmiddleware acm/action-creator-middleware)
  (reducers/define-reducers)
  (r/render-component [settings-page]
                      (js/document.getElementById "main-content")))


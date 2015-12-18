(ns autotc-web.settings.settings
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]))

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

(defn edit-server-form [{:keys [on-save
                                on-cancel]} data]
  [:form {:action ""
          :method "POST"
          :on-submit on-save}
   [Grid
    [Row
     [Col {:xs 12
           :md 6}
      [Input {:type "text"
              :label "Alias"
              :ref "alias"
              :placeholder "Enter text"}]
      [Input {:type "text"
              :label "Host"
              :ref "host"
              :placeholder "Enter text"}]
      [Input {:type "text"
              :label "Port"
              :ref "port"
              :placeholder "Enter text"}]
      [Input {:type "text"
              :label "Project"
              :ref "project"
              :placeholder "Enter text"}]
      [Input {:type "text"
              :label "Username"
              :ref "username"
              :placeholder "Enter text"}]
      [Input {:type "password"
              :label "Password"
              :ref "alias"
              :placeholder "Enter text"}]
      [ButtonToolbar
       [Button {:type "submit"
                :bs-style "success"}
        "Save"]
       [Button {:type "button"
                :on-click on-cancel}
        "Cancel"]]]]]])

(defn settings-page []
  (r/create-class
   {:get-initial-state
    (fn [this]
      {:show-list true
       :servers []})
    :begin-add-server
    (fn [this]
      (r/set-state this
                   {:show-list false}))
    :show-list
    (fn [this]
      (r/set-state this
                   {:show-list true}))
    :save-server
    (fn [this server]
      (ajax/POST "/settings/servers/add"
                 {:params server
                  :format (ajax/url-request-format)
                  :handler (fn [response]
                             (println response)
                             (.loadServerList this)
                             (.showList this))}))
    :cancel-edit-server
    (fn [this]
      (.showList this))
    :load-server-list
    (fn [this]
      (ajax/GET "/settings/servers/list"
                {:response-format (ajax/json-response-format {:keywords? true})
                 :handler (fn [response]
                            (r/set-state this {:servers (:servers response)}))}))
    :component-did-mount
    (fn [this]
      (.loadServerList this))
    :handle-delete
    (fn [this server]
      (ajax/POST "/settings/servers/delete"
                 {:params {:id (:id server)}
                  :format (ajax/url-request-format)
                  :handler (fn [response]
                             (println response)
                             (.loadServerList this)
                             (.hideDeleteConfirmationDialog this))}))
    :confirm-delete
    (fn [this server]
      (r/set-state this {:show-list true
                         :server-to-delete server}))
    :hide-delete-confirmation-dialog
    (fn [this]
      (r/set-state this {:server-to-delete nil}))
    :cancel-delete
    (fn [this]
      (.hideDeleteConfirmationDialog this))
    :render
    (fn [this]
      (let [{:keys [show-list
                    servers
                    server-to-delete]} (r/state this)
            show-confirm-delete-dialog? (not (nil? server-to-delete))]
        (if show-list
          [:div nil
           [add-server-form this.beginAddServer]
           [:br]
           [server-list {:servers servers
                         :on-delete (fn [server]  (.confirmDelete this server))}]
           (if show-confirm-delete-dialog?
             [delete-confirmation-dialog {:server server-to-delete
                                          :on-ok (fn [] (this.handleDelete server-to-delete))
                                          :on-cancel this.cancelDelete}])]
          [edit-server-form {:on-save this.saveServer
                             :on-cancel this.cancelEditServer}])))}))

(defn ^:export init []
  (r/render-component [settings-page]
                      (js/document.getElementById "main-content")))


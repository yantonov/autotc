(ns autotc-web.settings.settings
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]
            [ajax.core :as ajax]
            [rex.core :as rcore]
            [rex.ext.cursor :as rcur]
            [rex.reducer :as rr]
            [rex.watcher :as rwt]
            [rex.middleware :as rmw]
            [rex.ext.action-creator :as acm]
            [rex.ext.reducer-for-type :as rrtype]
            [autotc-web.util.reducer-helpers :as rhp]))

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

(defn- define-reducers []
  (rrtype/reducer-for-type :init-page
                           (fn [state action]
                             (rcur/update-state (:cursor action)
                                                state
                                                {:show-list true
                                                 :servers []})))

  (rrtype/reducer-for-type :server-list-loaded
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:servers (:servers action)})))

  (rrtype/reducer-for-type :toggle-list
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:show-list (:visible action)})))

  (rrtype/reducer-for-type :confirm-delete-server
                           (fn [state action]
                             (rhp/merge-state state
                                              (:cursor action)
                                              {:server-to-delete (:server action)})))

  (rrtype/reducer-for-type :text-input-changed
                           (fn [state action]
                             (let [cursor (:cursor action)
                                   value (:value action) ]
                               (rcur/update-state cursor
                                                  state
                                                  value))))
  )

(defn load-server-list-action-creator [cursor]
  (fn [dispatch get-state]
    (ajax/GET "/settings/servers/list"
              {:response-format (ajax/json-response-format {:keywords? true})
               :handler (fn [response]
                          (dispatch {:type :server-list-loaded
                                     :cursor cursor
                                     :servers (:servers response)}))
               :error-handler (fn [response]
                                (println response))})))

(defn show-list-command [cursor visible]
  {:type :toggle-list
   :cursor cursor
   :visible visible})

(defn confirm-delete-server-command [cursor server]
  {:type :confirm-delete-server
   :cursor cursor
   :server server})

(defn save-server-action-creator [form-cursor page-cursor]
  (fn [dispatch get-state]
    (let [server (rcur/get-state form-cursor (get-state))]
      (ajax/POST "/settings/servers/add"
                 {:params server
                  :format (ajax/url-request-format)
                  :handler (fn [response]
                             (dispatch (load-server-list-action-creator page-cursor))
                             (dispatch (show-list-command page-cursor true)))}))))

(defn delete-server-action-creator [cursor server]
  (fn [dispatch get-state]
    (ajax/POST "/settings/servers/delete"
               {:params {:id (:id server)}
                :format (ajax/url-request-format)
                :handler (fn [response]
                           (dispatch (load-server-list-action-creator cursor))
                           (dispatch (confirm-delete-server-command cursor nil)))
                :error-handler (fn [response]
                                 (println response))})))

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
                      (rcore/dispatch {:type :text-input-changed
                                       :cursor (rcur/nest form-cursor
                                                          field-key)
                                       :value (-> e
                                                  .-target
                                                  .-value)})))]
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
  (let [page-cursor (rcur/nest (rcur/make-cursor)
                               :page)
        edit-server-form-cursor (rcur/nest page-cursor
                                           :edit-server-form)]
    (r/create-class
     {:begin-add-server
      (fn [this]
        (rcore/dispatch (show-list-command page-cursor false)))
      :show-list
      (fn [this]
        (rcore/dispatch (show-list-command page-cursor true)))
      :save-server
      (fn [this]
        (rcore/dispatch (save-server-action-creator edit-server-form-cursor
                                                    page-cursor)))
      :cancel-edit-server
      (fn [this]
        (.showList this))
      :load-server-list
      (fn [this]
        (rcore/dispatch (load-server-list-action-creator page-cursor)))
      :component-did-mount
      (fn [this]
        (rwt/defwatcher
          (fn [old-state new-state]
            (r/set-state this (rcur/get-state page-cursor new-state))))
        (rcore/dispatch {:type :init-page
                         :cursor page-cursor})
        (.loadServerList this))
      :handle-delete
      (fn [this server]
        (rcore/dispatch (delete-server-action-creator page-cursor server)))
      :confirm-delete
      (fn [this server]
        (rcore/dispatch (confirm-delete-server-command page-cursor server)))
      :hide-delete-confirmation-dialog
      (fn [this]
        (rcore/dispatch (confirm-delete-server-command page-cursor nil)))
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
            [edit-server-form {:form-cursor edit-server-form-cursor
                               :on-save this.saveServer
                               :on-cancel this.cancelEditServer}])))})))

(defn ^:export init []
  (rmw/defmiddleware acm/action-creator-middleware)
  (define-reducers)
  (r/render-component [settings-page]
                      (js/document.getElementById "main-content")))


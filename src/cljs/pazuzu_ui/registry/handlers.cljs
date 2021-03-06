(ns pazuzu-ui.registry.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :refer [register-handler dispatch]]
            [pazuzu-ui.registry.service :as service]
            [taoensso.timbre :as log]))

;; whener a feature is clicked in the registry page
(register-handler :feature-selected
                  (fn [db [_ feature]]
                      (dispatch [:start-loading :feature-detail-loading?])
                    (service/get-feature (get-in db [:authentication :token])
                                         (:name feature)
                                         #(do (log/debug "Fetched : " %)
                                              (dispatch [:feature-selected-loaded %]))
                                         #(do (dispatch [:stop-loading :feature-detail-loading?])
                                              (dispatch [:add-message {:type "error" :header "Error Retrieving the feature" :message %} ])))
                      db))

;; whenever the feature selected is loaded, update the db
(register-handler :feature-selected-loaded
                  (fn [db [_ feature]]
                    (dispatch [:stop-loading :feature-detail-loading?])
                    (-> db
                        (assoc-in [:ui-state :registry-page :feature-pane :new-feature?] false)
                        (assoc-in [:ui-state :registry-page :feature-pane :feature] feature)
                        (assoc-in [:ui-state :registry-page :selected-feature-name] (:name feature)))))

;; when the search input value is updated, propagates the change to the db
(register-handler :search-input-changed
                  (fn [db [_ value]]
                    (assoc-in db [:ui-state :registry-page :search-input-value] value)))

(register-handler :feature-edited
                  (fn [db [_ feature]]
                    (assoc-in db [:ui-state :registry-page :feature-pane :feature] feature)))

;; when the save button is clicked, update the db by pushing the new feature to the list of features
(register-handler :save-feature-clicked
                  (fn [db [_ _]]
                    (let [feature (-> db :ui-state :registry-page :feature-pane :feature)
                          new-feature? (-> db :ui-state :registry-page :feature-pane :new-feature?)
                          token (get-in db [:authentication :token])]
                      (dispatch [:start-loading :feature-detail-loading?])
                      (if new-feature?
                        (service/add-feature token feature
                          #(dispatch [:saved-feature %])
                          #(do
                            (dispatch [:stop-loading :feature-detail-loading?])
                            (dispatch [:add-message {:type "error" :header "Error Saving the features" :message %}])))
                        (service/update-feature token feature
                          #(dispatch [:updated-feature %])
                          #(do
                            (dispatch [:stop-loading :feature-detail-loading?])
                            (dispatch [:add-message {:type "error" :header "Error Updating the features" :message %}]))))
                      db)))

;; when dependency is removed from the list, only db state is updated
(register-handler :delete-dependency-clicked
                  (fn [db [_ deleted_dep]]
                    (let [dependencies (-> db :ui-state :registry-page :feature-pane :feature :dependencies)
                          dependencies_after_removal (vec (remove #{deleted_dep} dependencies))]
                      (-> db
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :dependencies] dependencies_after_removal)))))

;; when dependency is removed from the list, only db state is updated
(register-handler :add-dependency-clicked
                  (fn [db [_]]
                    (let [new_dependency (-> db :ui-state :registry-page :feature-pane :feature :new-dependency)
                          dependencies (-> db :ui-state :registry-page :feature-pane :feature :dependencies)
                          extended_dependencies (vec (conj (set dependencies) {:name new_dependency}))]
                        (-> db
                            (assoc-in [:ui-state :registry-page :feature-pane :feature :dependencies] extended_dependencies)
                            (assoc-in [:ui-state :registry-page :feature-pane :feature :new-dependency] nil))
                        )))

;; when tag was added to a feature
(register-handler :add-feature-tag-clicked
                  (fn [db [_]]
                    (let [new_feature_tag  (-> db :ui-state :registry-page :feature-pane :feature :new-feature-tag)
                          tags (-> db :ui-state :registry-page :feature-pane :feature :tags)
                          updated_feature_tags (vec (conj (set tags) {:name new_feature_tag}))]

                      (-> db
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :tags] updated_feature_tags)
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :new-feature-tag] nil)
                          )
                      )))

;; when tag delete

(register-handler :delete-feature-tag-clicked
                  (fn [db [_ deleted_tag]]
                    (let [tags (-> db :ui-state :registry-page :feature-pane :feature :tags)
                          updated_feature_tags (vec (remove #{deleted_tag} tags))]
                      (-> db
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :tags] updated_feature_tags)))))

;; update db state after api retures success for adding a feature
(register-handler :saved-feature
                  (fn [db [_ feature]]
                    (let [current_features (-> db :registry :features)
                          per-page (-> db :ui-state :registry-page :per-page)
                          total-features (-> db :ui-state :registry-page :total-features)]
                      (dispatch [:add-message {:type "success" :header "Your feature has been saved" :time 2}])
                      (dispatch [:stop-loading :feature-detail-loading?])
                      (-> db
                          (assoc-in [:registry :features] (conj (if (< (count current_features) per-page) current_features (butlast current_features)) feature))
                          (assoc-in [:ui-state :registry-page :total-features] (inc (int total-features)))
                          (assoc-in [:ui-state :registry-page :selected-feature-name] (:name feature))
                          (assoc-in [:ui-state :registry-page :feature-pane :new-feature?] false)))))


;; update db state after api retures success for updating a feature
(register-handler :updated-feature
                  (fn [db [_ feature]]
                    (dispatch [:stop-loading :feature-detail-loading?])
                    (dispatch [:add-message {:type "success" :header "Your feature has been updated" :time 2}])
                    db))


;; when new feature button is clicked, push an empty feature to the db flagged new-feature = true
(register-handler :new-feature-clicked
                  (fn [db [_ _]]
                    (-> db
                        (assoc-in [:ui-state :registry-page :feature-pane :feature] {})
                        (assoc-in [:ui-state :registry-page :feature-pane :new-feature?] true))))

;; when the delete feature button is clicked, make an API call
(register-handler :delete-feature-clicked
                  (fn [db [_ _]]
                    (let [feature (-> db :ui-state :registry-page :feature-pane :feature)
                          token (get-in db [:authentication :token])]
                      (dispatch [:start-loading :feature-detail-loading?])
                      (service/delete-feature token feature
                        #(dispatch [:deleted-feature])
                        #(do
                          (dispatch [:stop-loading :feature-detail-loading?])
                          (dispatch [:add-message {:type "error" :header "Error Deleting the feature" :message %}])))
                      db)))

;; when the delete operation was successful, update the db state
(register-handler :deleted-feature
                  (fn [db [_ _]]
                    (let [feature (-> db :ui-state :registry-page :feature-pane :feature)
                          features (-> db :registry :features)
                          features_after_removal (vec (filter #(not= (:name %) (:name feature)) features))]
                      (dispatch [:stop-loading :feature-detail-loading?])
                      (-> db
                          (assoc-in [:registry :features] features_after_removal)
                          (assoc-in [:ui-state :registry-page :feature-pane :feature] {})
                          (assoc-in [:ui-state :registry-page :feature-pane :new-feature?] true)))))

;;load just a page of the registre features
(register-handler :load-features-page
                  (fn [db [_ _]]
                    (let [per-page (-> db :ui-state :registry-page :per-page)
                          page (-> db :ui-state :registry-page :page)
                          offset (* (- page 1) per-page)
                          token (get-in db [:authentication :token])]
                      (dispatch [:start-loading :features-loading?])
                      (service/get-features-page
                       token offset per-page
                       (fn [features total]
                         (log/debug "Features received from the backend : " total)
                         (dispatch [:loaded-features-page (list features total)]))
                       #(do (log/debug "Fail to retrive features : " %)
                            (dispatch [:stop-loading :features-loading?])
                            (dispatch [:add-message {:type "error" :header "Error Retrieving the features" :message %} ])))
                      db)))


;; update the db state by setting the features
(register-handler :loaded-features
                  (fn [db [_ features]]
                    (dispatch [:stop-loading :features-loading?])
                    (assoc-in db [:registry :features] features)))

;;when a page is loaded we got total-features parameter
(register-handler :loaded-features-page
                  (fn [db [_ params]]
                    (dispatch [:stop-loading :features-loading?])
                    (-> db
                        (assoc-in [:ui-state :registry-page :total-features] (nth params 1))
                        (assoc-in [:registry :features] (nth params 0)))))

;;set a new page and call load-features
(register-handler :change-feature-page
                  (fn [db [_ page]]
                    (dispatch [:load-features-page])
                    (assoc-in db [:ui-state :registry-page :page] page)))

(register-handler :check-initial-page
                  (fn [db]
                    (let [name (get-in db [:ui-state :active-page :route-params])]
                      (if name (dispatch [:feature-selected name]))
                    db)))

;;Loading hadnlers
(register-handler :stop-loading
                  (fn [db [_ type]]
                    (assoc-in db [:ui-state :registry-page type] false)))

(register-handler :start-loading
                  (fn [db [_ type]]
                    (assoc-in db [:ui-state :registry-page type] true)))

(register-handler :search-tag-started
                  (fn [db [_ query]]
                    (let [token (get-in db [:authentication :token])]
                      (do
                        (log/debug (str "changed field tag :" query))

                        (service/search-tags token query
                                             #(do (log/debug "Tags received from the backend : " %)
                                                  (dispatch [:search-tag-end %]))
                                             #(do (log/debug "Fail to retrive tags : " %)
                                                  (dispatch [:add-message {:type "error" :header  (str  "Error searching the tags by query '" query "'") :message %} ])))
                        (assoc-in db [:ui-state :registry-page :feature-pane :feature :new-feature-tag] query)
                        ))))

(register-handler :search-tag-end
                  (fn [db [_ search-tags]]
                    (case (count search-tags)
                      0 (-> db
                          (assoc-in  [:ui-state :registry-page :feature-pane :feature :tag-list] [])
                            (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list-index] -1)
                            )
                      1  (-> db
                             (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list] search-tags)
                             (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list-index] 0))
                      (-> db
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list] search-tags)
                          (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list-index] -1)))))

(defn navigation->list-index [event length current-index]
  (let [cycle-index (fn [index length] (if (neg? index) (dec length) (if (= index length) 0 index) ))]
          (do
            (log/debug (str event "-> len :" length  "current " current-index "next " (cycle-index (inc current-index) length) "prev " (cycle-index (dec current-index) length)))
            (if (number? event) event
                                (case event
                                  :list-item-first 0
                                  :list-item-last (dec length)
                                  :list-item-next (cycle-index (inc current-index) length)
                                  :list-item-prev (cycle-index (dec current-index) length)
                                  :list-item-current current-index
                                  :list-item-reset -1)))))

(register-handler :tag-list-navigation-change
                  (fn [db [_ navigation]]
                    (do
                      (log/debug (str " handler " :tag-list-navigation-change " nav " navigation))
                      (let [search-tags (-> db :ui-state :registry-page :feature-pane :feature :tag-list)
                            tags-length (count search-tags)
                            current-tag-list-index (-> db :ui-state :registry-page :feature-pane :feature :tag-list-index)
                            ]
                        (if (and (= :list-item-current navigation) (not (neg? current-tag-list-index)) (< current-tag-list-index tags-length))
                          (do
                            (log/debug "enter processing ")
                            (-> db
                                (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list] [])
                                (assoc-in [:ui-state :registry-page :feature-pane :feature :tag-list-index] -1)
                                (assoc-in [:ui-state :registry-page :feature-pane :feature :new-feature-tag] (:name (nth search-tags current-tag-list-index)))))

                          (do
                            (log/debug (str ":tag-list-index-change with [" navigation "] nav index is " (navigation->list-index navigation tags-length current-tag-list-index)))
                            (assoc-in db [:ui-state :registry-page :feature-pane :feature :tag-list-index] (navigation->list-index navigation tags-length current-tag-list-index))))))))

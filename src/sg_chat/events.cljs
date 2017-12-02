(ns sg-chat.events
  (:require
   [re-frame.core :refer [reg-event-db ->interceptor
                          dispatch-sync
                          dispatch reg-fx reg-event-fx]]
   [sg-chat.firebase :refer [firebase]]
   [sg-chat.constants :as c]
   [sg-chat.utils :as u]
   [sg-chat.effects]
   [sg-chat.db :as db :refer [app-db]]))


;; -- Handlers --------------------------------------------------------------


(reg-event-fx
 :initialize-db
 (fn [{:keys [db]} _]
   (let [on-success (fn [user]
                      (dispatch-sync [:set-initialized true])
                      (when-not (nil? user)
                        (dispatch-sync [:set-current-screen :main])
                        (dispatch-sync [:set-user-in-db (u/parse-json user)])
                        (dispatch [:get-channels-from-firebase])))]
     {:db app-db
      :get-user-from-local-storage {:on-success on-success
                                    :on-error println}})))

(reg-event-db
 :set-initialized
 (fn [db bool]
   (assoc db :initialized? bool)))

(reg-event-fx
 :get-channels-from-firebase
 (fn [_ _]
   (let [on-response (fn [response]
                       (dispatch [:set-channels-in-db (-> (.val response)
                                                          u/to-clj)]))]
     {:get-channels-from-firebase on-response})))

(reg-event-db
 :set-user-in-db
 (fn [db [_ user]]
   (assoc db :user user)))

(reg-event-fx
 :save-user-in-local-storage
 (fn [{:keys [db]}[_ username]]
   (let [user {:name username
               :_id (u/node-uuid)}
         on-save-success (fn []
                           (dispatch [:add-user-to-firebase user])
                           (dispatch [:set-current-screen :main])
                           (dispatch [:get-channels-from-firebase]))
         error-cb #(dispatch [:show-error "Unable to save user"])]
     {:db (assoc db :user user :reg-btn-loading? true)
      :save-user-in-local-storage {:user user
                                   :error-cb error-cb
                                   :success-cb on-save-success}})))

(reg-event-db
 :reset-routing-state
 (fn [db _]
   (assoc db :routing (clj->js {:index 0
                                :routes [{:key "MainStack"
                                          :routeName "MainStack"
                                          :index 0
                                          :routes [{:key "Channels"
                                                    :routeName "Channels"}
                                                   {:key "Chat"
                                                    :routeName "Chat"}]}]}))))

;; (reg-event-fx
;;  :navigate-to
;;  (fn [_ [_ opts]]
;;    {:navigate-to opts}))

;; channels
(reg-event-db
 :set-current-channel
 (fn [db [_ channel]]
   (assoc db :current-channel channel)))

(reg-event-fx
 :open-channel
 (fn [{:keys [db]} [_ channel]]
   (let [channel-key (keyword (:name channel))
         existing-messages (-> db :messages channel-key)
         latest-message (first existing-messages)]
     {:db (if (empty? existing-messages)
            (assoc db :fetching? true)
            db)
      :get-channel-messages {:channel channel
                             :latest-message latest-message}})))

(reg-event-fx
 :set-messages-in-db
 (fn [{:keys [db]}[_ channel-key new-messages]]
   (let [comparator-fn #(js/Date. (:createdAt %))
         existing-messages (or (-> db :messages channel-key) [])
         make-consumable (fn [m]
                           (when-not (nil? new-messages)
                             (->> new-messages
                                  (map #(second %1))
                                  (sort-by comparator-fn)
                                  reverse
                                  (into []))))
         remove-duplicates (fn [m]
                             (println (first m) (first existing-messages))
                             (when-not (nil? m)
                               (if (= (first m)
                                      (first existing-messages))
                                 (rest m)
                                 m)))
         sorted-messages (-> new-messages
                             make-consumable
                             remove-duplicates)]
     {:db (-> (update-in db [:messages channel-key] concat (or sorted-messages []))
              (assoc :fetching? false))})))

(reg-event-fx
 :set-channels-in-db
 (fn [{:keys [db]} [_ channels]]
   {:db (assoc db :channels (map second channels))}))

(reg-event-db
 :set-current-screen
 (fn [db [_ screen]]
   (assoc db :current-screen screen)))

(reg-event-fx
 :send-message
 (fn [{:keys [db]} [_ [channel-name message-coll]]]
   {:db (update-in db [:messages (keyword channel-name)]
                   concat message-coll)
    :append-message-to-firebase [channel-name (first message-coll)]}))

(reg-event-fx
 :add-user-to-firebase
 (fn [_ [_ user]]
   {:add-user-to-firebase user}))

(reg-event-fx
 :show-error
 (fn [{:keys [db]} [_ error]]
   {:show-error error
    :db (assoc db :reg-btn-loading? false)}))



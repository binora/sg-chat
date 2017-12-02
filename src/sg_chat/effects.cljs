(ns sg-chat.effects
  (:require
   [re-frame.core :refer [reg-fx dispatch]]
   [sg-chat.firebase :as f]
   [sg-chat.utils :as u]
   [sg-chat.db :refer [app-db]]
   [sg-chat.rn :refer [alert]]
   [sg-chat.constants :as c]
   [sg-chat.db :as db :refer [app-db]]))


(reg-fx
 :save-user-in-local-storage
 (fn [{:keys [user success-cb error-cb]}]
   (db/set-item-local-storage "user"
                              (.stringify js/JSON (clj->js user))
                              success-cb
                              error-cb)))

;; (reg-fx :sign-in-user f/sign-in-anonymously)

(reg-fx
 :get-user-from-local-storage
 (fn [{:keys [on-success on-error]}]
   (db/get-item-local-storage "user"
                              on-success
                              on-error)))

(reg-fx
 :append-message-to-firebase
 (fn [[channel-name message]]
   (f/send-message channel-name message)))

(reg-fx
 :get-channel-messages
 (fn [channel]
   (let [on-response (fn [response]
                       (dispatch [:set-messages-in-db
                                  (keyword (:name channel))
                                  (-> (.val response)
                                      u/to-clj)]))]
     (f/get-channel-messages  (:name channel)
                              on-response
                              c/hours-before))))

(reg-fx
 :get-channels-from-firebase
 (fn [on-response]
   (f/get-channels on-response)))

(reg-fx
 :add-user-to-firebase
 (fn [user]
   (f/add-user-to-firebase user)))

(reg-fx
 :show-error
 (fn [error]
   (alert {:title error
           :message ""
           :buttons [{:text "Try Again"
                      :onPress #(dispatch [:save-user-in-local-storage (-> app-db
                                                                           :user
                                                                           :name)])}]})))

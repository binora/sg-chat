(ns sg-chat.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :get-greeting
 (fn [db _]
   (:greeting db)))

(reg-sub
 :kv
 (fn [db [_ key]]
   (key db)))

(reg-sub
 :channel-messages
 (fn [{:keys [messages]} [_ channel-name]]

   (let [channel-messages (or ((keyword channel-name) messages)
                              [])
         comparator-fn #(js/Date. (:createdAt %))]
     (->> channel-messages
          (sort-by comparator-fn)
          reverse)
     channel-messages)))

(reg-sub
 :error
 (fn [db _]
   (or (:error db) "")))

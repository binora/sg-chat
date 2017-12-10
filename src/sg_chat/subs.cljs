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
   (or ((keyword channel-name) messages)
       [])))

(reg-sub
 :error
 (fn [db _]
   (or (:error db) "")))

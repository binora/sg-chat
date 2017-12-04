(ns sg-chat.firebase
  (:require [re-frame.core :refer [dispatch]]
            [sg-chat.utils :as u]))

(enable-console-print!)

(def firebase (js/require "firebase/app"))
(def firebase-auth (js/require "firebase/auth"))
(def firebase-db (js/require "firebase/database"))

(defonce api-key "KSk]MOzU<[Oz7@RV}kYO~C;_YTorP=OA:")

(defonce fb-config {:apiKey (->> api-key
                                 (map #(- (int %) 10))
                                 (map char)
                                 (apply str))
                    :authDomain "sg-chat-53577.firebaseapp.com",
                    :databaseURL "https://sg-chat-53577.firebaseio.com",
                    :projectId "sg-chat-53577",
                    :storageBucket "sg-chat-53577.appspot.com",
                    :messagingSenderId "175013113788"})

(defonce fb-init (.initializeApp firebase (clj->js fb-config)))


(defn send-message [channel-name message]
  (let [m (assoc message :createdAt (u/get-time (:createdAt message)))]
    (-> (.database firebase)
        (.ref (u/build-fpath (str channel-name "-messages")))
        (.push (clj->js m)))))

(defn get-channel-messages [channel-name on-response hours-before latest-message]
  (let [ref (-> (.database firebase)
                (.ref (-> channel-name
                          (str "-messages")
                          u/build-fpath)))]
    (-> ref
        (.orderByChild "createdAt")
        (.startAt (u/get-date-before hours-before))
        (.on "child_added" on-response))))

(defn get-channels [on-response]
  (-> (.database firebase)
      (.ref (u/build-fpath "channels"))
      (.on "value" on-response)))

(defn add-user-to-firebase [user]
  (-> (.database firebase)
      (.ref (u/build-fpath "users"))
      (.push (clj->js user))))

(defn get-matching-users-from-firebase
  [search-str on-response]
  (-> (.database firebase)
      (.ref (u/build-fpath "users"))
      (.orderByChild "name")
      (.startAt search-str)
      (.endAt (str search-str "\uf8ff"))
      (.once "value" on-response)))

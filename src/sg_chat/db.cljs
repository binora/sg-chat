(ns sg-chat.db
  (:require [clojure.spec.alpha :as s]))


;; initial state of app-db
(def app-db {:greeting "Hello Clojurescript in Expo!"
             :current-channel {}
             :current-screen :sign-in
             :fetching? false
             :initialized? false
             :reg-btn-loading? false
             :messages {}})

;; local storage
(def ReactNative (js/require "react-native"))
(def AsyncStorage (.-AsyncStorage ReactNative))

(defn set-item-local-storage
  [key value success-cb error-cb]
  (-> (.setItem AsyncStorage key value)
      (.then success-cb)
      (.catch error-cb)))

(defn get-item-local-storage
  [key success-cb error-cb]
  (-> (.getItem AsyncStorage key)
      (.then success-cb)
      (.catch error-cb)))


(defn delete-item-local-storage
  [key]
  (-> (.removeItem AsyncStorage key)
      (.then #(println %))
      (.catch #(println "error"))))

;; (delete-item-local-storage "user")

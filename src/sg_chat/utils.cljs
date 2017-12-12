(ns sg-chat.utils
  (:require [goog.crypt :as crypt]
            [goog.crypt.Sha1 :as Sha1]
            [sg-chat.constants :as c]))

(enable-console-print!)

(def node-uuid (js/require "uuid/v1"))
(def moment (js/require "moment"))

(defn to-clj [data]
  (js->clj data :keywordize-keys true))

(defn build-fpath
  [resource]
  (str c/env "-"
       c/country "-"
       resource))

(defn get-date-before [hours]
  (-> (js/Date.)
      (.getTime)
      (-  (* hours 60 60 1000))))

(defn parse-json [item]
  (when-not (nil? item)
    (->> item
         (.parse js/JSON)
         to-clj)))

(defn get-time [date]
  (-> date
      (js/Date.)
      (.getTime)))

(defn digest [hasher bytes]
  (.update hasher bytes)
  (.digest hasher))

(defn encrypt [input]
  (when-not (nil? input)
    (->> (crypt/stringToUtf8ByteArray input)
         (digest (goog.crypt.Sha1.))
         crypt/byteArrayToHex)))

(defn get-text [e]
  (-> e .-nativeEvent .-text))


(defn today?
  [time]
  (let [today (moment)
        input (moment time)]
    (.isSame today input "day")))

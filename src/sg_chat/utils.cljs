(ns sg-chat.utils
  (:require [goog.crypt :as crypt]
            [goog.crypt.Md5 :as Md5]
            [sg-chat.constants :as c]))

(enable-console-print!)

(def node-uuid (js/require "uuid/v1"))

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

(defn encrypt [input]
  (->> input
       (.digest (goog.crypt.Md5.))
       crypt/byteArrayToHex))

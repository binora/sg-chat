(ns sg-chat.analytics
  (:require [sg-chat.constants :as c]))

(def GATracker (.-GoogleAnalyticsTracker (js/require "react-native-google-analytics-bridge")))
(def ga (GATracker. c/ga-tracking-id))


(defn track-screen [screen-name]
  (.trackScreenView ga screen-name))

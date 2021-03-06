(ns sg-chat.android.core
    (:require [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch
                                     dispatch-sync]]
              [sg-chat.screens :refer [sign-in-screen chat-screen
                                      loading-screen
                                      channels-screen]]
              [cljs-react-navigation.re-frame :refer [router]]
              [sg-chat.rn :refer [ReactNative]]
              [sg-chat.router :refer [ChannelsStack]]
              [sg-chat.events]
              [sg-chat.constants :as c]
              [sg-chat.subs]))

(def app-registry (.-AppRegistry ReactNative))

(defn app-root []
  (let [current-screen (subscribe [:kv :current-screen])]
    (fn []
      (condp = @current-screen
        :sign-in [sign-in-screen]
        :main [:> ChannelsStack {}]))))

(defn init []
  (dispatch-sync [:initialize-db])
  (.registerComponent app-registry "sgChat" #(r/reactify-component app-root)))

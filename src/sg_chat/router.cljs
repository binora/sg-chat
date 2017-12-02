(ns sg-chat.router
  (:require [sg-chat.rn :as rn]
            [reagent.core :as r]
            [sg-chat.constants :as c]
            [sg-chat.screens :refer [sign-in-screen loading-screen
                                    channels-screen chat-screen]]
            [cljs-react-navigation.re-frame :refer [stack-navigator
                                                    stack-screen]]
            [cljs-react-navigation.base :as nav-base]))


(defn drawer-screen [react-component navigationOptions]
  (stack-screen react-component navigationOptions))

(defn drawer-navigator [routeConfigs drawerNavigatorConfig]
  (nav-base/DrawerNavigator (clj->js routeConfigs) (clj->js drawerNavigatorConfig)))


(defn chat-screen-nav-options [{:keys [navigation]}]
  (let [{:keys [navigate state goBack]} navigation
        {:keys [params]} state]
    {:headerTitle (:title params)
     :headerTitleStyle {:color "white"
                        :alignSelf "center"
                        :fontWeight "bold"
                        :marginBottom 0}
     :headerTintColor "white"
     :headerBackTitle " "
     :headerRight (r/as-element [rn/view])
     :headerStyle {:height 60
                   :backgroundColor c/header-bg-color}}))

(def ChannelsStack (stack-navigator
                {:Channels
                 {:screen (stack-screen channels-screen
                                        {:headerTitle "Channels"
                                         :headerStyle {:backgroundColor c/header-bg-color
                                                       :height 60}
                                         :headerTintColor "white"
                                         :headerBackTitle " "
                                         :headerTitleStyle {:color "white"
                                                            :fontWeight "bold"
                                                            :alignSelf "center"
                                                            :marginBottom 0}})}
                 :Chat
                 {:screen (stack-screen chat-screen
                                        chat-screen-nav-options)}}
                {:headerMode "screen"}))

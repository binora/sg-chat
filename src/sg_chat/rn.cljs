(ns sg-chat.rn
  (:require [reagent.core :as r :refer [atom]]
            [sg-chat.utils :as u]))

(enable-console-print!)

(def ReactNative (js/require "react-native"))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def dimensions (u/to-clj (-> (.-Dimensions ReactNative) (.get "window"))))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def FlatList (.-FlatList ReactNative))
(def flat-list (r/adapt-react-class FlatList))

(def Alert (.-Alert ReactNative))
(defn alert [{:keys [title message buttons]}]
  (.alert Alert title message (clj->js buttons)))
(def activity-indicator (r/adapt-react-class (.-ActivityIndicator ReactNative)))
(def linking (-> ReactNative .-Linking))

;; gifted chat
(def react-native-gifted-chat (js/require "react-native-gifted-chat"))
(def gifted-chat (r/adapt-react-class (.-GiftedChat react-native-gifted-chat)))
(def gc-send (r/adapt-react-class (.-Send react-native-gifted-chat)))

;; React navigation
(defonce ReactNavigation (js/require "react-navigation"))


;; react native elements
(def react-native-elements (js/require "react-native-elements"))
(def button (r/adapt-react-class (.-Button react-native-elements)))
(def rn-list (r/adapt-react-class (.-List react-native-elements)))
(def list-item (r/adapt-react-class (.-ListItem react-native-elements)))


;; react native keyboard spacer
(def keyboard-spacer (r/adapt-react-class (.-default (js/require "react-native-keyboard-spacer"))))


(def ios? (= "ios" (-> ReactNative .-Platform .-OS)))

(def react-native-animatable (js/require "react-native-animatable"))
(def animated-text (r/adapt-react-class (.-Text react-native-animatable)))
(def animated-view (r/adapt-react-class (.-View react-native-animatable)))

(def MaterialIcons (.-default (js/require "react-native-vector-icons/MaterialIcons")))
(def material-icons (r/adapt-react-class MaterialIcons))


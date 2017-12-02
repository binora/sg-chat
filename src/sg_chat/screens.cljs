(ns sg-chat.screens
  (:require [reagent.core :as r :refer [atom]]
            [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [sg-chat.rn :refer [text view image text-input dimensions
                               gifted-chat rn-list list-item
                               button activity-indicator gc-send
                               keyboard-spacer linking material-icons
                               animated-text animated-view
                               touchable-highlight]]
            [sg-chat.utils :as u]
            [sg-chat.constants :as c]
            [sg-chat.subs]
            [sg-chat.analytics :as analytics]
            [clojure.string :as str]))

(defonce device-height (:height dimensions))
(defonce device-width (:width dimensions))


(defn container [& children]
  [view {:style {:flex 1
                 :flex-direction "column"
                 :justify-content "center"
                 :height device-height
                 :width device-width
                 :align-items "center"}}
   (map-indexed #(with-meta %2 {:key %1})
                children)])

(defn loading-screen []
  [container
   [view {:flex 1
          :margin-top "50%"}
    [activity-indicator {:animating true
                         :size "large"}]]])

(defn sign-in-screen [{:keys [navigation screenProps] :as props}]
  (let [{:keys [navigate]} navigation
        db-initialized? (subscribe [:kv :initialized?])
        saved-user (subscribe [:kv :user])
        reg-btn-loading? (subscribe [:kv :reg-btn-loading?])
        state (r/atom {:username ""})
        get-text #(-> % .-nativeEvent .-text)
        on-input-change (fn [value]
                          (swap! state assoc :username (get-text value)))
        on-press (fn []
                   (when-not (-> (:username @state)
                                 string/trim
                                 empty?)
                     (dispatch [:save-user-in-local-storage
                                (string/trim (:username @state))])))]
    (fn [props]
      [container
       [view {:width "100%"
              :align-self "center"
              :height "100%"
              :align-items "center"
              :flex-direction "column"
              :style {:background-color c/header-bg-color}}
        (if @db-initialized?
          [animated-text (merge {:style {:font-size 30
                                         :margin-top 30
                                         :color "white"}

                                 :animation "fadeInDown"})
           "sg chat"]
          [activity-indicator])
        (if @db-initialized?
          [animated-view {:style {:width "100%"
                                  :flex-direction "column"
                                  :justify-content "center"}
                          :animation "fadeInUp"}
           [text-input {:style {:margin-top "30%"
                                :align-self "center"
                                :margin-bottom "10%"
                                :border-bottom-width 0.5
                                :border-bottom-color "white"
                                :color "white"
                                :width "60%"}
                        :auto-focus true
                        :on-submit-editing on-press
                        :tint-color "white"
                        :placeholder "Enter username"
                        :placeholder-text-color "white"
                        :underline-color-android "transparent"
                        :text-align "center"
                        :value (:username @state)
                        :on-change on-input-change}]
           (if @reg-btn-loading?
             [activity-indicator]
             [touchable-highlight {:on-press on-press
                                   :underlay-color "transparent"
                                   :style {:align-self "center"}}
              [text {:style {:color "white"}}
               "Register"]])])]])))


(defn render-channel [js-item navigate]
  (let [channel (-> js-item
                    u/to-clj
                    :item)
        on-press (fn []
                   (dispatch-sync [:set-current-channel channel])
                   (navigate "Chat" {:title (:name channel)})
                   (dispatch [:open-channel channel]))]
    (r/as-element
     [touchable-highlight {:margin 20
                           :on-press on-press}
      [view {:style {:margin 20}}
       [text (:name channel)]
       [text (:description channel)]]])))

(defn channels-screen [{:keys [navigation] :as props}]
  (let [{:keys [navigate]} navigation
        on-press (fn [channel]
                   (dispatch-sync [:set-current-channel channel])
                   (navigate "Chat" {:title (:name channel)})
                   (dispatch [:open-channel channel]))
        channels (subscribe [:kv :channels])
        channel-count (count @channels)]
    (fn [props]
      [container
       (if (empty? @channels)
         [loading-screen]
         [rn-list {:container-style {:width "100%"
                                     :height "100%"
                                     :margin-top 0
                                     :margin 0}}
          (map-indexed (fn [i {:keys [name icon description
                                      font-type] :as channel}]
                         ^{:key i}
                         [list-item {:key i
                                     :left-icon {:name icon
                                                 :type font-type}
                                     :on-press #(on-press channel)
                                     :title name}])
                       @channels)])])))


(defn render-username-on-message [props]
  (let [p (u/to-clj props)
        sender (-> p
                   :currentMessage
                   :user
                   :name)
        current-username (-> p :user :name)]
    (r/as-element
     [view
      [text {:style {:color "#3D7ED4"
                     :font-weight "bold"}}
       (if (= sender current-username) "" sender)]])))

(defn chat-screen [{:keys [navigation] :as props}]
  (let [user (subscribe [:kv :user])
        fetching? (subscribe [:kv :fetching?])
        channel (subscribe [:kv :current-channel])
        messages (subscribe [:channel-messages (:name @channel)])
        on-send (fn [message-coll]
                  (dispatch [:send-message
                             [(:name @channel)
                              (u/to-clj message-coll)]]))
        parse-patterns (fn [style]
                         (clj->js [{:type "url"
                                    :style style
                                    :onPress #(.openURL linking %)}]))
        render-send (fn [props]
                      (r/as-element
                       [gc-send (js->clj props)
                        [view {:style {:margin-bottom 5
                                       :margin-right 10}}
                         [material-icons {:name "send"
                                          :size 32}]]]))]
    (fn [props]
      [view {:style {:width "100%"
                     :height "100%"}}
       [gifted-chat {:messages @messages
                     :render-avatar nil
                     :render-send render-send
                     :render-custom-view render-username-on-message
                     :isLoadingEarlier @fetching?
                     :parse-patterns parse-patterns
                     :user @user
                     :on-send on-send}]])))


;; [flat-list {:data (into-array @channels)
;;             :key-extractor #(identity %2)
;;             :render-item #(render-channel %1 navigate)
;;             :style {:width "80%"
;;                     :flex 1

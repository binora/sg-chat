(ns sg-chat.screens
  (:require [reagent.core :as r :refer [atom]]
            [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [sg-chat.rn :refer [text view image text-input dimensions
                                gifted-chat rn-list list-item
                                button activity-indicator gc-send
                                keyboard-spacer linking material-icons
                                animated-text animated-view flat-list
                                mentions-input
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


(defn render-message [js-message current-sender]
  (let [message (-> js-message
                    u/to-clj
                    :item)
        message-sender (-> message :user :name)
        own-message? (= message-sender
                        current-sender)
        others-message-style {:align-self "flex-start"
                              :margin-left 10
                              :background-color "#e1e1e1"}
        own-message-style {:align-self "flex-end"
                           :margin-right 10
                           :color "white"
                           :background-color c/header-bg-color}]
    (r/as-element
     [view (merge {:flex 1
                   :max-width "40%"
                   :min-width 40
                   :min-height 40
                   :align-items "flex-start"
                   :flex-direction "column"
                   :border-radius 3
                   :margin-bottom "5%"}
                  (if own-message?
                    own-message-style
                    others-message-style))
      [view {:padding 2}
       (if-not own-message?
         [text {:style {:color "#3D7ED4"
                        :margin-bottom 5
                        :font-weight "bold"}}
          message-sender])
       [text  {:style {:color (if own-message? "white" "black")}}
        (:text message)]]])))

(defn chat-input [props]
  (let [init-state  {:input ""
                     :search-str ""}
        state (r/atom init-state)
        username-suggestions (subscribe [:kv :username-suggestions])
        trigger-cb (fn [str]
                     (when-not (empty? (subs str 1))
                       (swap! state assoc :search-str str)
                       (dispatch [:get-username-suggestions (subs str 1)])))
        on-suggestion-tap (fn [suggestion hide-panel]
                            (let [{:keys [input search-str]} @state
                                  before-text (->> (reverse input)
                                                   (drop (count search-str))
                                                   reverse
                                                   (apply str))]
                              (hide-panel)
                              (dispatch [:set-username-suggestions nil])
                              (swap! state assoc :input (str before-text suggestion))))
        render-suggestions-row (fn [props hide-panel]
                                 (r/as-element
                                  (let [suggestion (:item(u/to-clj props))]
                                    [view {:style {:width "50%"}}
                                     [touchable-highlight
                                      {:on-press #(on-suggestion-tap suggestion hide-panel)}
                                      [text suggestion]]])))
        on-send (fn [channel-name]
                  (dispatch [:send-message [channel-name {:text (string/trim (:input @state))
                                                          :_id (u/node-uuid)
                                                          :createdAt (u/get-time (js/Date.))
                                                          :user (:user props)}]])
                  (reset! state init-state))]
    (fn [props]
      [view {:style {:align-items "center"
                     :flex-direction "row"
                     :justify-content "space-between"
                     :width "100%"
                     :height 20
                     :margin-left 5}}
       [mentions-input {:on-change-text #(swap! state assoc :input %)
                        :trigger "@"
                        :trigger-callback trigger-cb
                        :horizontal false
                        :style {:width "100%"
                                :border-width 2}
                        :suggestions-data (or @username-suggestions [])
                        :render-suggestions-row render-suggestions-row
                        :value (or (:input @state) "")
                        :auto-grow true
                        :suggestion-row-height 20
                        :MaxVisibleRowCount 3
                        :key-extractor #(identity %2)
                        :trigger-location "new-word-only"
                        :placeholder "Write a message"}]
       [material-icons {:name "send"
                        :color c/header-bg-color
                        :on-press #(on-send (-> props :channel :name))
                        :size 30}]])))

(defn chat-screen [{:keys [navigation] :as props}]
  (let [user (subscribe [:kv :user])
        fetching? (subscribe [:kv :fetching?])
        channel (subscribe [:kv :current-channel])
        messages (subscribe [:channel-messages (:name @channel)])
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
      [container
       [view {:style {:width "100%"
                      :height "100%"
                      :margin-bottom 20}}
        [flat-list {:data (into-array @messages)
                    :key-extractor #(identity %2)
                    :render-item #(render-message %1 (:name @user))
                    :inverted true
                    :style {:width "100%"
                            :margin-bottom 10
                            :flex 1}}]
        [chat-input {:user @user
                     :channel @channel}]]])))

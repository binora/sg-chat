(ns sg-chat.screens
  (:require [reagent.core :as r :refer [atom]]
            [clojure.string :as string]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [sg-chat.rn :refer [text view image text-input dimensions
                                rn-list list-item parsed-text clipboard
                                button activity-indicator actionsheet
                                keyboard-spacer linking material-icons
                                animated-text animated-view flat-list
                                touchable-highlight input-scroll-view]]
            [sg-chat.utils :as u]
            [sg-chat.constants :as c]
            [sg-chat.subs]
            [sg-chat.analytics :refer [track-screen]]))

(enable-console-print!)

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
        state (r/atom {:username ""
                       :password ""})
        password-ref (atom nil)
        on-input-change (fn [key value]
                          (swap! state assoc key (if (= key :username)
                                                   (string/lower-case value)
                                                   value)))
        on-press (fn [event]
                   (let [{:keys [username password]} @state
                         invalid-input? (or (empty? username)
                                            (empty? password))
                         user (when-not invalid-input?
                                {:name (->> (:username @state)
                                            string/trim)
                                 :password (u/encrypt (:password @state))
                                 :id (u/node-uuid)})]
                     (if-not invalid-input?
                       (dispatch [event user])
                       (dispatch [:show-error "Please enter both username and password."]))))
        focus-on-password #(.focus @password-ref)]
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
                                         :margin-top "10%"
                                         :color "white"}

                                 :animation "fadeInDown"})
           "sg chat"]
          [activity-indicator {:color "white"
                               :size "large"
                               :margin-top "50%"}])
        (if @db-initialized?
          [animated-view {:style {:width "80%"
                                  :flex-direction "column"
                                  :justify-content "center"}
                          :animation "fadeInUp"}
           [text-input {:style {:margin-top "20%"
                                :align-self "center"
                                :border-bottom-width 0.5
                                :border-bottom-color "white"
                                :color "white"
                                :width "60%"}
                        :auto-focus true
                        :on-submit-editing focus-on-password
                        :tint-color "white"
                        :selection-color "white"
                        :auto-capitalize "none"
                        :placeholder "username"
                        :placeholder-text-color "white"
                        :underline-color-android "transparent"
                        :text-align "center"
                        :on-change #(on-input-change :username (u/get-text %))}]
           [text-input {:style {:margin-top "10%"
                                :align-self "center"
                                :margin-bottom "20%"
                                :border-bottom-width 0.5
                                :border-bottom-color "white"
                                :color "white"
                                :width "60%"}
                        :secure-text-entry true
                        :ref (fn [com] (reset! password-ref com))
                        :auto-capitalize "none"
                        :tint-color "white"
                        :selection-color "white"
                        :placeholder "password"
                        :placeholder-text-color "white"
                        :underline-color-android "transparent"
                        :text-align "center"
                        :on-change #(on-input-change :password (u/get-text %))}]
           (if @reg-btn-loading?
             [activity-indicator {:size "large"
                                  :color "white"}]
             [view {:style {:flex-direction "row"
                            :align-self "center"
                            :justify-content "space-between"
                            :width "60%"}}
              [touchable-highlight {:on-press #(on-press :check-user-availability)
                                    :underlay-color "transparent"
                                    :style {:align-self "center"}}
               [text {:style {:color "white"}}
                "Register"]]
              [touchable-highlight {:on-press #(on-press :login-user)
                                    :underlay-color "transparent"
                                    :style {:align-self "center"}}
               [text {:style {:color "white"}}
                "Login"]]])])]])))


(defn render-channel [{:keys [key channel on-select] :as props}]
  (fn [props]
    (let [{:keys [icon font-type name]} channel
          channel-messages (subscribe [:channel-messages name])
          last-message  (first (or @channel-messages []))
          {:keys [user text]} last-message]
      [list-item {:key key
                  :left-icon {:name icon
                              :type font-type}
                  :on-press on-select
                  :subtitle (if last-message
                              (str (:name user) ": " text)
                              "")
                  :title name}])))

(defn channels-screen [{:keys [navigation] :as props}]
  (let [{:keys [navigate]} navigation
        channels (subscribe [:kv :channels])
        channel-count (count @channels)
        on-select (fn [channel]
                    (track-screen (:name channel))
                    (dispatch-sync [:set-current-channel channel])
                    (navigate "Chat" {:title (:name channel)})
                    (dispatch [:open-channel channel]))]
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
                         [render-channel {:channel channel
                                          :on-select #(on-select channel)
                                          :key i}])
                       @channels)])])))


(defn render-message [js-message current-sender on-long-press]
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
                           :background-color c/header-bg-color}
        message-time (u/moment (:createdAt message))
        today? (u/today? message-time)
        on-url-press (fn [url]
                       (.openURL linking url))]
    (r/as-element
     [view (merge {:flex 1
                   :max-width "60%"
                   :width "30%"
                   :min-width 40
                   :min-height 40
                   :align-items "flex-start"
                   :flex-direction "column"
                   :border-radius 3
                   :margin-bottom "5%"}
                  (if own-message?
                    own-message-style
                    others-message-style))
      [view {:padding 2
             :width "100%"}
       (if-not own-message?
         [text {:style {:color "#3D7ED4"
                        :margin-bottom 5
                        :font-weight "bold"}}
          message-sender])
       [parsed-text {:style {:color (if own-message? "white" "black")}
                     :parse (clj->js [{:type "url"
                                       :onPress on-url-press
                                       :onLongPress #(on-long-press (:text message))
                                       :style {:textDecorationLine "underline"}}])
                     :on-long-press #(on-long-press (:text message))}
        (:text message)]
       [text {:style {:font-size 8
                      :align-self (if own-message? "flex-end" "flex-start")
                      :margin-top 5}}
        (.format message-time (if today? "[Today at] LT" "ddd, hh:mm a"))]]])))

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
                  (when-not (empty? (:input @state))
                    (dispatch [:send-message {:message {:text (string/trim (:input @state))
                                                        :_id (u/node-uuid)
                                                        :createdAt (u/get-time (js/Date.))
                                                        :user (select-keys (:user props) [:id :name])}
                                              :channel-name channel-name}]))
                  (reset! state init-state))
        change-height (fn [new-height]
                        (swap! state assoc :height (min 80 (max 50 new-height))))]
    (fn [props]
      [view {:style {:background-color "white"
                     :flex 1
                     :width device-width
                     :flex-direction "row"
                     :justify-content "space-between"
                     :max-height 60}}
       [input-scroll-view {:style {:width (* 0.85 device-width)}}
        [text-input {:on-change-text #(swap! state assoc :input %)
                     :default-value (:input @state)
                     :style {:width "100%"}
                     :auto-grow true
                     :multiline true
                     :underline-color-android "transparent"
                     :placeholder "Write a message"}]]
       [material-icons {:name "send"
                        :color c/header-bg-color
                        :style {:align-self "center"
                                :margin-right 10}
                        :size 30
                        :on-press #(on-send (-> props :channel :name))}]])))

(defn chat-screen [{:keys [navigation] :as props}]
  (let [user (subscribe [:kv :user])
        fetching? (subscribe [:kv :fetching?])
        actionsheet-ref (atom nil)
        selected-text (atom "")
        channel (subscribe [:kv :current-channel])
        messages (subscribe [:channel-messages (:name @channel)])
        options ["Cancel" "Copy Message" ]
        copy-btn-index 1
        on-action (fn [index]
                    (when (= index copy-btn-index))
                    (.setString clipboard @selected-text)
                    (reset! selected-text ""))
        on-long-press (fn [text]
                        (reset! selected-text text)
                        (.show @actionsheet-ref))]
    (fn [props]
      [container
       [view {:style {:width "100%"
                      :height "100%"}}
        [actionsheet {:options options
                      :ref (fn [com] (reset! actionsheet-ref com))
                      :cancel-button-index 0
                      :on-press on-action}]
        [flat-list {:data (into-array @messages)
                    :key-extractor #(identity %2)
                    :render-item #(render-message %1 (:name @user) on-long-press)
                    :inverted true
                    :style {:width "100%"
                            :margin-bottom 10
                            :flex 1}}]
        [chat-input {:user @user
                     :channel @channel}]]])))

(ns sg-chat.constants)


(def env "dev")
(defonce country "sg")
(defonce app-name "SG Chat")
(defonce version "1.0.0")


(defonce hours-before 48)
(defonce header-bg-color (if (= env "dev") "#3D7ED4" "#3D7ED4"))

(def ga-tracking-id "UA-110198163-2")

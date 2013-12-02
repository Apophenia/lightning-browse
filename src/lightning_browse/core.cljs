(ns lightning-browse.core
  (:require [lightning-browse.config :as config]
           [lightning-browse.utils :as utils]
           [cljs.core.async :refer [chan close! sliding-buffer put! >! <!]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(defn stream-track [track]
  (. js/SC (stream (str "/tracks/" track)
                   (fn [sound] (.play sound)))))

(defn get-tracks [limit]
  (. js/SC (get "/tracks"
                (clj->js {:limit limit})
                utils/console-log)))

(defn set-track [element track-id client-id]
  (let [url (str "http://api.soundcloud.com/tracks/" track-id "/stream?client_id=" client-id)]
    (set! (.-src (. js/document (getElementById element))) url)))

(. js/SC (initialize (clj->js config/settings)))

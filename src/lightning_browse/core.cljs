(ns lightning-browse.core
  (:require [lightning-browse.config :as config]
            [lightning-browse.utils :as utils]
            [cljs.core.async :as async :refer [chan close! sliding-buffer put! >! <!]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(defn stream-track [track]
  (. js/SC (stream (str "/tracks/" track)
                   (fn [sound] (.play sound)))))

(defn get-tracks [limit]
  (let [tracks (chan 1)]
    (. js/SC (get "/tracks"
                  (clj->js {:limit limit})
                (fn [response] (go
                                (doseq [x (filter #(% "streamable")(js->clj response))]
                                       (put! tracks x))))))
    tracks))

(defn event-chan [object type]
   (let [events (chan 10)]
    (.addEventListener object type (fn [e] (put! events e)))
    events))

(defn set-track [element track-id client-id]
  (let [url (str "http://api.soundcloud.com/tracks/" track-id "/stream?client_id=" client-id)]
    (set! (.-src (. js/document (getElementById element))) url)))

(. js/SC (initialize (clj->js config/settings)))

(defn -main []
(def queue (get-tracks 1000))

  (go
   (let [click-events (event-chan (.getElementById js/document "play") "click")]
     (<! click-events)
     (stream-track ((<! queue) "id")))))

(set! (.-onload js/window) -main)

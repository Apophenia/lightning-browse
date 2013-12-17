(ns lightning-browse.core
  (:require [lightning-browse.config :as config]
            [lightning-browse.utils :as utils]
            [cljs.core.async.impl.protocols :as impl]
            [cljs.core.async :as async :refer [chan close! sliding-buffer put! take! >! <!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(def current-track (atom nil))

(defn stop-current-track! []
  (when @current-track
    (.stop @current-track)))

(defn play-current-track! []
  (when @current-track
    (.play @current-track)))

(defn update-current-track! [track]
  (stop-current-track!)
  (reset! current-track track))

(defn pause-current-track! []
  (when @current-track
    (.pause @current-track)))

(defn refill [chan]
  nil)

(def buffer-size 5)

(defrecord AutofillChan [chan queued-count]
   impl/ReadPort
  (take! [this fn]
    "Takes a track from a channel while incrementing the count of tracks retrieved from the channel."
    (swap! queued-count dec)
    (when (<= queued-count buffer-size) (refill this))
    (.log js/console "taking track from channel")
    (.log js/console @queued-count)
    (impl/take! chan fn))
  impl/WritePort
  (put! [this val fn]
    "Adds a track to a channel while decrementing the count of tracks queued on the channel."
    (swap! queued-count inc)
    (.log js/console "putting track on channel")
    (.log js/console @queued-count)
    (impl/put! chan val fn)))

(defn stream-track [track]
  (let [artist ((track "user") "username")
        title (track "title")]
    (set! (.-innerHTML (.getElementById js/document "artist")) artist)
    (set! (.-innerHTML (.getElementById js/document "track")) title))
    (. js/SC (stream (str "/tracks/" (track "id"))
                     (fn [sound]
                       (update-current-track! sound)
                       (play-current-track!)))))

(defn make-chan []
  (let [tracks (AutofillChan. (chan) (atom 0))]
    (. js/SC (get "/tracks"
                  (clj->js {:limit buffer-size})
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
  (let [queue (make-chan)
        play-events (event-chan (.getElementById js/document "play") "click")
        transition {:init :playing, :paused :playing, :playing :paused}]
    (go-loop [state :init]
             (<! play-events)
             (case state
               :init (do (set! (.-innerHTML (.getElementById js/document "play")) "Pause")
                                                 (stream-track (<! queue)))
               :paused (do (set! (.-innerHTML (.getElementById js/document "play")) "Pause")
                           (play-current-track!))
               :playing (do (set! (.-innerHTML (.getElementById js/document "play")) "Play")
                            (pause-current-track!)))
             (recur (transition state)))))

(set! (.-onload js/window) -main)

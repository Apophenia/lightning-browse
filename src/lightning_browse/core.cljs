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

(defn fill-channel [chan track]
  (.stream js/SC (str "/tracks/" (track "id"))
           (fn [sound] (put! chan (assoc track :sound-manager sound)))))

(defn make-chan [tracks]
  (. js/SC (get "/tracks"
                  (clj->js {:limit buffer-size})
                  (fn [response] (go
                                (doseq [x (filter #(% "streamable")(js->clj response))]
                                  (fill-channel tracks x))))))
    tracks)

(defn event-chan [object type]
  (let [events (chan 10)]
     (.addEventListener object type (fn [e] (put! events e)))
    events))

(. js/SC (initialize (clj->js config/settings)))

(def main-queue (AutofillChan. (chan) (atom 0)))

(defn -main []
  (let [queue (make-chan main-queue)
        play-events (event-chan (.getElementById js/document "play") "click")
        next-events (event-chan (.getElementById js/document "next") "click")
        transition {:init :playing, :paused :playing, :playing :paused}]
    (go-loop [state :init
              immediate? false]
             (let [c (if immediate?
                       play-events
                       (second (alts! [play-events next-events])))]
               (if (= c play-events)
                 (do
                   (case state
                     :init (do (set! (.-innerHTML (.getElementById js/document "play")) "Pause")
                               (let [track (:sound-manager (<! queue))]
                                 (update-current-track! track)
                                 (play-current-track!)))
                     :paused (do (set! (.-innerHTML (.getElementById js/document "play")) "Pause")
                                 (play-current-track!))
                     :playing (do (set! (.-innerHTML (.getElementById js/document "play")) "Play")
                                  (pause-current-track!)))
                   (recur (transition state) false))
                 (recur :init true))))))

(set! (.-onload js/window) -main)

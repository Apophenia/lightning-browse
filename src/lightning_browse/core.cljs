(ns lightning-browse.core
  (:require [lightning-browse.config :as config]
            [lightning-browse.utils :as utils]
            [cljs.core.async :as async :refer [chan close! sliding-buffer put! >! <!]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(def current-track (atom nil))
;; three functions: stop current track, play current track, update current track
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

(defn stream-track [track]
  (. js/SC (stream (str "/tracks/" track)
                   (fn [sound]
                     (update-current-track! sound)
                     (play-current-track!)))))

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
  (let [queue (get-tracks 100)
        click-events (event-chan (.getElementById js/document "play") "click")
        transition {:init :playing, :paused :playing, :playing :paused}]
    (go-loop [state :init]
       (<! click-events)
       (case state
         :init (stream-track ((<! queue) "id"))
         :paused (play-current-track!)
         :playing (pause-current-track!))
       (recur (transition state)))))

  (set! (.-onload js/window) -main)

(ns lightning-browse.core
  (require [lightning-browse.config :as config])
  (require [lightning-browse.utils :as utils)

(type (utils/log config/settings))
(. js/SC (initialize (clj->js config/settings})))

(defn display-track [tracks]
 (let [track (first (clj->js tracks))]
 (. js/SC (oEmbed (.-uri track)
                  (. js/document (getElementById "track"))))))

(. js/SC (get "/tracks" (clj->js {limit: 1}) display-track))

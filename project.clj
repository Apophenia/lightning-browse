(defproject lightning-browse "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2030"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]]

  :plugins [[lein-cljsbuild "1.0.0-alpha2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "lightning-browse"
              :source-paths ["src"]
              :compiler {
                :output-to "lightning_browse.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})

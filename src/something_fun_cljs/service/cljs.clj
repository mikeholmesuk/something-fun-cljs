(ns something-fun-cljs.service.cljs
  (:require [bounce.figwheel :as cljs]))

(def cljs-config
  {:source-paths ["ui-src"]
   :target-path "target/cljs/"

   :web-context-path "/js"

   :figwheel {:client {:on-jsload "something-fun-cljs.ui.app/render-page!"}}

   :dev {:main 'something-fun-cljs.ui.app
         :optimizations :none
         :pretty-print? true}

   :build {:optimizations :advanced
           :pretty-print? false
           :classpath-prefix "js"}})

(defn make-cljs-compiler []
  (cljs/start-cljs-compiler! cljs-config))

(defn build-cljs! []
  (cljs/build-cljs! cljs-config))

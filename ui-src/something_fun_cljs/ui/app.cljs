(ns something-fun-cljs.ui.app
  (:require [bounce.core :as bc]
            [bounce.mux :as mux]
            [bounce.mux.bidi :as mux.bidi]
            [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

;; Very basic function to check a palindrome
(defn palindrome-checker
    [thing]
    (= (reverse thing) (seq thing)))

;; Basic button to increment a counter on the page
(defn button-with-app-state
    []
    [:div
        [:p (pr-str @(bc/ask :!app))]
            [:button {:on-click #(swap! (bc/ask :!app) update :my-counter inc)}
                      "Press me"]
        [:p (pr-str (:my-counter @(bc/ask :!app)))]])

;; Component to test whether a string is a palindrome and output the (coloured) result
(defn palindrome-component
    []
    [:div
        [:p "Palindrome checker"
            [:input.form-control {:on-change (fn [ev]
                                               (swap! (bc/ask :!app) assoc :palindrome? (palindrome-checker (.. ev -target -value))))
                                  :type :text}]]

        (let [{:keys [palindrome?]} @(bc/ask :!app)]
            [:p {:style {:color (if palindrome?
                                  :green
                                  :red)}}
                (if palindrome?
                  "is a palindrome"
                  "is not a palindrome")])])

;; Send a request to the server and render the response (inc handling the async channel)
(defn rest-thing
    []
    [:div
        [:p "enter your name"
          [:input.form-control {:on-change (fn [ev]
                                             (swap! (bc/ask :!app) assoc :my-name (.. ev -target -value)))}]
          [:button {:on-click (fn [evt]
                                (go
                                  (let [<resp (http/post "/api/echo"
                                                         {:edn-params {:name (:my-name @(bc/ask :!app))}})]
                                     (swap! (bc/ask :!app) assoc :server-msg (-> (a/<! <resp)
                                                                                 (get-in [:body :message]))))))}
            "Send to server"]]
        [:p (when (:server-msg @(bc/ask :!app))
              (str "From the server: " (:server-msg @(bc/ask :!app))))]])

;; This is the page that gets rendered
(defn page-view []

  [:div {:style {:margin "1em"}}
    ;; Add the components to the page
    [:div
        [button-with-app-state]]

    [:div
        [palindrome-component]]

    [:div
        [rest-thing]]])

(defn render-page! []
  (r/render-component [(fn []
                         [@(r/cursor (bc/ask :!app) [::root-component])])]
                      js/document.body))

(defn make-bounce-map []
  {:!app (fn []
           (bc/->component (r/atom {:my-counter 0})))

   :router (-> (fn []
                 (mux/make-router {:token-mapper (mux.bidi/token-mapper ["" {"/" ::main-page}])
                                   :listener (fn [{:keys [location page]}]
                                               (reset! (r/cursor (bc/ask :!app) [:location]) location)
                                               (reset! (r/cursor (bc/ask :!app) [::root-component]) page))

                                   :default-location {:handler ::main-page}

                                   :pages {::main-page (fn [{:keys [old-location new-location same-handler?]}]
                                                         (when-not same-handler?
                                                           ;; mount!
                                                           )

                                                         (mux/->page (fn []
                                                                       [page-view])

                                                                     (fn [{:keys [old-location new-location same-handler?]}]
                                                                       (when-not same-handler?
                                                                         ;; un-mount!
                                                                         ))))}}))
               (bc/using #{:!app}))

   :renderer (-> (fn []
                   (bc/->component (render-page!)
                                   (fn []
                                     (r/unmount-component-at-node js/document.body))))

                 (bc/using #{:!app :router}))})

(set! (.-onload js/window)
      (fn []
        (bc/set-system-map-fn! make-bounce-map)
        (bc/start!)))


(ns funnel.core
  (:import (clojure.lang IObj))
  (:require [clojure.core.async :refer [alt!! chan take! timeout]]))

(def defaults {:funnel-wait-timeout 30000
               :funnel-size 1})

(defn- millis []
  (System/currentTimeMillis))

(defn- offer [ch msg ms]
  (alt!!
    [[ch msg]] true
    (timeout ms) false))

(defn- get-opt [opts k]
  (get opts k (get defaults k)))

;; Public
;; ------

(defn wrap-funnel [handler & [opts]]
  (let [opt (partial get-opt opts)
        ch (chan (opt :funnel-size))]
    (fn [req]
      (let [wait-start (millis)
            check (offer ch req (opt :funnel-wait-timeout))
            wait-time (- (millis) wait-start)]
        (if check
          (try
            (let [handler-start (millis)
                  res (handler req)]
             (if (instance? IObj res)
               (with-meta res {:funnel-wait-time wait-time
                               :funnel-handler-time (- (millis) handler-start)})
               res))
            (finally
              (take! ch (constantly nil))))
          (with-meta {:status 429}
                     {:funnel-wait-time wait-time}))))))


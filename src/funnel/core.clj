
(ns funnel.core
  (:import (clojure.lang IObj)
           (java.util.concurrent TimeoutException TimeUnit))
  (:require [clojure.core.async :refer [alt!! chan take! timeout]]))

(def defaults {:funnel-size 1
               :funnel-wait-timeout 30000
               :funnel-handler-timeout 60000})

(defn- millis []
  (System/currentTimeMillis))

(defn- offer [ch msg ms]
  (alt!!
    [[ch msg]] true
    (timeout ms) false))

(defn- get-opt [opts k]
  (get opts k (get defaults k)))

(defn- with-timeout [ms f]
  (let [t (future (f))]
    (try
      (.get t ms TimeUnit/MILLISECONDS)
      (catch TimeoutException e
        (future-cancel t)
        (throw e)))))

(defn- error [status wait-time]
  (with-meta
    {:status status}
    {:funnel-wait-time wait-time}))

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
                  res (with-timeout
                        (opt :funnel-handler-timeout)
                        (partial handler req))]
             (if (instance? IObj res)
               (with-meta res {:funnel-wait-time wait-time
                               :funnel-handler-time (- (millis) handler-start)})
               res))
            (catch TimeoutException e
              (error 504 wait-time))
            (finally
              (take! ch (constantly nil))))
          (error 429 wait-time))))))


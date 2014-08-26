
(ns funnel.core
  (:import (clojure.lang IObj)
           (java.util.concurrent TimeoutException TimeUnit))
  (:require [clojure.core.async :refer [alt!! chan take! timeout]]))

(def defaults {:funnel-size 1
               :funnel-wait-timeout 30000
               :funnel-handler-timeout 60000})

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
      (finally
        (future-cancel t)))))

(defn- success [res wait-time handler-time]
  (if (instance? IObj res)
    (with-meta
      res
      {:funnel-wait-time wait-time
       :funnel-handler-time handler-time})
    res))

(defn- error [status wait-time]
  (with-meta
    {:status status}
    {:funnel-wait-time wait-time}))

;; Public
;; ------

(defmacro timed [& forms]
  `(let [start# (System/currentTimeMillis)]
     (let [res# (do ~@forms)
           total# (- (System/currentTimeMillis) start#)]
       [res# total#])))

(defn wrap-funnel [handler & [opts]]
  (let [opt (partial get-opt opts)
        ch (chan (opt :funnel-size))]
    (fn [req]
      (let [[check wait-time] (timed
                                (offer ch req (opt :funnel-wait-timeout)))]
        (if check
          (try
            (let [[res handler-time] (timed
                                       (with-timeout
                                         (opt :funnel-handler-timeout)
                                         (partial handler req)))]
              (success res wait-time handler-time))
            (catch TimeoutException e
              (error 504 wait-time))
            (finally
              (take! ch (constantly nil))))
          (error 429 wait-time))))))


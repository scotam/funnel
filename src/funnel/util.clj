
(ns funnel.util)

;; Public
;; ------

(defmacro timed [& forms]
  `(let [start# (System/currentTimeMillis)]
     (let [res# (do ~@forms)
           total# (- (System/currentTimeMillis) start#)]
       [res# total#])))


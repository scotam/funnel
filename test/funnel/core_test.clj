
(ns funnel.core-test
  (:require [funnel.core :refer [wrap-funnel]]
            [funnel.util :refer [timed]]
            [clojure.test :refer :all]))

(defn wrap-funnel-test [handler]
  (wrap-funnel handler {:funnel-wait-timeout 100
                        :funnel-handler-timeout 300
                        :funnel-size 1}))

(defmacro funnel [& forms]
  `((wrap-funnel-test
      (fn [req#]
        (do ~@forms)))
    {}))

(deftest requests-can-be-handled

  (testing "a request map is returned"
    (is (= {:body "foo"} (funnel {:body "foo"}))))

  (testing "multiple requests can be processed"
    (let [expected {:body "foo"}
          handler (wrap-funnel-test (fn [req] expected))]
      (is (= expected (handler {})))
      (is (= expected (handler {})))))

  (testing "requests block until complete"
    (let [handler (wrap-funnel-test (fn [req] (Thread/sleep 100)))]
      (is (<= 200 (second
                    (timed (handler {})
                           (handler {})))))))

  (testing "blocked requests timeout"
    (let [handler (wrap-funnel-test (fn [req]
                                      (or req (Thread/sleep 200))))]
      (future (handler false))
      (Thread/sleep 10)
      (is (= 429 (:status (handler {})))))))

(deftest requests-can-throw-errors

  (testing "exceptions propogated"
    (is (thrown? Exception (funnel (throw (Exception. "foo"))))))

  (testing "exceptions don't corrupt subsequent requests"
    (let [expected {:foo "asd"}
          handler (wrap-funnel-test (fn [req]
                                      (or req (throw (Exception. "foo")))))]
      (try (handler false)
           (catch Exception e nil))
      (is (= expected (handler expected))))))

(deftest requests-get-metadata-attached

  (testing "wait time attached"
    (is (<= 0 (:funnel-wait-time (meta (funnel {}))))))

  (testing "handler time attached"
    (is (<= 50 (:funnel-handler-time (meta (funnel (Thread/sleep 50) {})))))))

(deftest handlers-are-given-timeouts

  (testing "error returned when handler times out"
    (is (= {:status 504} (funnel (Thread/sleep 400))))))

;(run-tests)


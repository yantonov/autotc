(ns autotc-web.test.handler
  (:require [clojure.test :refer [deftest is testing]]
            [ring.mock.request :as ringm]
            [autotc-web.handler :as autotc]))

(deftest main-route-test
  (testing "main route response"
    (let [response (autotc/app (ringm/request :get "/"))]
      (is (= (:status response) 200))
      (is (.contains (:body response) "autotc")))))

(deftest not-found-route-test
  (testing "route not found"
    (let [response (autotc/app (ringm/request :get "/invalid"))]
      (is (= (:status response) 404)))))

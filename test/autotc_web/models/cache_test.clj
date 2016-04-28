(ns autotc-web.models.cache-test
  (:require [autotc-web.models.cache :as sut]
            [clojure.test :as t]))

(t/deftest multiple-requests-for-same-server
  (t/testing "Multiple requests for same server during short time inverval -> wait single evaluation"
    (let [evaluation-count (atom 0)
          request-agent-fn (fn []
                             (do
                               (swap! evaluation-count inc)
                               [:agent1 :agent2]))
          cache-time-in-seconds 3
          call-service-fn (fn []
                            (sut/get-memoized-info :some-server-id
                                                   request-agent-fn
                                                   sut/get-now
                                                   sut/get-initial-last-updated
                                                   sut/update-needed?
                                                   cache-time-in-seconds))]
      (sut/reset-cache)
      (dotimes [i 3]
        (call-service-fn))
      (let [result (call-service-fn)]
        (.wait-value result)
        (t/is (= 1 @evaluation-count))))))

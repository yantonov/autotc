(ns autotc-web.models.agent-service-test
  (:require [autotc-web.models.agent-service :as sut]
            [clojure.test :as t]))

(t/deftest multiple-requests-for-same-server
  (t/testing "Multiple requests for same server during short time inverval -> wait single evaluation"
    (let [evaluation-count (atom 0)
          request-agent-fn (fn [server-id]
                             (do
                               (swap! evaluation-count inc)
                               [:agent1 :agent2]))
          call-service-fn (fn []
                            (sut/get-memoized-agents :some-server-id
                                                     request-agent-fn
                                                     sut/get-now
                                                     sut/get-initial-last-updated
                                                     sut/update-needed?))]
      (sut/reset-cache)
      (dotimes [i 3]
        (call-service-fn))
      (let [result (call-service-fn)]
        (.wait-value result)
        (t/is (= 1 @evaluation-count))))))

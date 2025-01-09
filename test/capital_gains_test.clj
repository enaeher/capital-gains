(ns capital-gains-test
  (:require [capital-gains :as subject]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is are]]))

(deftest test-cases
  (are [input output] (= output (-> input subject/process-ops :tax))
    ;; Test case 1
    [{:operation "buy" :unit-cost 1000 :quantity 100}
     {:operation "sell" :unit-cost 1500 :quantity 50}
     {:operation "sell" :unit-cost 1500 :quantity 50}]
    [{:tax 0} {:tax 0} {:tax 0}]

    ;; Test case 2
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "sell" :unit-cost 2000 :quantity 5000}
     {:operation "sell" :unit-cost 500 :quantity 5000}]
    [{:tax 0} {:tax 1000000} {:tax 0}]

    ;; Test case 3
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "sell" :unit-cost 500 :quantity 5000}
     {:operation "sell" :unit-cost 2000 :quantity 3000}]
    [{:tax 0} {:tax 0} {:tax 100000}]

    ;; Test case 4
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "buy" :unit-cost 2500 :quantity 5000}
     {:operation "sell" :unit-cost 1500 :quantity 10000}]
    [{:tax 0} {:tax 0} {:tax 0}]

    ;; Test case 5
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "buy" :unit-cost 2500 :quantity 5000}
     {:operation "sell" :unit-cost 1500 :quantity 10000}
     {:operation "sell" :unit-cost 2500 :quantity 5000}]
    [{:tax 0} {:tax 0} {:tax 0} {:tax 1000000}]

    ;; Test case 6
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "sell" :unit-cost 200 :quantity 5000}
     {:operation "sell" :unit-cost 2000 :quantity 2000}
     {:operation "sell" :unit-cost 2000 :quantity 2000}
     {:operation "sell" :unit-cost 2500 :quantity 1000}]
    [{:tax 0} {:tax 0} {:tax 0} {:tax 0} {:tax 300000}]

    ;; Test case 7
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "sell" :unit-cost 200 :quantity 5000}
     {:operation "sell" :unit-cost 2000 :quantity 2000}
     {:operation "sell" :unit-cost 2000 :quantity 2000}
     {:operation "sell" :unit-cost 2500 :quantity 1000}
     {:operation "buy" :unit-cost 2000 :quantity 10000}
     {:operation "sell" :unit-cost 1500 :quantity 5000}
     {:operation "sell" :unit-cost 3000 :quantity 4350}
     {:operation "sell" :unit-cost 3000 :quantity 650}]
    [{:tax 0} {:tax 0} {:tax 0} {:tax 0} {:tax 300000}
     {:tax 0} {:tax 0} {:tax 370000} {:tax 0}]

    ;; Test case 8
    [{:operation "buy" :unit-cost 1000 :quantity 10000}
     {:operation "sell" :unit-cost 5000 :quantity 10000}
     {:operation "buy" :unit-cost 2000 :quantity 10000}
     {:operation "sell" :unit-cost 5000 :quantity 10000}]
    [{:tax 0} {:tax 8000000} {:tax 0} {:tax 6000000}]

    ;; Test case 9
    [{:operation "buy" :unit-cost 500000 :quantity 10}
     {:operation "sell" :unit-cost 400000 :quantity 5}
     {:operation "buy" :unit-cost 1500000 :quantity 5}
     {:operation "buy" :unit-cost 400000 :quantity 2}
     {:operation "buy" :unit-cost 2300000 :quantity 2}
     {:operation "sell" :unit-cost 2000000 :quantity 1}
     {:operation "sell" :unit-cost 1200000 :quantity 10}
     {:operation "sell" :unit-cost 1500000 :quantity 3}]
    [{:tax 0} {:tax 0} {:tax 0} {:tax 0}
     {:tax 0} {:tax 0} {:tax 100000} {:tax 240000}]))

(def multiline-json-input (slurp (io/resource "multiline-input.json")))
(def multiline-json-output (slurp (io/resource "multiline-output.json")))

(deftest test-integration
  (is (= multiline-json-output
         (with-out-str
           (with-in-str multiline-json-input
             (subject/run))))))

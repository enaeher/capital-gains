(ns capital-gains-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.data.json :as json]
            [capital-gains :as subject]))

(deftest test-case-1
  (is (= [{:tax 0} {:tax 0} {:tax 0}]
         (subject/process-ops
          [{:operation "buy" :unit-cost 1000 :quantity 100}
           {:operation "sell" :unit-cost 1500 :quantity 50}
           {:operation "sell" :unit-cost 1500 :quantity 50}]))))

(deftest test-case-2
  (is (= [{:tax 0} {:tax 1000000} {:tax 0}]
         (subject/process-ops
          [{:operation "buy" :unit-cost 1000 :quantity 10000}
           {:operation "sell" :unit-cost 2000 :quantity 5000}
           {:operation "sell" :unit-cost 500 :quantity 5000}]))))

(deftest test-case-3
  (is (= [{:tax 0} {:tax 0} {:tax 100000}]
         (subject/process-ops
          [{:operation "buy", :unit-cost 1000, :quantity 10000}
           {:operation "sell", :unit-cost 500, :quantity 5000}
           {:operation "sell", :unit-cost 2000, :quantity 3000}]))))

(deftest test-case-4
  (is (= [{:tax 0} {:tax 0} {:tax 0}]
         (subject/process-ops
          [{:operation "buy", :unit-cost 1000, :quantity 10000}
           {:operation "buy", :unit-cost 2500, :quantity 5000}
           {:operation "sell", :unit-cost 1500, :quantity 10000}]))))

(deftest test-case-5
  (is (= [{:tax 0} {:tax 0} {:tax 0} {:tax 1000000}]
         (subject/process-ops
          [{:operation "buy", :unit-cost 1000, :quantity 10000}
           {:operation "buy", :unit-cost 2500, :quantity 5000}
           {:operation "sell", :unit-cost 1500, :quantity 10000}
           {:operation "sell", :unit-cost 2500, :quantity 5000}]))))

(deftest test-case-6
  (is (= [{:tax 0} {:tax 0} {:tax 0} {:tax 0} {:tax 300000}]
       (subject/process-ops
        [{:operation "buy", :unit-cost 1000, :quantity 10000}
         {:operation "sell", :unit-cost 200, :quantity 5000}
         {:operation "sell", :unit-cost 2000, :quantity 2000}
         {:operation "sell", :unit-cost 2000, :quantity 2000}
         {:operation "sell", :unit-cost 2500, :quantity 1000}]))))

(deftest test-case-7
  (is (= [{:tax 0} {:tax 0} {:tax 0} {:tax 0} {:tax 300000}
          {:tax 0} {:tax 0} {:tax 370000} {:tax 0}]
       (subject/process-ops
        [{:operation "buy", :unit-cost 1000, :quantity 10000}
         {:operation "sell", :unit-cost 200, :quantity 5000}
         {:operation "sell", :unit-cost 2000, :quantity 2000}
         {:operation "sell", :unit-cost 2000, :quantity 2000}
         {:operation "sell", :unit-cost 2500, :quantity 1000}
         {:operation "buy", :unit-cost 2000, :quantity 10000}
         {:operation "sell", :unit-cost 1500, :quantity 5000}
         {:operation "sell", :unit-cost 3000, :quantity 4350}
         {:operation "sell", :unit-cost 3000, :quantity 650}]))))

(deftest test-case-8
  (is (= [{:tax 0} {:tax 8000000} {:tax 0} {:tax 6000000}]
         (subject/process-ops 
          [{:operation "buy", :unit-cost 1000, :quantity 10000}
           {:operation "sell", :unit-cost 5000, :quantity 10000}
           {:operation "buy", :unit-cost 2000, :quantity 10000}
           {:operation "sell", :unit-cost 5000, :quantity 10000}]))))

(deftest test-case-9
  (is (= [{:tax 0} {:tax 0} {:tax 0} {:tax 0}
          {:tax 0} {:tax 0} {:tax 100000} {:tax 240000}]
       (subject/process-ops
        [{:operation "buy", :unit-cost 500000, :quantity 10}
         {:operation "sell", :unit-cost 400000, :quantity 5}
         {:operation "buy", :unit-cost 1500000, :quantity 5}
         {:operation "buy", :unit-cost 400000, :quantity 2}
         {:operation "buy", :unit-cost 2300000, :quantity 2}
         {:operation "sell", :unit-cost 2000000, :quantity 1}
         {:operation "sell", :unit-cost 1200000, :quantity 10}
         {:operation "sell", :unit-cost 1500000, :quantity 3}]))))

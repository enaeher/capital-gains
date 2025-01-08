(ns capital-gains
  (:require [clojure.data.json :as json]
            [clojure.math :as math]))

(def ^:private ^:const small-transaction-limit 2000000)
(def ^:private ^:const capital-gains-tax-rate (/ 20 100))

(defn- compute-new-cost-basis
  [{:keys [weighted-avg current-qty] :as _state}
   {:keys [quantity unit-cost] :as _op}]
  (math/round
   (/
    (+ (* current-qty weighted-avg)
       (* quantity unit-cost))
    (+ current-qty quantity))))

(defn- process-purchase
  [{:keys [current-qty weighted-avg acc-loss] :as state} {:keys [quantity] :as op}]
  (-> state
      ;; no tax due at purchase time
      (update :tax conj {:tax 0})
      (update :current-qty + quantity)
      (update :weighted-avg compute-new-cost-basis current-qty op)))

(defn- process-sale
  [{:keys [weighted-avg acc-loss] :as state} {:keys [unit-cost quantity] :as _op}]

  (cond (< unit-cost weighted-avg) ;; capital loss
        (-> state
            (update :tax conj {:tax 0})
            (update :current-qty - quantity)
            (update :acc-loss + (* (- weighted-avg unit-cost) quantity)))

        (<= (* unit-cost quantity) small-transaction-limit) ;; small sale below 20,000
        (-> state
            (update :tax conj {:tax 0})
            (update :current-qty - quantity))

        :else ;; taxable capital gain
        (let [profit          (* (- unit-cost weighted-avg) quantity)
              deductible-loss (min acc-loss profit)
              taxable-gain    (- profit deductible-loss)
              tax-due         (math/round (* taxable-gain capital-gains-tax-rate))]
          (-> state
              (update :tax conj {:tax tax-due})
              (update :current-qty - quantity)
              ;; FIXME--should carried-over loss be decreased
              (update :acc-loss - deductible-loss)))))

(defn- process-op
  [state {:keys [operation] :as op}]
  (case operation
    "buy"  (process-purchase state op)
    "sell" (process-sale state op)))

(defn process-ops
  [ops]
  (:tax (reduce
         process-op
         {:tax          []
          :current-qty  0
          :weighted-avg 0
          :acc-loss     0}
         ops)))

(defn run
  []
  (loop [raw-line (read-line)]
    (when raw-line
      (-> raw-line json/read-str process-ops json/write-str)
      (recur (read-line)))))

(ns capital-gains
  (:require [clojure.data.json :as json]
            [clojure.math :as math])
  (:import [java.io BufferedReader]))

(def ^:private ^:const small-transaction-limit 2000000) ; in cents
(def ^:private ^:const capital-gains-tax-rate (/ 1 5)) ; as a ratio

(def ^:private ^:const initial-state
  {:tax           [] ; accumulates the tax due on each operation
   :loss          0  ; tracks the current total losses that can be applied to offset tax due
   :quantity-held 0  ; tracks the current quantity of stocks held
   :weighted-avg  0  ; tracks the current weighted average price paid for stocks held
   })

;; Purchase handling

(defn- new-weighted-avg
  "Given:
    - a state with an existing cost basis and quantity of holdings
    - a purchase operation
  this function returns a computed cost basis using the
  weighted-avg method."
  [{:keys [weighted-avg quantity-held] :as _state}
   {:keys [quantity unit-cost] :as _op}]
  (math/round
   (/
    (+ (* quantity-held weighted-avg)
       (* quantity unit-cost))
    (+ quantity-held quantity))))

(defn- process-purchase
  [state {:keys [quantity] :as op}]
  (-> state
      ;; no tax due at purchase time
      (update :tax conj {:tax 0})
      (update :quantity-held + quantity)
      (assoc :weighted-avg (new-weighted-avg state op))))

;; Sale handling

(defn- process-sale-capital-loss
  [{:keys [weighted-avg] :as state}
   {:keys [unit-cost quantity] :as _op}]
  (-> state
      (update :quantity-held - quantity)
      (update :tax conj {:tax 0})
      (update :loss + (* (- weighted-avg unit-cost) quantity))))

(defn- process-sale-below-limit
  [state {:keys [quantity] :as _op}]
  (-> state
      (update :quantity-held - quantity)
      (update :tax conj {:tax 0})))

(defn- process-sale-capital-gain
  [{:keys [loss weighted-avg] :as state}
   {:keys [unit-cost quantity] :as _op}]
  (let [profit          (* (- unit-cost weighted-avg) quantity)
        deductible-loss (min loss profit)
        taxable-gain    (- profit deductible-loss)
        tax-due         (math/round (* taxable-gain capital-gains-tax-rate))]
      (-> state
          (update :quantity-held - quantity)
          (update :tax conj {:tax tax-due})
          (update :loss - deductible-loss))))

(defn- process-sale
  [{:keys [weighted-avg] :as state}
   {:keys [unit-cost quantity] :as op}]
  (cond
    ;; capital loss
    (< unit-cost weighted-avg)
    (process-sale-capital-loss state op)

    ;; small sale below $20,000
    (<= (* unit-cost quantity) small-transaction-limit)
    (process-sale-below-limit state op)

    ;; taxable capital gain
    :else
    (process-sale-capital-gain state op)))

;; General operation handling

(defn- process-op
  [state {:keys [operation] :as op}]
  (case operation
    "buy"  (process-purchase state op)
    "sell" (process-sale state op)))

(defn process-ops
  [ops]
  (:tax (reduce process-op initial-state ops)))

;; CLI and JSON handling

(defn- dollars->cents
  [n]
  (* n 100))

(defn- cents->dollars
  [n]
  (/ n 100))

(defn run
  ([_opts]
   (run))
  ([]
   (doseq [line (line-seq (BufferedReader. *in*))]
     (->> (json/read-str line {:key-fn keyword})
          (map #(update % :unit-cost dollars->cents))
          (process-ops)
          (map #(update % :tax cents->dollars))
          (json/write-str)
          (println)))))

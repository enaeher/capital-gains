(ns capital-gains
  (:require [clojure.data.json :as json]
            [clojure.math :as math]
            [clojure.pprint :refer [cl-format]])
  (:import [java.math BigDecimal]))

(def ^:private ^:const small-transaction-limit 2000000) ; in cents
(def ^:private ^:const capital-gains-tax-rate (/ 1 5)) ; as a ratio

(def ^:private ^:const initial-state
  {:tax           []
   :quantity-held 0
   :weighted-avg  0
   :loss          0})

;; Purchase handling

(defn- calc-new-weighted-avg
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
      (assoc :weighted-avg (calc-new-weighted-avg state op))))

;; Sale handling

(defn- process-sale-capital-loss
  [{:keys [weighted-avg] :as state}
   {:keys [unit-cost quantity] :as _op}]
  (-> state
      (update :tax conj {:tax 0})
      (update :quantity-held - quantity)
      (update :loss + (* (- weighted-avg unit-cost) quantity))))

(defn- process-sale-below-limit
  [state {:keys [quantity] :as _op}]
  (-> state
      (update :tax conj {:tax 0})
      (update :quantity-held - quantity)))

(defn- process-sale-capital-gain
  [{:keys [loss weighted-avg] :as state}
   {:keys [unit-cost quantity] :as _op}]
  (let [profit          (* (- unit-cost weighted-avg) quantity)
        deductible-loss (min loss profit)
        taxable-gain    (- profit deductible-loss)
        tax-due         (math/round (* taxable-gain capital-gains-tax-rate))]
      (-> state
          (update :tax conj {:tax tax-due})
          (update :quantity-held - quantity)
          (update :loss - deductible-loss))))

(defn- process-sale
  [{:keys [weighted-avg] :as state} {:keys [unit-cost quantity] :as op}]

  (cond
    ;; capital loss
    (< unit-cost weighted-avg) (process-sale-capital-loss state op)

    ;; small sale below 20,000
    (<= (* unit-cost quantity) small-transaction-limit) (process-sale-below-limit state op)

    ;; taxable capital gain
    :else (process-sale-capital-gain state op)))

;; General operation handling

(defn- process-op
  [state {:keys [operation] :as op}]
  (case operation
    "buy"  (process-purchase state op)
    "sell" (process-sale state op)))

(defn- inbound-numeric-conversion
  "Unit costs are read from JSON as doubles, but because we know the
  required precision, we can convert to an integer number of cents for more
  accurate math."
  [op]
  (update op :unit-cost (comp long (partial * 100))))

(defn- outbound-numeric-conversion
  "To write the resulting tax amount values to JSON, we divide by 100 to
  convert back from cents to dollars, then cast to a BigDecimal with
  scale set to 2 to ensure the correct formatting (xx.xx) by
  data.json"
  [tax]
  (update tax :tax (comp #(.setScale % 2) bigdec #(/ % 100))))

(defn process-ops
  [ops]
  (reduce process-op initial-state ops))

(defn process-line
  [ops]
  (->> ops
       (map inbound-numeric-conversion)
       process-ops
       :tax
       (map outbound-numeric-conversion)))

(defn run
  ([_opts]
   (run))
  ([]
   (loop [raw-line (read-line)]
       (when raw-line
         (-> raw-line
             (json/read-str {:key-fn keyword})
             process-line
             (json/write-str)
             (println))
         (recur (read-line))))))

(ns fauxcel.base.button-actions
  (:require
   [clojure.string :as s]
   [reagent.ratom]
   [fauxcel.base.state :as state :refer [current-selection]]
   [fauxcel.base.utility :as base-util :refer [expand-cell-range str-empty?]]
   [fauxcel.base.format :as format :refer [remove-styles add-styles get-format-style]]))

(defn toggle-style! [style]
  (let [cells (expand-cell-range @current-selection)]
    (doseq [cell cells]
      (if (and (not (str-empty? (get-format-style cell))) (s/includes? (get-format-style cell) style))
        (remove-styles cell style)
        (add-styles cell style)))))

(defn toggle-bold! []
  (toggle-style! "cell-bold"))

(defn toggle-italic! []
  (toggle-style! "cell-italic"))

(defn toggle-underline! []
  (toggle-style! "cell-underline"))

(defn toggle-align-left! []
  (remove-styles @current-selection "cell-align-center")
  (remove-styles @current-selection "cell-align-right")
  (toggle-style! "cell-align-left"))

(defn toggle-align-center! []
  (remove-styles @current-selection "cell-align-left")
  (remove-styles @current-selection "cell-align-right")
  (toggle-style! "cell-align-center"))

(defn toggle-align-right! []
  (remove-styles @current-selection "cell-align-left")
  (remove-styles @current-selection "cell-align-center")
  (toggle-style! "cell-align-right"))


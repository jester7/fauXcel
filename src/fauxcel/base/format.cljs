(ns fauxcel.base.format
  (:require
   [clojure.string :as s]
   [fauxcel.base.state :as state :refer [cells-map]]))

(defn get-format [^string cell-ref]
  (let [format (:format (@cells-map cell-ref))]
    (if (nil? format)
      ""
      format)))

(defn get-format-style [^string cell-ref]
  (let [style (:style (:format (@cells-map cell-ref)))]
    (if (nil? style)
      nil
      style)))

(defn set-format! [^string cell-ref format]
  (swap! cells-map assoc-in [cell-ref :format] format))

(defn set-format-style! [^string cell-ref style]
  (swap! cells-map assoc-in [cell-ref :format :style] style))

(defn add-style [^string cell-ref ^string style]
  (let [current-styles (get-format-style cell-ref)
        styles (if
                (nil? current-styles) (list style)
                 (conj (s/split current-styles #" ") style))]
    (set-format-style! cell-ref (s/join " " (set styles))))) 
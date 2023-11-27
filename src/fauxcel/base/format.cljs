(ns fauxcel.base.format
  (:require [clojure.set :refer [difference]]
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

(defn add-styles [^string cell-ref ^string & new-styles]
  (let [current-styles (get-format-style cell-ref)
        styles (if
                (nil? current-styles) new-styles
                ; add each style to the set of styles
                (set (concat (s/split current-styles #" ") new-styles)))]
    (set-format-style! cell-ref (s/join " " styles))))

(defn remove-styles [^string cell-ref ^string & remove-styles]
  (let [current-styles (get-format-style cell-ref)
        styles (if
                (nil? current-styles) nil
                  ; remove each style from the set of styles
                (difference (set (s/split current-styles #" ")) remove-styles))]
    (set-format-style! cell-ref (s/join " " styles))))
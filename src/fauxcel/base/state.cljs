(ns fauxcel.base.state
  (:require
   [reagent.core :as r]))

(def current-formula (r/atom ""))
(def cells-map (r/atom {}))
(def current-selection (r/atom ""))
(def edit-mode (atom false))
(def current-cell-sheet (r/atom :cellsheet1))
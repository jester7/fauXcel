(ns fauxcel.components.spreadsheet
  (:require
   [fauxcel.base.state :as state :refer [cells-map edit-mode current-selection]]
   [fauxcel.base.constants :as constants :refer [max-cols max-rows]]
   [fauxcel.base.parser :as parser]
   [fauxcel.base.keyboard-handlers :as keyboard-handlers :refer [keyboard-navigation]]
   [fauxcel.base.utility :as base-util :refer [update-selection! col-label
                                               selection-cell-ref recursive-deref
                                               cell-ref-for-input row-in-range? col-in-range?]]))

(defn cellgrid []
  [:div.cellgrid.wrapper {:on-key-down keyboard-navigation}
   (doall (for [row (range 0 max-rows)]
            [:div.row.wrapper {:key (str "row" row)}
             [:span.row-label {:key (str "row-label" row) :class
                                   (if (row-in-range? row @current-selection) "selected" "")} row]
             (doall (for [col (range 1 max-cols)]
                      (if (= row 0)
                        (col-label col (col-in-range? col @current-selection))
                        [:input {:default-value (base-util/recursive-deref (:value (@cells-map (base-util/cell-ref row col))))
                                 :read-only true
                                 :key (str
                                       (base-util/cell-ref row col) "-"
                                       (base-util/recursive-deref (:value (@cells-map (base-util/cell-ref row col)))))
                                 :data-row row :data-col col :id (base-util/cell-ref row col)
                                 :on-change
                                 #(base-util/changed! (.-target %1))
                                 :on-click
                                 (fn [e]
                                   (when (and @edit-mode (not= @current-selection (cell-ref-for-input (.-target e))))
                                     (when (not (nil? (selection-cell-ref)))
                                       (reset! edit-mode false)
                                       (set! (-> (selection-cell-ref) .-value)
                                             (recursive-deref (:value (@cells-map @current-selection))))
                                       (set! (-> (selection-cell-ref) .-readOnly) true)))
                                   (update-selection! (.-target e)))
                                 :on-double-click
                                 (fn [e]
                                   (reset! edit-mode true)
                                   (update-selection! (.-target e) true)
                                   (set! (-> e .-target .-readOnly) false))
                                 :on-blur
                                 #(base-util/handle-cell-blur (.-target %1) parser/parse-formula)}])))]))])
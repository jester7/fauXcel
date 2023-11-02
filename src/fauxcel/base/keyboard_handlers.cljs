(ns fauxcel.base.keyboard-handlers
  (:require
   [reagent.ratom]
   [fauxcel.base.state :as state :refer [cells-map edit-mode]]
   [fauxcel.util.dom :as dom :refer [querySelector]]
   [fauxcel.base.constants :as c]
   [fauxcel.base.utility :as base-util :refer
    [update-selection! scroll-to-cell cell-ref
     selection-cell-ref recursive-deref
     row-col-for-el]]))


(defn row-col-add [rc-map row-n col-n]
  (let [new-row (+ (:row rc-map) row-n)
        new-col (+ (:col rc-map) col-n)]
    (if (or (< new-row 1)
            (> new-row (dec c/max-rows))
            (< new-col 1)
            (> new-col (dec c/max-cols)))
      rc-map ; return original if out of range
      {:row new-row :col new-col}))) ; else return updated row/col

(defn has-modifier-key? [key-press-info]
  (or (:shift? key-press-info)
      (:ctrl? key-press-info)
      (:alt? key-press-info)))

(defn has-modifier-key-except-shift? [key-press-info]
  (or (:ctrl? key-press-info)
      (:alt? key-press-info)))

(defn not-modifier-key? [key-press-info]
  (not (has-modifier-key? key-press-info)))

(defn not-modifier-key-except-shift? [key-press-info]
  (not (has-modifier-key-except-shift? key-press-info)))

(defn handle-keyboard-arrow-up!
  [^js/HTMLElement curr-cell key-press-info]
  (let [rc (row-col-for-el curr-cell)
        rc-new (row-col-add rc -1 0)]
    (when (and (not @edit-mode) (> (:row rc) 1))
      (set! (.-readOnly curr-cell) true)
      (scroll-to-cell (cell-ref rc-new) true)
      (update-selection! (querySelector (str "#" (cell-ref (:row rc-new) (:col rc-new))))))))

(defn handle-keyboard-arrow-down!
  [^js/HTMLElement curr-cell key-press-info]
  (let [rc (row-col-for-el curr-cell)
        rc-new (row-col-add rc 1 0)]
    (when (and (not @edit-mode) (< (:row rc) (dec c/max-rows)))
      (set! (.-readOnly curr-cell) true)
      (scroll-to-cell (cell-ref rc-new) true)
      (update-selection! (querySelector (str "#" (cell-ref (:row rc-new) (:col rc-new))))))))

(defn handle-keyboard-arrow-left!
  [^js/HTMLElement curr-cell key-press-info]
  (let [rc (row-col-for-el curr-cell)
        rc-new (row-col-add rc 0 -1)]
    (when (and (not @edit-mode) (> (:col rc) 1))
      (set! (.-readOnly curr-cell) true)
      (scroll-to-cell (cell-ref rc-new) true)
      (update-selection! (querySelector (str "#" (cell-ref (:row rc-new) (:col rc-new))))))))

(defn handle-keyboard-arrow-right!
  [^js/HTMLElement curr-cell key-press-info]
  (let [rc (row-col-for-el curr-cell)
        rc-new (row-col-add rc 0 1)]
    (when (and (not @edit-mode) (< (:col rc) (dec c/max-cols)))
      (set! (.-readOnly curr-cell) true)
      (scroll-to-cell (cell-ref rc-new) true)
      (update-selection! (querySelector (str "#" (cell-ref (:row rc-new) (:col rc-new))))))))

(defn handle-keyboard-tab!
  [^js/HTMLElement curr-cell key-press-info]
  (let [rc (row-col-for-el curr-cell)
        rc-new (if (:shift? key-press-info) (row-col-add rc 0 -1) (row-col-add rc 0 1))]
    (reset! edit-mode false)
    (set! (.-readOnly curr-cell) true)
    (when (< (:col rc) (dec c/max-cols))
      (scroll-to-cell (cell-ref rc-new) true)
      (update-selection! (querySelector (str "#" (cell-ref (:row rc-new) (:col rc-new))))))))

(defn handle-keyboard-enter! [^js/HTMLElement curr-cell]
  (let [rc (row-col-for-el curr-cell)]
    (if (not @edit-mode)
      (do ; edit mode is false, so enter edit mode
        (reset! edit-mode true)
        (set! (.-readOnly curr-cell) false)
        (update-selection! (querySelector (str "#" (cell-ref (:row rc) (:col rc)))) true))
      (do ; edit mode is true, so exit edit mode and move down a row if not at max already
        (reset! edit-mode false)
        (set! (.-readOnly curr-cell) true)
        (update-selection! (querySelector (str "#" (cell-ref (row-col-add rc 1 0)))))))))

(defn handle-keyboard-escape! [^js/HTMLElement curr-cell]
  (when @edit-mode
    (let [rc (row-col-for-el curr-cell)
          cell-r (cell-ref rc)
          cell-map ; ignore changes and get original value
          {:formula (:formula (@cells-map cell-r))
           :format (:format (@cells-map cell-r))
           :value (:value (@cells-map cell-r))}]
      (reset! edit-mode false)
      (set! (.-readOnly curr-cell) true)
      (swap! cells-map
             assoc cell-r cell-map) ; reset cell to original value
      (set! (-> curr-cell .-value) (recursive-deref (:value cell-map))))))

(defn handle-keyboard-any-key!
  [^js/HTMLElement curr-cell key-press-info]
  (when (and (not @edit-mode) (not-modifier-key-except-shift? key-press-info))
    (set! (-> curr-cell .-readOnly) false)
    (reset! edit-mode true)
    (update-selection! curr-cell true)
    (set! (-> curr-cell .-value) "")))


(defn handle-keyboard-events [^js/HTMLElement curr-cell key-press-info]
  (case (:key key-press-info)
    c/key-ArrowUp
    (handle-keyboard-arrow-up! curr-cell key-press-info)

    c/key-ArrowDown
    (handle-keyboard-arrow-down! curr-cell key-press-info)

    c/key-ArrowLeft
    (handle-keyboard-arrow-left! curr-cell key-press-info)

    c/key-ArrowRight
    (handle-keyboard-arrow-right! curr-cell key-press-info)

    c/key-Tab
    (handle-keyboard-tab! curr-cell key-press-info)

    c/key-Enter
    (handle-keyboard-enter! curr-cell)

    c/key-Escape
    (handle-keyboard-escape! curr-cell)

    ; default case, any other key pressed enter edit mode if not already
    (handle-keyboard-any-key! curr-cell key-press-info)))

(defn keyboard-navigation [e]
  (let [curr-cell (selection-cell-ref)
        key-press-info {:key (.-key e)
                        :shift? (.-shiftKey e)
                        :ctrl? (.-ctrlKey e)
                        :alt? (.-altKey e)}]
    (handle-keyboard-events curr-cell key-press-info)))
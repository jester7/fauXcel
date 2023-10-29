(ns fauxcel.base.utility
  (:require
   [clojure.string :as s]
   [reagent.ratom]
   [fauxcel.base.state :as state :refer [cells-map current-selection current-formula edit-mode]]
   [fauxcel.util.dom :as dom :refer [querySelector]]
   [fauxcel.base.constants :as c]
   [fauxcel.util.debug :as debug :refer [debug-log]]))

(def ^:const cells-parent-selector "#app")

(defn num-to-char [num]
  (char (+ num 64)))

(defn is-formula? ^boolean [str]
  (= (get str 0) "="))

(defn cell-ref
  ([cell] (cell-ref (:row cell) (:col cell))) ; if single arg, assumes map with :row and :col
  ([row col]
   (str (num-to-char col) row)))

(defn el-by-cell-ref [cell-ref]
  (querySelector (str "#" cell-ref)))

(defn changed! [^js/HTMLElement cell-el]
  (set! (.-changed (.-dataset cell-el)) true))

(defn not-changed! [^js/HTMLElement cell-el]
  (set! (.-changed (.-dataset cell-el)) false))

(defn changed? [^js/HTMLElement cell-el]
  (.-changed (.-dataset cell-el)))

(defn has-formula? [cell-ref]
  (let [data (@cells-map cell-ref)
        val (-> (el-by-cell-ref cell-ref) .-value)]
    (or (not (nil? (:formula data)))
        (is-formula? val))))

(defn cell-out-of-range? [child-bounding-rect parent-bounding-rect]
  {:horizontal (or (> (.-left child-bounding-rect) (.-left parent-bounding-rect) (.-width parent-bounding-rect))
              (> (+ (.-left child-bounding-rect) (.-width child-bounding-rect))
                (+ (.-left parent-bounding-rect) (.-width parent-bounding-rect))))
  :vertical (or (> (.-top child-bounding-rect) (.-top parent-bounding-rect) (.-height parent-bounding-rect))
            (> (+ (.-top child-bounding-rect) (.-height child-bounding-rect))
              (+ (.-top parent-bounding-rect) (.-height parent-bounding-rect))))})

(defn scroll-to-cell
  ([cell-ref] (scroll-to-cell cell-ref false true)) ; default just scroll, no range check, smooth yes
  ([cell-ref check-if-out-of-range?] (scroll-to-cell cell-ref check-if-out-of-range? true))
  ([cell-ref check-if-out-of-range? smooth-scroll?]
   (let [parent-el (querySelector cells-parent-selector)
         child-el (el-by-cell-ref cell-ref)
         child-bounding-rect (-> child-el .getBoundingClientRect)
         parent-bounding-rect (-> parent-el .getBoundingClientRect)
         smoothness (if smooth-scroll? "smooth" "auto")
         scroll-to-info {:left (- (.-left child-bounding-rect) (.-left parent-bounding-rect))
                         :top (- (.-top child-bounding-rect) (.-top parent-bounding-rect))
                         :behavior smoothness}
         out-of-range? (cell-out-of-range? child-bounding-rect parent-bounding-rect)]
     (when (or check-if-out-of-range? (:horizontal out-of-range?) (:vertical out-of-range?))
       (.scrollTo parent-el (clj->js scroll-to-info)))))) ; (.scrollIntoView child-el (clj->js {:behavior (if smooth-scroll? "smooth" "auto")}))


(defn selection-cell-ref []
  (querySelector (str cells-parent-selector " input.selected")))

(defn row-col-for-el [^js/HTMLElement el]
  {:row (js/parseInt (-> el .-dataset .-row))
   :col (js/parseInt (-> el .-dataset .-col))})

(defn row-col-for-cell-ref [cell-ref]
  (let [matches (re-matches c/cell-ref-re cell-ref)]
    {:row (js/parseInt (matches 2)) :col (matches 1)}))

(defn col-label [col-num]
  [:span.col-label {:key (str "col-label-" (num-to-char col-num))} (num-to-char col-num)])

(defn cell-ref-for-input [^js/HTMLElement input-el]
  (cell-ref (js/parseInt (-> input-el .-dataset .-row)) (js/parseInt (-> input-el .-dataset .-col))))

(defn cell-data-for
  ([cell-ref] (@cells-map cell-ref))
  ([row col] (@cells-map (cell-ref row col))))

(defn cell-value-for
  ([cell-ref] (:value (@cells-map cell-ref)))
  ([row col] (:value (@cells-map (cell-ref row col)))))

(defn derefable? ^boolean [val]
  (or (instance? cljs.core/Atom val)
      (instance? reagent.ratom/RAtom val)
      (instance? reagent.ratom/RCursor val)
      (instance? reagent.ratom/Reaction val)))

(defn recursive-deref [atom]
  (if (derefable? atom)
    (recursive-deref @atom) ; deref until no longer derefable
    atom))

(defn deref-or-val [val]
  (if (derefable? val) @val val))

(defn update-selection!
  ([el] (update-selection! el false))
  ([el get-formula?]
   (when (not= @current-selection "")
     (dom/remove-class (querySelector (str "#" @current-selection)) "selected"))
   (dom/add-class-name el "selected")
   (reset! current-selection (cell-ref-for-input el))
   (.focus el)
   (let [rc (row-col-for-el el)
         data (cell-data-for (:row rc) (:col rc))
         formula (:formula data) ;(or (:formula data) (:value data))
         value (:value data)]
     (if formula
       (reset! current-formula formula)
       (reset! current-formula value))
     (when get-formula?
       (set! (-> el .-value) (if (nil? formula) value formula))))))

(defn get-cell-row [^js/HTMLElement cell-el]
  (js/parseInt (-> cell-el .-dataset .-row)))

(defn get-cell-col [^js/HTMLElement cell-el]
  (js/parseInt (-> cell-el .-dataset .-col)))

(defn handle-cell-blur
  ;([^js/HTMLElement cell-el] (handle-cell-blur cell-el parser/parse-formula))
  [^js/HTMLElement cell-el parser]
  (when (changed? cell-el)
    (reset! edit-mode false)
    (set! (-> cell-el .-readOnly) true) ; set back to readonly
    (let [element-val (-> cell-el .-value)
          ;; if empty, set to nil because math functions count nil as 0 but fail on empty string ""
          val (if (= element-val "") nil element-val)
          row (get-cell-row cell-el)
          col (get-cell-col cell-el)
          cell-r (cell-ref row col)
          c-map {:formula (if (is-formula? val) val (:formula (@cells-map cell-r)))
                 :format ""
                 :value (if (is-formula? val) (parser val) val)}] ; invoke parser if formula, else just set value
      (set! (-> cell-el .-value) (deref-or-val (:value c-map)))
      (swap! cells-map
             assoc (cell-ref row col) c-map))
    (not-changed! cell-el)))

(defn cell-ref? ^boolean [val] ; fix regex and move to constants
  (cond
    (= "" val) false
    (number? val) false
    (coll? val) false
    :else
    (not (nil? (re-seq #"^[A-Z]{1,2}[0-9]{1,4}$" val)))))

(defn cell-range? ^boolean [token]
  (if (string? token)
    (not (nil? (re-seq c/cell-range-check-re token)))
    false))

(defn expand-cell-range [^string range-str]
  (debug-log "expand-cell-range was passed range-str: " range-str)
  (let [range-str-upper (s/upper-case range-str)]
    (cond
      (cell-range? range-str)
      (let [matches (re-matches c/cell-range-start-end-re range-str-upper)
            start-cell (matches 1)
            end-cell (matches 2)
            start (row-col-for-cell-ref start-cell)
            end (row-col-for-cell-ref end-cell)]
        (flatten (for [col (range (.charCodeAt (:col start)) (inc (.charCodeAt (:col end))))]
                   (for [row (range (:row start) (inc (:row end)))]
                     (str (char col) row)))))
      (cell-ref? range-str)
      (list range-str) ; return single cell
      :else
      nil)))

(defn empty-cell? [cell-ref]
  (let [data (cell-data-for cell-ref)]
    (debug-log "empty-cell? cell-ref & data: " cell-ref data)
    (or
     (nil? data)
     (nil? (:value data))
     (= (:value data) ""))))

(defn not-empty-cell? [cell-ref]
  (not (empty-cell? cell-ref)))

;; Returns true if all cells in the range are empty
(defn empty-cell-range? [^string cell-refs]
  (not-any? not-empty-cell? (expand-cell-range cell-refs)))

(defn not-empty-cell-range? [^string cell-refs]
  (not (empty-cell-range? cell-refs)))

(ns fauxcel.base.utility
  (:require
   [clojure.string :as s]
   [reagent.ratom]
   [fauxcel.base.state :as state :refer [cells-map current-selection
                                         current-formula edit-mode
                                         sel-col-offset sel-row-offset current-rc]]
   [fauxcel.util.dom :as dom :refer [querySelector]]
   [fauxcel.base.constants :as c]
   [fauxcel.util.debug :as debug :refer [debug-log-detailed]]))

(defn str-empty? [^string str]
  (or (nil? str) (= "" str)))

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
   (let [parent-el (querySelector c/cells-parent-id)
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
  (querySelector (str c/cells-parent-id " input.selected")))

(defn row-col-for-el [^js/HTMLElement el]
  {:row (js/parseInt (-> el .-dataset .-row))
   :col (js/parseInt (-> el .-dataset .-col))})

(defn row-for-el [^js/HTMLElement el]
  (:row (row-col-for-el el)))

(defn col-for-el [^js/HTMLElement el]
  (:col (row-col-for-el el)))

(defn row-col-for-cell-ref [cell-ref]
  (let [matches (re-matches c/cell-ref-re cell-ref)]
    {:row (js/parseInt (matches 2)) :col (matches 1)}))

(defn col-label [^number col-num ^boolean selected?]
  [:span.col-label {:key (str "col-label-" (num-to-char col-num))
                    :class (if selected? "selected" "")}
   (num-to-char col-num)])

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
   ;(dom/add-class-name el "selected") ; not needed, handled by setting current selection
   (reset! current-selection (cell-ref-for-input el))
   (reset! sel-row-offset 0)
   (reset! sel-col-offset 0)
   (.focus el)
   (let [rc (row-col-for-el el)
         data (cell-data-for (:row rc) (:col rc))
         formula (:formula data) ;(or (:formula data) (:value data))
         value (:value data)]
     (reset! current-rc rc)
     (if formula
       (reset! current-formula formula)
       (reset! current-formula value))
     (when get-formula?
       (set! (-> el .-value) (if (nil? formula) value formula))))))

(defn update-multi-selection!
  [^number start-row ^number start-col ^number end-row ^number end-col]
  (debug-log-detailed "update-multi-selection! start-row: " start-row " start-col: " start-col " end-row: " end-row " end-col: " end-col)
  (let [start (cell-ref start-row start-col)
        end (cell-ref end-row end-col)
        selection-range (if (not= start end) (str start ":" end) start)]
    (reset! current-selection selection-range)))

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
                 :format (:format (@cells-map cell-r))
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
  ;(debug-log "expand-cell-range was passed range-str: " range-str)
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
    (or
     (nil? data)
     (nil? (:value data))
     (= (:value data) ""))))

(def not-empty-cell?
  (complement empty-cell?))

;; Returns true if all cells in the range are empty
(defn empty-cell-range? [^string cell-refs]
  (not-any? not-empty-cell? (expand-cell-range cell-refs)))

(defn not-empty-cell-range? [^string cell-refs]
  (not (empty-cell-range? cell-refs)))

;; Flips cell range to be in order from top left to bottom right
(defn flip-range-if-unordered [^string range-str]
  (let [matches (re-matches c/cell-range-start-end-re range-str)
        start-cell (matches 1)
        end-cell (matches 2)
        start (row-col-for-cell-ref start-cell)
        end (row-col-for-cell-ref end-cell)]
    (if (and (<= (:row start) (:row end))
             (<= (.charCodeAt (:col start)) (.charCodeAt (:col end))))
      range-str
      (str end-cell ":" start-cell))))

;; Returns true if cell is contained in the range
(defn cell-in-range? [^string cell-ref ^string range-str]
  (if (cell-range? range-str)
    (let [range-str (flip-range-if-unordered range-str)
          matches (re-matches c/cell-range-start-end-re range-str)
          start-cell (matches 1)
          end-cell (matches 2)
          start (row-col-for-cell-ref start-cell)
          end (row-col-for-cell-ref end-cell)
          cell (row-col-for-cell-ref cell-ref)]
      (and (or (<= (:row start) (:row cell) (:row end))
                (>= (:row start) (:row cell) (:row end)))
           (or
            (<= (.charCodeAt (:col start)) (.charCodeAt (:col cell)) (.charCodeAt (:col end)))
            (>= (.charCodeAt (:col start)) (.charCodeAt (:col cell)) (.charCodeAt (:col end))))))
  (or (= cell-ref range-str) false))) ; or coerces to false if nil

;; Returns true if row number is contained in range string
(defn row-in-range? [^number row ^string range-str]
  (if (str-empty? range-str)
    false
    (let [matches (if (not (cell-range? range-str))
                    (re-matches c/cell-range-start-end-re (str range-str ":" range-str))
                    (re-matches c/cell-range-start-end-re range-str))
          start-cell (matches 1)
          end-cell (matches 2)
          start (row-col-for-cell-ref start-cell)
          end (row-col-for-cell-ref end-cell)]
      (or
       (<= (:row start) row (:row end)) ; for normal cell range: low to high
       (>= (:row start) row (:row end)))))) ; handles case where cell range goes from high to low

;; Returns true if col number is contained in range string
(defn col-in-range? [^number col ^string range-str]
  (if (str-empty? range-str)
    false
    (let [col-char (if (char? col) col (num-to-char col))
          matches (if (not (cell-range? range-str))
                    (re-matches c/cell-range-start-end-re (str range-str ":" range-str))
                    (re-matches c/cell-range-start-end-re range-str))
          start-cell (matches 1)
          end-cell (matches 2)
          start (row-col-for-cell-ref start-cell)
          end (row-col-for-cell-ref end-cell)]
      (or
       (<= (:col start) col-char (:col end)) ; for normal cell range: low to high
       (>= (:col start) col-char (:col end)))))) ; handles case where cell range goes from high to low

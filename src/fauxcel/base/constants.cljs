(ns fauxcel.base.constants)

(def ^:const max-cols 27)
(def ^:const max-rows 101)
(def ^:const cell-range-re #"([a-zA-Z]{1,2}[0-9]{0,4})[\:]([a-zA-Z]{1,2}[0-9]{0,4})")
(def ^:const cell-range-check-re #"^[a-zA-Z]{1,2}[0-9]{0,4}[\:][a-zA-Z]{1,2}[0-9]{0,4}$")
(def ^:const cell-range-start-end-re #"^([a-zA-Z]{1,2}[0-9]{0,4})[\:]([a-zA-Z]{1,2}[0-9]{0,4})$")

(def ^:const cell-ref-re #"^([a-zA-Z]{1,2})([0-9]{0,4})")

(def ^:const is-numeric-re #"^[+-]?(?:\d*[\.\,])?\d*$")

(def ^:const empty-cell {:formula "" :format "" :value ""})

(def ^:const key-Enter "Enter")
(def ^:const key-ArrowUp "ArrowUp")
(def ^:const key-ArrowDown "ArrowDown")
(def ^:const key-ArrowLeft "ArrowLeft")
(def ^:const key-ArrowRight "ArrowRight")
(def ^:const key-Tab "Tab")
(def ^:const key-Escape "Escape")

(def ^:const default-cell-sheet-names ["cellsheet1" "cellsheet2" "cellsheet3"])

;; turns the default-cell-sheet-names into keywords
(def ^:const default-cell-sheets (mapv keyword default-cell-sheet-names))

(def ^:const default-cell-sheet (first default-cell-sheets))
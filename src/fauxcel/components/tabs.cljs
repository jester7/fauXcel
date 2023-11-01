(ns fauxcel.components.tabs
  (:require
   [fauxcel.base.state :as state :refer [current-cell-sheet current-selection]]
   [fauxcel.base.parser :as parser]
   [fauxcel.base.utility :as util :refer [empty-cell-range? not-empty-cell-range?]]
   [fauxcel.base.constants :as c :refer [default-cell-sheets]]))

;; Tab name to keyword
(defn get-tab-keyword
  [tab-name]
  (keyword tab-name))

(defn sheet-change-handler
  [^string switch-to-tab]
  (reset! current-cell-sheet (get-tab-keyword switch-to-tab)))

;; Tab component
(defn tab [label]
  [:div.tab {:key (str "tab-" label)
             :class (if (= label @current-cell-sheet) "active" "")
             :on-click #(sheet-change-handler label)}
   [:span
    label]])

(defn tab-selector
  [tab-names tab-position]
  [:div.tab-selector {:class tab-position}
   (doall (map tab tab-names))])

(defn cell-sheet-selector
  ([] (cell-sheet-selector default-cell-sheets))
  ([cell-sheets]
   (tab-selector cell-sheets "bottom")))

(defn parse-formula [formula]
  (if (or (nil? @current-selection) (empty-cell-range? @current-selection))
    ""
    @(parser/parse-formula formula)))

(defn auto-formula-panel []
  (let [sum-formula (parse-formula (str "=SUM(" @current-selection ")"))
        count-formula (parse-formula (str "=COUNT(" @current-selection ")"))
        average-formula (parse-formula (str "=AVERAGE(" @current-selection ")"))]
    [:div.auto-formula-panel
     (if (not-empty-cell-range? @current-selection)
       [:<> ; react fragment
        ; show formula results if selection is not empty
        [:span.auto-formula.auto-sum "Sum"
         [:span.result sum-formula]]
        [:span.auto-formula.auto-count "Count"
         [:span.result count-formula]]
        [:span.auto-formula.auto-average "Average"
         [:span.result average-formula]]]
       ; else nothing selected
       [:span.empty-selection "Nothing in current selection."])]))

(defn cell-sheet-footer []
  [:div.cell-sheet-footer
   [:div.button-group
    [:button.simple.add-sheet {:title "Add New Cellsheet"} "+"] ;➕
    [:button.simple.sheet-menu {:title "All Cellsheets"} "≡"]]
   (cell-sheet-selector)
   (auto-formula-panel)])

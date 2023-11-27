(ns fauxcel.components.toolbar.toolbar
  (:require
   [fauxcel.base.button-actions :refer [toggle-bold! toggle-italic! toggle-underline!
                                         toggle-align-left! toggle-align-right!]]
   [fauxcel.components.toolbar.formula-input :refer [formula-input]]
   [fauxcel.components.toolbar.button :refer [button]]))

(defn toolbar []
  [:ul.toolbar.dark-bg
   [:li.toolbar-item
    [:span.small.logo]]
   [button "|" nil "separator" false nil]
  ;;  [:span.filename "Untitled Cellbook"]
  ;;  [button nil "File" "menu" false]
  ;;  [button nil "Edit" "menu" false]
  ;;  [button nil "Format" "menu" false]
  ;;  [button nil "Help" "menu" false]
  ;;  [button "|" nil "separator" false]
   [button "📂" "Open" "open disabled" true nil]
   [button "💾" "Save" "save disabled" true nil]
   [button "📋" "Copy" "copy disabled" true nil]
   [button "📄" "Paste" "paste disabled" true nil]
   [button "|" nil "separator" false nil]
   [formula-input]
   [button "|" nil "separator" false nil]
   [button "B" "Bold" "bold" true {:on-click toggle-bold!}]
   [button "I" "Italic" "italic" true {:on-click toggle-italic!}]
   [button "U" "Underline" "underline" true {:on-click toggle-underline!}]
   [button "|" nil "separator" false nil]
   [button "⬅️" "Align Left" "align" true {:on-click toggle-align-left!}]
   [button "➡️" "Align Right" "align" true {:on-click toggle-align-right!}]

   [button " " [:a {:href "https://github.com/jester7/fauxcel"} "GitHub"] "github" false nil]
  ;;  [:li.toolbar-item
  ;;   [:a {:href "#"} "About"]]
  ;;  [:li.toolbar-item
  ;;   [:a {:href "#"} "Contact"]]
   ])
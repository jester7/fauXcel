(ns fauxcel.components.toolbar.button)

 (defn button
   ([icon caption]
    [:li.toolbar-item.button
     (when icon
       [:span.icon icon])
     (when caption
       [:span.caption caption])])
 ([icon caption handlers]
  [:li.toolbar-item.button handlers
   (when icon
     [:span.icon icon])
   (when caption
     [:span.caption caption])])
 ([icon caption classname handlers]
  [:li.toolbar-item.button (conj {:class classname} handlers)
   (when icon
     [:span.icon icon])
   (when caption
     [:span.caption caption])])
 ([icon caption classname use-caption-as-tooltip? handlers]
  [:li.toolbar-item.button (conj {:class classname
                                  :title (when use-caption-as-tooltip? caption)}
                                 handlers)
   (when icon
     [:span.icon icon])
   (when (and caption (not use-caption-as-tooltip?))
     [:span.caption caption])]))


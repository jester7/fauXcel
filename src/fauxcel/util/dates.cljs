(ns fauxcel.util.dates)

(def ^:const default-separator "/")

; regex to check for date format of mm/dd/yyyy or dd/mm/yyyy or yyyy/mm/dd
(def ^:const is-date-re #"^([0-9]{1,4})[\/-]([0-9]{1,2})[\/-]([0-9]{4})$")

(defn date? [some-date]
  (cond
    (number? some-date) false
    (not (string? some-date)) false
    :else
    (re-seq is-date-re some-date)))

(defn today
  "Returns a string of the current day or current day plus optional number of days
   with optional separator. If called with no args, returns today's date with default
   separator."
  (^string [] (today 0 default-separator)) ; if called with no args, returns today's date with default separator
  (^string [plus-days] (today plus-days default-separator))
  (^string [plus-days separator]
   (let [date (js/Date.)]
     (println "date: " date (str (+ (.getMonth date) 1) separator ; add 1 because .getMonth starts months at zero
                            (+ (.getDate date) plus-days) separator
                            (.getFullYear date)))
     (str (+ (.getMonth date) 1) separator ; add 1 because .getMonth starts months at zero
          (+ (.getDate date) plus-days) separator
          (.getFullYear date)))))

(defn date-compare
  "Compares two dates using the supplied predicate funtion."
  [predicate date1 date2]
  (let [d1 (js/Date. date1)
        d2 (js/Date. date2)]
    (predicate (.getTime d1) (.getTime d2))))

(defn date-valid?
  "Returns true if the date is valid, false if not."
  ^boolean [some-date]
  (let [date (js/Date. some-date)] ; create a js Date instance
    (= (.getTime date) (.getTime date))))
; .getTime returns NaN if not valid, NaN == NaN is false, true if actual date

(defn date< [date1 date2]
  (date-compare < date1 date2))

(defn date<= [date1 date2]
  (date-compare <= date1 date2))

(defn date> [date1 date2]
  (date-compare > date1 date2))

(defn date>= [date1 date2]
  (date-compare >= date1 date2))

(defn date= [date1 date2]
  (date-compare = date1 date2))

(ns fauxcel.base.parser
  (:require
   [fauxcel.util.math :as m]
   [reagent.core :as r]
   [clojure.string :as s]
   [fauxcel.base.utility :as util :refer
    [cell-value-for cell-ref? expand-cell-range]]
   [fauxcel.base.constants :as c]
   [fauxcel.util.dates :as dates]
   [fauxcel.util.debug :as debug :refer [debug-log debug-log-detailed do-with-timer]]))

;;; This file contains the core parser functions for evaluating formulas
;;; including algebraic expressions, functions and cell references.
;;; It is based on the Shunting Yard algorithm by Edsger Dijkstra
;;; (https://en.wikipedia.org/wiki/Shunting_yard_algorithm)
;;; adapted to include support for variable argument functions (multi-arity).
;;; Tokenization is done with a regular expression. Unary minus is handled
;;; by swapping it with multiplication by -1 and surrounding with parentheses.

(def ^:const left-p "(")
(def ^:const right-p ")")
(def ^:const exp "^")
(def ^:const minus "-")
(def ^:const comma ",")
(def ^:const multi-arity 999) ; TODO check if it can be set to -1 instead

; Map of arithmetic operators to functions
(def ^:const operators {"^" {:fn Math/pow :precedence 3 :arity 2 :nil-equals-zero? true}
                        "*" {:fn * :precedence 2 :arity 2 :nil-equals-zero? true}
                        "/" {:fn / :precedence 2 :arity 2 :nil-equals-zero? true}
                        "+" {:fn + :precedence 1 :arity 2 :nil-equals-zero? true} ; maps to + function with arity 2
                        "-" {:fn - :precedence 1 :arity 2 :nil-equals-zero? true}})

; Map of string tokens to functions
(def ^:const functions {"SUM" {:fn m/sum :precedence 1 :arity multi-arity :nil-equals-zero? true}
                        "AVG" {:fn m/average :precedence 1 :arity multi-arity :nil-equals-zero? true}
                        "ROUND" {:fn m/round :precedence 1 :arity 2 :nil-equals-zero? true}
                        "COUNTA" {:fn m/count-all :precedence 1 :arity multi-arity :nil-equals-zero? false}
                        "COUNT" {:fn m/count-numeric :precedence 1 :arity multi-arity :nil-equals-zero? false}
                        "ABS" {:fn abs :precedence 1 :arity 1 :nil-equals-zero? true}
                        "STDEV" {:fn m/standard-deviation :precedence 1 :arity multi-arity :nil-equals-zero? true}
                        "MEDIAN" {:fn m/median :precedence 1 :arity multi-arity :nil-equals-zero? true}
                        "RAND" {:fn m/random-number :precedence 1 :arity 2 :nil-equals-zero? true}
                        "TODAY" {:fn dates/today :precedence 1 :arity 1 :nil-equals-zero? true}
                        "CONCAT" {:fn str :precedence 1 :arity multi-arity :nil-equals-zero? false}})

;; Create the dynamic regex pattern using re-pattern
(def dynamic-tokenize-re
  (re-pattern (str "\\,|" (str (s/join "|" (keys functions)))
                   "|[[a-zA-Z]{1,2}[0-9]{0,4}[\\:][a-zA-Z]{1,2}[0-9]{0,4}]*|[[0-9]?\\.?[0-9]+]*|[\\/*\\-+^\\(\\)]"
                   "|[[a-zA-Z]{1,2}[0-9]{1,4}]*"
                   "|(?<=\")[^,]*?(?=\")")))

(def tokenize-re dynamic-tokenize-re)

(defn get-function [^string token-str]
  (if (nil? token-str)
    nil
    (functions (s/upper-case token-str))))

(defn function? ^boolean [token-str]
  (cond
    (nil? token-str) false
    (number? token-str) false
    :else (not (nil? (get-function token-str)))))

(defn operator? ^boolean [^string token-str]
  (not (nil? (operators token-str))))

(defn operand? ^boolean [^string token-str]
  (and (not= left-p token-str) (not= comma token-str) (not (function? token-str)) (not= right-p token-str) (nil? (operators token-str))))

(defn get-arity [^string token-str]
  (cond
    (function? token-str) (:arity (get-function token-str))
    (operator? token-str) 2
    :else 0))

(defn strip-whitespace ^string [^string input-str] ; discards whitespace, used before tokenizing
  (s/replace input-str #"\s(?=(?:\"[^\"]*\"|[^\"])*$)" ""))

;;; Turns an algebraic expression string into a sequence of strings with individual tokens 
(defn tokenize-as-str [^string expression-str]
  (let [cell-ref-re c/cell-range-re
        expanded-refs (s/replace expression-str
                                 cell-ref-re
                                 #(str (s/join "," (expand-cell-range (%1 0)))))]
    ;(println "expression-str: " expression-str)
    ;(println "expanded-refs: " expanded-refs)
    ;(println ">>> tokenize-re: " (re-seq tokenize-re (strip-whitespace expanded-refs)))
    (re-seq tokenize-re (s/upper-case (strip-whitespace expanded-refs)))))

;;; Scans tokens for minus signs and determines if the minus sign should
;;; be treated as a subtraction operator or a unary negation operator.
;;; If the latter it replaces the token with multiplication by -1 and surrounds
;;; with parentheses. This effectively makes unary minus the highest priority
;;; operator (same as Excel, Numbers and Google Sheets).
(defn swap-unary-minus [infix-tokens]
  (debug-log-detailed "(swap-unary-minus) infix-tokens arg: " infix-tokens)
  (loop [original-tokens (into [] infix-tokens)
         prev-token nil ; nil at start of loop
         prev-token-unary? false ; false at start of loop
         new-tokens []] ; as tokens are processed they are added to vector
    (if original-tokens
      (let [token (first original-tokens)
            replace?
            (if (= minus token)
              (cond
                (nil? prev-token) true ; if minus is at beginning, is unary minus
                (operator? prev-token) true ; if previous token is an operator, assume unary -
                (= left-p prev-token) true ; if prev token is left (, must be unary -
                :else false) ; if current token is minus but none of above conditions match, it can't be a unary operator
              false)] ; if it isn't a minus sign, don't replace
        (recur (next original-tokens) token replace?
               (if replace?
                 (conj new-tokens "(" "-1" "*") ; if above conditions set the replace flag, add -1 multiplication
                 (if prev-token-unary? ; else check unary flag for previous token
                   (conj new-tokens token ")") ; if the previous token was unary -, conj token and closing parenthesis
                   (conj new-tokens token))))) ; else just add the token and nothing extra
      new-tokens)))

;;; Looks up the precedence value from the operators map. Returns 0 if not found.
(defn precedence [v]
  (or (:precedence (operators v)) 0))

(defn eval-cell-ref
  ([cell-ref] (eval-cell-ref cell-ref true))
  ([cell-ref when-not-cell-ref-return-nil?]
   (if (cell-ref? cell-ref)
     (r/track! #(let [cell-data (cell-value-for cell-ref)]
                  (if (m/numeric? cell-data) (m/eval-number cell-data) cell-data)))
     (if when-not-cell-ref-return-nil? nil cell-ref))))

;;; Takes a token in string format and returns the corresponding function (if an operator)
;;; or the text in the cell (nil if empty) or the numeric value.
(defn eval-token [token]
  (cond
    ; If cell ref, evaluate and return 
    (cell-ref? token) ; moved this cond up for formulas with many cell refs, slight performance boost maybe?
    (util/recursive-deref (eval-cell-ref token))

    ; If it's an operator, return the function
    (:fn (operators token)) ; (operators token) returns nil if not found
    (:fn (operators token))

    (function? token)
    (:fn (get-function token))

    ;(cell-range? token) ; TODO check if safe to delete; moved cell range expansion to tokenizer
    ;(expand-cell-range token) 

    (and (string? token) (not (m/numeric? token)))
    token ; if string and not numeric, return the string

    ; must be a number then
    :else
    (m/eval-number token)))

;;; Needed to handle parentheses swapping for expression reversal
(defn swap-parentheses [expression] ; turns "(" into ")" and vice versa
  (debug-log-detailed "swap-parentheses expression arg: " expression)
  (let [new-expression (atom [])]
    (dotimes [i (count expression)]
      (let [token (nth expression i)]
        (cond
          (= token left-p) (swap! new-expression assoc i right-p)
          (= token right-p) (swap! new-expression assoc i left-p)
          :else (swap! new-expression assoc i token))))
    (debug-log-detailed "swap-parentheses returning new-expression: " @new-expression)
    @new-expression))

;;; Pops the operator stack while the predicate function evaluates to true and
;;; pushes the result to the output/operand stack. Used by infix-expression-eval
(defn pop-stack-while! [predicate op-stack out-stack arity-stack]
  (while (predicate)
    (let [op-or-fn-token (peek @op-stack)
          func? (function? op-or-fn-token) ; unlike built-in fn? , function? only returns true for non operator functions in function map
          nil-equals-zero? (cond (operator? op-or-fn-token)
                                 (:nil-equals-zero? (operators op-or-fn-token)) ; get nil-equals-zero? flag from operators map
                                 (function? op-or-fn-token)
                                 (:nil-equals-zero? (get-function op-or-fn-token))) ; get nil-equals-zero? flag from functions map
          arity (if func? (peek @arity-stack) 2)] ; assume binary operator if not a function 
      (when func? (swap! arity-stack pop))
      (reset! out-stack
              (conj
               (nthrest @out-stack arity) ; pop operands equal to arity of func
               (apply (eval-token op-or-fn-token)
                      (map #(if nil-equals-zero? ; if function or operator has nil-equals-zero? flag
                              (or %1 0) ; replace nil with 0
                              %1) ; else just eval the token
                           (take arity @out-stack)))))
      (swap! op-stack pop))))

;;; Parses any tokenized infix algebraic expression and returns the result.
;;; TODO After switching to vectors in all of the eval functions,
;;; I've now got a weird mix of an infix expression vector with swapped parentheses
;;; and having to process the vector in reverse order. TODO investigate and simplify.
;;; Performance has increased over 12x from the original version
;;; when processing ranges with 1000's of cells so I'm leaving as is for now.
;;; TODO Potential improvement idea: eval all tokens in place and return vector of
;;; operands, operators, functions, etc. Then offload to web worker to eval the vector.
;;; Parentheses handling would be done by having vectors within vectors.
;;; TODO catch exceptions and return error msg or throw exception
(defn infix-expression-eval [reversed-expr]
  (let [num-items (count reversed-expr)
        op-stack (atom ())
        arity-stack (atom ())
        out-stack (atom ())]
    (dotimes [i num-items]

      (let [token (reversed-expr (- num-items i 1))]
        (cond
          ; if operand, adds it to the operand stack
          (operand? token)
          (swap! out-stack conj (eval-token token))

          ; left parenthesis 
          (= left-p token)
          (swap! op-stack conj token)

          ; right parenthesis
          (= right-p token)
          (do
            (pop-stack-while!
             #(not= left-p (peek @op-stack)) op-stack out-stack arity-stack)
            (swap! op-stack pop))

          ; comma
          (= comma token)
          (reset! arity-stack (conj (rest @arity-stack) (inc (peek @arity-stack))))

          ; if token is an operator and is the first one found in this expression
          (and (operator? token) (empty? @op-stack))
          (swap! op-stack conj token)

          (function? token)
          (do
            (reset! arity-stack (conj (rest @arity-stack) (inc (peek @arity-stack))))
            (swap! op-stack conj token)
            (pop-stack-while!
             #(function? (peek @op-stack)) op-stack out-stack arity-stack))
            ;(swap! op-stack conj token) ;)

          ; handles all other operators when not the first one
          (operator? token)
          (do
            (pop-stack-while!
             #(or (< (precedence token) (precedence (peek @op-stack)))
                  (and (<= (precedence token) (precedence (peek @op-stack)))
                       (= exp token)))
             op-stack out-stack arity-stack)
            (swap! op-stack conj token)))))
    ;; Once all tokens have been processed, pop and eval the stacks while op stack is not empty.
    (pop-stack-while! #(seq @op-stack) op-stack out-stack arity-stack)
    ;; Assuming the expression was a valid one, the last item is the final result.
    (eval-token (peek @out-stack)))) ; handle edge case where formula is a single cell reference

(defn infix-expression-prepare [infix-expression]
  (let [reversed-expr (swap-parentheses (swap-unary-minus (tokenize-as-str infix-expression)))]
    (debug-log "infix-expression was: " infix-expression)
    (do-with-timer "infix-expression-eval" (infix-expression-eval reversed-expr))))

(defn parse-formula [^string formula-str]
  (r/track! #(infix-expression-prepare formula-str)))

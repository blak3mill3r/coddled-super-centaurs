(ns coddled-super-centaurs.core
  (:refer-clojure :exclude [distinct drop drop-while interpose keep-indexed map-indexed partition-all partition-by take take-nth]))

;;; The root binding just calls `volatile!`. This allows them to work without state instrumentation, perhaps useful for development.

(defn ^:dynamic *state* [v] (volatile! v))

(defn distinct
  "Returns a stateful transducer which removes duplicate items and transduces each distinct item."
  []
  (fn [rf]
    (let [seen (*state* #{})]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (contains? @seen input)
           result
           (do (vswap! seen conj input)
               (rf result input))))))))

(defn drop
  "Returns a stateful transducer which drops n items before transducing any remaining items."
  [n]
  (fn [rf]
    (let [nv (*state* n)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [n @nv]
           (vswap! nv dec)
           (if (pos? n)
             result
             (rf result input))))))))

(defn drop-while
  "Returns a stateful transducer which drops items as long as they all pass the predicate pred, then transduces the remaining items."
  [pred]
  (fn [rf]
    (let [dv (*state* true)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [drop? @dv]
           (if (and drop? (pred input))
             result
             (do
               (vreset! dv nil)
               (rf result input)))))))))

;; see `clojure.core/interpose`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L5206
(defn interpose
  "Returns a stateful transducer which separates elements by sep."
  [sep]
  (fn [rf]
    (let [started (*state* false)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if @started
           (let [sepr (rf result sep)]
             (if (reduced? sepr)
               sepr
               (rf sepr input)))
           (do
             (vreset! started true)
             (rf result input))))))))

(defn keep-indexed
  "Returns a stateful transducer which transduces the non-nil results of (f index item).
  Note, this means false return values will be included."
  [f]
  (fn [rf]
    (let [iv (*state* -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [i (vswap! iv inc)
               v (f i input)]
           (if (nil? v)
             result
             (rf result v))))))))

(defn map-indexed
  "Returns a stateful transducer which transduces the result of applying f to 0 and the first item,
  followed by applying f to 1 and the second item, etc."
  [f]
  (fn [rf]
    (let [i (*state* -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (rf result (f (vswap! i inc) input)))))))


;; see `clojure.core/partition-all`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L7240
(defn partition-all
  "Returns a stateful transducer which partitions values together into output vectors of a fixed length n."
  [^long n]
  (fn [rf]
    (let [a (*state* [])]
      (fn
        ([] (rf))
        ([result]
         (let [av @a result (if (empty? av)
                              result
                              (do
                                (reset! a [])
                                (unreduced (rf result av))))]
           (rf result)))
        ([result input]
         (let [av (vswap! a conj input)]
           (if (= n (count av))
             (do 
               (vreset! a [])
               (rf result av))
             result)))))))

;; see `clojure.core/partition-by`
;; https://github.com/clojure/clojure/blob/clojure-1.10.1/src/clj/clojure/core.clj#L7160
(defn partition-by
  "Returns a stateful transducer which applies f to each value, outputting vectors
  which are split each time f returns a new value."
  [f]
  (fn [rf]
    (let [a (*state* [])
          p (*state* ::none)]
      (fn
        ([] (rf))
        ([result]
         (let [av @a result (if (empty? av)
                              result
                              (do
                                (reset! a [])
                                (unreduced (rf result av))))]
           (rf result)))
        ([result input]
         (let [pv @p val (f input)]
           (vreset! p val)
           (if (or (identical? pv ::none) (= val pv))
             (do (vswap! a conj input) result)
             (let [av @a]
               (vreset! a [])
               (let [ret (rf result av)]
                 (when-not (reduced? ret)
                   (vswap! a conj input))
                 ret)))))))))

(defn take
  "Returns a stateful transducer which stops after n items. This can cause records to be skipped."
  [n]
  (fn [rf]
    (let [nv (*state* n)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [n @nv
               nn (vswap! nv dec)
               result (if (pos? n)
                        (rf result input)
                        result)]
           (if (not (pos? nn))
             (ensure-reduced result)
             result)))))))

(defn take-nth
  "Returns a stateful transducer which transduces every nth item."
  [n]
  (fn [rf]
    (let [iv (*state* -1)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (let [i (vswap! iv inc)]
           (if (zero? (rem i n))
             (rf result input)
             result)))))))

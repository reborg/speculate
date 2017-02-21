(ns speculate.lens-test
  (:refer-clojure :exclude [set])
  (:require
   [bifocal.lens :as bi :refer [view over set]]
   [clojure.spec :as s]
   [clojure.test :refer [deftest is]]
   [speculate.ast :as ast]
   [speculate.lens :as lens]
   [speculate.spec :as u]))

(s/def ::a string?)
(s/def ::b int?)
(s/def ::c keyword?)

(s/def ::d #{"test"})

(s/def ::x (s/keys :req-un [::a ::b ::c]))
(s/def ::y (s/keys :req-un [::d]))

(s/def ::z (s/or :x ::x :y ::y))
(s/def ::z2 (s/and ::z map?))

(deftest mk-keys-test
  (let [x {:a "test" :b 10 :c :thing}
        l (lens/mk (ast/parse ::x))]
    ;; ;; ;; ;; ;; (is (= x (set l nil (view l x))))
    (set l {} (view l x))
    ))
#_(let [x {:a "test" :b 10 :c :thing}
        l (lens/mk (ast/parse ::x))]
    ;; ;; ;; ;; ;; (is (= x (set l nil (view l x))))
    (set l {} [(view l x)])
    )

(def test-10-thing-values
  [(bi/value {:name ::a} "test")
   (bi/value {:name ::b} 10)
   (bi/value {:name ::c} :thing)])

(deftest mk-or-test
  (is (= (view (lens/mk (ast/parse ::z)) {:a "test" :b 10 :c :thing})
         test-10-thing-values))
  (is (= (view (lens/mk (ast/parse ::z)) {:d "test"})
         [(bi/value {:name ::d} "test")])))

(deftest mk-and-test
  (is (= (view (lens/mk (ast/parse (s/and ::z map?)))
               {:a "test" :b 10 :c :thing})
         test-10-thing-values)))

(deftest mk-every-test
  (let [x {:a "test" :b 10 :c :thing}
        s (s/coll-of ::z2)
        l (lens/mk (ast/parse s))]
    (is (= (view l [x x x])
           (repeat 3 test-10-thing-values)))))

(deftest mk-nilable-test
  (let [x {:a "test" :b 10 :c :thing}
        s (s/nilable (s/coll-of ::z2))
        l (lens/mk (ast/parse s))]
    (is (= (view l [x x x])
           (repeat 3 test-10-thing-values)))
    (is (nil? (view l nil)))))

(s/def ::arg-list (s/cat :a ::a :b ::b :c (s/coll-of ::z)))

;; (view (lens/mk (ast/parse (s/form ::arg-list))) '("test" 10 [:thing]))

;; (ast/conform-1 (ast/parse (s/cat :a (s/? int?) :b string?)) [1 "test"])

;; (require 'clojure.spec.override)

;; (let [x ["test" 10 :thing]
;;       s (s/cat :a ::a :b ::b :c ::c)
;;       p (ast/parse s)
;;       c (ast/conform-1 p x)
;;       ]
;;   c)

;; Limitations of cat as a transform target are that regex ops are
;; pretty useless to target. They are not specific, so cannot be
;; targetted properly. They are just patterns.
;; But using cat to define positional targets would work
;; Maybe tuple would be better? That is more specific.

;; (set (lens/mk (ast/parse (s/tuple ::x)))
;;      nil
;;      [["test" 10 :thing]])

;; (s/tuple ::a ::b ::c)

;; (s/conform (s/? int? ) ["set"])

;; (ast/unparse (ast/parse (s/cat :a (s/? int?) :b string?)))

;; ["test"]

;; (ast/conform-1 (ast/parse ::arg-list) '("test" 10 [{:d "test"}]))
;; (ast/conform-1 (ast/parse ::arg-list) [{:d "test"}])
(s/def ::value int?)
(s/def ::value-a ::value)
(s/def ::value-b ::value)

(deftest categorize-test
  (let [spec (u/categorize (s/keys :req-un [::value]) :type :type)]
    (= (view (lens/mk (ast/parse spec))
             {:type "type-1" :value 5})
       (bi/value {:type "type-1"} 5))))

(s/def ::map-1 map?)
(s/def ::type-1
  (-> ::map-1
      (u/categorize :type :type)
      (u/select :type #{"type-1"})))

(s/def ::map-2 (s/keys :req-un [::value-a ::value-b]))
(s/def ::type-2
  (-> ::map-2
      (u/categorize :type :type)
      (u/select :type #{"type-2"})))

(deftest categorize-select-test
  (let [spec (s/keys :req-un [::type-1])
        lens (lens/mk (ast/parse spec))]
    (is (= (view lens
                 {:type-1 [{:type "type-1" :value-a 5 :value-b 6}
                           {:type "type-2" :value 4}]})
           [(bi/value {:type "type-1" :name :speculate.lens-test/map-1}
                      {:type "type-1", :value-a 5, :value-b 6})])))
  (let [spec (s/keys :req-un [::type-2])
        lens (lens/mk (ast/parse spec))]
    (is (= (view lens
                 {:type-2 [{:type "type-1" :value-a 5 :value-b 6}
                           {:type "type-2" :value-a 3 :value-b 4}]})
           [(bi/value {:type "type-2" :name :speculate.lens-test/value-a} 3)
            (bi/value {:type "type-2" :name :speculate.lens-test/value-b} 4)]))))

;; {:speculate.ast/type speculate.spec/categorize
;;  :form {:speculate.ast/type clojure.core/symbol?
;;         :form clojure.core/map?}
;;  :categorize {:type :type}}

;; (clojure.test/run-tests)
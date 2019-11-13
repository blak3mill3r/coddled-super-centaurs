(ns coddled-super-centaurs.core-test
  (:require [coddled-super-centaurs.core :as sut]
            [clojure.test :as t :refer [deftest is]]))

(deftest fail
  "There aren't any tests"
  (is (= 1 2)))

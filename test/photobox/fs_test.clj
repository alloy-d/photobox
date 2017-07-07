(ns photobox.fs-test
  "Tests for miscellaneous filesystem-related utilities."
  (:require [photobox.fs :refer :all]
            [me.raynes.fs :as fs]
            [clojure.java.io :refer [file]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def extension (gen/elements [".RAF", ".JPG"]))
(def filename-gen
  (gen/fmap (fn [[name extension]]
              (str name extension))
            (gen/tuple (gen/not-empty gen/string-alphanumeric) extension)))
(def file-gen (gen/fmap file filename-gen))

(defspec all-jpegs-come-first 100
  (prop/for-all [v (gen/not-empty (gen/vector file-gen))]
                (let [sorted-by-extension
                      (sort-by-extension v)
                      sorted-jpegs-only
                      (sort (filter #(= (fs/extension %) ".JPG") v))]
                  (= sorted-jpegs-only
                     (take (count sorted-jpegs-only) sorted-by-extension)))))

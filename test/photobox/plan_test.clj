(ns photobox.plan-test
  (:require [photobox.plan :refer :all]
            [photobox.fs-test :refer [filename-gen]]
            [clojure.string :refer [join]]
            [me.raynes.fs :as fs]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

(def dir-gen
  "Generates an absolute path with no extension."
  (gen/fmap (fn [path-parts]
              (str "/" (join "/" path-parts)))
            (gen/not-empty (gen/vector (gen/not-empty gen/string-alphanumeric)))))

(def path-gen
  "Generates an absolute path with a filename."
  (gen/fmap (fn [[dir name]]
              (str dir "/" name))
            (gen/tuple dir-gen filename-gen)))

(def photo-data-gen
  "Generates a map with a path at `:path`, to mimic what photocopier expects."
  (gen/fmap (fn [path] {:path path}) path-gen))

(defspec photocopier-only-copies-files 20
  (prop/for-all [photos (gen/vector photo-data-gen)
                 dest-dir dir-gen]
                (let [copier (photocopier identity dest-dir)
                      operations (into [] copier photos)]
                  (or (= 0 (count operations))
                      (= #{:photobox.plan/copy-file}
                         (set (map :operation operations)))))))

(defspec photocopier-generates-correct-paths 50
  (prop/for-all [photos (gen/vector photo-data-gen)
                 dest-dir dir-gen]
                (let [copier (photocopier identity dest-dir)
                      operations (into [] copier photos)
                      expected-paths (map #(str dest-dir "/"
                                                (fs/base-name (:path %)))
                                          photos)]
                  (= expected-paths
                     (map :dest-file operations)))))

(defspec photocopier-applies-transforms 20
  (prop/for-all [input (gen/vector (gen/tuple gen/boolean photo-data-gen))]
                (let [copier (photocopier (comp (filter first) (map second))
                                          "/dev/null")]
                  (= (into #{} (comp (filter first) (map #(:path (second %)))) input)
                     (into #{} (comp copier (map :src-file)) input)))))

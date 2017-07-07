(ns photobox.plan-test
  (:require [photobox.plan :refer :all]
            [photobox.fs-test :refer [filename-gen]]
            [clojure.java.io :refer [file writer reader]]
            [clojure.string :refer [join]]
            [clojure.test :refer [deftest is testing]]
            [me.raynes.fs :as fs]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop])
  (:import [java.io BufferedReader]))

(def dir-gen
  "Generates an absolute path with no extension."
  (gen/fmap (fn [path-parts]
              (str "/" (join "/" path-parts)))
            (gen/not-empty (gen/vector
                             (gen/not-empty gen/string-alphanumeric)))))

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
                  (or (empty? operations)
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
                  (= (into #{}
                           (comp (filter first) (map #(:path (second %))))
                           input)
                     (into #{}
                           (comp copier (map :src-file))
                           input)))))


(def ^:private test-file-contents "Pretend this is some image data.")
(defn- create-test-file
  "Creates a temporary file with some contents."
  []
  (let [f (fs/temp-file "photobox-test-file")]
    (with-open [w (writer f)]
      (.write w test-file-contents))
    f))
(defn- imagine-test-path
  "Returns a temporary path that should not exist."
  []
  (str (fs/tmpdir) (fs/temp-name "photobox-test-file")))

(deftest copy-assessment
  (let [existent-src (create-test-file)
        existent-dest (create-test-file)
        nonexistent-src (imagine-test-path)
        nonexistent-dest (imagine-test-path)]
    (testing "assesses an existing destination file as a no-op"
      (is (= :photobox.plan/noop
             (:operation (assess (copy existent-src existent-dest))))))
    (testing "assesses a nonexistent source file as impossible"
      (is (= :photobox.plan/impossible
             (:operation (assess (copy nonexistent-src nonexistent-dest))))))
    (testing "assesses a valid copy operation as itself"
      (let [valid-op (copy existent-src nonexistent-dest)]
        (is (= (assess valid-op) valid-op))))))

(deftest copy-execution-success
  (let [src (create-test-file)
        dest (imagine-test-path)
        op (copy src dest)
        result (execute! op)]
    (testing "copies a file"
      (is (fs/exists? dest))
      (is (= test-file-contents
             (slurp (file dest)))))
    (testing "returns a success result"
      (is (= :success (:type result))))))

(deftest copy-execution-failure
  (let [src (imagine-test-path)
        dest (imagine-test-path)
        op (copy src dest)
        result (execute! op)]
    (testing "returns a failure result"
      (is (= :failure (:type result))))))

(deftest archive-assessment
  (let [existent-source-file (create-test-file)
        nonexistent-source-file (imagine-test-path)
        existent-root (fs/temp-dir "photobox-test-archive-root")
        nonexistent-root (imagine-test-path)
        archival-path (fs/temp-name "photobox-archival-path")]
    (testing "assesses archival to an nonexistent root as impossible"
      (let [op (archive existent-source-file nonexistent-root archival-path)]
        (is (= :photobox.plan/impossible
               (:operation (assess op))))))
    (testing "assesses possible archival as a copy operation"
      (let [unassessed-op (archive
                            existent-source-file
                            existent-root archival-path)
            expected-op (copy
                          existent-source-file
                          (str existent-root "/" archival-path))]
        (is (= expected-op
               (assess unassessed-op))))
      (testing "including assessment of the copy operation"
        (let [assessed-op (assess (archive
                                    nonexistent-source-file
                                    existent-root archival-path))]
          (is (= :photobox.plan/impossible
                 (:operation assessed-op)))
          (is (= :photobox.plan/copy-file
                 (get-in assessed-op [:planned :operation]))))))))

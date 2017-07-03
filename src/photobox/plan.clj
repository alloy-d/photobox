(ns photobox.plan
  "Utilities for making a plan and then executing it.

  The point here is to isolate the carrying out of actions
  to more or less one place, so that everything else is pure
  planning that is easier to test and harder to get wrong."
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]))

(defn copy
  "Returns a plan equivalent to `cp src-file dest-file`."
  [src-file dest-file]
  {:operation ::copy-file
   :src-file src-file
   :dest-file dest-file})

(defn copy-to-dir
  "Returns a plan equivalent to `cp src-file dest-dir`."
  [src-file dest-dir]
  (let [dest-file (string/join "/" [dest-dir (fs/base-name src-file)])]
    (copy src-file dest-file)))

(defn photocopier
  "Produces a list of operations that will copy photos produced
  by `xf` to `dest-dir`."
  [xf dest-dir]
  (comp xf
        (map (fn [photo-data]
               (copy-to-dir (photo-data :path) dest-dir)))))


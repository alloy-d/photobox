(ns photobox.fs
  (:require [me.raynes.fs :as fs]))

(defn find-photos
  "Returns a list of all photos under a given path.
  Naively searches for things with a .JPG or .RAF extension."
  [root]
  (fs/find-files root #".+\.(JPG|RAF)$"))

(defn sort-by-extension [files]
  "Sorts a list of files, giving precedence to the extension."
  (letfn [(key [file]
            (let [path (.getPath file)
                  ext (fs/extension path)]
              [ext path]))]
    (sort-by key files)))
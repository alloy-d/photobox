(ns photobox.archive
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]))

(s/def ::path string?)

;; FIXME: this is ridiculous.
(def root
  ({"Linux" {:photo "/mnt/henry/media/Photos"
             :video "/mnt/henry/media/Videos"}
    "Mac OS X" {:photo "/Volumes/Multimedia/Photos"
                :video "/Volumes/Multimedia/Videos"}}
   (System/getProperty "os.name")))

(defn filename-for-path [archive-path]
  (str (root :photo) "/" archive-path))

(defn exists?
 "Is archive-path really in the archive?"
 [archive-path]
 (fs/exists? (filename-for-path archive-path)))


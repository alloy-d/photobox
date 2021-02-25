(ns photobox.archive
  "Utilities for dealing with the archive.

  'The archive' here is the store of photos on a filesystem.
  For our purposes, the archive is the ultimate source of truth for most
  data about photos."
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]))

(s/def ::path string?)
(s/keys ::entry [::path])

;; FIXME: this is ridiculous.
(def root
  ({"Linux" {:photo "/mnt/henry/media/Photos"
             :video "/mnt/henry/media/Videos"}
    "Mac OS X" {:photo "/Volumes/Multimedia/Photos"
                :video "/Volumes/Multimedia/Videos"}}
   (System/getProperty "os.name")))

(defn absolute-path
  "Produces the absolute path for an archive entry."
  [{archive-path ::path}]
  (str (root :photo) "/" archive-path))

(def exists?
  "Is this entry really in the archive?"
  (comp fs/exists? absolute-path))

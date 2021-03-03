(ns photobox.archive
  "Utilities for dealing with the archive.

  'The archive' here is the store of photos on a filesystem.
  For our purposes, the archive is the ultimate source of truth for most
  data about photos."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [environ.core :refer [env]]
            [me.raynes.fs :as fs]))

(s/def ::path string?)
(s/keys ::entry [::path])

(defn root
  "Returns the archive root when called with no arguments.

  If called with `:photo` or `:video`, returns the appropriate
  subdirectory for Historical Reasons(TM)."

  ([]
   (if-let [archive-root (env :photobox-archive-root)]
     (let [expanded (-> archive-root fs/expand-home fs/normalized)]
       (if (string/ends-with? expanded "/")
         expanded
         (str expanded "/")))
     (throw (Exception. ":photobox-archive-root is not set. Try setting $PHOTOBOX_ARCHIVE_ROOT."))))

  ([subdir-type]
   (cond (= subdir-type :photo) (str (root) "Photos")
         (= subdir-type :video) (str (root) "Videos")
         :else nil)))

(defn absolute-path
  "Produces the absolute path for an archive entry."
  [{archive-path ::path}]
  (str (root :photo) "/" archive-path))

(def exists?
  "Is this entry really in the archive?"
  (comp fs/exists? absolute-path))

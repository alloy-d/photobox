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

(defn photo-paths
  "Returns, as strings, the archive-relative paths for all photo files
  from the archive."
  []

  (let [base (root :photo)
        base-components (count (fs/split base))
        files (fs/find-files*
                base
                #(re-matches #".+/\d{4}/\d{4}-\d{2}/\d{4}-\d{2}-\d{2}/\d{8}-[^/]+\.(RAF|JPG|DNG|NEF)$"
                             (.getPath %)))]
    (map #(->> %
              .getPath
              fs/split
              (drop base-components)
              (string/join "/"))
         files)))

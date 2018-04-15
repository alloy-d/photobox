(ns photobox.core
  (:require [clojure.core.async :as async
             :refer (<! >! <!! go go-loop chan
                        onto-chan mult tap pipe)]
            [clojure.string :as string]
            [clojure.pprint :refer (pprint)]
            [java-time :as t]
            [me.raynes.fs :as fs]
            [photobox.metadata.core :as metadata]
            [photobox.metadata.image :as exif]
            [photobox.metadata.video :as video-metadata]
            [photobox.fs :refer (find-photos find-videos sort-by-extension)]
            [photobox.plan :as plan]
            [photobox.execute :as execute]))

(def archive-root
  {:photo "/Volumes/Multimedia/Photos"
   :video "/Volumes/Multimedia/Videos"})
(def good-capture-destination-dir (fs/expand-home "~/Desktop/good-photos/"))
(def great-capture-destination-dir (fs/expand-home "~/Desktop/great-photos/"))
(def file-source "/Volumes/Untitled")

(def files
  (let [photo-files (sort-by-extension (find-photos file-source))
        video-files (find-videos file-source)
        classifier (fn [type]
                     (fn [file] {:type type :file file}))]
    (concat (map (classifier :photo) photo-files)
            (map (classifier :video) video-files))))

(defn- get-rating [capture-data]
  ((capture-data :exif-data) "Rating" -1))
(defn- get-date [capture-data]
  (metadata/parse-exif-date ((capture-data :exif-data) "Date/Time")))

(defn archival-path
  "Returns a path in the format I use to archive files:
  `yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-(original filename)`."
  [capture-data]
  (let [creation-date (get-date capture-data)]
    (str
      (t/format "yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-" creation-date)
      (fs/base-name (capture-data :path)))))

(def transductions
  [(plan/photocopier (filter #(> (get-rating %) 3)) good-capture-destination-dir)
   (plan/photocopier (filter #(= (get-rating %) 5)) great-capture-destination-dir)
   (map #(plan/archive (:path %) (archive-root (:type %)) (archival-path %)))
   (comp (filter #(>= (get-rating %) 1))
         (map #(assoc (plan/archive (:path %) (archive-root (:type %)) (archival-path %)) :overwrite true)))
   ])

(comment
  ;; Example usage: clean out files before a certain point.
  ;; This obviously shouldn't live here; just keeping it around
  ;; until it has a better place to go.
  (def transductions
    [(comp
       (filter #(t/before? (get-date %)
                           (t/local-date-time "2017-07-03T16:18:16")))
       (map #(plan/delete (:path %))))]))

(defmulti info-for-file :type)
(defmethod info-for-file :photo [{:keys [file]}]
  (let [exif-data (exif/interesting-data-for-file file)
        file-path (.getAbsolutePath file)]
    {:type :photo
     :path file-path
     :exif-data exif-data}))
(defmethod info-for-file :video [{:keys [file]}]
  (let [file-path (.getAbsolutePath file)
        date-time (video-metadata/date-time-for-filename file-path)]
    {:type :video
     :path file-path
     :exif-data {"Date/Time" date-time}}))

(defn plan []
  (let [files-data (map info-for-file files)
        results-by-transduction (map #(into [] % files-data) transductions)]
    (apply concat results-by-transduction)))

(defn process []
  (execute/finalize-and-execute! (plan)))

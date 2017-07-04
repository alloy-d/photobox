(ns photobox.core
  (:require [clojure.core.async :as async
             :refer (<! >! <!! go go-loop chan
                        onto-chan mult tap pipe)]
            [clojure.string :as string]
            [clojure.pprint :refer (pprint)]
            [java-time :as t]
            [me.raynes.fs :as fs]
            [photobox.exif :as exif]
            [photobox.fs :refer (find-photos sort-by-extension)]
            [photobox.plan :as plan]
            [photobox.execute :as execute]))

(def archival-root "/Volumes/Multimedia/Photos")
(def good-photo-destination-dir (fs/expand-home "~/Desktop/good-photos/"))
(def great-photo-destination-dir (fs/expand-home "~/Desktop/great-photos/"))
(def photo-source "/Volumes/Untitled")
(def photo-files (sort-by-extension (find-photos photo-source)))

(defn- get-rating [photo-data]
  ((photo-data :exif-data) "Rating"))
(defn- get-date [photo-data]
  (exif/parse-exif-date ((photo-data :exif-data) "Date/Time")))

(defn archival-path
  "Returns a path in the format I use to archive files:
  `yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-(original filename)`."
  [photo-data]
  (let [creation-date (get-date photo-data)]
    (str
      archival-root "/"
      (t/format "yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-" creation-date)
      (fs/base-name (photo-data :path)))))

(def transductions
  [(plan/photocopier (filter #(> (get-rating %) 3)) good-photo-destination-dir)
   (plan/photocopier (filter #(= (get-rating %) 5)) great-photo-destination-dir)
   (map #(plan/copy (:path %) (archival-path %)))])

(defn info-for-file [file]
  (let [exif-data (exif/interesting-data-for-file file)
        file-path (.getAbsolutePath file)]
    {:path file-path
     :exif-data exif-data}))

(defn plan []
  (let [photo-data (map info-for-file photo-files)
        results-by-transduction (map #(into [] % photo-data) transductions)]
    (apply concat results-by-transduction)))

(defn process []
  (execute/finalize-and-execute! (plan)))

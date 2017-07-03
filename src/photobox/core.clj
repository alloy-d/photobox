(ns photobox.core
  (:require [clojure.core.async :as async
             :refer (<! >! <!! go go-loop chan
                        onto-chan mult tap pipe)]
            [clojure.string :as string]
            [clojure.pprint :refer (pprint)]
            [java-time :as t]
            [me.raynes.fs :as fs]
            [photobox.exif :as exif]
            [photobox.fs :refer (find-photos sort-by-extension)]))

(def archival-root "/Volumes/Multimedia/Photos")
(def good-photo-destination-dir (fs/expand-home "~/Desktop/good-photos/"))
(def great-photo-destination-dir (fs/expand-home "~/Desktop/great-photos/"))
(def photo-source "/Volumes/Untitled")
(def photo-files (sort-by-extension (find-photos photo-source)))

(defn- get-rating [photo-data]
  ((photo-data :exif-data) "Rating"))
(defn- get-date [photo-data]
  (exif/parse-exif-date ((photo-data :exif-data) "Date/Time")))

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
  [(photocopier (filter #(> (get-rating %) 3)) good-photo-destination-dir)
   (photocopier (filter #(= (get-rating %) 5)) great-photo-destination-dir)
   (map #(copy (:path %) (archival-path %)))])

(defn info-for-file [file]
  (let [exif-data (exif/interesting-data-for-file file)
        file-path (.getAbsolutePath file)]
    {:path file-path
     :exif-data exif-data}))

(defn plan []
  (let [photo-data (map info-for-file photo-files)
        results-by-transduction (map #(into [] % photo-data) transductions)]
    (apply concat results-by-transduction)))


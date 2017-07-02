(ns photobox.core
  (:require [clojure.core.async :as async :refer (<! >! <!! chan go go-loop mult onto-chan pipe tap thread)]
            [clojure.string :as string]
            [clojure.pprint :refer (pprint)]
            [me.raynes.fs :as fs]
            [photobox.exif :as exif]))

(def good-photo-destination-dir (fs/expand-home "~/Desktop/good-photos/"))
(def great-photo-destination-dir (fs/expand-home "~/Desktop/great-photos/"))
(def jpeg-pattern "/Volumes/Untitled/DCIM/103_FUJI/*.JPG")
(def jpeg-files (fs/glob jpeg-pattern))

(defn is-good-photo [photo-data] 
  (> ((photo-data :exif-data) "Rating") 3))
(defn is-great-photo [photo-data]
  (= ((photo-data :exif-data) "Rating") 5))

(defn process-good-photo [photo-data]
  (do
    (let [src-file (photo-data :path)
          dest-file (string/join "/" [good-photo-destination-dir (fs/base-name src-file)])]
      (if (not (fs/exists? dest-file))
        (fs/copy src-file dest-file)))))
(defn process-great-photo [photo-data]
  (do 
    (let [src-file (photo-data :path)
          dest-file (string/join "/" [great-photo-destination-dir (fs/base-name src-file)])]
      (if (not (fs/exists? dest-file))
        (fs/copy src-file dest-file)))))

(defn info-for-file [file]
  (let [exif-data (exif/interesting-data-for-file file)
        file-path (.getAbsolutePath file)]
    {:path file-path
     :exif-data exif-data}))

(defn- run-async [processor channel]
  "In a (go ...) block, runs `processor` for each entry in `channel`.
  Finishes when `channel` becomes empty."
  (go-loop []
           (if-let [entry (<! channel)]
             (do (processor entry)
                 (recur)))))

(defn do-things []
  (let [photo-data (chan 20 (map info-for-file))
        great-photos (chan 20 (filter is-great-photo))
        photo-data-mult (mult photo-data)
        good-photo-processor (let [good-photos (chan 20 (filter is-good-photo))]
                               (tap photo-data-mult good-photos)
                               (run-async process-good-photo good-photos))
        great-photo-processor (let [great-photos (chan 20 (filter is-great-photo))]
                                (tap photo-data-mult great-photos)
                                (run-async process-great-photo great-photos))]
    (onto-chan photo-data jpeg-files)
    (<!! good-photo-processor)
    (<!! great-photo-processor)
    "all done"))

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

(defn- copy-to-directory [directory src-file]
  (let [dest-file (string/join "/" [directory (fs/base-name src-file)])]
    (if (not (fs/exists? dest-file))
      (fs/copy src-file dest-file))))

(defprotocol PProcessor
  "A thing that prepares and then acts on items."

  (prepare [this] "Returns a transducer that prepares data in the pipeline for action.")
  (act [this data] "Take data transduced by `prepare`, and act on it."))

(defrecord GoodPhotoProcessor []
  PProcessor
  (prepare [_]
    (filter (fn [photo-data]
              (> ((photo-data :exif-data) "Rating") 3))))
  (act [_ photo-data]
    (copy-to-directory good-photo-destination-dir (photo-data :path))))

(defrecord GreatPhotoProcessor []
  PProcessor
  (prepare [_]
    (filter (fn [photo-data]
              (= ((photo-data :exif-data) "Rating") 5))))
  (act [_ photo-data]
    (copy-to-directory great-photo-destination-dir (photo-data :path))))

(def processors [(GoodPhotoProcessor.) (GreatPhotoProcessor.)])

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
        photo-data-mult (mult photo-data)
        running-processors (map (fn [processor]
                                  (let [input (chan 20 (prepare processor))]
                                    (tap photo-data-mult input)
                                    (run-async #(act processor %) input)))
                                processors)]
    (onto-chan photo-data jpeg-files)
    (doseq [proc running-processors] (<!! proc))
    "all done"))

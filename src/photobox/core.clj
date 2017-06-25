(ns photobox.core
  (:require [clojure.pprint :refer (pprint)]
            [clojure.set :as set]
            [exif-processor.core :as processor]))

(def test-file "/Volumes/Untitled/DCIM/103_FUJI/DSCF3788.JPG")
(def interesting-tags #{"Date/Time"
                        "Development Dynamic Range"
                        "Dynamic Range"
                        "Dynamic Range Setting"
                        "Exposure Count"
                        "Exposure Mode"
                        "Exposure Program"
                        "Exposure Time"
                        "F-Number"
                        "Focal Length"
                        "Focus Mode"
                        "Highlight Tone"
                        "ISO Speed Ratings"
                        "Image Count"
                        "Image Generation"
                        "Lens Make"
                        "Lens Model"
                        "Make"
                        "Model"
                        "Rating"
                        "Saturation"
                        "Sequence Number"
                        "Shadow Tone"
                        "Shutter Speed Value"
                        "Shutter Type"
                        "Software"})

(def postprocessors
  {0x1022 {:name "AF Mode"
           :value {"0" "No"
                   "1" "Single Point"
                   "256" "Zone"
                   "512" "Wide/Tracking"}}
   0x1032 {:name "Exposure Count"
           :value #(Integer/parseInt %)}
   0x1040 {:name "Shadow Tone"
           :value #(-> % Integer/parseInt - (/ 4))}
   0x1041 {:name "Highlight Tone"
           :value #(-> % Integer/parseInt - (/ 4))}
   0x1050 {:name "Shutter Type"
           :value {"0" "Mechanical", "1" "Electronic"}}
   0x1431 {:name "Rating"
           :value #(Integer/parseInt %)}
   0x1436 {:name "Image Generation"
           :value {"0" "Original Image"
                   "1" "Re-developed from RAW"}}
   0x1438 {:name "Image Count"
           :value #(Integer/parseInt %)}})

(defn- unknown-tag-number [tag-name]
  "Parses the numerical component of auto-generated unknown tag names."
  (if-let [matches (re-matches #"Unknown tag \(0x([0-9a-f]+)\)" tag-name)]
    (Integer/parseInt (nth matches 1) 16)))

(defn- postprocess [data]
  "Supplements exif-processor's data with extra stuff we know how to interpret."
  (letfn [(postprocess-single [[tag value]]
            (let [number (unknown-tag-number tag)
                  postprocessor (if number (postprocessors number))]
              (if postprocessor
                {(:name postprocessor) ((:value postprocessor) value)}
                {tag value})))]
    (apply merge (map postprocess-single data))))

(def exif-for-filename
  "Same as exif-processor.core/exif-for-filename, but with our very specific postprocessing."
  (comp postprocess processor/exif-for-filename))

(def available-tags-for-filename
  "Lists Exif data keys available in a file."
  (comp keys exif-for-filename))

(defn interesting-data [data]
  "Returns the interesting stuff from extracted Exif data."
  (select-keys data interesting-tags))

(defn missing-tags [data]
  "Lists Exif data keys that we're interested in but don't have in the given data."
  (set/difference interesting-tags (set (keys data))))

(defn unknown-data [data]
  "Returns the unknown stuff from extracted Exif data."
  (filter (fn [[tag _]] (unknown-tag-number tag)) data))

(def interesting-data-for-filename
  (comp interesting-data exif-for-filename))
(def missing-tags-for-filename
  (comp missing-tags exif-for-filename))
(def unknown-data-for-filename
  (comp unknown-data exif-for-filename))

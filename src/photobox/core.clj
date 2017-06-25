(ns photobox.core
  (:require [clojure.pprint :refer (pprint)]
            [clojure.set :as set]
            [exif-processor.core :refer (exif-for-filename)]))

(def test-file "/Volumes/Untitled/DCIM/103_FUJI/DSCF3788.JPG")
(def interesting-data #{"Date/Time"
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
  {"0x1022" {:name "AF Mode"
             :value {"0" "No"
                     "1" "Single Point"
                     "256" "Zone"
                     "512" "Wide/Tracking"}}
   "0x1032" {:name "Exposure Count"
             :value #(Integer/parseInt %)}
   "0x1040" {:name "Shadow Tone"
             :value #(-> % Integer/parseInt - (/ 4))}
   "0x1041" {:name "Highlight Tone"
             :value #(-> % Integer/parseInt - (/ 4))}
   "0x1050" {:name "Shutter Type"
             :value {"0" "Mechanical", "1" "Electronic"}}
   "0x1431" {:name "Rating"
             :value #(Integer/parseInt %)}
   "0x1436" {:name "Image Generation"
             :value {"0" "Original Image"
                     "1" "Re-developed from RAW"}}
   "0x1438" {:name "Image Count"
             :value #(Integer/parseInt %)}})

(defn available-data [filename]
  (keys (exif-for-filename filename)))

(defn visit-file [filename]
  (exif-for-filename filename))

(defn unknown-tag-number [tag-name]
  (if-let [matches (re-matches #"Unknown tag \((0x[0-9a-f]+)\)" tag-name)]
    (nth matches 1)))

(defn postprocess [data]
  (letfn [(postprocess-single [[k v]]
              (if-let [number (unknown-tag-number k)]
                (if-let [postprocessor (postprocessors number)]
                  {(:name postprocessor) ((:value postprocessor) v)}
                  {k v})
                {k v}))]
    (apply merge (map postprocess-single data))))

(defn extract-interesting [data]
  (filter (fn [[k _]] (interesting-data k)) data))

(defn missing-data [data]
  (set/difference interesting-data (set (keys data))))

(defn unknown-data [data]
  (apply merge (map (fn [[k v]] {(unknown-tag-number k) v})
                    (filter (fn [[k _]] (unknown-tag-number k)) data))))
(defn unknown-data [data]
  (filter (fn [[k _]] (unknown-tag-number k)) data))

(def print-interesting (comp pprint (partial sort-by first) extract-interesting postprocess))
(def print-unknown (comp pprint (partial sort-by first) unknown-data))
(def print-postprocessed (comp pprint (partial sort-by first) postprocess))
(def print-postprocessed-unknowns (comp pprint (partial sort-by first) postprocess unknown-data))

(extract-interesting (exif-for-filename test-file))
(missing-data (extract-interesting (exif-for-filename test-file)))
(unknown-data (exif-for-filename test-file))


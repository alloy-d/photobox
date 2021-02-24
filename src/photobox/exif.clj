(ns photobox.exif
  "Utilities for working with Exif data.

  Includes some hacked-in special processing for files from the Fujifilm X-T2."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [exif-processor.core :as processor]
            [photobox.db :refer [defschema]]
            [photobox.metadata.core :refer [parse-exif-date]]))

(defn- exif-name->keyword
  "Converts an exif tag name to a keyword in this namespace."
  [exif-name]
  (keyword (str *ns*)
           (-> exif-name
               string/lower-case
               (string/replace #"[ /]+" "-")
               (string/replace #"[()]+" ""))))

(defn- unknown
  "Returns the name that exif-processor would assign for the given unknown tag number."
  [number]
  (format "Unknown tag (0x%04x)" number))

(s/def ::exif-tag (s/cat :tag string? :value string?))
(s/def ::exif-tag-list (s/coll-of ::exif-tag))

(defmulti decode-exif-key "Produces the keyword key for an exif tag." :tag)
(defmulti decode-exif-value "Decodes the value from an exif tag." :tag)

;; By default, we'll assume that any tag that hasn't been explicitly
;; defined is one we don't care about.
(defmethod decode-exif-key :default [_] nil)

;; If a tag has no conversion function defined, we'll just pass its
;; string value through.
(defmethod decode-exif-value :default [{:keys [value]}] value)

(defmacro def-exif
  "Registers an EXIF tag that we care about, by...

  - defining a spec for the tag's value, with the given `value-spec` at
    the keyword produced by `exif-name->keyword`.
  - defining `decode-exif-key` to turn the raw tag name into the desired
    key.
  - defining `decode-exif-value` to transform the raw value, if
    `:convert` is given.
  - defining a DB schema with `db/defschema`, if `:schema` is given."
  ([tag-name value-spec & {:keys [convert raw-name schema]}]
   (let [spec-key# (exif-name->keyword tag-name)
         raw-name# (or raw-name tag-name)]
     `(do
        (s/def ~spec-key# ~value-spec)
        (defmethod decode-exif-key ~raw-name# [_#] ~spec-key#)
        ~(when convert
           `(defmethod decode-exif-value ~raw-name# [tag-obj#]
              (~convert (:value tag-obj#))))
        ~(when schema
           `(defschema ~spec-key# ~schema))

        ~spec-key#))))

(defmacro def-exif-from-map
  "Like def-exif, but instead of taking a spec and a `:convert`, takes
  a `value-map`.

  `value-map` is used as the conversion function, and `(set (val
  value-map))` is used as the spec."
  [tag-name value-map & opts]
  (let [spec# (set (vals value-map))]
    `(def-exif ~tag-name ~spec# :convert ~value-map ~@opts)))

(def-exif-from-map "AF Mode" {"0" "No"
                              "1" "Single Point"
                              "256" "Zone"
                              "512" "Wide/Tracking"}
  :raw-name (unknown 0x1022)
  :schema {:db/valueType :db.type/string})
(def-exif "Date/Time" inst? :convert parse-exif-date
  :schema {:db/valueType :db.type/instant})
(def-exif "Development Dynamic Range" integer? :convert Integer/parseInt
  :schema {:db/valueType :db.type/number})
(def-exif "Dynamic Range" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Dynamic Range Setting" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Exposure Count" integer?
  :raw-name (unknown 0x1032) :convert Integer/parseInt
  :schema {:db/valueType :db.type/number})
(def-exif "Exposure Mode" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Exposure Program" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Exposure Time" string?  ; e.g. "1/2000 sec"
  :schema {:db/valueType :db.type/string})
(def-exif "F-Number" string? ; e.g. "f/2.8"
  :schema {:db/valueType :db.type/string})
(def-exif "Focal Length" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Focal Length 35" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Focus Mode" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Highlight Tone" integer?
  :raw-name (unknown 0x1041) :convert #(-> % Integer/parseInt - (/ 4))
  :schema {:db/valueType :db.type/number})
(def-exif "Image Count" integer? :convert Integer/parseInt
  :raw-name (unknown 0x1438)
  :schema {:db/valueType :db.type/number})
(def-exif-from-map "Image Generation" {"0" "Original Image"
                                       "1" "Re-developed from RAW"}
  :raw-name (unknown 0x1436)
  :schema {:db/valueType :db.type/string})
(def-exif "ISO Speed Ratings" integer? :convert Integer/parseInt
  :schema {:db/valueType :db.type/number})
(def-exif "Lens Make" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Lens Model" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Make" string? :convert string/trim
  :schema {:db/valueType :db.type/string})
(def-exif "Model" string? :convert string/trim
  :schema {:db/valueType :db.type/string})
(def-exif "Rating" (s/and integer? #(<= 1 % 5))
  :raw-name (unknown 0x1431) :convert Integer/parseInt
  :schema {:db/valueType :db.type/number})
(def-exif "Sequence Number" integer? :convert Integer/parseInt
  :schema {:db/valueType :db.type/number})
(def-exif "Shadow Tone" integer?
  :raw-name (unknown 0x1040) :convert #(-> % Integer/parseInt - (/ 4))
  :schema {:db/valueType :db.type/number})
(def-exif "Sharpness" string?
  :schema {:db/valueType :db.type/string})
(def-exif "Shutter Speed Value" string?  ; APEX exposure value
  :schema {:db/valueType :db.type/string})
(def-exif-from-map "Shutter Type" {"0" "Mechanical", "1" "Electronic"}
  :raw-name (unknown 0x1050)
  :schema {:db/valueType :db.type/string})
(def-exif "Software" string? :convert string/trim  ; camera firmware and version
  :schema {:db/valueType :db.type/string})
(def-exif "Subject Distance Range" string? ; hypothesis: used to indicate macro mode by GR III
  :schema {:db/valueType :db.type/string})

(defn- decode-tag
  "Decodes a single EXIF tag.  Returns nil for unregistered tags."
  [tag]
  (when-let [key (decode-exif-key tag)]
    {key (decode-exif-value tag)}))

(defn- postprocess
  "Decodes the data we want from exif-processor's output."
  [data]
  (->> data
       (into [])
       (s/conform ::exif-tag-list)
       (map decode-tag)
       (remove nil?)
       (apply merge)))

(def exif-for-filename
  "Same as `exif-processor.core/exif-for-filename`,
  but with our very specific postprocessing."
  (comp postprocess processor/exif-for-filename))

(def exif-for-file
  "Same as `exif-processor.core/exif-for-file`,
  but with our very specific postprocessing."
  (comp postprocess processor/exif-for-file))

(def available-tags-for-filename
  "Lists Exif data keys available in a file."
  (comp keys exif-for-filename))

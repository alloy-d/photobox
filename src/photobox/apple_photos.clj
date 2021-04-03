(ns photobox.apple-photos
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [java-time :as t]
            [photobox.archive :as archive]
            [photobox.db :as db]))

(s/def ::id string?)
(s/def ::original-filename string?)
(s/def ::date inst?)
(s/def ::favorite boolean?)

(s/def ::record
  (s/keys :req [::archive/path ::id ::original-filename ::date ::favorite]))

(s/def ::dump
  (s/coll-of ::record))

(db/defschema ::id
  {:db/valueType :db.type/string
   :db/unique :db.unique/value
   :db/cardinality :db.cardinality/one})

(db/defschema ::original-filename
  {:db/valueType :db.type/string
   :db/unique :db.unique/value
   :db/cardinality :db.cardinality/one})

(db/defschema ::favorite
  {:db/valueType :db.type/boolean
   :db/cardinality :db.cardinality/one})

(db/defschema ::date
  {:db/valueType :db.type/instant
   :db/cardinality :db.cardinality/one})

(def json-key->keyword
  {"id" ::id
   "original-filename" ::original-filename
   "date" ::date
   "archive-path" ::archive/path
   "favorite" ::favorite})

(defn normalized-date
  "Oh boy.

  My cameras are always set to UTC-4 (EDT).

  Dates from Apple Photos appear to be camera dates, assumed to be in
  the local timezone of the importing computer, then converted to UTC.

  This reinterprets those dates as UTC-4, assuming that the importing
  computer was in America/New_York.  If the photos get imported in
  another timezone entirely, then all hell breaks loose."
  [apple-photos-date-string]
  (let [;; the zone we assume the date was interpreted in on import:
        assumed-zone (t/zone-id "America/New_York")
        ;; the offset, applied at import, that we can fix:
        fixable-offset (t/zone-offset -5)
        ;; the Photos date, (in UTC):
        apple-photos-date (t/zoned-date-time apple-photos-date-string)
        ;; the Photos date, returned to the assumed zone:
        rezoned-date (t/with-zone-same-instant apple-photos-date assumed-zone)]

    ;; Basically: if the date in Eastern Time would have been in EST
    ;; (UTC-5), then Apple Photos has overadjusted it by an hour.
    ;;
    ;; We'll put it back in UTC-5, subtract an hour, then convert it to
    ;; UTC-4 to rearrive at the original date.
    ;;
    ;; If Eastern Time in that instant was UTC-4, then we'll just put
    ;; the date in UTC-4 and assume that everything's correct. YOLO!
    (if (= fixable-offset (t/zone-offset rezoned-date))
      (t/with-offset
        (t/minus (t/offset-date-time (t/instant rezoned-date) -5)
                 (t/hours 1))
        -4)
      (t/offset-date-time (t/instant rezoned-date) -4))))

(defmulti json-value->value (fn [key _] key))
(defmethod json-value->value :default [_ value] value)
(defmethod json-value->value ::date [_ value]
  (t/java-date (normalized-date value)))

(defn archival-info
  "Produces the expected archive path for an entry."
  [{date ::date original-filename ::original-filename}]
  {::archive/path
   (str
     (t/format "yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-"
               (t/offset-date-time (t/instant date) -4))
     original-filename)})

(defn- load-raw
  "Loads an Apple Photos data dump from `filename`."
  [filename]
  (json/read-str
    (slurp filename)
    :key-fn json-key->keyword
    :value-fn json-value->value))

(defn load-filename
  "Loads and conforms an Apple Photos data dump from `filename`."
  [filename]
  (let [raw (load-raw filename)
        with-archival-info (map #(merge % (archival-info %)) raw)]
    (if (s/valid? ::dump with-archival-info) with-archival-info
      (throw
        (ex-info "Dump file is not valid"
                 (s/explain-data ::dump with-archival-info))))))

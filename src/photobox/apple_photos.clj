(ns photobox.apple-photos
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [taoensso.timbre :as log]
            [datahike.api :as d]
            [java-time :as t]
            [photobox.archive :as archive]
            [photobox.db :as db]
            [photobox.exif :as exif]
            [progrock.core :as pr]))

(s/def ::id string?)
(s/def ::original-filename string?)
(s/def ::date inst?)
(s/def ::favorite boolean?)

(s/def ::record
  (s/keys :req [::id ::original-filename ::date ::archive/path ::favorite]))

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
   "archive-path" ::archive/path
   "original-filename" ::original-filename
   "date" ::date
   "favorite" ::favorite})

(defmulti json-value->value (fn [key _] key))
(defmethod json-value->value :default [_ value] value)
(defmethod json-value->value ::date [_ value]
  (t/java-date (t/zoned-date-time value)))

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
  (let [raw (load-raw filename)]
    (if (s/valid? ::dump raw) raw
      (throw
        (ex-info "Dump file is not valid"
                 (s/explain-data ::dump raw))))))

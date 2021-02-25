(ns photobox.db
  "The DB is a fancy cache of otherwise slow-to-access metadata."
  (:require [datahike.api :as d]
            [photobox.archive :as archive]))

(defonce ^:private registry-ref (atom {}))

(defn schema-for
  "Given a key, returns the datahike schema for that key."
  [key] (@registry-ref key))

(defn full-schema
  "Produces the full DB schema."
  [] (vals @registry-ref))

(defn defschema
  "Defines the schema for `key` as `schema`.

  Merges `schema` into
      {:db/ident key
       :db/cardinality :db.cardinality/one}
  "
  [key schema]
  (let [schema (merge {:db/ident key
                       :db/cardinality :db.cardinality/one}
                      schema)]
    (swap! registry-ref assoc key schema)))

(defschema ::archive/path
  {:db/valueType :db.type/string
   :db/unique :db.unique/identity
   :db/cardinality :db.cardinality/one})

(def db-config
  {:name "photobox"
   :schema-flexibility :write
   :keep-history? true
   :initial-tx [(schema-for ::archive/path)]})

(defn- ensure-db-exists! []
  (when (not (d/database-exists? db-config))
    (d/create-database db-config)))

(defn- update-schema! [conn]
  (d/transact conn (full-schema)))

(defn connect
  "Connects to the DB."
  []
  (ensure-db-exists!)
  (let [conn (d/connect db-config)]
    ;; FIXME: this feels a little weird.
    (update-schema! conn)
    conn))

(defn recreate!
  "Deletes and recreates the DB."
  []
  (when (d/database-exists? db-config)
    (d/delete-database db-config))
  (let [conn (connect)]
    (d/transact conn (full-schema))
    (d/release conn)))

;(when (not (d/database-exists? db-config))
  ;(recreate!))

;(defonce conn (recreate!))
;(comment (def conn (recreate!)))


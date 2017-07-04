(ns photobox.plan
  "Utilities for making a plan and then executing it.

  The point here is to isolate the carrying out of actions
  to more or less one place, so that everything else is pure
  planning that is easier to test and harder to get wrong."
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]))

(defmulti assess
  "Checks if an operation makes sense to carry out.

  Should return an operation that can definitely be done.
  If an operation can't be done, should return an `::impossible` or
  `::noop` operation, with a `:reason` and the `:planned` op."
  :operation)

(defmulti doable?
  "Checks if an operation is doable.  Used for filtering."
  :operation)

(defmulti execute!
  "Performs an operation."
  :operation)


(defn noop
  "Returns a noop operation for the given `planned` op,
  with a given `reason`.
  
  Should be used when a planned operation would have no effect.
  If a planned operation *can't* be carried out, then use `impossible`."
  [planned reason]
  {:operation ::noop
   :reason reason
   :planned planned})

(defmethod doable? ::noop [_] false)

(defn impossible
  "Returns an impossible operation for the given `planned` op,
  with a given `reason`.
  
  Should be used when an operation can't be carried out,
  e.g. because a filesystem isn't mounted."
  [planned reason]
  {:operation ::impossible
   :reason reason
   :planned planned})

(defmethod doable? ::impossible [_] false)


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

(defmethod assess ::copy-file [op]
  (cond (fs/exists? (:dest-file op))
        (noop op "Destination file already exists. Assuming equivalence.")
        
        (not (fs/exists? (:src-file op)))
        (impossible op "Source file does not exist.")
        
        (not (fs/directory?
               (string/join "/" (butlast (fs/split (:dest-file op))))))
        (impossible
          op "Destination is not in an existent directory.")
        
        :else op))

(defmethod doable? ::copy-file [_] true)
(defmethod execute! ::copy-file [op]
  (fs/copy+ (op :src-file) (op :dest-file)))


(defn photocopier
  "Helper: produces a list of operations that will copy photos produced
  by `xf` to `dest-dir`."
  [xf dest-dir]
  (comp xf
        (map (fn [photo-data]
               (copy-to-dir (photo-data :path) dest-dir)))))


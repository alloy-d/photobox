(ns photobox.plan
  "Utilities for making a plan and then executing it.

  A \"plan\" is a map of the form

      {:operation ::some-operation-name
       ; ... operation-specific arguments
      }

  A \"result\" is a map of the form

      {:result :success  ; (or maybe :failure)
       :executed the-plan
      }

  The point here is to isolate the carrying out of actions
  to more or less one place, so that everything else is pure
  planning that is easier to test and harder to get wrong.

  It works like this:

  1. Make a plan.  This should be as pure as possible.
  2. Assess the plan.  This modifies the plan based on relevant state,
     but does not have any side effects of its own.
  3. Execute the plan.  This is where all the side effects happen.

  ---

  This is all implemented using multimethods.
  The contract of those methods is something like this:

  - Every operation must have a corresponding implementation of the
    `assess` method.
  - Any operation returned by the `assess` method must have an
    implementation of the `doable?` method.
  - Any operation for which `doable?` is true must have an implementation
    of the `execute!` method.

  ---

  The assessment phase is intended to prevent as many ineffective or
  impossible plans as possible, but does not replace error handling
  in the execution phase.  This was a philosophical choice: as much
  as possible, I'd rather have an accurate-ish idea of what will
  _probably_ happen beforehand than an exact accounting of what went
  wrong once we have tried and failed.

  The assumption, of course, is that the world won't change too much
  between the assessment and execution phases.  This could result in
  some TOCTTOU errors."
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
  "Performs an operation.  Should return a result."
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

(defn result
  "Returns a result of type `type` for a given `op`,
  including optional `details`."
  ([type op]
   (result type op nil))

  ([type op details]
   (let [res {:type type
              :executed op}]
     (if details
       (assoc res :details details)
       res))))

(defmacro do-a-thing
  "Does a thing and returns a result.

  Wraps the thing in very rudimentary error handling."
  [op & thing-body]
  `(try
     (do
       ~@thing-body
       (result :success ~op))
     (catch Exception e#
       (result :failure ~op
               (str "Exception: " (.getClass e#) ": " (.getMessage e#))))))

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
  (do-a-thing op
              (fs/copy+ (op :src-file) (op :dest-file))))

(defn photocopier
  "Helper: produces a list of operations that will copy photos produced
  by `xf` to `dest-dir`."
  [xf dest-dir]
  (comp xf
        (map (fn [photo-data]
               (copy-to-dir (photo-data :path) dest-dir)))))


(defn archive
  "Plans to copy a file to an archival destination.

  Very similar to `copy`, except that this checks for the existence
  of `root`, and then creates directories as needed to put `path`
  under `root`.

  After assessment, this becomes a plain copy operation.
  If the archive root were to disappear between assessment
  and execution, that would be bad."
  [src-file archive-root archival-path]
  {:operation ::archive
   :archive-root archive-root
   :archival-path archival-path
   :src-file src-file})

(defmethod assess ::archive [op]
  (cond (not (fs/directory? (op :archive-root)))
        (impossible
          op "Archive root does not exist.")

        :else
        (assess (copy (op :src-file)
                      (str (op :archive-root) "/" (op :archival-path))))))


(defn delete
  "Returns a plan to delete a file."
  [file]
  {:operation ::delete-file
   :file file})

(defmethod assess ::delete-file [op]
  (cond (not (fs/exists? (:file op)))
        (noop op "Target file does not exist.")

        (fs/directory? (:file op))
        (impossible op "Target file is a directory.")

        :else op))

(defmethod doable? ::delete-file [_] true)
(defmethod execute! ::delete-file [op]
  (do-a-thing op
              (fs/delete (:file op))))

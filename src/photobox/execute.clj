(ns photobox.execute
  "Utilities for executing many plans."

  (:require [photobox.plan :as plan]))

(def final-plans
  "Produces all the plans that would be carried out, after
  assessing and filtering out what's not doable."
  (comp (map plan/assess) (filter plan/doable?)))

(defn execute-all!
  "Executes all the plans, assuming that they are all doable."
  [doable-plans]
  (map plan/execute! doable-plans))

(defn finalize-and-execute!
  "Finalizes the given `plans` and executes those that are doable."
  [plans]
  (let [start-time (java.lang.System/nanoTime)
        assessed (map plan/assess plans)
        not-doable (filter (comp not plan/doable?) assessed)
        results (into []
                      (comp (filter plan/doable?) (map plan/execute!))
                      assessed)]
    {:skipped not-doable
     :results results
     :elapsed (double (/ (- (java.lang.System/nanoTime) start-time)
                         1000000))}))

(defn- group-and-process
  "Like `group-by`, plus a processing function on the grouped results."
  [process group coll]
  (apply merge (map (fn [[k items]] {k (process items)})
                    (group-by group coll))))

(def ^:private group-and-count (partial group-and-process count))

(defn summarize-execution
  "Produces a summary of results that would be helpful to a human."
  [result]
  (let [skipped (:skipped result)
        results (:results result)]
    {:elapsed (:elapsed result)
     :skipped {:count (count skipped)
               :types (group-and-count :operation skipped)}
     :results {:count (count results)
               :types (group-and-process
                        (partial group-and-count (comp :operation :executed))
                        :type results)}}))

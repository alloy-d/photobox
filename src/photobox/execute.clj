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
  (into [] (comp final-plans (map plan/execute!)) plans))

(ns photobox.runner
  (:require [photobox.core :as core]
            [photobox.execute :as execute]
            [photobox.plan :as plan]
            [clojure.java.io :refer (writer)]
            [clojure.pprint :refer (pprint)]
            [java-time :as t]
            [me.raynes.fs :as fs]))

(def photobox-dir "~/.photobox.d")

(defn- ensure-directory!
  "Make sure photobox's ~/.photobox.d/ exists."
  []
  (let [expanded (fs/expand-home photobox-dir)]
    (when-not (or (fs/directory? expanded)
                  (fs/exists? expanded))
      (fs/mkdir expanded))))

(defn show-plan
  "Print what photobox would like to do with the available photos."
  [& _]
  (pprint (core/plan)))

(defn show-assessed-plan
  "Print what photobox can actually do with the available photos."
  [& _]
  (pprint (map plan/assess (core/plan))))

(defn show-process-plan
  "Print exactly what photobox would do with the available photos."
  [& _]
  (pprint (into [] execute/final-plans (core/plan))))

(defn process-photos
  "Process the available photos."
  [& _]
  (let [start-time (t/zoned-date-time)
        results-name (fs/expand-home (str photobox-dir "/results-" (t/format :iso-offset-date-time start-time) ".edn"))
        results (assoc (core/process)
                       :start (t/format :iso-date-time start-time))]
    (ensure-directory!)
    (with-open [results-file (writer results-name)]
      (pprint results results-file))
    (pprint (assoc (execute/summarize-execution results)
                   :results-file results-name))))

(defn -main [] (process-photos))

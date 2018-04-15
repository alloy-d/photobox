(ns photobox.metadata.video
  "Hacks for reading video metadata."

  (:require [clojure.java.shell :refer (sh)]
            [clojure.string :refer (split split-lines)]))

(defn date-time-for-filename
  "Uses exiftool to extract the original date and time for a file."
  [filename]
  (let [output (:out (sh "exiftool" "-csv" filename "-DateTimeOriginal"))
        data-line (second (split-lines output))]
    (second (split data-line #","))))

(ns photobox.metadata.core
  "General utilities for working with metadata."

  (:require [java-time :as t]))

(defn parse-exif-date
  "Parses an Exif date into something reasonable."
  [exif-date]
  (t/java-date
    (t/offset-date-time
      (t/local-date-time "yyyy:MM:dd HH:mm:ss" exif-date)
      ;; My cameras are always set to UTC-4 (EDT).
      (t/zone-offset -4))))

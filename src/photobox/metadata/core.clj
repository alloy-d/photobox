(ns photobox.metadata.core
  "General utilities for working with metadata."

  (:require [java-time :as t]))

(defn zonify-date
  "Takes some date thing and makes it UTC-4 (EDT)."
  [date-thing]
  (t/offset-date-time
    date-thing
    ;; My cameras are always set to UTC-4 (EDT).
    (t/zone-offset -4)))

(defn exif-date->date-time
  "Parses an EXIF date into something reasonable."
  [exif-date]
  (->> exif-date
       (t/local-date-time "yyyy:MM:dd HH:mm:ss")
       zonify-date))

(defn exif-date->java-date
  "Parses an Exif date into something reasonable but then returns it as
  a Java Date."
  [exif-date]
  (->> exif-date
       exif-date->date-time
       t/java-date))

(ns photobox.metadata.core
  "General utilities for working with metadata."
  
  (:require [java-time :as t]))

(defn parse-exif-date
  "Parses an Exif date into something reasonable."
  [exif-date]
  (t/local-date-time "yyyy:MM:dd HH:mm:ss" exif-date))


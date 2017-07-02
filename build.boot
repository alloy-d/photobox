;; vim: ft=clojure
(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/core.async "0.3.443"]
                  [io.joshmiller/exif-processor "0.2.0"]
                  [me.raynes/fs "1.4.6"]])

(require '[photobox.core :as photobox])
(deftask process-photos
  "Import photos using photobox."
  []
  (photobox/do-things))

;; vim: ft=clojure
(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/core.async "0.3.443"]
                  [io.joshmiller/exif-processor "0.2.0"]
                  [me.raynes/fs "1.4.6"]
                  [tolitius/boot-check "0.1.4"]])

(require '[tolitius.boot-check :as check])
(deftask check-sources []
  (set-env! :source-paths #{"src" "test"})
  (comp
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed)))

(require '[photobox.core :as photobox])
(deftask process-photos
  "Import photos using photobox."
  []
  (photobox/do-things))

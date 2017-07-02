;; vim: ft=clojure
(require '[boot.git :as git])

(def project-name 'photobox)
(def project-version
  ;; TODO: use git/describe once I've actually tagged something.
  (let [commit (git/last-commit)]
    (str (subs commit 0 8) (when (git/dirty?) "-dirty"))))
(def project-description
  "Imports and organizes photos.")

(set-env!
  :source-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.8.0"]
                  [org.clojure/core.async "0.3.443"]
                  [io.joshmiller/exif-processor "0.2.0"]
                  [it.frbracch/boot-marginalia "0.1.3-1" :scope "test"]
                  [me.raynes/fs "1.4.6"]
                  [tolitius/boot-check "0.1.4" :scope "test"]]
  :exclusions '[org.clojure/clojure])

(deftask check-sources []
  (require 'tolitius.boot-check)
  (let [eastwood (resolve 'tolitius.boot-check/with-eastwood)
        kibit (resolve 'tolitius.boot-check/with-kibit)
        bikeshed (resolve 'tolitius.boot-check/with-bikeshed)]
    (set-env! :source-paths #{"src" "test"})
    (comp
      (eastwood)
      (kibit)
      (bikeshed))))

(deftask docs
  "Generate documentation using Marginalia."
  []
  (require 'it.frbracch.boot-marginalia)
  (let [marginalia (resolve 'it.frbracch.boot-marginalia/marginalia)]
    (comp
      (marginalia
        :name project-name
        :version project-version
        :desc project-description)
      (target))))

(deftask process-photos
  "Import photos using photobox."
  []
  (require 'photobox.core)
  (let [do-things (resolve 'photobox.core/do-things)]
    (comp
      (notify :visual true :title "Photobox")
      (fn [_] (fn [_] (do-things))))))

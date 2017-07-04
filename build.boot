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
                  [org.clojure/test.check "0.9.0" :scope "test"]
                  [adzerk/boot-test "1.2.0" :scope "test"]
                  [clojure.java-time "0.3.0"]
                  [io.joshmiller/exif-processor "0.2.0"]
                  [it.frbracch/boot-marginalia "0.1.3-1" :scope "test"]
                  [me.raynes/fs "1.4.6"]
                  [tolitius/boot-check "0.1.4" :scope "test"]]
  :exclusions '[org.clojure/clojure])

(deftask run-tests []
  (set-env! :source-paths #(conj % "test"))
  (require 'adzerk.boot-test)
  (let [test (resolve 'adzerk.boot-test/test)]
    (test)))

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

(deftask show-plan
  "See all the things photobox would do with the available photos."
  []
  (require 'photobox.core 'clojure.pprint)
  (let [plan (resolve 'photobox.core/plan)
        print-table (resolve 'clojure.pprint/print-table)]
    (comp
      (notify :visual true :title "Photobox")
      (fn [_] (fn [_] (print-table (plan)))))))

(deftask show-assessed-plan
  "See what photobox can actually do with the available photos."
  []
  (require 'photobox.core 'photobox.plan 'clojure.pprint)
  (let [plan (resolve 'photobox.core/plan)
        assess (resolve 'photobox.plan/assess)
        print-table (resolve 'clojure.pprint/print-table)]
    (fn [_] (fn [_] (print-table (map assess (plan)))))))

(deftask show-process-plan
  "See exactly what photobox would do with the available photos."
  []
  (require 'photobox.core 'photobox.plan 'photobox.execute 'clojure.pprint)
  (let [plan (resolve 'photobox.core/plan)
        final-plans (resolve 'photobox.execute/final-plans)
        pprint (resolve 'clojure.pprint/pprint)]
    (fn [_]
      (fn [_]
        (pprint (into [] final-plans (plan)))))))

(deftask process-photos
  "Process the available photos."
  []
  (require 'photobox.core 'clojure.pprint)
  (let [process (resolve 'photobox.core/process)
        pprint (resolve 'clojure.pprint/pprint)]
    (comp
      (notify :visual true :title "Photobox")
      (fn [_] (fn [_] (pprint (process)))))))

(ns photobox.core
  (:require [java-time :as t]
            [me.raynes.fs :as fs]
            [photobox.archive :as archive]
            [photobox.exif :as exif]
            [photobox.metadata.core :as metadata]
            [photobox.metadata.video :as video-metadata]
            [photobox.fs :refer (find-photos find-videos sort-by-extension)]
            [photobox.plan :as plan]
            [photobox.execute :as execute]))

(defn- get-rating [capture-data]
  ((capture-data :exif-data) :photobox.exif/rating -1))
(defn- get-date [capture-data]
  ((capture-data :exif-data) :photobox.exif/date-time))

(defn archival-path
  "Returns a path with the following directory structure:
  `yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-(original filename)`."
  [capture-data]
  (let [creation-date (get-date capture-data)]
    (str
      (t/format "yyyy/yyyy-MM/yyyy-MM-dd/yyyyMMdd-" creation-date)
      (fs/base-name (capture-data :path)))))

(def archival-process
  (map #(plan/archive (:path %) (archive/root (:type %)) (archival-path %))))

(def xt2-process
  "This is the process I use for photos from my Fujifilm X-T2.

  I use the on-camera rating system to encode what I'd like done
  with some photos.  Based on those ratings, this process sorts
  photos into directories on my desktop before archiving everything.

  It then forcibly re-archives anything with a rating, to ensure
  that rating data makes it into the archive."

  (let [good-capture-destination-dir (fs/expand-home "~/images/good-photos/")
        great-capture-destination-dir (fs/expand-home "~/images/great-photos/")
        notes-capture-destination-dir (fs/expand-home "~/images/photos-to-note/")
        fuji-review-dir (fs/expand-home "~/images/fuji-review/")]

    ;; Do some special handling for photos that I rated on-camera...)
    [(plan/photocopier (filter #(> (get-rating %) 3)) good-capture-destination-dir)
     (plan/photocopier (filter #(= (get-rating %) 5)) great-capture-destination-dir)
     (plan/photocopier (filter #(= (get-rating %) 3)) notes-capture-destination-dir)

     (plan/photocopier (map identity) fuji-review-dir)

     ;; ...then do the usual archival process...
     archival-process

     ;; ...then re-archive anything that was rated, in case I've rated
     ;; some files since they were first archived.
     (comp (filter #(>= (get-rating %) 1))
           (map #(assoc (plan/archive (:path %) (archive/root (:type %)) (archival-path %)) :overwrite true)))]))

(def gr-iii-process
  "This is the process I use for photos from my RICOH GR III.

  I put everything in a review directory.
  Then I archive everything."

  (let [ricoh-review-dir (fs/expand-home "~/images/ricoh-review/")]
     [(plan/photocopier (map identity) ricoh-review-dir)

     archival-process]))

(comment
  ;; Example usage: clean out files before a certain point.
  ;; This obviously shouldn't live here; just keeping it around
  ;; until it has a better place to go.
  (def transductions
    [(comp
       (filter #(t/before? (get-date %)
                           (t/local-date-time "2017-07-03T16:18:16")))
       (map #(plan/delete (:path %))))]))

(defmulti info-for-file :type)
(defmethod info-for-file :photo [{:keys [file]}]
  (try
    (let [exif-data (exif/exif-for-file file)
          file-path (.getAbsolutePath file)]
      {:type :photo
       :path file-path
       :exif-data exif-data})
    (catch Exception ex
           (println (str "Error handling " file))
           (throw ex))))
(defmethod info-for-file :video [{:keys [file]}]
  (let [file-path (.getAbsolutePath file)
        date-time (metadata/parse-exif-date (video-metadata/date-time-for-filename file-path))]
    {:type :video
     :path file-path
     :exif-data {:photobox.exif/date-time date-time}}))


(def processes
  ({"Linux" [{:src "/run/media/awl/disk"
              :process xt2-process}
             {:src "/run/media/awl/RICOH GR"
              :process gr-iii-process}]
    "Mac OS X" [{:src "/Volumes/Untitled 1"
                 :process [archival-process]}
                {:src "/Volumes/RICOH GR"
                 :process [archival-process]}]}
   (System/getProperty "os.name")))

(defn files-for-source [source]
  (let [photo-files (sort-by-extension (find-photos source))
        video-files (find-videos source)
        classifier (fn [type]
                     (fn [file] {:type type :file file}))]
    (concat (map (classifier :photo) photo-files)
            (map (classifier :video) video-files))))

(defn plan-single-process [{:keys [src process]}]
  (let [files-data (map info-for-file (files-for-source src))]
    (mapcat #(into [] % files-data) process)))

(defn plan []
  (mapcat plan-single-process processes))

(defn process []
  (execute/finalize-and-execute! (plan)))

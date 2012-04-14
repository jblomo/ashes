(ns ashes.briss
  "Wrapper around the functionality we'd like from Briss
  http://sourceforge.net/projects/briss/
  "
  (:import [java.awt.image BufferedImage]
           [at.laborg.briss.exception CropException]
           [at.laborg.briss.model ClusterDefinition CropDefinition CropFinder PageCluster]
           [at.laborg.briss.utils BrissFileHandling ClusterCreator ClusterRenderWorker DocumentCropper]
           [com.itextpdf.text DocumentException]))

(defn- Float-array
  "Array of boxed Floats"
  [fs]
  (->> fs (map #(Float. %)) into-array))

(defn split!
  "Takes a src PDF file and writes a dst PDF file that is cropped and split.
  Used for two column PDFs so that e-readers can page through one column at a
  time"
  [src dst]
  (let [clusterDefinition (ClusterCreator/clusterPages src nil)
        cRW (doto (ClusterRenderWorker. src clusterDefinition)
              (.start))]

    ; rednering clusters
    (while (.isAlive cRW)
      (Thread/sleep 500))

    ; setup crop rectangles
    (doseq [cluster (.getClusterList clusterDefinition)]
      (let [image (.. cluster getImageData getPreviewImage)
            middle-margin 0.0
            auto (CropFinder/getAutoCropFloats image)
            ; split auto crop in half
            left (Float-array [(get auto 0)
                               (get auto 1)
                               (+ 0.5 middle-margin)
                               (get auto 3)])
            right (Float-array [(+ 0.5 middle-margin)
                                (get auto 1)
                                (get auto 2)
                                (get auto 3)])]
        (.addRatios cluster left)
        (.addRatios cluster right)))

    ;crop files
    (let [cropDefintion (CropDefinition/createCropDefinition src dst clusterDefinition)]
      (DocumentCropper/crop cropDefintion))))

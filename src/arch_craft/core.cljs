(ns ^:figwheel-always arch-craft.core
    (:require))

(def THREE js/THREE)

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

(defonce app-state (atom {:text "Hello world!"}))

(def app-element (.getElementById js/document "app"))
(def aspect (/ (.-innerWidth js/window) (.-innerHeight js/window)))
(def camera-distance 50)
(def camera
  (THREE.OrthographicCamera.
    (* aspect (- camera-distance))
    (* aspect camera-distance)
    camera-distance
    (- camera-distance)
    1
    1000))

(def scene (THREE.Scene.))
(def raycaster (THREE.Raycaster.))
(def mouse (THREE.Vector2.))

(def renderer (THREE.WebGLRenderer. (js-obj "antialias" false)))
(.setPixelRatio renderer (.-devicePixelRatio js/window))
(.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))

(.set (.-position camera) 100 100 100)  ;all three must be equal
(def geometry (THREE.BoxGeometry. 10 10 10))
(def material (THREE.MeshNormalMaterial.))
(def mesh (THREE.Mesh. geometry material))

(defn new-space [] (THREE.Mesh.
  (THREE.PlaneGeometry. 10 10 1 1)
  (THREE.MeshBasicMaterial. (js-obj "wireframe" false "transparent" true "opacity" 0.5))))

; rotate grid to be flat
;(def grid (THREE.Group.))
(def grid (THREE.Mesh.
  (THREE.PlaneGeometry. 100 100 10 10)
  (THREE.MeshBasicMaterial. (js-obj "wireframe" false "transparent" true "opacity" 0.5))))

(set! (.. grid -rotation -x) -1.57)
(set! (.. grid -rotation -y) -1.57)
(set! (.. grid -rotation -order) "YXZ")
;(for [row (range 10) col (range 10)]
;  (let [grid-item (new-space)]
;    (set! (.. grid-item -position -x) (+ (.. grid-item -position -x) row))
;    (set! (.. grid-item -position -y) (+ (.. grid-item -position -y) col)))))

(.add scene mesh)
(.add scene (THREE.AmbientLight. 0x444444))
(.add scene (THREE.AxisHelper. 40))
(.add scene grid)

(.appendChild app-element (.-domElement renderer))

(.lookAt camera (.-position scene))

(defn on-mousemove [event]
  (.preventDefault event)
  (set! (.-x mouse)
    (- (* (/ (.-clientX event) (.-innerWidth js/window)) 2) 1))
  (set! (.-y mouse)
    (+ (* (/ (.-clientY event) (.-innerHeight js/window)) -2) 1))
  (.setFromCamera raycaster mouse camera)
  (let [intersection (nth (.intersectObjects raycaster (.-children scene)) 0)]
    (println intersection)
    (let [face (.-face intersection)]
      (.set (.-color face) (THREE.Color. 0xf2b640)))))

(defn render []
  ;(set! (.-y (.-rotation scene)) (+ (.-y (.-rotation scene)) 0.0005))
  ;(.setFromCamera raycaster mouse camera)
  (.render renderer scene camera))

(defn animate []
  (.requestAnimationFrame js/window animate)
  (render))

(.addEventListener js/document "mousemove" on-mousemove)
(animate)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  ; this is a smell, make the setup idompotent
  (.removeChild app-element (.item (.-children (.getElementById js/document "app")) 0))
  (.removeEventListener js/document "mousemove" on-mousemove)
)

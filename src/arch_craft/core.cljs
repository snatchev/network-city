(ns ^:figwheel-always arch-craft.core
    (:require))

(def THREE js/THREE)

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

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

(def renderer (THREE.WebGLRenderer. (js-obj "antialias" false)))
(.setPixelRatio renderer (.-devicePixelRatio js/window))
(.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))

(.set (.-position camera) 100 100 100)  ;all three must be equal
(def geometry (THREE.BoxGeometry. 10 10 10))
(def material (THREE.MeshNormalMaterial.))
(def mesh (THREE.Mesh. geometry material))

(def grid (THREE.Mesh.
  (THREE.PlaneGeometry. 100 100 10 10)
  (THREE.MeshBasicMaterial. (js-obj "wireframe" true "transparent" true "opacity" 0.5))))

; rotate grid to be flat
(set! (.. grid -rotation -x) -1.57)
(set! (.. grid -rotation -y) -1.57)
(set! (.. grid -rotation -order) "YXZ")

(.add scene mesh)
(.add scene (THREE.AmbientLight. 0x444444))
(.add scene (THREE.AxisHelper. 40))
(.add scene grid)

(.appendChild app-element (.-domElement renderer))

(.lookAt camera (.-position scene))

(defn render []
  ;(set! (.-y (.-rotation scene)) (+ (.-y (.-rotation scene)) 0.0005))
  (.render renderer scene camera))

(defn animate []
  (.requestAnimationFrame js/window animate)
  (render))

(animate)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  ; delete the render element
  (.removeChild app-element (.item (.-children (.getElementById js/document "app")) 0))
)

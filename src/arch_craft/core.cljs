(ns ^:figwheel-always arch-craft.core
    (:require))

(def THREE js/THREE)

(enable-console-print!)

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
(def camera-target (THREE.Object3D.))

(def scene (THREE.Scene.))
;(.add scene camera)
(def raycaster (THREE.Raycaster.))
(def mouse (THREE.Vector2.))

(def renderer (THREE.WebGLRenderer. (js-obj "antialias" false)))
(.setPixelRatio renderer (.-devicePixelRatio js/window))
(.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))

(.set (.-position camera) 100 100 100)
(.add camera-target camera)
(.add camera-target (THREE.AxisHelper. 8))
(.add scene camera-target)

(def cursor (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial. (js-obj "transparent" true "opacity" 0.5))))

(defonce app-state (atom {
  :active-tool 'insert-model,
  :active-cursor nil,
  :objects (list),
  }))

(defn add-object [object & {:keys [interactive]}]
  (.add scene object)
  (if interactive
    (swap! app-state assoc :objects (conj (@app-state :objects) object))))

(def grid (THREE.Geometry.))

(let [size 100 step 10 vert (.-vertices grid)]
  (doseq [idx (range (- size) size step)]
    (.push vert (THREE.Vector3. (- size) 0 idx))
    (.push vert (THREE.Vector3. size 0 idx))
    (.push vert (THREE.Vector3. idx 0 (- size)))
    (.push vert (THREE.Vector3. idx 0 size))))

(add-object (THREE.Line. grid
              (THREE.LineBasicMaterial. (js-obj "transparent" true "color" 0xFFFFFF))
               THREE.LinePieces))

(def planebuffer (THREE.PlaneBufferGeometry. 1000 1000))
(aset planebuffer "visible" false)
(.applyMatrix planebuffer (.makeRotationX (THREE.Matrix4.) -1.57079633))

(add-object (THREE.Mesh. planebuffer (THREE.MeshBasicMaterial. (js-obj "color" 0x333333))) :interactive true)
(add-object cursor)
(add-object (THREE.AmbientLight. 0x444444))
;(.add scene (THREE.AxisHelper. 40))

(.appendChild app-element (.-domElement renderer))

(.lookAt camera (THREE.Vector3. 0 0 0))
;(.lookAt camera camera-target)

(defn snap-to-grid [mesh intersection]
  (if (.-face intersection) ;only snap to things that have faces.
    (-> (.-position mesh)
        (.copy (.-point intersection))
        (.add (.. intersection -face -normal))
        (.divideScalar 10)
        (.floor)
        (.multiplyScalar 10)
        (.addScalar 5))))

;conversion functions
(defn deg->rad [deg]
  (* deg (/ js/Math.PI 180.0)))

(defn screen->space [vector x y]
  (aset vector "x" (-> x
                      (/ (.-innerWidth js/window))
                      (* 2)
                      (- 1)))
  (aset vector "y" (-> y
                      (/ (.-innerHeight js/window))
                      (* -2)
                      (+ 1))))
; Actions
(defn insert-model [model]
  (let [intersections (.intersectObjects raycaster (clj->js (@app-state :objects)))]
    (if-not (empty? intersections)
      (let [model (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial.))]
        (add-object model)
        (snap-to-grid model (first intersections))))))

; Camera movement
(defn rotate-camera [camera deg]
  (let [rotation (.-rotation camera)]
    (aset rotation "y" (+ (deg->rad deg) (.-y rotation) ))))
(defn rotate-camera-cw []
  (rotate-camera camera-target 10))
(defn rotate-camera-ccw []
  (rotate-camera camera-target -10))

(def pan-camera-damper 0.1)
(defn pan-camera [delta-x delta-y] ;pan in pixel space
  (let [position (.-position camera-target) offset (THREE.Vector3.) y-axis (THREE.Vector3. 0 1 0)]
    (.add position (-> offset
                       (.set delta-x 0 delta-y)
                       (.multiplyScalar pan-camera-damper)
                       (.applyAxisAngle y-axis (deg->rad 45))))))

; Event Handling
(defn on-windowresize [event]
  (let [width (.-innerWidth js/window) height (.-innerHeight js/window)]
    (aset camera "aspect" (/ width height))
    (.updateProjectionMatrix camera)
    (.setSize renderer width height)))

(defn on-mousemove [event]
  (.preventDefault event)
  (screen->space mouse (.-clientX event) (.-clientY event))
  (.setFromCamera raycaster mouse camera)
  (let [intersections (.intersectObjects raycaster (clj->js (@app-state :objects)))]
    (if-not (empty? intersections)
      (snap-to-grid cursor (first intersections)))))

; look into window.WheelEvent API which is the standard
(defn on-mousewheel [event]
  (.preventDefault event)
  (pan-camera (.-wheelDeltaX event) (.-wheelDeltaY event)))

(defn on-click [event]
  (.preventDefault event)
  (let [click-coord (THREE.Vector2.)]
    (screen->space click-coord (.-clientX event) (.-clientY event))
    (.setFromCamera raycaster click-coord camera)
    (insert-model (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial.)))))

(defn on-keyup [event]
  (let [code (.-keyCode event)]
    (case code
      81 (rotate-camera-cw)  ;q
      69 (rotate-camera-ccw) ;e
      :default)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  ; this is a smell, make the setup idompotent
  (.removeChild app-element (.item (.-children (.getElementById js/document "app")) 0))
  (.removeEventListener js/document "click" on-click)
  (.removeEventListener js/document "mousemove" on-mousemove)
  (.removeEventListener js/document "mousewheel" on-mousemove)
  (.removeEventListener js/document "keyup" on-keyup)
  (.removeEventListener js/document "resize" on-mousemove))

(.addEventListener js/document "click" on-click false)
(.addEventListener js/document "mousemove" on-mousemove false)
(.addEventListener js/document "mousewheel" on-mousewheel false)
(.addEventListener js/document "keyup" on-keyup false)
(.addEventListener js/document "resize" on-windowresize false)

; Rendering
(defn render []
  (.render renderer scene camera))

(defn animate []
  ;limit FPS:
  (.setTimeout js/window #(.requestAnimationFrame js/window animate) (/ 1000 30))
  ;(.requestAnimationFrame js/window animate)
  (render))

(animate)

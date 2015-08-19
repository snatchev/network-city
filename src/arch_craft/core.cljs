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

(def scene (THREE.Scene.))
(def raycaster (THREE.Raycaster.))
(def mouse (THREE.Vector2.))
(def screen-coords (THREE.Vector2.))
(def scroll-pos (THREE.Vector2.))

(def renderer (THREE.WebGLRenderer. (js-obj "antialias" false)))
(.setPixelRatio renderer (.-devicePixelRatio js/window))
(.setSize renderer (.-innerWidth js/window) (.-innerHeight js/window))

(.set (.-position camera) 100 100 100)  ;all three must be equal
(def mesh (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial.)))
(def cursor (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial. (js-obj "transparent" true "opacity" 0.5))))

(defonce app-state (atom {:active-cursor nil, :scroll-pos scroll-pos, :objects (list)}))

(defn add-object [object & {:keys [interactive]}]
  (.add scene object)
  (if interactive
    (swap! app-state assoc :objects (conj (@app-state :objects) object))))

;move it up so that it stays on the baseline
(.applyMatrix mesh (.makeTranslation (THREE.Matrix4.) 0 10 0))

(def grid (THREE.Geometry.))

(let [size 100 step 10 vert (.-vertices grid)]
  (doseq [idx (range (- size) size step)]
    (.push vert (THREE.Vector3. (- size) 0 idx))
    (.push vert (THREE.Vector3. size 0 idx))
    (.push vert (THREE.Vector3. idx 0 (- size)))
    (.push vert (THREE.Vector3. idx 0 size))))

(.add scene
  (THREE.Line. grid
              (THREE.LineBasicMaterial. (js-obj "transparent" true "color" 0xFFFFFF))
               THREE.LinePieces))

(def planebuffer (THREE.PlaneBufferGeometry. 1000 1000))
(aset planebuffer "visible" false)
(.applyMatrix planebuffer (.makeRotationX (THREE.Matrix4.) -1.57079633))


(add-object (THREE.Mesh. planebuffer) :interactive true)
(add-object mesh :interactive true)
(add-object cursor)
(add-object (THREE.AmbientLight. 0x444444))
;(.add scene (THREE.AxisHelper. 40))

(.appendChild app-element (.-domElement renderer))

(set! scroll-pos (.-position scene))
(.lookAt camera scroll-pos)

(defn snap-to-grid [mesh intersection]
  (.log js/console intersection)
  (if (.-face intersection) ;only snap to things that have faces.
    (-> (.-position mesh)
        (.copy (.-point intersection))
        (.add (.. intersection -face -normal))
        (.divideScalar 10)
        (.floor)
        (.multiplyScalar 10)
        (.addScalar 5))))

(defn intersect-has-face? [intersection]
  (not= (.-face intersection) nil))

; (defn reset-grid-state! []
;   (let [grid-items (.-children grid)]
;     (doseq [grid-item (js->clj grid-items)]
;       (aset grid-item "material" default-grid-material))))

(defn on-windowresize [event]
  (let [width (.-innerWidth js/window) height (.-innerHeight js/window)]
    (aset camera "aspect" (/ width height))
    (.updateProjectionMatrix camera)
    (.setSize renderer width height)))

(defn on-mousemove [event]
  (.preventDefault event)
  (aset mouse "x"
    (- (* (/ (.-clientX event) (.-innerWidth js/window)) 2) 1))
  (aset mouse "y"
    (+ (* (/ (.-clientY event) (.-innerHeight js/window)) -2) 1))
  (.setFromCamera raycaster mouse camera)
  (println (@app-state :objects))
  (let [intersections (.intersectObjects raycaster (clj->js (@app-state :objects)))]
    (if-not (empty? intersections)
      (snap-to-grid cursor (first intersections)))))

  ;(snap-to-grid cursor mouse)

; look into window.WheelEvent API which is the standard
(defn on-mousewheel [event]
  (.preventDefault event)
  (aset scroll-pos "x" (+ (/ event.wheelDeltaX 10.0) (.-x scroll-pos)))
  (aset scroll-pos "y" (+ (/ event.wheelDeltaY 10.0) (.-y scroll-pos))))
  ;(swap! app-state assoc :screen-pos (.-wheelDeltaX event))

(defn render []
  (.render renderer scene camera))

(defn animate []
  ;limit FPS:
  (.setTimeout js/window #(.requestAnimationFrame js/window animate) (/ 1000 30))
  ;(.requestAnimationFrame js/window animate)
  (render))

(.addEventListener js/document "mousemove" on-mousemove false)
(.addEventListener js/document "mousewheel" on-mousewheel false)
(.addEventListener js/document "resize" on-windowresize false)
(animate)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  ; this is a smell, make the setup idompotent
  (.removeChild app-element (.item (.-children (.getElementById js/document "app")) 0))
  (.removeEventListener js/document "mousemove" on-mousemove)
)

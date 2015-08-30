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

(add-object (THREE.Line. grid
              (THREE.LineBasicMaterial. (js-obj "transparent" true "color" 0xFFFFFF))
               THREE.LinePieces))

(def planebuffer (THREE.PlaneBufferGeometry. 1000 1000))
(aset planebuffer "visible" false)
(.applyMatrix planebuffer (.makeRotationX (THREE.Matrix4.) -1.57079633))

(add-object (THREE.Mesh. planebuffer (THREE.MeshBasicMaterial. (js-obj "color" 0x333333))) :interactive true)
(add-object cursor)
(add-object mesh :interactive false)
(add-object (THREE.AmbientLight. 0x444444))
;(.add scene (THREE.AxisHelper. 40))

(.appendChild app-element (.-domElement renderer))

(set! scroll-pos (.-position scene))
(.lookAt camera scroll-pos)

(defn snap-to-grid [mesh intersection]
  (if (.-face intersection) ;only snap to things that have faces.
    (-> (.-position mesh)
        (.copy (.-point intersection))
        (.add (.. intersection -face -normal))
        (.divideScalar 10)
        (.floor)
        (.multiplyScalar 10)
        (.addScalar 5))))

; Event Handling
(defn on-windowresize [event]
  (let [width (.-innerWidth js/window) height (.-innerHeight js/window)]
    (aset camera "aspect" (/ width height))
    (.updateProjectionMatrix camera)
    (.setSize renderer width height)))

(defn screen->space [vector x y]
  (aset vector "x" (-> x
                      (/ (.-innerWidth js/window))
                      (* 2)
                      (- 1)))
  (aset vector "y" (-> y
                      (/ (.-innerHeight js/window))
                      (* -2)
                      (+ 1))))

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
  (aset scroll-pos "x" (+ (/ event.wheelDeltaX 10.0) (.-x scroll-pos)))
  (aset scroll-pos "y" (+ (/ event.wheelDeltaY 10.0) (.-y scroll-pos))))

(defn on-click [event]
  (.preventDefault event)
  (let [click-coord (THREE.Vector2.)]
    (screen->space click-coord (.-clientX event) (.-clientY event))
    (.setFromCamera raycaster click-coord camera)
    (let [intersections (.intersectObjects raycaster (clj->js (@app-state :objects)))]
      (if-not (empty? intersections)
        (let [model (THREE.Mesh. (THREE.BoxGeometry. 10 10 10) (THREE.MeshNormalMaterial.))]
          (add-object model)
          (snap-to-grid model (first intersections)))))))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)

  ; this is a smell, make the setup idompotent
  (.removeChild app-element (.item (.-children (.getElementById js/document "app")) 0))
  (.removeEventListener js/document "mousemove" on-mousemove)
  (.removeEventListener js/document "mousewheel" on-mousemove)
  (.removeEventListener js/document "resize" on-mousemove))

(.addEventListener js/document "click" on-click false)
(.addEventListener js/document "mousemove" on-mousemove false)
(.addEventListener js/document "mousewheel" on-mousewheel false)
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

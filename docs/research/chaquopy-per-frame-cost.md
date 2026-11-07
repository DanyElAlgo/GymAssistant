# Chaquopy per-frame cost and the Kotlin-Python boundary

Research for [#3](https://github.com/DanyElAlgo/GymAssistant/issues/3) (map [#1](https://github.com/DanyElAlgo/GymAssistant/issues/1)). Researched 2026-11-07 against Chaquopy 17.2.0 (the version the spike pins in `android/settings.gradle.kts`), Python 3.11, MediaPipe tasks-vision 0.10.26.

## Question

Is calling Python through Chaquopy once per camera frame (~15-30 fps, JSON of 33 landmarks in, small JSON out, numpy available) fast enough on a mid-range Android phone for a live Cue? What are the known costs, and which boundary shapes are worth considering?

## Short answer

**Yes, a per-frame call is fast enough, and it is the shape to keep.** None of the primary sources reports a per-call cost that comes anywhere near a 33-66 ms frame budget for a payload this small. The real costs are elsewhere:

1. **Interpreter start-up (~2 s on a mid-range device)**, which the spike currently pays on the UI thread when the camera opens.
2. **numpy's packaging cost**: about 9 MB of compressed wheels per ABI. More importantly, its OpenBLAS/libgfortran dependencies are **not 16 KB-page aligned** (open upstream issue). Google Play enforces 16 KB alignment for updates from **2027-02-01**.
3. **Marshalling style**: boxed Java arrays or element-wise conversion are slow. A `String` (JSON) or a primitive array is fine.

The GIL is not a concern while all Python calls stay on one thread.

What the sources do **not** give is a measured per-call latency on a mid-range phone. The figure must be measured on our own device. The instrumentation for that is given under [Verification](#verification-to-do-in-the-walking-skeleton).

## The budget

- At 30 fps a frame lasts 33 ms; at 15 fps it lasts 66 ms.
- MediaPipe `LIVE_STREAM` "automatically drops incoming frames when busy so results stay current with low latency" ([MediaPipe Pose Landmarker guide](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker); [Android guide](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android)).
- The spike adds its own back-pressure: CameraX uses `STRATEGY_KEEP_ONLY_LATEST`, and `CameraActivity.onPoseResult` drops results while `analysisBusy` is set, so Python runs on a single-thread `analysisExecutor` (`CameraActivity.kt` lines 157-159, 226-243). A slow Python call therefore lowers the Cue rate. It does not build up latency or a queue.
- The Python work per frame is small: `json.loads` of ~33x4 floats, six `atan2` angles, and a state-machine step (`backend.py`). The maintainer's own guidance is that pure Python is slower than Kotlin, so heavy data work belongs in numpy ([chaquopy#653](https://github.com/chaquo/chaquopy/issues/653)). Six angles are not heavy data.
- *Inference (not sourced)*: on desktop CPython this work takes tens of microseconds. A mid-range ARM core is maybe 3-10x slower. Add a JNI crossing and the result is well under 1 ms, at least 30x headroom. This is an estimate, not a primary-source figure. Confirm it with the measurement below.

## Known costs, one by one

### 1. Interpreter start-up

- "Starting Python takes about 2 seconds on a mid-range device. The time taken to load a module will of course depend on what the module does." (maintainer mhsmith, [chaquopy#653](https://github.com/chaquo/chaquopy/issues/653))
- Adding packages "usually doesn't make a difference to how quickly Python itself starts". The exception is data files, which are extracted on first start ([chaquopy#653](https://github.com/chaquo/chaquopy/issues/653)).
- Precompiled `.pyc` is on by default: "Your app will start up faster if its Python code is compiled to .pyc format" ([Chaquopy Android guide, bytecode compilation](https://chaquo.com/chaquopy/doc/current/android.html)). Historical numbers: 1.4.0's stdlib-`.pyc` change made Python 3 start-up "50-60% faster" ([chaquopy#8](https://github.com/chaquo/chaquopy/issues/8)).
- "Before your app can run any Python code, you must call `Python.start()`." Apps that always use Python should start it from a once-per-process place such as `PyApplication` in the manifest ([Android guide](https://chaquo.com/chaquopy/doc/current/android.html)).
- `extractPackages`: "Each extracted file will slightly slow down your app's startup" ([Android guide](https://chaquo.com/chaquopy/doc/current/android.html)).

**Spike finding:** `CameraActivity.startCamera()` runs on the main thread, from `onCreate` and the permission callback. It calls `PoseAnalyzer.init()` → `Python.start()` + `getModule("backend")` there, and that call also pays for `import numpy`. That can mean a multi-second UI freeze, or an ANR, the first time the camera opens. **Fix:** start Python and import `backend` on a background thread when the app launches (or when the Exercise screen is entered), and gate the Set start on "analysis ready".

### 2. GIL and threads

- "Chaquopy is thread-safe. However, because it's based on CPython ... it is limited by CPython's global interpreter lock (GIL)." "The GIL can be a bottleneck for threads which release and acquire it many times in quick succession." ([Cross-language issues: multi-threading](https://chaquo.com/chaquopy/doc/current/cross.html))
- Only one Python instance is allowed per app. The GIL lets Python run on one core at a time, and calls into native code (numpy), Java or I/O give other threads a chance to run ([chaquopy#1251](https://github.com/chaquo/chaquopy/issues/1251), maintainer).
- "The GIL is automatically released whenever Python code calls a Java method or constructor" ([Python API: multi-threading](https://chaquo.com/chaquopy/doc/current/python.html)).
- "Chaquopy runs Python within your main app process; there is no separate Python process" ([FAQ](https://chaquo.com/chaquopy/doc/current/faq.html)).
- One user saw slower Python on a Java background thread than on the main thread. The issue was closed without a reproduction ([chaquopy#633](https://github.com/chaquo/chaquopy/issues/633)), so it is weak evidence. Still worth watching in the measurement.
- A `PyObject.finalize()` timeout crash can occur if a native call holds the GIL for over 10 s while the Java GC finalizes a PyObject on another thread ([chaquopy#465](https://github.com/chaquo/chaquopy/issues/465), open). This does not apply to sub-millisecond calls, but it is a reason never to run long Python work per frame.

**Implication:** with one Python consumer thread there is no contention. Keep *every* Python call on the same executor, including `reset()`. Today `reset()` is called from the main thread, so it can interleave with `analyze()` on the executor and corrupt the module-level `_state`. Put Set state in an object created per Set rather than in module globals.

### 3. Per-call and marshalling overhead

- No Chaquopy document or issue gives a per-call (`callAttr`) microbenchmark. The FAQ and Java API pages have no performance section ([FAQ](https://chaquo.com/chaquopy/doc/current/faq.html), [Java API](https://chaquo.com/chaquopy/doc/current/java.html)).
- Conversion, not the call, is where slowness has been reported. In [chaquopy#41](https://github.com/chaquo/chaquopy/issues/41), `callAttr` took ~12 ms on the phone. Converting a ~14 000-element result with `toJava(int[].class)` took ~600 ms, which the maintainer fixed with a `bytes` fast path. In [chaquopy#1136](https://github.com/chaquo/chaquopy/issues/1136), converting a 15 000-element *boxed* `Double[]` to numpy was slow. Switching to a primitive `DoubleArray` (`toDoubleArray`) cut the time by 90%. "Fast conversion between Java and NumPy arrays is only possible if their data types match exactly."
- Primitive Java arrays "implement the Python buffer protocol", so `numpy.array(jarray)` picks the right dtype ([Python API: arrays](https://chaquo.com/chaquopy/doc/current/python.html)).
- Our payload is ~33 landmarks x 4 floats, about 3 KB as JSON: one `String` in and one `String` out. Strings cross as a single object, with no per-element crossings. This is the cheap case.

**Implication:** JSON strings are fine for the walking skeleton and are easy to log and replay. If the measurement ever shows the boundary matters, pass a primitive `FloatArray(132)` instead (buffer protocol, zero per-element JNI). Never pass `List<Float>`, `Array<Double>` or `JSONArray` objects element by element.

### 4. APK size with numpy

- Chaquopy itself adds "about 3.5 MB, plus 5 MB per ABI", plus ~12.5 MB of installed user data ([chaquopy#653](https://github.com/chaquo/chaquopy/issues/653), maintainer). "Each ABI will add several MB to the size of the app, plus the size of any native requirements" ([Android guide](https://chaquo.com/chaquopy/doc/current/android.html)).
- numpy 1.26.2 is the newest Chaquopy build for cp311. Its wheel is 4.8 MB for `arm64_v8a` and 6.2 MB for `x86_64` ([Chaquopy package index: numpy](https://chaquo.com/pypi-13.1/numpy/)). It depends on `chaquopy-openblas 0.2.20` ([recipe](https://github.com/chaquo/chaquopy/blob/master/server/pypi/packages/numpy/meta.yaml)), which is another 4.1 MB / 5.1 MB per ABI ([index](https://chaquo.com/pypi-13.1/chaquopy-openblas/)). These are compressed wheel sizes; installed size is larger.
- APK splits and app bundles "won't help much" because of how Chaquopy packages native code. The FAQ instead recommends product flavors per ABI, or fewer ABIs ([FAQ: app size](https://chaquo.com/chaquopy/doc/current/faq.html#faq-size)).

### 5. ABI limits and 16 KB pages (the sharpest risk)

- ABIs: `arm64-v8a` (current devices), `x86_64` (emulators). `armeabi-v7a`/`x86` are available only on Python 3.11 and older; 3.12+ is 64-bit only ([Android guide](https://chaquo.com/chaquopy/doc/current/android.html); [chaquopy#709](https://github.com/chaquo/chaquopy/issues/709)). The spike's `abiFilters` of `arm64-v8a` + `x86_64` is correct for mid-range phones.
- Chaquopy 17.0.0: "Devices with 16 KB pages are now supported by Chaquopy itself. However, many existing Android wheels will still fail to load on 16 KB devices. For best compatibility with these devices, use Python 3.13 or later." ([changelog](https://chaquo.com/chaquopy/doc/current/changelog.html))
- numpy specifically: "On a 16 KB page size device, `import numpy` fails even with Python 3.13". The cause is `chaquopy-openblas 0.2.20` and `chaquopy-libgfortran 4.9`, which are 4 KB aligned and shared by every Python version ([chaquopy#1464](https://github.com/chaquo/chaquopy/issues/1464), open). A newer OpenBLAS is in progress, and official NumPy Android wheels on PyPI are expected "in December" ([chaquopy#1462](https://github.com/chaquo/chaquopy/issues/1462)).
- Google Play: "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory page sizes on 64-bit devices ... Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates." ([Android: page sizes](https://developer.android.com/guide/practices/page-sizes)). On a 16 KB device, an app with 4 KB-aligned `.so` files may run only in "16 KB backcompat mode", with a warning dialog (same page).
- The spike uses `targetSdk = flutter.targetSdkVersion`, which is API 35+ on current Flutter.

**Implication:** numpy is the only part of the stack carrying a 16 KB risk, and the per-frame maths does not need it. `backend.py` uses only `np.arctan2`, `np.degrees` and `np.mean` on scalars, all available in `math`/`statistics`. The thesis constraint is *Python*, not numpy. Recommended: write the analysis in pure Python with the standard library and drop `pip install("numpy")`. Re-add numpy only if a later feature truly needs vectorised work, and once 16 KB-aligned wheels exist. Dropping it also removes the `import numpy` share of start-up and ~9 MB per ABI.

## Boundary shapes

| Shape | Cue latency | Cost / risk | Verdict |
|---|---|---|---|
| **A. Per-frame call; Python owns angles, Postural error check, Rep state machine** (spike today) | 1 frame + Python time (sub-ms est.) | One String round-trip per frame; trivial at 30 Hz | **Recommended.** Matches the thesis ("Python analyses"). |
| B. Batched (Kotlin buffers N frames, one call per batch) | +N frames (e.g. N=5 → +166 ms at 30 fps) | Fewer crossings, but the overhead it saves is already negligible | Not for live use. Do expose a batch/replay entry point for tests (same core, list of frames), which helps the replay-test goal in #1. |
| C. Python owns more (camera / MediaPipe in Python) | n/a | MediaPipe has no Chaquopy wheel ([chaquopy#479](https://github.com/chaquo/chaquopy/issues/479), open since 2021, maintainer has no time). Frames would cross the boundary as large buffers (#41-style costs) | Not viable. |
| D. Python owns less (Kotlin computes angles and the live Cue; Python only judges finished Reps) | Kotlin-only per frame | Fewer calls, but the live Cue logic moves out of Python and Postural error rules split across two languages | Fallback only if measurement fails. |
| E. Long-running Python loop fed by a queue (one `callAttr` that never returns, Java pushes frames) | Same as A | Avoids per-call setup but adds lifecycle and shutdown complexity. Possible via Chaquopy APIs on a Python `queue` ([chaquopy#1251](https://github.com/chaquo/chaquopy/issues/1251)) | No benefit over A at this payload size. |

Recommended contract for A: the Kotlin wrapper exposes `startSet(exercise)`, `onFrame(landmarks, timestampMs) → FrameResult`, `endSet() → SetSummary`. All calls go through one executor. The Python core is a plain module with no Android imports, so the same code runs in desktop `pytest` against recorded landmark JSON.

## Verification to do in the walking skeleton

No source gives an on-device number, so measure it on the target mid-range phone:

1. In Kotlin, time `System.nanoTime()` around `callAttr("analyze", ...)`. In Python, time `time.perf_counter_ns()` around the body. The difference is the boundary cost.
2. Log p50/p95/max over ~1 000 frames and the effective analysed-fps, against the MediaPipe result rate.
3. Time `Python.start()` + `getModule("backend")` separately, with and without numpy.
4. Pass criterion (proposal): p95 per-frame Python round-trip < 5 ms, with analysed-fps equal to MediaPipe's result rate.
5. If numpy is kept: run on a 16 KB emulator image ("Google APIs Experimental 16 KB Page Size ARM 64 v8a", [page-sizes guide](https://developer.android.com/guide/practices/page-sizes)) and check `import numpy`.

## Sources

- Chaquopy docs: [Android guide](https://chaquo.com/chaquopy/doc/current/android.html), [Python API](https://chaquo.com/chaquopy/doc/current/python.html), [Java API](https://chaquo.com/chaquopy/doc/current/java.html), [Cross-language issues](https://chaquo.com/chaquopy/doc/current/cross.html), [FAQ](https://chaquo.com/chaquopy/doc/current/faq.html), [Changelog](https://chaquo.com/chaquopy/doc/current/changelog.html)
- Chaquopy package index: [numpy](https://chaquo.com/pypi-13.1/numpy/), [chaquopy-openblas](https://chaquo.com/pypi-13.1/chaquopy-openblas/), [numpy recipe](https://github.com/chaquo/chaquopy/blob/master/server/pypi/packages/numpy/meta.yaml)
- Chaquopy issues: [#8](https://github.com/chaquo/chaquopy/issues/8), [#41](https://github.com/chaquo/chaquopy/issues/41), [#465](https://github.com/chaquo/chaquopy/issues/465), [#479](https://github.com/chaquo/chaquopy/issues/479), [#633](https://github.com/chaquo/chaquopy/issues/633), [#653](https://github.com/chaquo/chaquopy/issues/653), [#709](https://github.com/chaquo/chaquopy/issues/709), [#1136](https://github.com/chaquo/chaquopy/issues/1136), [#1251](https://github.com/chaquo/chaquopy/issues/1251), [#1462](https://github.com/chaquo/chaquopy/issues/1462), [#1464](https://github.com/chaquo/chaquopy/issues/1464)
- MediaPipe: [Pose Landmarker overview](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker), [Android guide](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android)
- Android: [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
- Spike code (uncommitted at time of research): `android/app/src/main/python/backend.py`, `android/app/src/main/kotlin/com/example/gym_assistant/PoseAnalyzer.kt`, `CameraActivity.kt`, `android/app/build.gradle.kts`

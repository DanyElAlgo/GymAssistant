# Flutter owns the live screen; Kotlin is a headless camera-and-pose pipeline

During a Set, Flutter draws the camera preview (a `Texture` fed by CameraX through `TextureRegistry.SurfaceProducer`), the landmark skeleton, the Cue and the Rep count. Kotlin runs CameraX, MediaPipe and the Chaquopy call with no View of its own and sends only results to Flutter. We replaced the spike's native `CameraActivity` so that presentation lives in one layer (the thesis frames the app as Flutter + Python). That way a new Exercise touches only Python analysis and Flutter Cue presentation, never Kotlin UI. The extra EventChannel hop costs about 1 ms, well inside the 33–66 ms frame budget.

## Considered Options

- **Keep the native `CameraActivity`** (least effort, already working): rejected because it splits UI across two toolkits and Flutter can show nothing during a Set.
- **Embed `PreviewView` as a PlatformView**: rejected because hybrid composition costs frame rate and adds lifecycle quirks on a full-screen camera.
- **Flutter `camera` plugin**: not viable, because frames must reach MediaPipe in Kotlin. The Texture is fed by the same CameraX session that does the analysis.

## Consequences

- Kotlin sends 0–1 landmarks already rotated and mirrored to match the preview. Flutter only scales them (preview is fit/letterboxed, never cropped), and Python receives the same orientation.
- Flutter controls the pipeline's lifecycle (`startPreview() → textureId`, `stopPreview()`) and requests the camera permission.

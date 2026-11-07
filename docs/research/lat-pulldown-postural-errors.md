# Lat pulldown Postural errors: landmark signals and thresholds

Research for issue #2 (child of map #1). Question: for each lat pulldown **Postural error** named in the docs (bar behind the neck, torso leaning; thesis §4.3 also names altered scapular control), how does the biomechanics literature recognise it, how could it be detected from MediaPipe Pose's 33 2D landmarks (landmarks, angles, thresholds, camera view), and which ones can't one phone camera see reliably?

Vocabulary follows `GLOSSARY.md`: a **Postural error** is detected per frame. It drives the live **Cue** and is summarised into one **Rep verdict** per **Rep**.

## TL;DR

| Postural error | What the literature says it is | 2D landmark signal | View needed | Single-camera verdict |
|---|---|---|---|---|
| **Torso leaning** (excessive lean-back / swinging) | Trials allow only a *slight* trunk extension. "Upright" and "30° inclined" are used as distinct variants. No study defines a fault threshold. | Image-plane angle of the hip-mid→shoulder-mid vector vs. vertical (landmarks 11, 12, 23, 24), compared with that Rep's start pose. | Side (sagittal) or oblique. **Invisible from the front.** | **Reliably detectable.** It uses large, rarely occluded segments. Best candidate for the walking skeleton. |
| **Bar behind the neck** | The bar travels behind the head down to the base of the neck. Users with limited shoulder mobility must flex the neck forward to let it pass. | At the bottom of the Rep: wrists (15, 16) behind the ears (7, 8) along the anteroposterior image axis, plus head-forward flexion (nose 0 / ears vs. shoulders). | Side or oblique. From the front, front and back bar paths differ almost only in depth. | **Detectable from the side or oblique; unreliable from the front.** It depends on wrist landmarks near the head, which are partly hidden when the bar goes behind it. |
| **Altered scapular control** | Abnormal 3D scapular rotations (upward/downward rotation, tilt, internal rotation) and clavicular motion, plus more upper-trapezius and less serratus activity. Clinicians judge it visually from **behind**, with only moderate agreement. | No scapula landmarks exist. The only proxy is **shoulder shrugging**: vertical ear-to-shoulder distance (7/8 vs. 11/12), normalised by torso length. | Front or back for the shrug proxy. Real scapular motion needs a posterior view and 3D. | **Not reliably detectable.** At most a "shoulders shrugged" proxy, which is not scapular control in the clinical sense. |

**Recommendation:** use **torso leaning** as the single Postural error in the walking skeleton. Keep bar-behind-the-neck as the second error. Treat altered scapular control as out of scope, or as a shrug-only proxy that is named honestly. Whichever error is picked, the camera must see the user's **side** (sagittal or oblique), not the front.

## 1. Torso leaning

### Literature

- The thesis lists "inclinación del tronco" as a lat pulldown error to count (TG1 §Alcances, item on calidad técnica; `docs/specs/main-description.md` "torso leaning during the lat pulldown"). It gives no biomechanical definition.
- In a controlled HD-EMG study, the trunk "was allowed to slightly extend" during the front pulldown. An operator "visually checked for avoiding any enhancement in the trunk movement". So *slight* trunk extension is acceptable, and a noticeable increase during the pull is not ([Padovan et al., 2024, J Hum Kinet 91:47-60](https://pmc.ncbi.nlm.nih.gov/articles/PMC11057623/)).
- Buonsenso et al. (2025) tested seven variants in which trunk inclination was either "upright or 30° inclined angle". At 30° of inclination only the posterior deltoid was more active. Latissimus dorsi activation did not differ significantly ([Buonsenso et al., 2025, J Funct Morphol Kinesiol](https://pmc.ncbi.nlm.nih.gov/articles/PMC12452428/)). So a static 30° lean is a recognised *variant*, not by itself a defined fault.
- Novices "often rely on the elbow flexors to complete the movement rather than fully utilizing" the latissimus dorsi. Verbal technique instruction measurably raised latissimus activity ([Snyder & Leech, 2009, JSCR 23(8):2204-9](https://pubmed.ncbi.nlm.nih.gov/19826307/)). This supports Cueing beginners, but it does not define a lean threshold.
- **No primary source found gives a numeric "too much lean" cut-off.** Any threshold below is a design heuristic to calibrate on recordings, not a literature value.

### Detection from landmarks

- Torso vector: `mid_hip = (23+24)/2` to `mid_shoulder = (11+12)/2`. Torso angle = angle of that vector from the image vertical. Landmark indices and normalisation are from the [MediaPipe Pose Landmarker docs](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker). x and y are normalised by image width and height, so compute angles in pixel space (multiply by width and height) to avoid aspect-ratio distortion. Skvortsov et al. showed that the aspect ratio alone changed their cue counts ([Skvortsov et al., 2026, Sensors 26(20):6651](https://pmc.ncbi.nlm.nih.gov/articles/PMC13709264/)).
- Make the Postural error relative, not absolute. Take the torso angle at the Rep's start (arms extended, bar up) and flag the Rep if the angle increases by more than Δ during the pull, or if it exceeds an absolute ceiling. Suggested starting values: Δ ≈ 15°, ceiling ≈ 30° true lean. The 30° figure matches the "inclined" variant in Buonsenso et al. Both numbers are heuristics.
- The swing / momentum variant shows as a fast change in torso angle that coincides with the concentric phase. Torso angular velocity can separate it from a steady, deliberate lean.

### Camera view

- Forward or backward lean is a sagittal-plane rotation, so it is foreshortened or lost when the camera looks along the anteroposterior axis. Skvortsov et al. state plainly that "forward lean is invisible in a frontal view" ([PMC13709264](https://pmc.ncbi.nlm.nih.gov/articles/PMC13709264/)). Markerless accuracy for sagittal movements is best from a lateral view ([Shrestha et al., 2026, Sensors 26(19):6248](https://pmc.ncbi.nlm.nih.gov/articles/PMC13667498/), citing Carnevale et al.).
- In an oblique view, the apparent lean is `atan(tan(true) · cos θ)`, where θ is the camera's rotation away from a pure side view (simple projection geometry, derived here). Example: a true 30° lean appears as about 27° at θ = 30° and about 16° at θ = 60°. The thesis fixes a "vista de cámara diagonal (30°)" (TG1, "Determinación del punto de vista óptimo de cámara") without saying whether 30° is measured from the side or from the front. That matters: thresholds must be scaled by cos θ, or calibrated per setup.

## 2. Bar behind the neck

### Literature

- Thesis §4.3: it forces the neck forward and demands horizontal abduction with external rotation, which loads the rotator cuff. Muscle activation is similar to the front pulldown, so the front variant is preferred.
- Sperandei et al. (24 trained men, 80% 1RM) found no difference in latissimus activity between behind-the-neck, front and V-bar pulldowns. They concluded that behind-the-neck "is not a good lat pull-down technique and should be avoided" ([Sperandei et al., 2009, JSCR 23(7):2033-8](https://pubmed.ncbi.nlm.nih.gov/19855327/)). Signorile et al. found more latissimus activity with the wide-grip anterior position than with the wide-grip posterior one ([Signorile, Zink & Szwed, 2002, JSCR 16(4):539-46](https://pubmed.ncbi.nlm.nih.gov/12423182/)). A 2026 narrative review of 23 studies found that the anterior bar path was "the configuration most consistently associated with greater LD EMG amplitude" ([Di Fonza et al., 2026, J Funct Morphol Kinesiol](https://pubmed.ncbi.nlm.nih.gov/42647355/)).
- Injury association: in Kolber et al.'s review, "most injuries with a known positional mechanism" cite the high-five position, which includes "behind the neck latissimus pull-downs". The review also reports a suggested link between behind-the-neck pulldowns and both rotator cuff injury and anterior shoulder instability. Most of that evidence is retrospective ([Kolber et al., 2010, JSCR 24(6):1696-1704](https://doi.org/10.1519/JSC.0b013e3181dc4330)).
- **Kinematic definition usable for detection:** the back pulldown pulls the bar "to the lowest point of the neck behind the head", while the front one pulls "to the middle chest passing in front of the head". In the back version "the trunk was allowed to slightly ... flex". People with poor mobility "would be forced to flex the neck to let the bar pass backward" ([Padovan et al., 2024](https://pmc.ncbi.nlm.nih.gov/articles/PMC11057623/)).

### Detection from landmarks

At the bottom of the Rep (wrist y at its maximum, elbows most flexed):

1. **Bar path, the primary signal:** compare the wrists' anteroposterior position with the head. In a side view this is the image x of the wrist midpoint (15, 16) against the x of the ears (7, 8) and nose (0), using the person's facing direction (nose x minus ear x). A wrist behind the ear line means the bar is behind the neck. The front pulldown ends with the wrists in front of the face, at roughly chin to upper-chest height.
2. **Head-forward flexion, the supporting signal:** the ear–shoulder vector tips forward, or the nose drops toward the shoulder line. Measure both relative to the Rep's start pose.
3. **Trunk flexion instead of extension:** the torso angle from section 1 moves *forward* during the pull instead of slightly back. This is a cheap tie-breaker.

Thresholds: the bar-path signal is close to binary (behind or in front of the ear line), so it needs only a small margin, e.g. a fraction of the ear-to-shoulder distance. The head-flexion margin is a heuristic to calibrate on recordings.

### Camera view and limits

- From the front, the bar's position relative to the head is mostly a depth difference. MediaPipe's normalised z is "depth relative to the midpoint of the hips" with no stated scale ([MediaPipe docs](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker)). Out-of-plane tracking with single-camera MediaPipe showed poor agreement (ICC ≤ 0.34, RMSE > 30°) ([Shrestha et al., 2026](https://pmc.ncbi.nlm.nih.gov/articles/PMC13667498/)). **A frontal view can't reliably see this Postural error.**
- From the side, the wrists pass behind the head and become partly occluded. Use landmark `visibility` and hold the last confident estimate, rather than trusting low-visibility wrists.

## 3. Altered scapular control

### Literature

- Thesis §4.3 cites Ludewig & Reynolds (2009). That review links abnormal scapular *kinematics* (3D rotations of the scapula and clavicle) with impingement, rotator cuff tendinopathy and tears, glenohumeral instability and other conditions. It also links them with "reduced serratus anterior and increased upper trapezius activation" ([Ludewig & Reynolds, 2009, JOSPT 39(2):90-104](https://pubmed.ncbi.nlm.nih.gov/19194022/)).
- The clinical reference test for scapular dyskinesis is **visual, from the posterior aspect**. Expert raters scored winging or dysrhythmia on video and agreed only moderately (κw 0.48–0.61) ([McClure et al., 2009, J Athl Train 44(2):160-4](https://pubmed.ncbi.nlm.nih.gov/19295960/)). Visually rated dyskinesis did correspond to 3D differences: less upward rotation, less clavicular elevation, more protraction ([Tate et al., 2009, J Athl Train 44(2):165-73](https://pubmed.ncbi.nlm.nih.gov/19295961/)). The 2013 Scapular Summit consensus agrees that dyskinesis is common in shoulder injury, but "the exact role ... is not clearly defined" ([Kibler et al., 2013, BJSM 47(14):877-85](https://pubmed.ncbi.nlm.nih.gov/23580420/)).
- Neither the thesis nor the sources found give a lat-pulldown-specific operational definition of "altered scapular control".

### Detection from landmarks

- MediaPipe has no scapula, acromion or spine landmarks. The shoulder landmarks (11, 12) are the "approximate location" of the shoulder joints ([MediaPipe docs](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker)). Scapular upward or downward rotation, tilt and winging can't be computed from them.
- **Shrug proxy:** during the pull the shoulders should come down (depression), not rise toward the ears. Signal: vertical distance from each ear to its shoulder (7→11, 8→12), normalised by torso length. Flag the Rep if this distance shrinks by more than a heuristic fraction against the start pose.
- Pitfall from a MediaPipe rule-based system: a left–right shoulder-height difference "cannot detect elevation of both shoulders". In their technical check it caught **none** of 107 simulated shoulder-elevation Reps, because subjects raised both shoulders ([Skvortsov et al., 2026](https://pmc.ncbi.nlm.nih.gov/articles/PMC13709264/)). So use the ear-to-shoulder distance per side, not an asymmetry. Also normalise by body size, because image-normalised thresholds vary with camera distance (same source).

### Camera view and limits

- The shrug proxy works best from the front or back, where the ear–shoulder vertical gap is in the image plane. A side view still shows it, but one side occludes the other.
- Real scapular control needs a posterior view, and even trained humans agree only moderately on it. **Flag as not reliably detectable with one phone camera.** If it is kept, call it a "shrugged shoulders" Postural error in the glossary rather than "altered scapular control".

## 4. Cross-cutting implications for the walking skeleton

- **One view can't serve all three.** Torso leaning and bar-behind-the-neck both need the user's **side** (sagittal or oblique). The shrug proxy prefers front or back. Thesis assumption: one diagonal camera (TG1 §Alcances item 6, and §"Determinación del punto de vista óptimo de cámara", where the 30° diagonal was chosen for the leg press). For the lat pulldown, the diagonal should be closer to the side than to the front.
- **Rep segmentation** comes from the same landmarks: elbow angle (11-13-15 / 12-14-16) or wrist height relative to the shoulders. Bottom of the Rep = wrists lowest. The bar-behind-the-neck check is evaluated there. Torso leaning is evaluated across the whole Rep against the start pose.
- **Per-frame to Rep verdict:** compute the signal per frame for the live Cue. Set the Rep verdict to "incorrect" if the Postural error persists for N consecutive frames within the Rep. Debouncing matters: Skvortsov et al. decided cues on single frames and saw their counts swing with smoothing and frame rate ([PMC13709264](https://pmc.ncbi.nlm.nih.gov/articles/PMC13709264/)).
- **All numeric thresholds here are heuristics.** No primary source found sets a numeric lean or neck-flexion cut-off for the lat pulldown. Calibrate them on labelled recordings, which the map's replay-in-tests requirement already enables.
- **Possible occlusion:** the machine's frame, the thigh pad and the cable can hide the hips or wrists from a side view. Check landmark `visibility` before judging a frame.

## Sources

Primary (peer-reviewed studies and official docs):

- Sperandei S, Barros MA, Silveira-Júnior PC, Oliveira CG. Electromyographic analysis of three different types of lat pull-down. *J Strength Cond Res.* 2009;23(7):2033-8. doi:10.1519/JSC.0b013e3181b8d30a. https://pubmed.ncbi.nlm.nih.gov/19855327/
- Signorile JF, Zink AJ, Szwed SP. A comparative electromyographical investigation of muscle utilization patterns using various hand positions during the lat pull-down. *J Strength Cond Res.* 2002;16(4):539-46. https://pubmed.ncbi.nlm.nih.gov/12423182/
- Padovan R et al. High-density electromyography excitation in front vs. back lat pull-down prime movers. *J Hum Kinet.* 2024;91:47-60. doi:10.5114/jhk/185211. https://pmc.ncbi.nlm.nih.gov/articles/PMC11057623/
- Buonsenso A et al. Electromyographic analysis of back muscle activation during lat pulldown exercise: effects of grip variations and forearm orientation. *J Funct Morphol Kinesiol.* 2025;10(3):345. https://pmc.ncbi.nlm.nih.gov/articles/PMC12452428/
- Snyder BJ, Leech JR. Voluntary increase in latissimus dorsi muscle activity during the lat pull-down following expert instruction. *J Strength Cond Res.* 2009;23(8):2204-9. https://pubmed.ncbi.nlm.nih.gov/19826307/
- Lusk SJ, Hale BD, Russell DM. Grip width and forearm orientation effects on muscle activity during the lat pull-down. *J Strength Cond Res.* 2010;24(7):1895-900. https://pubmed.ncbi.nlm.nih.gov/20543740/ (background: anterior, pronated grip recommended)
- Di Fonza D et al. Electromyographic analysis of latissimus dorsi activation during common resistance training exercises: a narrative review. *J Funct Morphol Kinesiol.* 2026;11(3):315. https://pubmed.ncbi.nlm.nih.gov/42647355/
- Kolber MJ, Beekhuizen KS, Cheng MS, Hellman MA. Shoulder injuries attributed to resistance training: a brief review. *J Strength Cond Res.* 2010;24(6):1696-1704. doi:10.1519/JSC.0b013e3181dc4330
- Ludewig PM, Reynolds JF. The association of scapular kinematics and glenohumeral joint pathologies. *J Orthop Sports Phys Ther.* 2009;39(2):90-104. https://pubmed.ncbi.nlm.nih.gov/19194022/
- McClure P et al. A clinical method for identifying scapular dyskinesis, part 1: reliability. *J Athl Train.* 2009;44(2):160-4. https://pubmed.ncbi.nlm.nih.gov/19295960/
- Tate AR et al. A clinical method for identifying scapular dyskinesis, part 2: validity. *J Athl Train.* 2009;44(2):165-73. https://pubmed.ncbi.nlm.nih.gov/19295961/
- Kibler WB et al. Clinical implications of scapular dyskinesis in shoulder injury: the 2013 consensus statement from the 'Scapular Summit'. *Br J Sports Med.* 2013;47(14):877-85. https://pubmed.ncbi.nlm.nih.gov/23580420/
- Shrestha S et al. Concurrent validity and agreement of viewpoint-dependent markerless motion tracking in upper-limb assessment. *Sensors.* 2026;26(19):6248. https://pmc.ncbi.nlm.nih.gov/articles/PMC13667498/
- Skvortsov DV et al. Remote monitoring of algorithm-detected compensation cues during long-term home-based upper-limb training after stroke. *Sensors.* 2026;26(20):6651. https://pmc.ncbi.nlm.nih.gov/articles/PMC13709264/ (rule-based MediaPipe cues: a useful source of pitfalls)
- Google AI Edge. MediaPipe Pose Landmarker guide. https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker

Project docs: `docs/specs/TG1-EC1-DanielPenaranda-v1.5.2.md` (§4.3, Alcances, camera viewpoint section) and `docs/specs/main-description.md`. Both are in the main checkout and are not yet committed to `main`.

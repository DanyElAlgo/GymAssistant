# Sources of lat pulldown reference footage

Research for [#4](https://github.com/DanyElAlgo/GymAssistant/issues/4) (child of map [#1](https://github.com/DanyElAlgo/GymAssistant/issues/1)). Researched 2026-11-07.

**Question.** Besides the user recording themselves at a gym, where can lat pulldown footage come from, covering correct Reps and the two lat pulldown Postural errors (bar behind the neck, torso leaning), so it can be used to tune Rep detection and as test fixtures? Fixtures can be either video files or pre-extracted per-frame landmark JSON. For each source this note covers availability, camera angles, whether incorrect technique is present, and legal and ethical constraints.

Vocabulary follows `GLOSSARY.md`: Exercise, Rep, Set, Postural error, Cue, Rep verdict.

## Constraints that shape the answer

- **The repo is public and MIT-licensed** (`gh repo view` reports `visibility: PUBLIC` and license `MIT`). Anything committed as a fixture gets redistributed to the world. Material under a non-commercial (NC) or share-alike (SA) licence can't be relicensed as MIT, and material whose owner never licensed it can't be redistributed at all.
- **Target camera view.** The thesis fixes a single camera at a **diagonal/oblique ~30° view** (`docs/specs/TG1-EC1-DanielPenaranda-v1.5.2.md`, Alcances §6 and "Determinación del punto de vista óptimo de cámara"). Footage from other angles helps with exploration, but its landmark geometry won't match what the app sees.
- **Postural errors in scope** (`docs/specs/main-description.md`, Scope: "bar-behind-the-neck or torso leaning during the lat pulldown"). The thesis §4.3 describes the bar behind the neck as forcing the neck forward and putting the shoulder in horizontal abduction with external rotation.
- **Pipeline.** MediaPipe Pose Landmarker has a `VIDEO` running mode that processes decoded frames synchronously, in order, with monotonic millisecond timestamps, and "never drops frames". Each frame gives 33 normalized landmarks (x, y in [0,1], z relative to the hip midpoint, plus `visibility`/`presence`) and 33 world landmarks in metres ([MediaPipe Pose Landmarker, Python guide](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/python); [overview](https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker)). So any video source can be turned into landmark JSON offline with the same model the app runs, and **the choice of source is mostly a question of footage and rights, not tooling**.

## 1. Public datasets

Each candidate below was checked against its primary page. Only one public video dataset was found that actually contains lat pulldown clips.

| Dataset | Lat pulldown? | Incorrect technique? | Views | Access / licence | Verdict |
|---|---|---|---|---|---|
| **Kaggle "Workout/Exercises Video"** (Hasyim Abdillah) | **Yes: 51 clips** in `lat pulldown/` (counted via Kaggle API file listing, 652 files across 22 classes) | Not labelled. Clips are class-only, "at least 1 rep on each video" | Mixed, uncontrolled (internet videos) | Page says **CC BY-NC-SA 4.0**, but the description says videos are "**Mostly sourced from YouTube**, but I also create some of it by myself with my friends" ([Kaggle API metadata](https://www.kaggle.com/api/v1/datasets/view/hasyimabdillah/workoutfitness-video); [dataset page](https://www.kaggle.com/datasets/hasyimabdillah/workoutfitness-video)) | Exploration only. The uploader probably can't license YouTube creators' videos, and NC-SA is incompatible with the MIT repo anyway. |
| Kaggle "Gym Exercise DataSet" (amanyadav2030a, ~68k frames) | Yes ("Lat Pulldown" among same 22 classes) | Not labelled | Same as above | Tagged **CC0**, but it is "a pre-processed frame-level version of a gym workout/exercise video dataset" with the identical 22 classes ([Kaggle API metadata](https://www.kaggle.com/api/v1/datasets/view/amanyadav2030a/processed-train-test-exercise-68k)) | Same underlying footage re-tagged CC0. The CC0 tag doesn't cure the upstream rights problem. JPEG frames at ~5 FPS are too sparse for Rep timing. |
| Hugging Face `34data/workout-vids` | Has a lat pulldown folder (652 rows, 22 labels, matching the Kaggle set) | Not labelled | Same | Empty dataset card, no licence ([HF page](https://huggingface.co/datasets/34data/workout-vids)) | Mirror. Don't use. |
| **FLEX** (2025, multi-view, sEMG, expert error labels) | **No.** The 20 exercises are free-weight lifts (presses, curls, rows, squat, deadlift, etc.) ([arXiv 2506.03198v4, Table 2](https://arxiv.org/html/2506.03198v4)) | Yes, expert-annotated error types | 4 corner cinema cameras + 1 smartphone | Signed licence, "non-commercial academic research only" (same paper) | No lat pulldown. |
| **Fit3D / AIFit** | Not listed. 47 exercises in "Warmups, Barbell, Dumbbell, Equipment-Free" categories ([fit3d.imar.ro](https://fit3d.imar.ro/fit3d)). Exercise names weren't retrievable, and the categories exclude cable machines | Trainer vs trainee | 4 views | Non-commercial research only. "shall not be copied, shared, distributed" ([licence](https://fit3d.imar.ro/legal)) | Not usable for fixtures in any case. |
| **QEVD / QEVD-Fit-Coach** (Qualcomm) | No evidence. Exercises are bodyweight-style (push-ups, squats, lunges, jumping jacks…), with participants "facing the camera" ([arXiv 2407.08101v3](https://arxiv.org/html/2407.08101v3)) | **Yes**: participants were "instructed to perform common mistakes and their corrections" | Frontal, home | Research + commercial per the paper's crowd-worker agreement. Dataset terms on [Qualcomm page](https://www.qualcomm.com/developer/software/qevd-dataset) | Good model of *how* to record mistakes, but no lat pulldown. |
| **RepCount** (TransRAC, CVPR 2022) / RepCount-pose | **No.** Classes: bench press, front raise, jumping jack, pommel horse, sit-up, squat, pull-up, push-up ([PoseRAC, arXiv 2303.08450, Table 1](https://arxiv.org/html/2303.08450); [TransRAC](https://arxiv.org/abs/2204.01018)) | No | Internet video | Research | Useful only as a reference for how Rep start/end annotations are laid out. |
| Kinetics-700 (and Countix, built on Kinetics) | **No** lat pulldown class. The closest are `pull ups`, `exercising arm` ([K700 label map, mmaction2](https://github.com/open-mmlab/mmaction2/blob/main/tools/data/kinetics/label_map_k700.txt)) | No | YouTube | YouTube-ID lists | No. |
| WAVd | **No.** 11 classes (PullUps, PushUps, Squats, WeightLifting…) ([WAVd site](https://sites.google.com/view/wavd/home)) | No | Social media / YouTube | Academic only, request form | No. |
| Mendeley "Multi-View Raw Video Dataset of Seven Fitness Exercises with Good/Bad Form Labels" | **No.** Dumbbell/bodyweight exercises only ([Mendeley](https://data.mendeley.com/datasets/kgbb3yn47p/2)) | Yes, good/bad labels | Front, side, diagonal | Mendeley/CC (licence not confirmed) | No lat pulldown, but its front/side/diagonal × good/bad design is a good template for our own recordings. |
| Kaggle "Gym Workout IMU Dataset" | Yes ("Straight Bar Lat Pulldown"), but **wrist IMU, not video** ([Kaggle API metadata](https://www.kaggle.com/api/v1/datasets/view/shakthisairam123/gym-workout-imu-dataset)) | No | n/a | MIT | Not applicable (no pose). |
| InfiniteRep (synthetic, Infinity AI) | No. Ten free-weight/bodyweight exercises per a secondary mirror ([paperswithcode](https://cs.paperswithcode.com/dataset/infiniterep)). The primary repo `toinfinityai/InfiniteRep` returned 404 | No | Varied synthetic | Unverified | No. |

**Finding:** no public dataset found has **labelled lat pulldown Postural errors**, and none has lat pulldown at a controlled 30° oblique view. The only lat pulldown video collection is the Kaggle set (and its re-uploads), which is unlabelled internet footage with doubtful redistribution rights.

## 2. Online videos (YouTube, stock sites, Wikimedia Commons)

**Availability and angles.** There's plenty of it. Coaching channels routinely show correct form and "mistakes" segments, often including the bar behind the neck and excessive leaning back, filmed from front, side and three-quarter angles. Coverage is anecdotal, and angle and framing vary from clip to clip.

**Licensing:**

- **YouTube (standard licence).** The Terms of Service only allow viewing "for personal, non-commercial use". They forbid accessing, reproducing or **downloading** content unless the Service expressly authorises it or YouTube and the rights holders give prior written permission. Automated access (scrapers) is also forbidden ([YouTube ToS, "Permissions and restrictions"](https://www.youtube.com/static?template=terms)). Downloading a clip to run it through MediaPipe already breaches the ToS, even before anything gets committed.
- **YouTube Creative Commons.** Uploaders can mark videos CC BY, which lets others reuse the work with attribution ([YouTube Help: Creative Commons](https://support.google.com/youtube/answer/2797468?hl=en)). CC BY is compatible with an MIT repo if attribution is kept. The download restriction in the ToS still applies, so these clips have to be obtained through YouTube's own tools or with permission.
- **Bolivian copyright.** Ley 1322 de Derecho de Autor (1992) has no general research or private-use exception. Art. 24 only permits citing "cortos fragmentos" of published works, with source and author, "con fines docentes o de investigación", in honest use and to the extent justified. Art. 15 gives the author the exclusive right to reproduce the work "total o parcialmente" ([Ley 1322, Lexivox](https://www.lexivox.org/norms/BO-L-1322.html)). Short excerpts discussed in the thesis document fit the citation right, but whole clips committed to a public repo don't.
- **Stock video (Pexels).** Pexels lists at least six clips explicitly titled lat pulldown (for example [35585699](https://www.pexels.com/video/back-workout-in-gym-with-lat-pulldown-machine-35585699/), [4110016](https://www.pexels.com/video/a-man-doing-lateral-pull-down-exercise-4110016/), [30890377](https://www.pexels.com/video/intense-gym-workout-lat-pulldowns-and-motivation-30890377/); [search](https://www.pexels.com/search/videos/lat%20pulldown/)). The Pexels licence allows free use and modification with no attribution required. It forbids redistributing the files on other stock platforms and showing identifiable people "in a bad light" ([Pexels licence](https://www.pexels.com/license/)). These clips are cinematic b-roll, with no controlled angle, few full Reps and correct-ish form only. Committing *derived landmarks* is a modification and looks fine under the licence. Committing the raw .mp4 to a public repo is closer to redistribution and is best avoided.
- **Wikimedia Commons.** Two lat pulldown videos exist. "Common Lat Pulldown Mistakes.webm" is CC BY 3.0, re-hosted from a YouTube creator, 11 s, vertical, and covers shrugging, forearm alignment and flared elbows, which is not our two Postural errors ([file page](https://commons.wikimedia.org/wiki/File:Common_Lat_Pulldown_Mistakes.webm)). "Lat pulldown exercise demonstration.webm" is CC BY-SA 4.0, but it **has been nominated for deletion (2026-11-05) for lacking evidence of a CC release** ([file page](https://commons.wikimedia.org/wiki/File:Lat_pulldown_exercise_demonstration.webm)). Even "free" hosts can carry laundered licences.

**Video vs extracted landmarks.** A landmark JSON file contains no pixels: it's 33 coordinates per frame. That makes it far less of a copy of the creative work than the video, and much less identifying. It doesn't fix how the video was *obtained* (the YouTube ToS breach happens at download time), and it's still derived from someone's image (Constitución Política del Estado, Art. 21.2: right to "privacidad, intimidad, honra, honor, propia imagen y dignidad", [CPE, Lexivox](https://www.lexivox.org/norms/BO-CPE-20090207.html)). The cautious line for a degree project:

- **Exploration only (not committed):** watching online videos, and locally extracting landmarks from them to eyeball thresholds. Low risk, but ToS-non-compliant for YouTube downloads. Prefer screen-viewing plus the user's own recordings.
- **Committed fixtures:** only footage you own or that has an explicit licence compatible with redistribution (your own recordings, consenting recruits, CC BY or Pexels-style stock), and preferably committed as **landmark JSON plus a provenance note**, not raw video.

## 3. Synthetic landmark generation

Procedurally generate 33-landmark sequences: seated torso, shoulder/elbow/wrist chains driven by a scripted elbow and shoulder angle trajectory, projected through a virtual camera at the 30° oblique. Postural errors are injected as parameters (head and neck forward with the bar path behind the head; torso pitch beyond a threshold). The VHM Fitness Motion dataset shows the approach is accepted practice: 1,200 sequences "generated by the included code", released CC BY ([Kaggle](https://www.kaggle.com/api/v1/datasets/view/minhhong552/vhm-fitness-motion-60k-synthetic-rgb-and-pose)). It doesn't include lat pulldown.

- **Availability:** unlimited, and every Rep is labelled exactly (Rep boundaries and Rep verdict known by construction).
- **Angles:** any, including exactly 30°.
- **Incorrect technique:** yes, parameterised and as extreme or subtle as needed.
- **Legal/ethical:** none. It's your own code and contains no person.
- **Limits:** it isn't MediaPipe output. Real jitter, `visibility` drops (the arm occluding the face, the machine pad hiding the legs) and depth (z) noise are missing unless modelled. Synthetic data validates the *logic* (Rep state machine, angle thresholds, Cue switching) but can't validate the thresholds against reality, and a thesis tribunal is likely to ask for real data.

## 4. Recruiting other people

Record gym peers, classmates or instructors (the thesis already surveyed n=15 Santa Cruz instructors) performing correct Reps and **deliberately performed** Postural errors, as QEVD did ("instructed to perform common mistakes and their corrections", [arXiv 2407.08101v3](https://arxiv.org/html/2407.08101v3)).

- **Availability:** good, and it adds body-type variety (the scope mentions male and female body types).
- **Angles:** fully controlled. Record 30° oblique as primary, and optionally front and side, like the Mendeley good/bad dataset design.
- **Incorrect technique:** yes, on request. Behind-the-neck should be done **with light load** because it's the injury mechanism the thesis describes (§4.3). An instructor present to supervise is recommended.
- **Legal/ethical:** Bolivia has **no comprehensive personal-data protection law in force**. A bill was still pending as of 2025–2026 according to secondary trackers ([AGETIC anteproyecto 2024](https://agetic.gob.bo/wp-content/uploads/2024/08/DATOS-PERSONALES-PRESENTACION-ANTEPROYECTO-DE-LEY-2024-firmado.pdf); [UNGOVR](https://law.ungovr.org/ai/bo)). The constitutional right to one's own image (CPE Art. 21.2) and the *acción de protección de privacidad* (Art. 130) still apply. In practice:
  - Get **written informed consent** that names the purpose (thesis, public GitHub repo), what gets published (landmark JSON only, or video too), and the right to withdraw.
  - Get the **gym owner's permission** to film on the premises, and frame so that bystanders aren't recorded.
  - Commit **only landmark JSON** (no faces or pixels) and keep raw videos private, off the repo.
  - Check whether the university requires ethics or tutor sign-off for human participants.

## 5. Simulating the movement at home (resistance band)

Anchor a band high (door anchor or bar) and perform a kneeling or seated band pulldown. This is a recognised lat pulldown variant (ACE lists "Kneeling Lat Pulldown" with resistance bands/cables, [ACE exercise library](https://www.acefitness.org/education-and-resources/lifestyle/exercise-library/35/kneeling-lat-pulldown)). A broomstick held as a bar works too, to reproduce bar-in-front vs bar-behind-the-neck paths.

- **Availability:** immediate and repeatable, so any number of takes is possible during development.
- **Angles:** fully controlled, and the actual phone and tripod setup can be tested at 30°.
- **Incorrect technique:** yes. Both Postural errors can be acted out safely at very low load.
- **Legal/ethical:** none (it's the user's own body and home).
- **Limits:** no machine (no thigh pad, no seat, no cable stack occluding the body or appearing in frame), different background and lighting, and band resistance grows toward the bottom of the movement, which changes tempo. Good for Rep-detection logic and early fixtures, but final threshold tuning and evaluation should still use real-machine footage.

## Comparison

| Option | Lat pulldown available | 30° oblique view | Postural errors present | Can be a committed fixture | Main constraint |
|---|---|---|---|---|---|
| Public datasets | Only the Kaggle 22-class set (51 unlabelled clips) | No (uncontrolled) | Unlabelled, unknown | **No** (YouTube-sourced, NC-SA vs MIT) | Rights and no labels |
| YouTube videos | Plenty | Rarely | Yes, in "mistakes" videos | **No** (ToS forbids download, Ley 1322 has no research exception) | ToS and copyright |
| CC BY / stock (Pexels, Commons) | A handful of clips | No | Mostly correct form only | Landmarks yes (keep attribution) | Few clips, b-roll quality, licence laundering risk |
| Synthetic landmarks | Unlimited | Yes | Yes, parameterised | **Yes** | Not real MediaPipe noise |
| Recruited people | Good | Yes | Yes, on request | Yes, landmarks with consent | Consent, gym permission, safety |
| Home band simulation | Unlimited | Yes | Yes, acted | **Yes** | No machine context |
| (Baseline) self at gym | Good | Yes | Yes, acted | Yes | Gym permission, time |

## Recommendation

There's no off-the-shelf labelled lat pulldown dataset, so plan to produce the data:

1. **Now, to build the walking skeleton:** record **home resistance-band (or broomstick) pulldowns** at the 30° oblique view. Cover correct Reps and both acted Postural errors, run them through MediaPipe `VIDEO` mode offline, and commit the **landmark JSON** as replay fixtures. Add a small **synthetic generator** for edge cases (exact Rep counts, borderline torso angle, missing landmarks) in unit tests.
2. **For tuning and thesis evaluation:** record **real-machine footage** of the user plus a few **consenting recruits** (ideally with an instructor supervising light-load behind-the-neck Reps), with written consent and gym permission. Commit only landmark JSON and keep raw video private.
3. **Exploration only:** use YouTube "mistakes" videos and the Kaggle 22-class lat pulldown clips to *look at* what the errors look like in the wild and sanity-check thresholds. Don't commit them or anything derived from them.
4. Add a `PROVENANCE.md` (or a per-fixture field) to every fixture recording source, licence or consent, camera angle and the Rep verdict labels.

## Open points / not verified

- Fit3D's individual exercise names couldn't be retrieved. "No lat pulldown" is inferred from its published category list (no machine/cable category).
- The 51 Kaggle lat pulldown clips were counted but not watched. Whether any show behind-the-neck or heavy leaning is unknown.
- The QEVD dataset licence text wasn't retrieved from Qualcomm's page (it rendered empty).
- The MediaPipe BlazePose GHUM 3D model card (limitations for seated and occluded poses) couldn't be text-extracted here.
- This isn't legal advice. Confirm consent and ethics requirements with the thesis tutor or the university.

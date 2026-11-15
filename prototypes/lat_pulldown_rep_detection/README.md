# PROTOTYPE: lat pulldown Rep detection (throwaway, never merge)

Answers the wayfinder ticket "Lat pulldown Rep detection" (#8). Open `PROTOTYPE_rep_detection.html` by double-clicking: it embeds per-frame signals (wrist height above shoulder in torso lengths, elbow angle) derived from the 16 reference clips. No video is included.

Regenerate:
1. `python extract_landmarks.py <video_dir> android/app/src/main/assets/pose_landmarker_lite.task landmarks.json` (needs `mediapipe`, `opencv-python-headless`)
2. `python signals.py landmarks.json template.html PROTOTYPE_rep_detection.html`

The portable part is the `RepCounter` block in the HTML: it is what gets ported to `backend.py`.

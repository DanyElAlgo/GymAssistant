"""PROTOTYPE: run MediaPipe PoseLandmarker (the app's lite model) over the
reference clips and dump upper-body landmarks per frame as JSON.

usage: python extract_landmarks.py <video_dir> <model.task> <out.json>
"""
import glob
import json
import os
import sys

import cv2
import mediapipe as mp
from mediapipe.tasks import python as mp_tasks
from mediapipe.tasks.python import vision

# nose, ears, shoulders, elbows, wrists, hips
KEEP = [0, 7, 8, 11, 12, 13, 14, 15, 16, 23, 24]

video_dir, model_path, out_path = sys.argv[1:4]
clips = {}
for path in sorted(glob.glob(os.path.join(video_dir, "*.mp4"))):
    name = os.path.splitext(os.path.basename(path))[0]
    opts = vision.PoseLandmarkerOptions(
        base_options=mp_tasks.BaseOptions(model_asset_path=model_path),
        running_mode=vision.RunningMode.VIDEO,
        num_poses=1,
    )
    frames = []
    with vision.PoseLandmarker.create_from_options(opts) as lm:
        cap = cv2.VideoCapture(path)
        fps = cap.get(cv2.CAP_PROP_FPS)
        i = 0
        while True:
            ok, bgr = cap.read()
            if not ok:
                break
            rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
            res = lm.detect_for_video(
                mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb), int(i * 1000 / fps)
            )
            if res.pose_landmarks:
                p = res.pose_landmarks[0]
                frames.append([[round(p[k].x, 4), round(p[k].y, 4), round(p[k].visibility, 2)] for k in KEEP])
            else:
                frames.append(None)
            i += 1
    clips[name] = {"fps": fps, "frames": frames}
    seen = sum(f is not None for f in frames)
    print(f"{name}: {len(frames)} frames, person in {seen}", flush=True)

with open(out_path, "w") as f:
    json.dump({"keep": KEEP, "clips": clips}, f, separators=(",", ":"))
print("wrote", out_path)

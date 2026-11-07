# backend.py
"""Pose analysis backend, run on-device through Chaquopy.

Kotlin runs MediaPipe PoseLandmarker and passes the 33 normalized landmarks
here as JSON. This module computes joint angles and runs a squat rep counter,
then returns the analysis as JSON.
"""
import json

import numpy as np

# MediaPipe Pose landmark indices
L_SHOULDER, R_SHOULDER = 11, 12
L_ELBOW, R_ELBOW = 13, 14
L_WRIST, R_WRIST = 15, 16
L_HIP, R_HIP = 23, 24
L_KNEE, R_KNEE = 25, 26
L_ANKLE, R_ANKLE = 27, 28

JOINTS = {
    "left_elbow": (L_SHOULDER, L_ELBOW, L_WRIST),
    "right_elbow": (R_SHOULDER, R_ELBOW, R_WRIST),
    "left_hip": (L_SHOULDER, L_HIP, L_KNEE),
    "right_hip": (R_SHOULDER, R_HIP, R_KNEE),
    "left_knee": (L_HIP, L_KNEE, L_ANKLE),
    "right_knee": (R_HIP, R_KNEE, R_ANKLE),
}

MIN_VISIBILITY = 0.5
SQUAT_UP_ANGLE = 160.0
SQUAT_DOWN_ANGLE = 100.0  # roughly thighs parallel to the floor

_state = {"stage": None, "reps": 0}


def reset():
    """Reset the rep counter (called each time the camera is opened)."""
    _state["stage"] = None
    _state["reps"] = 0


def _angle(a, b, c):
    """Angle ABC in degrees, in the image plane."""
    radians = np.arctan2(c[1] - b[1], c[0] - b[0]) - np.arctan2(a[1] - b[1], a[0] - b[0])
    angle = abs(np.degrees(radians))
    return float(360.0 - angle if angle > 180.0 else angle)


def _visible(landmark):
    return landmark.get("visibility", 1.0) >= MIN_VISIBILITY


def analyze(landmarks_json):
    landmarks = json.loads(landmarks_json)
    if len(landmarks) < 33:
        return json.dumps({
            "angles": {},
            "stage": _state["stage"],
            "reps": _state["reps"],
            "feedback": "No person detected",
        })

    angles = {}
    for name, (a, b, c) in JOINTS.items():
        pa, pb, pc = landmarks[a], landmarks[b], landmarks[c]
        if _visible(pa) and _visible(pb) and _visible(pc):
            angles[name] = round(
                _angle((pa["x"], pa["y"]), (pb["x"], pb["y"]), (pc["x"], pc["y"])), 1
            )

    knees = [angles[k] for k in ("left_knee", "right_knee") if k in angles]
    if not knees:
        feedback = "Step back so your legs are visible"
    else:
        knee = float(np.mean(knees))
        if knee > SQUAT_UP_ANGLE:
            if _state["stage"] == "down":
                _state["reps"] += 1
            _state["stage"] = "up"
            feedback = "Squat down"
        elif knee < SQUAT_DOWN_ANGLE:
            _state["stage"] = "down"
            feedback = "Good depth, stand up"
        else:
            feedback = "Go lower" if _state["stage"] != "down" else "Stand up fully"

    return json.dumps({
        "angles": angles,
        "stage": _state["stage"],
        "reps": _state["reps"],
        "feedback": feedback,
    })

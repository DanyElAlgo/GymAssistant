# GymAssistant

An on-device assistant that watches a beginner gym user perform a machine exercise through the phone's camera and gives live feedback on their technique.

## Language

### Training

**Exercise**:
A specific gym machine movement the app can assist with (e.g. lat pulldown, leg press, leg extension).
_Avoid_: Workout, movement, routine

**Rep**:
One full cycle of an Exercise's movement, from the start position through the working phase and back.
_Avoid_: Repetition count, cycle

**Set**:
A continuous run of Reps of one Exercise, from when the user starts until they stop.
_Avoid_: Series, round

**Session**:
All the Sets a user performs in one gym visit.
_Avoid_: Workout, training

### Technique

**Postural error**:
A named, detectable deviation from correct technique for a specific Exercise (e.g. bar behind the neck on the lat pulldown).
_Avoid_: Mistake, bad form, fault

**Cue**:
The live indication shown to the user while they move, saying whether their current pose is correct or shows a Postural error.
_Avoid_: Hint, alert, message, feedback

**Rep verdict**:
The judgement recorded for a finished Rep: correct, or incorrect because of a Postural error.
_Avoid_: Rep score, rep result

### Progress

**Goal**:
A measurable target a user or instructor sets for one Exercise over a period, made of a metric, a condition and a target value (e.g. at least 90% correct Reps on the lat pulldown over 4 weeks).
_Avoid_: Milestone, meta, objective, target

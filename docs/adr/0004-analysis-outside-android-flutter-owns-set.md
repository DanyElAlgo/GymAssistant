# On-device analysis lives outside `android/`; Flutter owns the Set

The **on-device analysis** is a plain Python package at the repo root (`analysis/src/gym_analysis/`, tests in `analysis/tests/`), not in Chaquopy's default `android/app/src/main/python`. Chaquopy is pointed at it with `srcDir("../../analysis/src")`, which packages only the package and never the tests. This lets pytest replay the reference recordings on desktop CPython and in CI without Gradle or Android. Packaging and runtime are unchanged: Chaquopy copies and compiles a `srcDir` exactly like its default folder, so there is no performance cost.

Each Exercise is one Python module behind a registry keyed by Exercise id. Kotlin is a headless camera/pose/Chaquopy adapter that passes the Exercise id through without interpreting it, so adding an Exercise never touches Kotlin. **Flutter's application layer owns the Set lifecycle and the Set record**: it starts and ends the Set, collects Rep events and saves the finished Set. Python holds only the running Set's analysis state and never decides when a Set is saved.

## Considered Options

- **Keep Python under `android/app/src/main/python`** (Chaquopy default): rejected because tests and CI would have to reach into the Android tree, which ties the analysis to the Android project.
- **Point `srcDir` at `analysis/`**: rejected because `analysis/tests/` would be bundled into the APK.
- **Python drives the Set (decides start/end and saves it)**: rejected because the Set is a user-facing record that Flutter shows and stores. Keeping Python to per-frame and per-Rep judgement keeps it pure and replayable.

## Consequences

- Don't "fix" the Python location back into `android/`. The split is deliberate.
- A second Exercise means a new Python module and registry entry, a recordings folder, and one entry in Flutter's Exercise catalogue.

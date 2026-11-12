# Analysis is fully on-device; the cloud backend only receives finished Sets

All per-frame work (landmarks, angles, the Postural error check, Rep state and the Rep verdict) runs offline on the phone, in the **on-device analysis** (Python via Chaquopy). The **cloud backend** is a separate Python server that never sees frames, landmarks or video: it receives only finished Sets (Exercise, start/end, Rep count, one Rep verdict per Rep) from an upload queue on the phone, and computes long-term progress and Goals from them. A live Cue must fit a 33–66 ms frame budget and work in gyms without connectivity, which rules out cloud analysis; keeping the cloud to finished Sets also keeps uploads small and video off the network.

## Considered Options

- **Analyse frames in the cloud**: rejected for latency and because the app must work offline (thesis Alcances §11).
- **Database only in the cloud (Supabase backup)**: replaced, because progress and Goal evaluation is its own workflow and belongs to a backend that owns it, not to the phone.

## Consequences

- The phone's Rep verdict is final; the cloud backend never re-judges technique.
- Each saved Set carries a stable client-generated ID, so queued uploads can be retried without duplicates.
- "Python backend" is ambiguous; say **on-device analysis** or **cloud backend**.

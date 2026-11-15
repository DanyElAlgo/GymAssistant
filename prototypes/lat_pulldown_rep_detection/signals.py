"""PROTOTYPE: turn landmarks.json into per-frame signals for the HTML demo."""
import json, math, sys
d = json.load(open(sys.argv[1])); K = {k: i for i, k in enumerate(d["keep"])}
W, H = 1080, 1920
def px(p): return (p[0] * W, p[1] * H)
def angle(a, b, c):
    a, b, c = px(a), px(b), px(c)
    r = abs(math.degrees(math.atan2(c[1]-b[1], c[0]-b[0]) - math.atan2(a[1]-b[1], a[0]-b[0])))
    return 360 - r if r > 180 else r
out = {}
for name, c in d["clips"].items():
    wh, ea = [], []
    for f in c["frames"]:
        if f is None:
            wh.append(None); ea.append(None); continue
        sh = [f[K[11]], f[K[12]]]; el = [f[K[13]], f[K[14]]]; wr = [f[K[15]], f[K[16]]]; hp = [f[K[23]], f[K[24]]]
        s = 0 if sh[0][2] + el[0][2] + wr[0][2] >= sh[1][2] + el[1][2] + wr[1][2] else 1  # side nearer the camera
        smid = ((px(sh[0])[0] + px(sh[1])[0]) / 2, (px(sh[0])[1] + px(sh[1])[1]) / 2)
        hmid = ((px(hp[0])[0] + px(hp[1])[0]) / 2, (px(hp[0])[1] + px(hp[1])[1]) / 2)
        torso = math.dist(smid, hmid)
        wh.append(round((px(sh[s])[1] - px(wr[s])[1]) / torso, 3))
        ea.append(round(angle(sh[s], el[s], wr[s]), 1))
    out[name] = {"fps": c["fps"], "wrist": wh, "elbow": ea}
html = open(sys.argv[2], encoding="utf-8").read().replace("/*__CLIPS__*/null", json.dumps(out, separators=(",", ":")))
open(sys.argv[3], "w", encoding="utf-8").write(html)
print("wrote", sys.argv[3], len(html) // 1024, "KB")

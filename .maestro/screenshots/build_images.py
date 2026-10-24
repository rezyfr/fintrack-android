"""Builds the images in docs/screenshots from the Maestro screenshots of light.yaml and dark.yaml."""
import glob
import os

from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
OUT = os.path.join(ROOT, "docs", "screenshots") + os.sep


def newest(*patterns):
    found = [d for p in patterns for d in glob.glob(os.path.join(ROOT, ".maestro/reports/debug", p, "*/takeScreenshot/"))]
    return sorted(found, key=os.path.getmtime)[-1]


LIGHT = newest("*light*", "*showcase*")
DARK = newest("*dark*")
FEATURES = newest("*features*")
FEATURE_SHOTS = {"budget-compare", "schedules-empty", "schedules-list", "schedule-start", "schedule-what", "schedule-when", "schedule-warning"}


def phone(name, w=540, bezel=7, radius=54):
    folder = DARK if name.endswith("-dark") else FEATURES if name in FEATURE_SHOTS else LIGHT
    im = Image.open(folder + name + ".png").convert("RGB")
    im = im.crop((0, 0, im.width, int(im.height * 0.945)))  # without the system buttons
    im = im.resize((w, int(im.height * w / im.width)), Image.LANCZOS)
    r = Image.new("RGBA", (w + 2 * bezel, im.height + 2 * bezel), (0, 0, 0, 0))
    ImageDraw.Draw(r).rounded_rectangle((0, 0, r.width - 1, r.height - 1), radius + bezel, fill=(16, 22, 19, 255))
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height - 1), radius, fill=255)
    r.paste(im, (bezel, bezel), mask)
    return r


def save(img, name):
    img.save(OUT + name, optimize=True)


def strip(names, fname, bg, w=420, gap=28, pad=36):
    ps = [phone(n, w) for n in names]
    base = Image.new("RGBA", (sum(p.width for p in ps) + gap * (len(ps) - 1) + 2 * pad, max(p.height for p in ps) + 2 * pad), bg)
    x = pad
    for p in ps:
        base.paste(p, (x, pad), p)
        x += p.width + gap
    save(base, fname)


def hero():
    w, h = 2000, 1000
    bg = Image.new("RGB", (w, h))
    top, bot = (8, 70, 56), (5, 38, 31)
    for y in range(h):
        c = tuple(int(top[i] * (1 - y / h) + bot[i] * y / h) for i in range(3))
        ImageDraw.Draw(bg).line((0, y, w, y), fill=c)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((w // 2 - 700, -300, w // 2 + 700, 500), fill=(76, 194, 160, 70))
    bg = Image.alpha_composite(bg.convert("RGBA"), glow.filter(ImageFilter.GaussianBlur(140)))
    names, sizes, ys = ["transactions", "budget", "home", "rules-list", "suggest-rules"], [420, 460, 520, 460, 420], [300, 240, 150, 240, 300]
    ps = [phone(n, s) for n, s in zip(names, sizes)]
    xs, cx = [], (w - (sum(p.width for p in ps) - 120)) // 2
    for p in ps:
        xs.append(cx)
        cx += p.width - 30
    for i in [0, 1, 3, 4, 2]:
        sh = Image.new("RGBA", (ps[i].width + 120, ps[i].height + 120), (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((60, 60, 60 + ps[i].width, 60 + ps[i].height), 70, fill=(0, 0, 0, 150))
        bg.alpha_composite(sh.filter(ImageFilter.GaussianBlur(26)), (xs[i] - 60, ys[i] - 40))
        bg.alpha_composite(ps[i], (xs[i], ys[i]))
    save(bg.convert("RGB").crop((0, 120, w, h)), "hero.png")


for n in ["home", "transactions", "budget", "insights", "accounts", "add-transaction", "settings", "budget-compare"]:
    save(phone(n, 460), n + ".png")
strip(["setup-welcome", "setup-currency", "setup-cycle", "setup-accounts"], "setup-strip.png", (234, 238, 234, 255))
strip(["rules-list", "rule-editor", "suggest-rules"], "rules-strip.png", (234, 238, 234, 255), w=470)
strip(["schedules-list", "schedule-what", "schedule-when", "schedule-warning"], "schedules-strip.png", (234, 238, 234, 255), w=420)
strip(["home-dark", "budget-dark", "insights-dark", "rules-dark"], "dark-strip.png", (14, 21, 18, 255))
hero()

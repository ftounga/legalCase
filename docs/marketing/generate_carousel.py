# -*- coding: utf-8 -*-
#!/usr/bin/env python3
"""
Generate LinkedIn carousel PDF for AI LegalCase.
No emojis — geometric shapes only.
Output: m32-carousel-comment-ca-marche.pdf
"""

import os
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

# ── Fonts ────────────────────────────────────────────────────────────────────
NOTO_SERIF_BOLD    = "/usr/share/fonts/truetype/noto/NotoSerif-Bold.ttf"
NOTO_SERIF_REG     = "/usr/share/fonts/truetype/noto/NotoSerif-Regular.ttf"
NOTO_SANS_BOLD     = "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
NOTO_SANS_REG      = "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
NOTO_SANS_ITALIC   = "/usr/share/fonts/truetype/noto/NotoSans-Italic.ttf"

pdfmetrics.registerFont(TTFont("NotoSerifBold",  NOTO_SERIF_BOLD))
pdfmetrics.registerFont(TTFont("NotoSerifReg",   NOTO_SERIF_REG))
pdfmetrics.registerFont(TTFont("NotoSansBold",   NOTO_SANS_BOLD))
pdfmetrics.registerFont(TTFont("NotoSansReg",    NOTO_SANS_REG))
pdfmetrics.registerFont(TTFont("NotoSansItalic", NOTO_SANS_ITALIC))

# ── Colors ───────────────────────────────────────────────────────────────────
NAVY      = HexColor("#1A3A5C")
GOLD      = HexColor("#C9973A")
WHITE     = HexColor("#FFFFFF")
LIGHT_BG  = HexColor("#F5F6FA")
GRAY      = HexColor("#999999")

# ── Page setup ───────────────────────────────────────────────────────────────
W = H = 1080   # square slide
MARGIN = 70

OUTPUT = os.path.join(os.path.dirname(__file__), "m32-carousel-comment-ca-marche.pdf")


# ── Helpers ──────────────────────────────────────────────────────────────────

def fill_bg(c, color):
    c.setFillColor(color)
    c.rect(0, 0, W, H, fill=1, stroke=0)


def gold_circle_bullet(c, x, y, r=5):
    """Draw a small filled gold circle as a bullet marker."""
    c.setFillColor(GOLD)
    c.circle(x, y, r, fill=1, stroke=0)


def gold_line(c, x, y, width, thickness=2):
    c.setStrokeColor(GOLD)
    c.setLineWidth(thickness)
    c.line(x, y, x + width, y)


def gray_line(c, x, y, width, thickness=1):
    c.setStrokeColor(GRAY)
    c.setLineWidth(thickness)
    c.line(x, y, x + width, y)


def draw_text(c, text, x, y, font, size, color, align="left"):
    c.setFillColor(color)
    c.setFont(font, size)
    if align == "center":
        c.drawCentredString(x, y, text)
    elif align == "right":
        c.drawRightString(x, y, text)
    else:
        c.drawString(x, y, text)


def brand_tag(c, bg_is_navy=True):
    """Bottom-right brand tag 'AI LegalCase'."""
    color = GOLD if bg_is_navy else GOLD
    draw_text(c, "AI LegalCase", W - MARGIN, 38, "NotoSansBold", 15, color, align="right")


def wrap_text_lines(c, lines, x, y, font, size, color, line_gap=30):
    """Draw a list of pre-wrapped text lines."""
    c.setFillColor(color)
    c.setFont(font, size)
    for line in lines:
        c.drawString(x, y, line)
        y -= line_gap
    return y


def wrap_centered_lines(c, lines, cx, y, font, size, color, line_gap=30):
    c.setFillColor(color)
    c.setFont(font, size)
    for line in lines:
        c.drawCentredString(cx, y, line)
        y -= line_gap
    return y


# ── Slides ───────────────────────────────────────────────────────────────────

def slide1(c):
    """Cover — navy background."""
    fill_bg(c, NAVY)

    # Top-left brand with gold underline
    brand_x = MARGIN
    brand_y = H - MARGIN - 5
    draw_text(c, "AI LegalCase", brand_x, brand_y, "NotoSansBold", 16, GOLD)
    gold_line(c, brand_x, brand_y - 6, 90, 2)

    # Centered question title (multi-line, white Noto Serif Bold ~56px)
    title_lines = [
        "Vous passez combien d\u2019heures",
        "\u00e0 lire vos dossiers avant de",
        "pouvoir conseiller\u00a0?",
    ]
    title_font_size = 54
    line_h = 66
    total_title_h = len(title_lines) * line_h
    title_top_y = H / 2 + total_title_h / 2 + 20

    c.setFillColor(WHITE)
    c.setFont("NotoSerifBold", title_font_size)
    y = title_top_y
    for line in title_lines:
        c.drawCentredString(W / 2, y, line)
        y -= line_h

    # Gold separator line
    sep_y = y - 20
    gold_line(c, W / 2 - 75, sep_y, 150, 3)

    # Subtitle
    subtitle_y = sep_y - 40
    subtitle_lines = [
        "Voici comment les avocats r\u00e9duisent",
        "ce temps \u00e0 quelques minutes.",
    ]
    wrap_centered_lines(c, subtitle_lines, W / 2, subtitle_y, "NotoSansReg", 20, WHITE, line_gap=32)

    # Bottom-right "Swipez ->"
    draw_text(c, "Swipez  ->", W - MARGIN, 42, "NotoSansBold", 16, GOLD, align="right")


def slide2(c):
    """Problem — light background."""
    fill_bg(c, LIGHT_BG)

    # Title
    title_y = H - MARGIN - 20
    draw_text(c, "Le probl\u00e8me que personne", MARGIN, title_y, "NotoSerifBold", 38, NAVY)
    draw_text(c, "ne r\u00e9sout vraiment", MARGIN, title_y - 48, "NotoSerifBold", 38, NAVY)

    # Gold underline
    gold_line(c, MARGIN, title_y - 70, 200, 2)

    # Bullets
    bullet_items = [
        "Des dizaines de PDFs \u00e0 lire pour chaque dossier",
        "Des heures de lecture avant de pouvoir conseiller",
        "Un risque d\u2019oubli sous la pression des d\u00e9lais",
    ]
    bullet_x_circle = MARGIN + 10
    bullet_x_text   = MARGIN + 30
    bullet_start_y  = title_y - 140
    bullet_gap      = 60

    for i, item in enumerate(bullet_items):
        by = bullet_start_y - i * bullet_gap
        # Gold circle bullet
        gold_circle_bullet(c, bullet_x_circle, by + 7, r=6)
        draw_text(c, item, bullet_x_text, by, "NotoSansReg", 21, NAVY)

    # Bottom italic note
    draw_text(c, "Il y a une meilleure fa\u00e7on.", MARGIN, 80, "NotoSansItalic", 17, NAVY)

    brand_tag(c, bg_is_navy=False)


def slide3(c):
    """Step 01 — navy background."""
    fill_bg(c, NAVY)

    # Large "01"
    draw_text(c, "01", 60, H - 175, "NotoSerifBold", 130, GOLD)

    # Title
    title_y = H - 290
    draw_text(c, "Cr\u00e9ez un dossier.", MARGIN, title_y, "NotoSerifBold", 44, WHITE)
    draw_text(c, "Glissez vos documents.", MARGIN, title_y - 56, "NotoSerifBold", 44, WHITE)

    # Gold separator
    gold_line(c, MARGIN, title_y - 85, 250, 2)

    # Body
    body_lines = [
        "Contrats, courriers, jugements, attestations \u2014",
        "tous vos PDFs en un seul endroit.",
    ]
    wrap_text_lines(c, body_lines, MARGIN, title_y - 130, "NotoSansReg", 20, WHITE, line_gap=34)

    # Bottom arrow + time label
    arrow_y = 68
    draw_text(c, "->  30 secondes", MARGIN, arrow_y, "NotoSansBold", 18, GOLD)

    brand_tag(c, bg_is_navy=True)


def slide4(c):
    """Step 02 — light background."""
    fill_bg(c, LIGHT_BG)

    # Large "02"
    draw_text(c, "02", 60, H - 175, "NotoSerifBold", 130, NAVY)

    # Title
    title_y = H - 290
    draw_text(c, "Un clic. L\u2019IA lit tout.", MARGIN, title_y, "NotoSerifBold", 44, NAVY)

    # Gold separator
    gold_line(c, MARGIN, title_y - 40, 250, 2)

    # Body
    body_lines = [
        "AI LegalCase analyse l\u2019int\u00e9gralit\u00e9 de vos documents en temps r\u00e9el.",
        "Une lecture compl\u00e8te, structur\u00e9e, juridiquement pr\u00e9cise.",
    ]
    wrap_text_lines(c, body_lines, MARGIN, title_y - 90, "NotoSansReg", 20, NAVY, line_gap=34)

    # Bottom arrow
    arrow_y = 68
    draw_text(c, "->  Quelques minutes", MARGIN, arrow_y, "NotoSansBold", 18, GOLD)

    brand_tag(c, bg_is_navy=False)


def slide5(c):
    """Step 03 — navy background."""
    fill_bg(c, NAVY)

    # Large "03"
    draw_text(c, "03", 60, H - 175, "NotoSerifBold", 130, GOLD)

    # Title
    title_y = H - 290
    draw_text(c, "Votre synth\u00e8se compl\u00e8te.", MARGIN, title_y, "NotoSerifBold", 40, WHITE)
    draw_text(c, "Pr\u00eate \u00e0 l\u2019emploi.", MARGIN, title_y - 52, "NotoSerifBold", 40, WHITE)

    # Gold separator
    gold_line(c, MARGIN, title_y - 82, 250, 2)

    # Bullet items: (bold label, regular description)
    bullet_items = [
        ("Faits cl\u00e9s",        "Les \u00e9l\u00e9ments d\u00e9terminants du dossier"),
        ("Risques juridiques", "Class\u00e9s par niveau, avec r\u00e9f\u00e9rence l\u00e9gale"),
        ("Timeline",          "Chronologie compl\u00e8te des \u00e9v\u00e9nements"),
        ("Points de droit",   "Articles et jurisprudences applicables"),
    ]

    bullet_x_circle = MARGIN + 10
    bullet_x_text   = MARGIN + 30
    start_y = title_y - 140
    gap = 52

    for i, (label, desc) in enumerate(bullet_items):
        by = start_y - i * gap
        gold_circle_bullet(c, bullet_x_circle, by + 6, r=5)
        # Bold label in gold
        c.setFillColor(GOLD)
        c.setFont("NotoSansBold", 19)
        c.drawString(bullet_x_text, by, label + " \u2014 ")
        label_w = c.stringWidth(label + " \u2014 ", "NotoSansBold", 19)
        draw_text(c, desc, bullet_x_text + label_w, by, "NotoSansReg", 19, WHITE)

    brand_tag(c, bg_is_navy=True)


def slide6(c):
    """CTA — light background."""
    fill_bg(c, LIGHT_BG)

    cx = W / 2

    # Main title
    title_lines = ["Essayez sur un vrai dossier."]
    title_y = H / 2 + 200
    wrap_centered_lines(c, title_lines, cx, title_y, "NotoSerifBold", 52, NAVY, line_gap=68)

    # Gold centered line
    gold_line(c, cx - 60, title_y - 70, 120, 3)

    # Subtitle lines
    sub1_y = title_y - 120
    draw_text(c, "14 jours gratuits. Toutes les fonctionnalit\u00e9s.", cx, sub1_y, "NotoSansReg", 22, NAVY, align="center")
    draw_text(c, "Sans carte bancaire.", cx, sub1_y - 38, "NotoSansReg", 20, NAVY, align="center")

    # Gray separator
    sep_y = sub1_y - 90
    gray_line(c, cx - 150, sep_y, 300, 1)

    # URL
    draw_text(c, "legalcase.ng-itconsulting.com", cx, sep_y - 48, "NotoSansBold", 26, GOLD, align="center")

    # Tagline
    draw_text(c,
              "Con\u00e7u pour les avocats du travail, de l\u2019immigration et de la famille.",
              cx, sep_y - 100, "NotoSansReg", 15, GRAY, align="center")

    brand_tag(c, bg_is_navy=False)


# ── Main ─────────────────────────────────────────────────────────────────────

def build_pdf():
    c = canvas.Canvas(OUTPUT, pagesize=(W, H))

    slides = [slide1, slide2, slide3, slide4, slide5, slide6]
    for i, slide_fn in enumerate(slides, 1):
        slide_fn(c)
        c.showPage()
        print(f"  Slide {i} done.")

    c.save()
    size = os.path.getsize(OUTPUT)
    print(f"\nSaved: {OUTPUT}")
    print(f"Size:  {size:,} bytes  ({size/1024:.1f} KB)")


if __name__ == "__main__":
    build_pdf()

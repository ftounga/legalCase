"""
Convert .txt fixtures to PDFs with proper Unicode support (accents, ş, etc.)
Used to regenerate test-data PDFs for staging demo (e.g. Vermeersch BE divorce, Yilmaz BE asylum).

Usage:
    python3 convert_txt_to_pdf.py <folder>
    python3 convert_txt_to_pdf.py dossier-divorce-belgique-vermeersch
    python3 convert_txt_to_pdf.py --all  # process all dossier-*/

For each <prefix>.txt in the folder, generates <prefix>.pdf.
Detects "Field : value" patterns and renders them as one paragraph per line.
"""
import os
import re
import sys
from pathlib import Path

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)

DEJAVU = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
DEJAVU_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

pdfmetrics.registerFont(TTFont("DejaVuSans", DEJAVU))
if os.path.exists(DEJAVU_BOLD):
    pdfmetrics.registerFont(TTFont("DejaVuSans-Bold", DEJAVU_BOLD))

FIELD_LINE_PATTERN = re.compile(r"^([^:]+?)\s*:\s*(.+)$")


def txt_to_pdf(txt_path: Path, pdf_path: Path) -> None:
    text = txt_path.read_text(encoding="utf-8")
    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=2 * cm,
        rightMargin=2 * cm,
        topMargin=2 * cm,
        bottomMargin=2 * cm,
    )

    styles = getSampleStyleSheet()
    body_style = ParagraphStyle(
        "body",
        parent=styles["BodyText"],
        fontName="DejaVuSans",
        fontSize=10,
        leading=14,
        spaceAfter=4,
    )
    heading_style = ParagraphStyle(
        "heading",
        parent=styles["Heading2"],
        fontName="DejaVuSans-Bold" if os.path.exists(DEJAVU_BOLD) else "DejaVuSans",
        fontSize=12,
        leading=16,
        spaceBefore=8,
        spaceAfter=6,
    )

    story = []
    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        if not line:
            story.append(Spacer(1, 0.3 * cm))
            continue

        # Section heading (uppercase line with no colon, or ALL CAPS)
        is_heading = (
            (line.isupper() and len(line) > 3 and ":" not in line)
            or line.startswith("===")
            or line.startswith("---")
        )
        if is_heading:
            story.append(Paragraph(_escape(line), heading_style))
            continue

        # Field line "Name : value"
        m = FIELD_LINE_PATTERN.match(line)
        if m:
            field, value = m.group(1), m.group(2)
            story.append(
                Paragraph(
                    f"<b>{_escape(field)} :</b> {_escape(value)}",
                    body_style,
                )
            )
            continue

        # Default paragraph
        story.append(Paragraph(_escape(line), body_style))

    doc.build(story)
    print(f"✓ {pdf_path.name}")


def _escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    )


def process_folder(folder: Path) -> None:
    if not folder.exists():
        print(f"✗ {folder} does not exist", file=sys.stderr)
        return
    txt_files = sorted(folder.glob("*.txt"))
    if not txt_files:
        print(f"  (no .txt in {folder})")
        return
    print(f"→ {folder.name}")
    for txt in txt_files:
        pdf = txt.with_suffix(".pdf")
        txt_to_pdf(txt, pdf)


def main() -> None:
    args = sys.argv[1:]
    base = Path(__file__).parent
    if not args:
        print(__doc__)
        return
    if args[0] == "--all":
        for sub in sorted(base.iterdir()):
            if sub.is_dir() and sub.name.startswith("dossier-"):
                process_folder(sub)
        return
    for target in args:
        process_folder(base / target)


if __name__ == "__main__":
    main()

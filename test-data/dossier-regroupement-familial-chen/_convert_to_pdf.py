#!/usr/bin/env python3
"""
Convertit les .txt du dossier Chen en PDF A4 propres pour upload
LegalCase (vidéo V5). Chaque .txt → PDF portant le même nom.

Rendu : police Helvetica 10pt, marges 20mm, conservation des retours à la ligne.
Produit des PDF avec texte natif — extraction classique LegalCase (pas OCR).
"""
import os
import sys
from pathlib import Path
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_LEFT
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

HERE = Path(__file__).parent

STYLES = getSampleStyleSheet()
BODY = ParagraphStyle(
    "body",
    parent=STYLES["Normal"],
    fontName="Helvetica",
    fontSize=10,
    leading=13,
    alignment=TA_LEFT,
    spaceAfter=2,
)
TITLE = ParagraphStyle(
    "title",
    parent=STYLES["Normal"],
    fontName="Helvetica-Bold",
    fontSize=12,
    leading=15,
    alignment=TA_LEFT,
    spaceAfter=6,
    spaceBefore=6,
)
SEP = ParagraphStyle(
    "sep",
    parent=STYLES["Normal"],
    fontName="Helvetica",
    fontSize=8,
    textColor=(0.4, 0.4, 0.4),
    alignment=TA_LEFT,
    spaceAfter=4,
    spaceBefore=4,
)


def line_to_paragraph(line: str):
    stripped = line.rstrip()
    # Convertit caractères HTML-significatifs
    safe = (
        stripped.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    )
    if not safe.strip():
        return Spacer(1, 4 * mm)
    if safe.strip() == "---":
        return Paragraph(
            '<para><font color="#888888">' + "_" * 80 + "</font></para>",
            SEP,
        )
    # Heuristique : ligne majuscule en début → titre
    is_title = (
        stripped.isupper()
        and len(stripped) >= 4
        and any(c.isalpha() for c in stripped)
    )
    style = TITLE if is_title else BODY
    return Paragraph(safe, style)


def convert(txt_path: Path, pdf_path: Path):
    with txt_path.open(encoding="utf-8") as f:
        content = f.read()
    flow = [line_to_paragraph(l) for l in content.splitlines()]
    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=20 * mm,
        rightMargin=20 * mm,
        topMargin=20 * mm,
        bottomMargin=20 * mm,
        title=pdf_path.stem,
        author="LegalCase Test Data",
    )
    doc.build(flow)
    print(f"  ✓ {pdf_path.name}")


def main():
    target_dir = HERE
    txt_files = sorted(target_dir.glob("*.txt"))
    if not txt_files:
        print("Aucun .txt trouvé dans", target_dir, file=sys.stderr)
        sys.exit(1)
    print(f"Conversion de {len(txt_files)} fichier(s) .txt → PDF")
    for t in txt_files:
        pdf_path = target_dir / (t.stem + ".pdf")
        convert(t, pdf_path)
    print("Terminé.")


if __name__ == "__main__":
    main()

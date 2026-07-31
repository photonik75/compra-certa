from pathlib import Path
import re

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import (
    Image,
    KeepTogether,
    ListFlowable,
    ListItem,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
)

BASE_DIR = Path(__file__).resolve().parent
SOURCE = BASE_DIR / "manual-do-usuario.md"
OUTPUT = BASE_DIR.parents[1] / "output" / "pdf" / "manual-do-usuario.pdf"
ACCENT = colors.HexColor("#166534")
ACCENT_LIGHT = colors.HexColor("#ECFDF3")
INK = colors.HexColor("#17202A")
MUTED = colors.HexColor("#566573")


def styles():
    sheet = getSampleStyleSheet()
    sheet.add(ParagraphStyle(
        name="ManualTitle", parent=sheet["Title"], fontName="Helvetica-Bold",
        fontSize=30, leading=35, textColor=ACCENT, alignment=TA_CENTER,
        spaceAfter=16,
    ))
    sheet.add(ParagraphStyle(
        name="ManualSubtitle", parent=sheet["Heading2"], fontName="Helvetica",
        fontSize=17, leading=22, textColor=INK, alignment=TA_CENTER,
        spaceAfter=12,
    ))
    sheet.add(ParagraphStyle(
        name="Chapter", parent=sheet["Heading1"], fontName="Helvetica-Bold",
        fontSize=20, leading=24, textColor=ACCENT, spaceBefore=8,
        spaceAfter=12, keepWithNext=True,
    ))
    sheet.add(ParagraphStyle(
        name="Section", parent=sheet["Heading2"], fontName="Helvetica-Bold",
        fontSize=14, leading=18, textColor=INK, spaceBefore=10,
        spaceAfter=7, keepWithNext=True,
    ))
    sheet.add(ParagraphStyle(
        name="BodyManual", parent=sheet["BodyText"], fontName="Helvetica",
        fontSize=10.5, leading=15, textColor=INK, spaceAfter=7,
    ))
    sheet.add(ParagraphStyle(
        name="Step", parent=sheet["BodyText"], fontName="Helvetica",
        fontSize=10.5, leading=15, textColor=INK, leftIndent=8,
        firstLineIndent=0, spaceAfter=4,
    ))
    sheet.add(ParagraphStyle(
        name="Note", parent=sheet["BodyText"], fontName="Helvetica",
        fontSize=10, leading=14, textColor=INK, backColor=ACCENT_LIGHT,
        borderColor=ACCENT, borderWidth=0.8, borderPadding=8,
        leftIndent=4, rightIndent=4, spaceBefore=5, spaceAfter=9,
    ))
    sheet.add(ParagraphStyle(
        name="Caption", parent=sheet["BodyText"], fontName="Helvetica-Oblique",
        fontSize=8.5, leading=11, textColor=MUTED, alignment=TA_CENTER,
        spaceBefore=4, spaceAfter=10,
    ))
    sheet.add(ParagraphStyle(
        name="Footer", parent=sheet["BodyText"], fontName="Helvetica",
        fontSize=8, textColor=MUTED,
    ))
    return sheet


def inline(text):
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"\*(.+?)\*", r"<i>\1</i>", text)
    return text


def page_number(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(colors.HexColor("#D5DBDB"))
    canvas.line(2 * cm, 1.45 * cm, A4[0] - 2 * cm, 1.45 * cm)
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(2 * cm, 1.05 * cm, "CompraCerta — Manual do usuário")
    canvas.drawRightString(A4[0] - 2 * cm, 1.05 * cm, f"Página {doc.page}")
    canvas.restoreState()


def scaled_image(path):
    image = Image(str(path))
    max_width = 14.4 * cm
    # Mantém espaço para legenda e continuação do procedimento na mesma página.
    max_height = 16 * cm
    factor = 0.5 * min(max_width / image.imageWidth, max_height / image.imageHeight)
    image.drawWidth = image.imageWidth * factor
    image.drawHeight = image.imageHeight * factor
    image.hAlign = "CENTER"
    return image


def build_story(lines, sheet):
    story = [Spacer(1, 4.5 * cm)]
    first_h1 = True
    pending_image = None
    in_style_block = False
    numbered = []
    bullets = []

    def flush_lists():
        nonlocal numbered, bullets
        if numbered:
            items = [ListItem(Paragraph(inline(item), sheet["Step"])) for item in numbered]
            story.append(ListFlowable(
                items, bulletType="1", start="1", leftIndent=24,
                bulletFontName="Helvetica-Bold", bulletColor=ACCENT,
            ))
            story.append(Spacer(1, 5))
            numbered = []
        if bullets:
            items = [ListItem(Paragraph(inline(item), sheet["Step"])) for item in bullets]
            story.append(ListFlowable(
                items, bulletType="bullet", leftIndent=24,
                bulletFontName="Helvetica", bulletColor=ACCENT,
            ))
            story.append(Spacer(1, 5))
            bullets = []

    for raw in lines:
        line = raw.strip()
        if line == "<style>":
            in_style_block = True
            continue
        if line == "</style>":
            in_style_block = False
            continue
        if in_style_block:
            continue
        if not line:
            flush_lists()
            continue
        if line == "---":
            flush_lists()
            story.append(Spacer(1, 12))
            continue
        match_image = re.match(r"!\[(.+?)\]\((.+?)\)", line)
        if match_image:
            flush_lists()
            image_path = (BASE_DIR / match_image.group(2)).resolve()
            pending_image = scaled_image(image_path)
            continue
        if pending_image and line.startswith("*Figura"):
            caption = Paragraph(inline(line.strip("*")), sheet["Caption"])
            story.append(KeepTogether([pending_image, caption]))
            pending_image = None
            continue
        match_number = re.match(r"\d+\.\s+(.+)", line)
        if match_number:
            bullets = []
            numbered.append(match_number.group(1))
            continue
        if line.startswith("- "):
            numbered = []
            bullets.append(line[2:])
            continue
        flush_lists()
        if line.startswith("# "):
            if first_h1:
                story.append(Paragraph(inline(line[2:]), sheet["ManualTitle"]))
                first_h1 = False
            else:
                story.append(Spacer(1, 10))
                story.append(Paragraph(inline(line[2:]), sheet["Chapter"]))
        elif line.startswith("## "):
            text = line[3:]
            style = "ManualSubtitle" if text == "Manual do usuário" else "Section"
            story.append(Paragraph(inline(text), sheet[style]))
        elif line.startswith("> "):
            story.append(Paragraph(inline(line[2:]), sheet["Note"]))
        else:
            story.append(Paragraph(inline(line), sheet["BodyManual"]))
    flush_lists()
    return story


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sheet = styles()
    text = SOURCE.read_text(encoding="utf-8")
    story = build_story(text.splitlines(), sheet)
    document = SimpleDocTemplate(
        str(OUTPUT), pagesize=A4, rightMargin=2 * cm, leftMargin=2 * cm,
        topMargin=1.8 * cm, bottomMargin=2 * cm,
        title="CompraCerta — Manual do usuário",
        author="CompraCerta",
        subject="Orientações de uso do CompraCerta",
    )
    document.build(story, onFirstPage=page_number, onLaterPages=page_number)
    print(OUTPUT)


if __name__ == "__main__":
    main()

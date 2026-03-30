# LinkedIn Company Page Banner — Design Philosophy

**File:** `linkedin-banner-ailegalcase.png`
**Dimensions:** 1128 × 191 px (LinkedIn company page banner specification)
**Generated with:** Python / Pillow

---

## Brand Identity

| Token | Value | Usage |
|-------|-------|-------|
| Background | `#1A3A5C` — deep navy blue | Full canvas fill |
| Accent | `#C9973A` — legal gold | Decorative line, URL text |
| Primary text | `#FFFFFF` — white | Brand name, tagline |
| Heading font | Noto Serif Display Bold | "AI LegalCase" — authority, elegance |
| Body font | Noto Sans Regular / Bold | Tagline, URL — clarity, modernity |

---

## Composition

The banner is structured around three horizontal zones:

```
┌─────────────────────────────────────────────────────────┐
│  [60px top margin]                                       │
│  AI LegalCase          ← white, serif bold, 64px        │
│  L'IA au service…      ← white, sans regular, 24px      │
│  [15px gap]                                              │
│════════════════════════════════════════════════════════  │  ← gold line (3px)
│  legalcase.ng-itconsulting.com   ← gold, sans 16px      │
└─────────────────────────────────────────────────────────┘
```

- **Left margin:** 60 px — gives breathing room aligned to the left third of the canvas.
- **Gold accent line:** full-width, 3 px, at y = 155. Acts as a visual separator between content and the URL footer zone. Drawn with slight transparency layering to avoid harshness.
- **URL:** right-aligned to 60 px from right edge, vertically centered in the 35 px footer strip below the gold line.

---

## Design Rationale

### Sobriety over decoration
The French legal audience is conservative. No gradients, no icons, no photography. The gold line is the single decorative element — it references the tradition of ruled lines in legal documents.

### Typographic hierarchy
Three levels, three type sizes (64 / 24 / 16 px), all in a single column to the left — conventional for institutional banners. The brand name is dominant; the tagline explains without competing.

### Color discipline
Only two colors in use: white for information, gold for emphasis and identity. The navy background provides maximum contrast (WCAG AA passes at all three text sizes).

### Serif for the brand, sans for the rest
`Noto Serif Display Bold` carries the weight of the brand name — it reads as established and precise. `Noto Sans` for the tagline and URL keeps the AI/technology dimension present without looking like a startup.

---

## Constraints respected

- All text elements remain within a 60 px horizontal margin (left and right).
- No element overflows the 191 px canvas height.
- The gold line runs edge-to-edge to anchor the composition.
- URL rendered in gold, not white, to distinguish it from product copy.

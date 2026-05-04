package fr.ailegalcase.superadmin;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * F-187 SF-187-01 — Composition du PDF one-pager traction commerciale.
 *
 * <p>Le service compose le PDF à la volée via Apache PDFBox (déjà présent dans
 * pom.xml). Le layout est figé (charte navy + or, polices PDF standard
 * Helvetica/Courier en V1 — l'embedding TTF Inter/JetBrains Mono est hors
 * périmètre).
 *
 * <p>Aucune persistance : chaque appel régénère un nouveau PDF.
 */
@Service
public class TractionOnePagerService {

    private static final Logger log = LoggerFactory.getLogger(TractionOnePagerService.class);

    /** Charte : navy #1A3A5C (titre / labels / texte courant). */
    private static final PDColor NAVY = rgb(0x1A, 0x3A, 0x5C);

    /** Charte : or #C9973A (chiffres KPI / accents). */
    private static final PDColor GOLD = rgb(0xC9, 0x97, 0x3A);

    /** Charte : gris foncé (texte secondaire). */
    private static final PDColor GREY = rgb(0x55, 0x55, 0x55);

    /** Page A4 : 595 x 842 points (PDF default unit). */
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_BOTTOM = 40f;

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private final Clock clock;

    /** Constructor par défaut Spring : utilise une horloge UTC système. */
    public TractionOnePagerService() {
        this(Clock.system(ZoneId.of("UTC")));
    }

    /**
     * Constructor de test (package-private) : permet d'injecter une horloge
     * fixe pour rendre la date footer (et le nom de fichier) déterministe.
     */
    TractionOnePagerService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Compose le PDF à partir de l'input (déjà validé) et des KPIs courants
     * (lus côté controller via {@code SuperAdminService.getMetrics(...)} pour
     * mutualiser le gate super-admin et la lecture).
     *
     * @param input payload utilisateur (déjà validé Bean Validation)
     * @param metrics métriques courantes lues côté controller via le service F-76
     * @return bytes du PDF (≥ 1 page, header %PDF-)
     */
    public byte[] generate(TractionOnePagerInput input, SuperAdminMetricsResponse metrics) {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float contentWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;

                // Position courante (curseur Y descendant depuis le haut)
                float y = pageHeight - MARGIN_TOP;

                // 1. Headline (Helvetica Bold 22pt navy)
                y = drawHeadline(cs, input.headline(), MARGIN_LEFT, y, contentWidth);
                y -= 18;

                // 2. Bandeau or sous le titre (séparateur visuel)
                drawAccentBar(cs, MARGIN_LEFT, y, 80f, 2f);
                y -= 25;

                // 3. KPIs en grille 2 colonnes
                y = drawKpisSection(cs, input.includeKpis(), metrics, MARGIN_LEFT, y, contentWidth);

                // 4. Verbatims (si présents)
                if (input.verbatims() != null && !input.verbatims().isEmpty()) {
                    y -= 10;
                    y = drawVerbatimsSection(cs, input.verbatims(), MARGIN_LEFT, y, contentWidth);
                }

                // 5. Partenaires (si présents)
                if (input.partners() != null && !input.partners().isEmpty()) {
                    y -= 10;
                    y = drawPartnersSection(cs, input.partners(), MARGIN_LEFT, y, contentWidth);
                }

                // 6. Footer (date + contact) — toujours en bas de page
                drawFooter(cs, input.contact(), MARGIN_LEFT, MARGIN_BOTTOM, contentWidth, pageWidth);
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IOException ex) {
            log.error("Failed to compose traction one-pager PDF", ex);
            throw new TractionOnePagerGenerationException("PDF generation failed", ex);
        }
    }

    /** Date du jour (utilisée pour le footer + nom de fichier). */
    public String today() {
        return LocalDate.now(clock.withZone(ZoneId.of("UTC"))).format(ISO_DATE);
    }

    // ---------------------------------------------------------------------
    // Layout helpers
    // ---------------------------------------------------------------------

    private float drawHeadline(PDPageContentStream cs, String headline, float x, float y, float width) throws IOException {
        return drawWrappedText(cs, headline,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22f,
                NAVY, x, y, width, 26f);
    }

    private void drawAccentBar(PDPageContentStream cs, float x, float y, float width, float thickness) throws IOException {
        cs.setNonStrokingColor(GOLD);
        cs.addRect(x, y, width, thickness);
        cs.fill();
        cs.setNonStrokingColor(NAVY); // reset
    }

    private float drawKpisSection(PDPageContentStream cs, TractionOnePagerInput.IncludedKpis flags,
                                   SuperAdminMetricsResponse metrics, float x, float y, float width) throws IOException {
        // Titre section
        y = drawText(cs, "Indicateurs courants",
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f, NAVY, x, y);
        y -= 18;

        List<Kpi> kpis = collectKpis(flags, metrics);
        if (kpis.isEmpty()) {
            return y;
        }

        // Grille 2 colonnes : chaque case = (chiffre or 28pt en Courier Bold + label navy 11pt Helvetica)
        float colWidth = width / 2f;
        float rowHeight = 56f;
        for (int i = 0; i < kpis.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float cellX = x + col * colWidth;
            float cellY = y - row * rowHeight;

            Kpi kpi = kpis.get(i);
            // Chiffre (Courier Bold 28pt or)
            drawText(cs, kpi.value,
                    new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD), 28f, GOLD,
                    cellX, cellY);
            // Label (Helvetica 11pt navy)
            drawText(cs, kpi.label,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11f, NAVY,
                    cellX, cellY - 16f);
        }

        int rows = (kpis.size() + 1) / 2;
        return y - rows * rowHeight;
    }

    private List<Kpi> collectKpis(TractionOnePagerInput.IncludedKpis flags, SuperAdminMetricsResponse m) {
        List<Kpi> kpis = new ArrayList<>();
        if (Boolean.TRUE.equals(flags.totalWorkspaces())) {
            kpis.add(new Kpi(formatLong(m.totalWorkspaces()), "Workspaces inscrits"));
        }
        if (Boolean.TRUE.equals(flags.trialWorkspaces())) {
            kpis.add(new Kpi(formatLong(m.trialWorkspaces()), "Workspaces en essai"));
        }
        if (Boolean.TRUE.equals(flags.paidWorkspaces())) {
            kpis.add(new Kpi(formatLong(m.paidWorkspaces()), "Workspaces payants"));
        }
        if (Boolean.TRUE.equals(flags.conversionRatePct())) {
            kpis.add(new Kpi(formatPct(m.conversionRatePct()), "Taux de conversion"));
        }
        if (Boolean.TRUE.equals(flags.activeWorkspaces30d())) {
            kpis.add(new Kpi(formatLong(m.activeWorkspaces30d()), "Workspaces actifs (30j)"));
        }
        if (Boolean.TRUE.equals(flags.analysesLast7Days())) {
            kpis.add(new Kpi(formatLong(m.analysesLast7Days()), "Analyses des 7 derniers jours"));
        }
        if (Boolean.TRUE.equals(flags.analysesLast30Days())) {
            kpis.add(new Kpi(formatLong(m.analysesLast30Days()), "Analyses des 30 derniers jours"));
        }
        return kpis;
    }

    private float drawVerbatimsSection(PDPageContentStream cs, List<TractionOnePagerInput.VerbatimDto> verbatims,
                                        float x, float y, float width) throws IOException {
        y = drawText(cs, "Témoignages",
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f, NAVY, x, y);
        y -= 18;

        for (TractionOnePagerInput.VerbatimDto v : verbatims) {
            // Citation en italique
            y = drawWrappedText(cs, "« " + v.quote() + " »",
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), 11f,
                    GREY, x, y, width, 14f);
            y -= 4;
            // Auteur sur une ligne
            y = drawWrappedText(cs, "— " + v.author(),
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE), 10f,
                    NAVY, x, y, width, 13f);
            y -= 10;
        }
        return y;
    }

    private float drawPartnersSection(PDPageContentStream cs, List<String> partners,
                                       float x, float y, float width) throws IOException {
        y = drawText(cs, "Partenaires",
                new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14f, NAVY, x, y);
        y -= 16;

        for (String p : partners) {
            y = drawWrappedText(cs, "• " + p,
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11f, NAVY,
                    x, y, width, 14f);
        }
        return y;
    }

    private void drawFooter(PDPageContentStream cs, TractionOnePagerInput.ContactDto contact,
                             float x, float baselineY, float width, float pageWidth) throws IOException {
        // Bandeau or fin
        cs.setNonStrokingColor(GOLD);
        cs.addRect(x, baselineY + 28f, width, 1.2f);
        cs.fill();

        String date = today();
        // Ligne 1 : date à gauche, contact name+email au centre, url à droite
        drawText(cs,
                "Généré le " + date,
                new PDType1Font(Standard14Fonts.FontName.COURIER), 9f, GREY,
                x, baselineY + 14f);

        String contactLine = contact.name() + " — " + contact.email();
        drawText(cs, contactLine,
                new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9f, NAVY,
                x, baselineY);

        // URL alignée à droite (estimation simple : décalage selon longueur)
        drawText(cs, contact.url(),
                new PDType1Font(Standard14Fonts.FontName.COURIER), 9f, NAVY,
                pageWidth - MARGIN_RIGHT - estimateTextWidth(contact.url(), 9f, true), baselineY);
    }

    // ---------------------------------------------------------------------
    // PDFBox primitives
    // ---------------------------------------------------------------------

    private float drawText(PDPageContentStream cs, String text,
                            PDType1Font font, float fontSize, PDColor color,
                            float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y;
    }

    /**
     * Découpe le texte en lignes en respectant la largeur disponible et le
     * dessine. Retourne la position Y après la dernière ligne.
     */
    private float drawWrappedText(PDPageContentStream cs, String text,
                                   PDType1Font font, float fontSize, PDColor color,
                                   float x, float y, float maxWidth, float lineHeight) throws IOException {
        String safe = sanitize(text);
        List<String> lines = wrap(safe, font, fontSize, maxWidth);
        for (String line : lines) {
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, y);
            cs.showText(line);
            cs.endText();
            y -= lineHeight;
        }
        return y;
    }

    private List<String> wrap(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            float w = font.getStringWidth(candidate) / 1000f * fontSize;
            if (w > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (current.length() > 0) current.append(' ');
                current.append(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    /** Estime la largeur d'un texte (Helvetica si helvetica=false, Courier sinon). */
    private float estimateTextWidth(String text, float fontSize, boolean monospace) throws IOException {
        PDType1Font font = monospace
                ? new PDType1Font(Standard14Fonts.FontName.COURIER)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        return font.getStringWidth(sanitize(text)) / 1000f * fontSize;
    }

    /**
     * Les polices PDF standard (WinAnsi) ne supportent pas tous les caractères
     * Unicode. On remplace les caractères non encodables par un caractère sûr
     * (espace ou approximation ASCII).
     */
    private String sanitize(String input) {
        if (input == null) return "";
        // PDFBox lèvera une exception si un char est hors WinAnsi.
        // On garde les caractères latins courants (FR), on remplace le reste.
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                sb.append(' ');
            } else if (c >= 0x80 && c < 0xA0) {
                // contrôles C1 → espace
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static PDColor rgb(int r, int g, int b) {
        return new PDColor(new float[]{r / 255f, g / 255f, b / 255f}, PDDeviceRGB.INSTANCE);
    }

    private static String formatLong(long v) {
        return String.valueOf(v);
    }

    private static String formatPct(double v) {
        return String.format(Locale.ROOT, "%.1f%%", v);
    }

    private record Kpi(String value, String label) {}

    /** Exception interne pour signaler un échec de génération PDF. */
    public static class TractionOnePagerGenerationException extends RuntimeException {
        public TractionOnePagerGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

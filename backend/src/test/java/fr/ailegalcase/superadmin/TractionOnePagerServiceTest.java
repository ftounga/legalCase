package fr.ailegalcase.superadmin;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-187 SF-187-01 — Tests unitaires de {@link TractionOnePagerService}.
 *
 * <p>Ces tests exercent uniquement la composition PDF — la lecture des KPIs
 * via {@code SuperAdminService.getMetrics(...)} est testée séparément en IT.
 */
class TractionOnePagerServiceTest {

    /** Horloge fixe au 2026-05-04 UTC pour stabilité du footer / nom fichier. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));

    private final TractionOnePagerService service = new TractionOnePagerService(FIXED_CLOCK);

    private static SuperAdminMetricsResponse sampleMetrics() {
        return new SuperAdminMetricsResponse(
                42L,    // totalWorkspaces
                17L,    // activeWorkspaces30d
                25L,    // inactiveWorkspaces30d
                30L,    // trialWorkspaces
                12L,    // paidWorkspaces
                28.5,   // conversionRatePct
                123L,   // analysesLast7Days
                456L,   // analysesLast30Days
                7L      // newWorkspacesLast30Days
        );
    }

    private static TractionOnePagerInput.IncludedKpis allKpisOn() {
        return new TractionOnePagerInput.IncludedKpis(true, true, true, true, true, true, true);
    }

    private static TractionOnePagerInput.ContactDto sampleContact() {
        return new TractionOnePagerInput.ContactDto(
                "Franck Tounga", "franck@ng-itconsulting.com", "https://legalcase.ng-itconsulting.com");
    }

    /** UT-01 : cas nominal — 7 KPIs cochés, 2 verbatims, 3 partenaires. */
    @Test
    void generate_withFullPayload_producesValidPdf() throws IOException {
        TractionOnePagerInput input = new TractionOnePagerInput(
                allKpisOn(),
                List.of(
                        new TractionOnePagerInput.VerbatimDto(
                                "On a gagne 2 h par dossier en 1 mois",
                                "Maitre X, Cabinet Paris 8e"),
                        new TractionOnePagerInput.VerbatimDto(
                                "L outil m a evite de rediger une assignation a la main",
                                "Maitre Y, Lille")),
                List.of("Village de la Justice", "ACE", "Conseil National des Barreaux"),
                "L IA juridique souveraine pour les avocats droit du travail",
                sampleContact());

        byte[] pdf = service.generate(input, sampleMetrics());

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThanOrEqualTo(1024); // ≥ 1 KB
        assertThat(new String(pdf, 0, 5)).startsWith("%PDF-");

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            // PDFTextStripper réinjecte des sauts de ligne quand un texte est
            // wrappé sur plusieurs lignes — on normalise pour les assertions.
            String text = normalize(new PDFTextStripper().getText(doc));
            assertThat(text).contains("L IA juridique souveraine pour les avocats droit du travail");
            assertThat(text).contains("Workspaces inscrits");
            assertThat(text).contains("Workspaces payants");
            assertThat(text).contains("Maitre X, Cabinet Paris 8e");
            assertThat(text).contains("Village de la Justice");
            assertThat(text).contains("franck@ng-itconsulting.com");
        }
    }

    /** UT-02 : KPI {@code paidWorkspaces=false} → label "Workspaces payants" absent. */
    @Test
    void generate_whenPaidWorkspacesOff_omitsLabel() throws IOException {
        TractionOnePagerInput.IncludedKpis flags = new TractionOnePagerInput.IncludedKpis(
                true, true, false /*paid OFF*/, true, true, true, true);

        TractionOnePagerInput input = new TractionOnePagerInput(
                flags, List.of(), List.of(), "Headline test", sampleContact());

        byte[] pdf = service.generate(input, sampleMetrics());

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = normalize(new PDFTextStripper().getText(doc));
            assertThat(text).doesNotContain("Workspaces payants");
            assertThat(text).contains("Workspaces inscrits"); // les autres restent
            assertThat(text).contains("Taux de conversion");
        }
    }

    /** UT-03 : 0 verbatim → section "Témoignages" absente. */
    @Test
    void generate_whenNoVerbatims_omitsTestimonialsSection() throws IOException {
        TractionOnePagerInput input = new TractionOnePagerInput(
                allKpisOn(), List.of(), List.of("Partner Unique"), "Headline", sampleContact());

        byte[] pdf = service.generate(input, sampleMetrics());

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = normalize(new PDFTextStripper().getText(doc));
            assertThat(text).doesNotContain("Temoignages");
            assertThat(text).doesNotContain("Témoignages");
            assertThat(text).contains("Partenaires"); // partenaires conservés
            assertThat(text).contains("Partner Unique");
        }
    }

    /** UT-04 : 0 partenaire → section "Partenaires" absente. */
    @Test
    void generate_whenNoPartners_omitsPartnersSection() throws IOException {
        TractionOnePagerInput input = new TractionOnePagerInput(
                allKpisOn(),
                List.of(new TractionOnePagerInput.VerbatimDto("Une citation utile", "Auteur Test")),
                List.of(),
                "Headline",
                sampleContact());

        byte[] pdf = service.generate(input, sampleMetrics());

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = normalize(new PDFTextStripper().getText(doc));
            assertThat(text).doesNotContain("Partenaires");
            // Section verbatims présente
            assertThat(text).containsAnyOf("Temoignages", "Témoignages");
            assertThat(text).contains("Auteur Test");
        }
    }

    /** UT-05 : la date footer reflète l'horloge injectée. */
    @Test
    void generate_footerDateMatchesInjectedClock() throws IOException {
        TractionOnePagerInput input = new TractionOnePagerInput(
                allKpisOn(), List.of(), List.of(), "Headline", sampleContact());

        byte[] pdf = service.generate(input, sampleMetrics());

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String text = normalize(new PDFTextStripper().getText(doc));
            assertThat(text).contains("2026-05-04");
        }
        assertThat(service.today()).isEqualTo("2026-05-04");
    }

    /**
     * Normalise le texte extrait : retire les sauts de ligne pour rendre les
     * assertions {@code contains(...)} robustes au wrapping PDF (qui peut
     * couper un titre en plusieurs lignes selon la largeur disponible).
     */
    private static String normalize(String pdfText) {
        return pdfText.replace('\r', ' ').replace('\n', ' ').replaceAll(" +", " ");
    }
}

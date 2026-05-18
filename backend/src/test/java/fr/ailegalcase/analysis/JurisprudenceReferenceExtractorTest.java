package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-179 SF-179-01 — tests du pré-filtrage regex des références jurisprudentielles.
 */
class JurisprudenceReferenceExtractorTest {

    @Test
    void extract_nullOrBlank_returnsEmpty() {
        assertThat(JurisprudenceReferenceExtractor.extract(null)).isEmpty();
        assertThat(JurisprudenceReferenceExtractor.extract("   ")).isEmpty();
    }

    @Test
    void extract_textWithoutReference_returnsEmpty() {
        String text = "Le salarié a été convoqué à un entretien préalable le 12 mars 2024.";
        assertThat(JurisprudenceReferenceExtractor.extract(text)).isEmpty();
    }

    @Test
    void extract_cassationSociale_fr() {
        String text = "La partie adverse invoque Cass. soc. 25 septembre 2013, n° 12-17.516 "
                + "pour fonder sa thèse.";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0)).contains("Cass. soc.").contains("12-17.516");
    }

    @Test
    void extract_conseilEtat_fr() {
        String text = "Voir CE 30 juin 2017, n° 398445 sur la régularité de la procédure.";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0)).contains("398445");
    }

    @Test
    void extract_courAppel_fr() {
        String text = "Confirmé par CA Paris, 12 mars 2020, n° 18/12345.";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0)).contains("CA Paris").contains("18/12345");
    }

    @Test
    void extract_conseilConstitutionnel_fr() {
        String text = "Le Conseil constitutionnel, décision n°2020-800 QPC, a jugé que…";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0)).contains("2020-800");
    }

    @Test
    void extract_courConstitutionnelle_be() {
        String text = "La Cour constitutionnelle belge, dans son arrêt n° 45/2021, a estimé…";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).hasSize(1);
        assertThat(refs.get(0)).contains("45/2021");
    }

    @Test
    void extract_tribunalDuTravail_be() {
        String text = "Selon Trib. trav. Bruxelles, 05/05/2020, le licenciement est abusif.";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        assertThat(refs).isNotEmpty();
        assertThat(refs.get(0)).contains("Trib. trav. Bruxelles");
    }

    @Test
    void extract_multipleReferences_deduplicates() {
        String text = "Cass. soc. 25 septembre 2013, n° 12-17.516 puis à nouveau "
                + "Cass. soc. 25 septembre 2013, n° 12-17.516 et CE 30 juin 2017, n° 398445.";
        List<String> refs = JurisprudenceReferenceExtractor.extract(text);
        // 2 références distinctes — la cassation dupliquée n'apparaît qu'une fois.
        assertThat(refs).hasSize(2);
    }
}

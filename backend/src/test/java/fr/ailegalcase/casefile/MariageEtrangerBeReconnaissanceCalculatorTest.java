package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.MariageEtrangerBeReconnaissanceVerdict;
import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.MotifReconnaissanceEtrangerBeCode;
import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.NationalitePartiesBe;
import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.NatureActeEtrangerBe;
import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.ResidenceHabituelleBe;
import fr.ailegalcase.casefile.MariageEtrangerBeReconnaissanceCalculator.SeveriteMotifBe;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-16 : tests unitaires du moteur décisionnel BE de reconnaissance d'un
 * mariage / divorce étranger (incluant le talaq).
 */
class MariageEtrangerBeReconnaissanceCalculatorTest {

    /** Base : mariage civil étranger marocain, fond + forme OK, sans convention. */
    private MariageEtrangerBeReconnaissanceInput.Builder baseMariageCivilMarocain() {
        return new MariageEtrangerBeReconnaissanceInput.Builder()
                .natureActe(NatureActeEtrangerBe.MARIAGE_CIVIL_ETRANGER)
                .paysOrigine("MA")
                .dateActe(LocalDate.of(2018, 6, 12))
                .residenceHabituelleAuMoinsUnePartie(ResidenceHabituelleBe.BELGIQUE)
                .nationaliteAuMoinsUnePartie(NationalitePartiesBe.HORS_UE)
                .conformiteDroitFondPersonnel(true)
                .conformiteFormeLocusRegitActum(true)
                .conventionBilateraleApplicable(false);
    }

    /** Base : talaq marocain, tous les booleans favorables. */
    private MariageEtrangerBeReconnaissanceInput.Builder baseTalaqFavorable() {
        return new MariageEtrangerBeReconnaissanceInput.Builder()
                .natureActe(NatureActeEtrangerBe.TALAQ_REPUDIATION)
                .paysOrigine("MA")
                .dateActe(LocalDate.of(2024, 8, 15))
                .residenceHabituelleAuMoinsUnePartie(ResidenceHabituelleBe.BELGIQUE)
                .nationaliteAuMoinsUnePartie(NationalitePartiesBe.HORS_UE)
                .conformiteDroitFondPersonnel(true)
                .conformiteFormeLocusRegitActum(true)
                .consentementEpouse(true)
                .epousePresente(true)
                .procedureContradictoire(true)
                .decisionEcriteOfficielle(true)
                .conventionBilateraleApplicable(false);
    }

    // ---------------------------------------------------------------
    // Branche MARIAGE_POLYGAME — refus systématique d'ordre public
    // ---------------------------------------------------------------

    @Test
    void compute_mariagePolygame_refusOrdrePublic() {
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.MARIAGE_POLYGAME)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifsRefus()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.POLYGAMIE_ORDRE_PUBLIC
                        && m.severite() == SeveriteMotifBe.HIGH);
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.actesAProduire()).isNotEmpty();
    }

    @Test
    void compute_mariagePolygame_messageEffetsResiduels() {
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.MARIAGE_POLYGAME)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("succession") || m.toLowerCase().contains("résiduels"));
    }

    // ---------------------------------------------------------------
    // Branche MARIAGE_RELIGIEUX_NON_CIVIL
    // ---------------------------------------------------------------

    @Test
    void compute_mariageReligieuxNonCivil_refusOrdrePublic() {
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.MARIAGE_RELIGIEUX_NON_CIVIL)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifsRefus()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.MARIAGE_RELIGIEUX_NON_CIVIL
                        && m.severite() == SeveriteMotifBe.HIGH);
    }

    // ---------------------------------------------------------------
    // Branche TALAQ_REPUDIATION
    // ---------------------------------------------------------------

    @Test
    void compute_talaqTousBooleansFavorables_reconnaissancePossibleSousConditions() {
        var in = baseTalaqFavorable().build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(
                MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS);
        assertThat(r.motifsRefus()).isEmpty();
    }

    @Test
    void compute_talaqConsentementEpouseAbsent_refusOrdrePublic() {
        var in = baseTalaqFavorable().consentementEpouse(false).build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifsRefus()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.TALAQ_CONSENTEMENT_EPOUSE_ABSENT
                        && m.severite() == SeveriteMotifBe.HIGH);
    }

    @Test
    void compute_talaqProcedureNonContradictoire_motifReserveHigh() {
        var in = baseTalaqFavorable().procedureContradictoire(false).build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(
                MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS);
        assertThat(r.motifsReserve()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.TALAQ_PROCEDURE_NON_CONTRADICTOIRE
                        && m.severite() == SeveriteMotifBe.HIGH);
    }

    @Test
    void compute_talaqEpouseNonPresente_motifReserveHigh() {
        var in = baseTalaqFavorable().epousePresente(false).build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.motifsReserve()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.TALAQ_EPOUSE_NON_PRESENTE_NI_REPRESENTEE);
    }

    @Test
    void compute_talaqDecisionNonOfficielle_motifReserveHigh() {
        var in = baseTalaqFavorable().decisionEcriteOfficielle(false).build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.motifsReserve()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.DECISION_NON_OFFICIELLE);
    }

    @Test
    void compute_talaqConsentementFauxCumulProcedureFausse_refusOrdrePublic() {
        // Refus dur dès lors que consentement absent — la procédure
        // non contradictoire reste signalée comme réserve secondaire.
        var in = baseTalaqFavorable()
                .consentementEpouse(false)
                .procedureContradictoire(false)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifsRefus()).hasSize(1);
    }

    @Test
    void compute_talaqSansBooleanConsentement_rejete400() {
        // natureActe = TALAQ mais consentementEpouse null → IllegalArgument.
        var in = baseTalaqFavorable().consentementEpouse(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_talaqSansBooleanProcedure_rejete400() {
        var in = baseTalaqFavorable().procedureContradictoire(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // Branche DIVORCE_JUDICIAIRE_ETRANGER
    // ---------------------------------------------------------------

    @Test
    void compute_divorceJudiciaireFR_reconnaissanceDePleinDroit() {
        // FR ∈ UE → Bruxelles II bis = reconnaissance plein droit.
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.DIVORCE_JUDICIAIRE_ETRANGER)
                .paysOrigine("FR")
                .nationaliteAuMoinsUnePartie(NationalitePartiesBe.UE)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT);
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("bruxelles"));
    }

    @Test
    void compute_divorceJudiciaireMA_exequaturRequis() {
        // MA ∉ UE → exequatur CDIP art. 22+.
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.DIVORCE_JUDICIAIRE_ETRANGER)
                .paysOrigine("MA")
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.EXEQUATUR_REQUIS);
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("tribunal de la famille")
                || a.toLowerCase().contains("exequatur"));
    }

    @Test
    void compute_divorceJudiciaireTR_exequaturRequis() {
        // TR ∉ UE → exequatur (Turquie hors UE en 2026).
        var in = baseMariageCivilMarocain()
                .natureActe(NatureActeEtrangerBe.DIVORCE_JUDICIAIRE_ETRANGER)
                .paysOrigine("TR")
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.EXEQUATUR_REQUIS);
    }

    // ---------------------------------------------------------------
    // Branche MARIAGE_CIVIL_ETRANGER
    // ---------------------------------------------------------------

    @Test
    void compute_mariageCivilFondEtFormeOk_reconnaissanceDePleinDroit() {
        var in = baseMariageCivilMarocain().build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_DE_PLEIN_DROIT);
        assertThat(r.motifsRefus()).isEmpty();
        assertThat(r.motifsReserve()).isEmpty();
    }

    @Test
    void compute_mariageCivilFondKo_refusOrdrePublic() {
        var in = baseMariageCivilMarocain()
                .conformiteDroitFondPersonnel(false)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC);
        assertThat(r.motifsRefus()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.FOND_DROIT_PERSONNEL_NON_CONFORME);
    }

    @Test
    void compute_mariageCivilFormeKo_reconnaissancePossibleSousConditions() {
        var in = baseMariageCivilMarocain()
                .conformiteFormeLocusRegitActum(false)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(
                MariageEtrangerBeReconnaissanceVerdict.RECONNAISSANCE_POSSIBLE_SOUS_CONDITIONS);
        assertThat(r.motifsReserve()).anyMatch(m ->
                m.code() == MotifReconnaissanceEtrangerBeCode.FORME_LOCUS_REGIT_ACTUM_NON_CONFORME
                        && m.severite() == SeveriteMotifBe.MEDIUM);
    }

    // ---------------------------------------------------------------
    // Convention bilatérale
    // ---------------------------------------------------------------

    @Test
    void compute_conventionBilateraleApplicable_messageDedie() {
        var in = baseMariageCivilMarocain()
                .conventionBilateraleApplicable(true)
                .build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.messages()).anyMatch(m -> m.contains("convention bilatérale"));
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    @Test
    void compute_paysNonBelgique_rejete() {
        var in = baseMariageCivilMarocain().build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_natureActeAbsente_rejete() {
        var in = baseMariageCivilMarocain().natureActe(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysOrigineAbsent_rejete() {
        var in = baseMariageCivilMarocain().paysOrigine(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysOrigineNonIso2_rejete() {
        var in = baseMariageCivilMarocain().paysOrigine("FRA").build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysOrigineMinuscule_rejete() {
        var in = baseMariageCivilMarocain().paysOrigine("ma").build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateActeAbsente_rejete() {
        var in = baseMariageCivilMarocain().dateActe(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_dateActeFuture_rejete() {
        var in = baseMariageCivilMarocain().dateActe(LocalDate.now().plusDays(2)).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_conformiteFondAbsente_rejete() {
        var in = baseMariageCivilMarocain().conformiteDroitFondPersonnel(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_conformiteFormeAbsente_rejete() {
        var in = baseMariageCivilMarocain().conformiteFormeLocusRegitActum(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_conventionBilateraleAbsente_rejete() {
        var in = baseMariageCivilMarocain().conventionBilateraleApplicable(null).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = baseMariageCivilMarocain().commentaire("x".repeat(1001)).build();
        assertThatThrownBy(() ->
                MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // Bases juridiques + actes à produire
    // ---------------------------------------------------------------

    @Test
    void compute_basesJuridiquesContiennentCDIP() {
        var in = baseMariageCivilMarocain().build();
        var r = MariageEtrangerBeReconnaissanceCalculator.compute(in, "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CDIP"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 27"));
    }

    @Test
    void compute_actesAProduireNonVidesPourTousVerdicts() {
        for (NatureActeEtrangerBe n : NatureActeEtrangerBe.values()) {
            var builder = baseMariageCivilMarocain().natureActe(n);
            if (n == NatureActeEtrangerBe.TALAQ_REPUDIATION) {
                builder.consentementEpouse(true)
                        .epousePresente(true)
                        .procedureContradictoire(true)
                        .decisionEcriteOfficielle(true);
            }
            var r = MariageEtrangerBeReconnaissanceCalculator.compute(builder.build(), "BELGIQUE");
            assertThat(r.actesAProduire())
                    .as("Actes à produire pour " + n)
                    .isNotEmpty();
        }
    }
}

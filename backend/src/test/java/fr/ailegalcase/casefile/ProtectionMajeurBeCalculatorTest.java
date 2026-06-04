package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.GraviteIncapaciteBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.JuridictionProtectionBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.MandatEtendueBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.MesureProtectionBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.ModeSaisineProtectionBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.NatureAlterationBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.NecessiteActeBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.NiveauUrgenceProtectionBe;
import fr.ailegalcase.casefile.ProtectionMajeurBeCalculator.ProtectionMajeurBeVerdict;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-14 : tests unitaires du moteur décisionnel BE de protection du majeur.
 */
class ProtectionMajeurBeCalculatorTest {

    /** Base : altération médicale durable, incapacité étendue, sans mandat ni
     *  déclaration anticipée, environnement familial protecteur, urgence
     *  patrimoniale, saisine JP. */
    private ProtectionMajeurBeInput.Builder baseInput() {
        return new ProtectionMajeurBeInput.Builder()
                .natureAlteration(NatureAlterationBe.MEDICALE_DURABLE)
                .graviteIncapacite(GraviteIncapaciteBe.LES_DEUX)
                .mandatExtraJudiciaireSigne(false)
                .declarationAnticipeeExiste(false)
                .environnementFamilialProtecteur(true)
                .niveauUrgence(NiveauUrgenceProtectionBe.URGENCE_PATRIMONIALE)
                .modeSaisineEnvisage(ModeSaisineProtectionBe.REQUETE_JP);
    }

    @Test
    void compute_casNominal_lesDeux_mesureProtectionRecommandee_administrationBiensEtPersonne() {
        var in = baseInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MESURE_PROTECTION_RECOMMANDEE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.ADMINISTRATION_BIENS_ET_PERSONNE);
        assertThat(r.juridictionCompetente()).isEqualTo(JuridictionProtectionBe.JUSTICE_PAIX);
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void compute_gestionBiensSeule_administrationBiens() {
        var in = baseInput()
                .graviteIncapacite(GraviteIncapaciteBe.INCAPACITE_GERER_BIENS)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MESURE_PROTECTION_RECOMMANDEE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.ADMINISTRATION_BIENS);
    }

    @Test
    void compute_gestionPersonneSeule_administrationPersonne() {
        var in = baseInput()
                .graviteIncapacite(GraviteIncapaciteBe.INCAPACITE_GERER_PERSONNE)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MESURE_PROTECTION_RECOMMANDEE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.ADMINISTRATION_PERSONNE);
    }

    @Test
    void compute_incapacitePartielle_administrationBiensParDefaut() {
        var in = baseInput()
                .graviteIncapacite(GraviteIncapaciteBe.INCAPACITE_PARTIELLE)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MESURE_PROTECTION_RECOMMANDEE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.ADMINISTRATION_BIENS);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("partielle"));
    }

    // --- Mandat extra-judiciaire ---

    @Test
    void compute_mandatSigneApres2014_mandatExtraJudiciaireValable() {
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2020, 1, 15))
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.MANDAT_EXTRA_JUDICIAIRE);
        assertThat(r.juridictionCompetente()).isNull();
    }

    @Test
    void compute_mandatSigneSansDate_qualificationIncomplete() {
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(null)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.AUCUNE_RECOMMANDATION);
        assertThat(r.juridictionCompetente()).isNull();
        assertThat(r.actesProteges()).isEmpty();
    }

    @Test
    void compute_mandatSigneAvant2014_qualificationIncompleteAvecMessageDroitTransitoire() {
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2010, 5, 10))
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.AUCUNE_RECOMMANDATION);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("droit transitoire")
                || m.toLowerCase().contains("antérieur"));
    }

    @Test
    void compute_mandatSigneExactement2014_09_01_mandatExtraJudiciaireValable() {
        // Borne basse : entrée en vigueur 2014-09-01. La date exactement à la
        // borne est valable (isBefore strict).
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2014, 9, 1))
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
    }

    // --- Urgence vitale ---

    @Test
    void compute_urgenceVitale_urgenceMesureProvisoire() {
        var in = baseInput()
                .niveauUrgence(NiveauUrgenceProtectionBe.URGENCE_VITALE)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.URGENCE_MESURE_PROVISOIRE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.MESURE_PROVISOIRE_URGENCE);
        assertThat(r.juridictionCompetente()).isEqualTo(JuridictionProtectionBe.JUSTICE_PAIX);
        assertThat(r.actesProteges()).isNotEmpty();
    }

    @Test
    void compute_urgenceVitaleEtMandatSigne_mandatPrimeSurUrgence() {
        // Règle 3 (mandat) > règle 4 (urgence vitale) — le mandat valable prime.
        var in = baseInput()
                .niveauUrgence(NiveauUrgenceProtectionBe.URGENCE_VITALE)
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2020, 1, 1))
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
    }

    // --- Actes protégés ---

    @Test
    void compute_administrationBiensEtPersonne_actesProtegesNonVides() {
        var in = baseInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.actesProteges()).isNotEmpty();
        assertThat(r.actesProteges()).anyMatch(a ->
                a.necessite() == NecessiteActeBe.AUTORISATION_JP_PREALABLE);
        assertThat(r.actesProteges()).anyMatch(a ->
                a.necessite() == NecessiteActeBe.AUTONOMIE_PRESERVEE);
    }

    @Test
    void compute_administrationBiensSeule_pasActesPersonne() {
        var in = baseInput()
                .graviteIncapacite(GraviteIncapaciteBe.INCAPACITE_GERER_BIENS)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        // Les actes liés à la personne (LIEU_VIE, SOINS) ne doivent pas figurer
        // dans une mesure limitée aux biens — sauf MARIAGE_PACS_TESTAMENT et
        // ACTES_QUOTIDIENS qui sont communs à toute mesure.
        assertThat(r.actesProteges())
                .noneMatch(a -> a.code() ==
                        ProtectionMajeurBeCalculator.ActeProtegeBeCode.ACTES_PERSONNELS_LIEU_VIE);
        assertThat(r.actesProteges())
                .noneMatch(a -> a.code() ==
                        ProtectionMajeurBeCalculator.ActeProtegeBeCode.ACTES_PERSONNELS_SOINS);
    }

    @Test
    void compute_mandatValable_pasActesProteges() {
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2020, 1, 1))
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        // En cas de mandat valable, pas de mesure judiciaire → pas d'actes
        // protégés exposés.
        assertThat(r.actesProteges()).isEmpty();
    }

    // --- Validation ---

    @Test
    void compute_paysNonBelgique_rejete() {
        var in = baseInput().build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_inputNull_rejete() {
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_natureAlterationAbsente_rejete() {
        var in = baseInput().natureAlteration(null).build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_graviteIncapaciteAbsente_rejete() {
        var in = baseInput().graviteIncapacite(null).build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_niveauUrgenceAbsent_rejete() {
        var in = baseInput().niveauUrgence(null).build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_modeSaisineAbsent_rejete() {
        var in = baseInput().modeSaisineEnvisage(null).build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_commentaireTropLong_rejete() {
        var in = baseInput().commentaire("x".repeat(1001)).build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysNull_rejete() {
        var in = baseInput().build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBlank_rejete() {
        var in = baseInput().build();
        assertThatThrownBy(() -> ProtectionMajeurBeCalculator.compute(in, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_paysBelgiqueMinuscule_normaliseEtAccepte() {
        var in = baseInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "belgique");
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    // --- Messages / bases juridiques ---

    @Test
    void compute_basesJuridiquesNonVides() {
        var in = baseInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("17/03/2013"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("CC art. 490"));
    }

    @Test
    void compute_actionsConcretesContiennentSaisineJP() {
        var in = baseInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.actionsConcretes()).anyMatch(a ->
                a.toLowerCase().contains("justice de paix"));
    }

    @Test
    void compute_environnementProtecteurInfluenceActionsConcretes() {
        var inProtecteur = baseInput().environnementFamilialProtecteur(true).build();
        var inSansProtecteur = baseInput().environnementFamilialProtecteur(false).build();
        var rProt = ProtectionMajeurBeCalculator.compute(inProtecteur, "BELGIQUE");
        var rSansProt = ProtectionMajeurBeCalculator.compute(inSansProtecteur, "BELGIQUE");
        // Sans environnement familial protecteur, un message d'aide signale
        // la désignation d'un administrateur professionnel.
        assertThat(rSansProt.messages()).anyMatch(m ->
                m.toLowerCase().contains("administrateur professionnel"));
        assertThat(rProt.actionsConcretes()).anyMatch(a ->
                a.toLowerCase().contains("proche"));
    }

    @Test
    void compute_declarationAnticipeeExisteEtMandat_messageComplementaire() {
        var in = baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2020, 1, 1))
                .declarationAnticipeeExiste(true)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("déclaration anticipée"));
    }

    // ---------------------------------------------------------------------------
    // SF-223-10 — branche mandat extra-judiciaire approfondie
    // ---------------------------------------------------------------------------

    /** Base mandat valable (signé après 2014) à enrichir des nouveaux champs. */
    private ProtectionMajeurBeInput.Builder mandatValableInput() {
        return baseInput()
                .mandatExtraJudiciaireSigne(true)
                .mandatExtraJudiciaireDateSignature(LocalDate.of(2020, 1, 15));
    }

    @Test
    void compute_mandatComplet_capaciteEtEnregistrementConfirmes_mandatValable() {
        // Mandat couvrant les deux volets, capacité + enregistrement confirmés,
        // incapacité LES_DEUX → voie privée prime entièrement (pas de juridiction).
        var in = mandatValableInput()
                .graviteIncapacite(GraviteIncapaciteBe.LES_DEUX)
                .mandatEtendue(MandatEtendueBe.BIENS_ET_PERSONNE)
                .mandatCapaciteMandantConfirmee(true)
                .mandatEnregistreRegistreCentral(true)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.MANDAT_EXTRA_JUDICIAIRE);
        assertThat(r.juridictionCompetente()).isNull();
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("biens et personne"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("capacité du mandant"));
    }

    @Test
    void compute_mandatPartiel_valableAvecAdministrationComplementaire() {
        // Mandat limité aux biens mais incapacité LES_DEUX → mandat partiel :
        // valable + administration judiciaire complémentaire pour le reste.
        var in = mandatValableInput()
                .graviteIncapacite(GraviteIncapaciteBe.LES_DEUX)
                .mandatEtendue(MandatEtendueBe.BIENS)
                .mandatCapaciteMandantConfirmee(true)
                .mandatEnregistreRegistreCentral(true)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
        assertThat(r.juridictionCompetente()).isEqualTo(JuridictionProtectionBe.JUSTICE_PAIX);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("partiel"));
        assertThat(r.actionsConcretes()).anyMatch(a -> a.toLowerCase().contains("complément"));
    }

    @Test
    void compute_mandatCapaciteNonConfirmee_qualificationIncompleteReserve() {
        // Capacité du mandant explicitement infirmée → réserve.
        var in = mandatValableInput()
                .mandatEtendue(MandatEtendueBe.BIENS_ET_PERSONNE)
                .mandatCapaciteMandantConfirmee(false)
                .mandatEnregistreRegistreCentral(true)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.mesureRecommandee()).isEqualTo(MesureProtectionBe.AUCUNE_RECOMMANDATION);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("capacité du mandant"));
    }

    @Test
    void compute_mandatNonEnregistre_qualificationIncompleteReserve() {
        // Non-enregistrement au registre central explicitement infirmé → réserve.
        var in = mandatValableInput()
                .mandatEtendue(MandatEtendueBe.BIENS_ET_PERSONNE)
                .mandatCapaciteMandantConfirmee(true)
                .mandatEnregistreRegistreCentral(false)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.QUALIFICATION_INCOMPLETE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("registre central"));
    }

    @Test
    void compute_declarationAnticipeeAdministrateurDesigne_messageJuridictionLiee() {
        var in = mandatValableInput()
                .graviteIncapacite(GraviteIncapaciteBe.LES_DEUX)
                .mandatEtendue(MandatEtendueBe.BIENS)
                .declarationAnticipeeAdministrateurDesigne(true)
                .build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.messages()).anyMatch(m ->
                m.toLowerCase().contains("choix anticipé") || m.toLowerCase().contains("liée"));
    }

    @Test
    void compute_mandatValableSansNouveauxChamps_comportementF217Inchange() {
        // Anti-régression : mandat valable sans aucun des nouveaux champs (null)
        // → comportement F-217 inchangé (voie privée pleine, pas de juridiction).
        var in = mandatValableInput().build();
        var r = ProtectionMajeurBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(ProtectionMajeurBeVerdict.MANDAT_EXTRA_JUDICIAIRE_VALABLE);
        assertThat(r.juridictionCompetente()).isNull();
        assertThat(r.actesProteges()).isEmpty();
    }
}

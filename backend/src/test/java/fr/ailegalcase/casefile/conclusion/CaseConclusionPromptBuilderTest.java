package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;
import fr.ailegalcase.casefile.conclusion.CaseConclusionPromptBuilder.ConclusionPromptInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F-98 / SF-98-01 + SF-98-02 — tests unitaires de l'assemblage du prompt : prompt de
 * base résolu via le registre, consigne de style appliquée par-dessus, présence des
 * intrants du message utilisateur, tolérance d'un dossier sans piste / sans outil.
 */
class CaseConclusionPromptBuilderTest {

    /** Cellule CPH / FOND / DEMANDEUR — droit du travail FR. */
    private static final CombinationKey DEMANDEUR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CPH", "FOND", "DEMANDEUR");

    private final ConclusionPromptRegistry registry = new ConclusionPromptRegistry(List.of(
            new CphFondDemandeurPromptProvider(), new CphFondDefendeurPromptProvider()));

    private final CaseConclusionPromptBuilder builder =
            new CaseConclusionPromptBuilder(new ObjectMapper(), registry);

    @Test
    void buildSystemPrompt_describesProsecutorRoleAndStructure() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("avocat du demandeur");
        assertThat(system).contains("Conseil de prud'hommes");
        assertThat(system).contains("PROJET DE CONCLUSIONS");
        assertThat(system).contains("PAR CES MOTIFS");
        assertThat(system).contains("Pièce n°");
    }

    @Test
    void buildSystemPrompt_unknownCombination_throws() {
        CombinationKey unknown = new CombinationKey(
                ProcedureStageCatalog.DROIT_FAMILLE, ProcedureStageCatalog.FRANCE,
                "JAF", "DIVORCE_FOND", "DEMANDEUR");

        assertThatThrownBy(() -> builder.buildSystemPrompt(unknown, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun provider de prompt");
    }

    // ── SF-98-47 — consigne d'adaptation de style ────────────────────────────

    @Test
    void buildSystemPrompt_withStyleSignatures_includesStyleInstruction() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of(
                "Phrases courtes, registre assertif, transitions « En conséquence ».",
                "Argumentation faits puis droit, paragraphes denses."));

        assertThat(system).contains("avocat du demandeur");
        assertThat(system).contains("Adopte le style rédactionnel suivant");
        assertThat(system).contains("Phrases courtes, registre assertif");
        assertThat(system).contains("Argumentation faits puis droit");
    }

    @Test
    void buildSystemPrompt_withoutStyleSignatures_isUnchanged() {
        String generic = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(builder.buildSystemPrompt(DEMANDEUR_KEY, null)).isEqualTo(generic);
        assertThat(generic).doesNotContain("Adopte le style rédactionnel suivant");
    }

    @Test
    void buildSystemPrompt_blankSignaturesOnly_isUnchanged() {
        String generic = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        String result = builder.buildSystemPrompt(DEMANDEUR_KEY,
                java.util.Arrays.asList(null, "", "   "));

        assertThat(result).isEqualTo(generic);
        assertThat(result).doesNotContain("Adopte le style rédactionnel suivant");
    }

    @Test
    void buildUserMessage_containsAllInputs() {
        String analysisJson = """
                {
                  "faits": [{"texte": "Licenciement notifié le 12 mars 2026"}],
                  "points_juridiques": [{"texte": "Absence d'entretien préalable"}],
                  "risques": ["Forclusion du délai de saisine"]
                }
                """;
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                analysisJson,
                List.of(new ConclusionPromptInput.NumberedPiece(1, "Lettre de licenciement", "LETTRE"),
                        new ConclusionPromptInput.NumberedPiece(2, "Contrat de travail", "CONTRAT")),
                List.of(new DashboardTile("F-DT-01-licenciement", "VALIDITE",
                        "Validité du licenciement", "Licenciement sans cause réelle et sérieuse",
                        "Indemnité estimée 18 000 €", "ALERT")),
                List.of(new ConclusionPromptInput.RetainedStrategy(
                        "Demander la requalification en licenciement sans cause", "Art. L.1235-3 C. trav.")),
                List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Dossier Dupont c/ SARL Martin");
        assertThat(message).contains("Conseil de prud'hommes");
        assertThat(message).contains("Bureau de jugement (fond)");
        assertThat(message).contains("Demandeur (salarié)");
        assertThat(message).contains("Licenciement notifié le 12 mars 2026");
        assertThat(message).contains("Absence d'entretien préalable");
        assertThat(message).contains("Forclusion du délai de saisine");
        assertThat(message).contains("Pièce n° 1 — Lettre de licenciement");
        assertThat(message).contains("Pièce n° 2 — Contrat de travail");
        assertThat(message).contains("Validité du licenciement");
        assertThat(message).contains("Licenciement sans cause réelle et sérieuse");
        assertThat(message).contains("Demander la requalification");
        assertThat(message).contains("Art. L.1235-3 C. trav.");
    }

    @Test
    void buildUserMessage_emptyPiecesAndToolsAndStrategies_isStillValid() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier minimal",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Aucune pièce numérotée identifiée.");
        assertThat(message).contains("Aucun outil décisionnel rempli sur ce dossier.");
        assertThat(message).contains("Aucune piste stratégique retenue.");
        assertThat(message).contains("Aucune référence de jurisprudence fournie.");
        assertThat(message).doesNotContain("null");
    }

    @Test
    void buildUserMessage_nullAnalysisJson_marksSynthesisUnavailable() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans synthèse", "Conseil de prud'hommes",
                "Bureau de jugement (fond)", "Demandeur (salarié)",
                null, null, null, null, null);

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Synthèse indisponible.");
        assertThat(message).contains("Aucune pièce numérotée identifiée.");
    }

    @Test
    void buildUserMessage_malformedAnalysisJson_doesNotThrow() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier JSON cassé", "Conseil de prud'hommes",
                "Bureau de jugement (fond)", "Demandeur (salarié)",
                "{ ceci n'est pas du JSON", List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Synthèse indisponible (format inattendu).");
    }

    // ── F-242 / SF-242-01 — jurisprudence d'appui ────────────────────────────

    @Test
    void buildSystemPrompt_includesJurisprudenceAntiHallucinationGuard() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // Garde transverse présente même sans signature de style.
        assertThat(system).contains("Garde jurisprudence");
        assertThat(system).contains("ne cite aucune référence de jurisprudence");
        assertThat(system).contains("JURISPRUDENCE À L'APPUI");
        assertThat(system).contains("n'invente aucun arrêt");
    }

    @Test
    void buildSystemPrompt_jurisprudenceGuardPresentWithStyleSignaturesToo() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of(
                "Phrases courtes, registre assertif."));

        assertThat(system).contains("Garde jurisprudence");
        assertThat(system).contains("Adopte le style rédactionnel suivant");
        assertThat(system).contains("Phrases courtes, registre assertif");
    }

    @Test
    void buildUserMessage_withCitations_groupsByPointJuridique() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(),
                List.of(
                        new ConclusionPromptInput.JurisprudenceCitationForPrompt(
                                0, "Absence d'entretien préalable",
                                "Cass. soc. 12 oct. 2022, n° 21-12345",
                                "L'absence d'entretien préalable rend le licenciement irrégulier."),
                        new ConclusionPromptInput.JurisprudenceCitationForPrompt(
                                0, "Absence d'entretien préalable",
                                "Cass. soc. 3 mai 2018, n° 16-26.796", null),
                        new ConclusionPromptInput.JurisprudenceCitationForPrompt(
                                1, "Forclusion du délai de saisine",
                                "Cass. soc. 8 juin 2017, n° 15-28.599",
                                "Le délai de saisine du conseil de prud'hommes est de douze mois.")));

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== JURISPRUDENCE À L'APPUI ===");
        // Le texte de chaque point juridique apparaît une seule fois (regroupement).
        assertThat(message).contains("Point juridique : Absence d'entretien préalable");
        assertThat(message).contains("Point juridique : Forclusion du délai de saisine");
        // Les références sont rattachées sous leur point.
        assertThat(message).contains("Cass. soc. 12 oct. 2022, n° 21-12345");
        assertThat(message).contains("Cass. soc. 3 mai 2018, n° 16-26.796");
        assertThat(message).contains("Cass. soc. 8 juin 2017, n° 15-28.599");
        // La portée est rendue quand elle est présente.
        assertThat(message).contains("Portée : L'absence d'entretien préalable rend le licenciement irrégulier.");
        assertThat(message).contains("Portée : Le délai de saisine du conseil de prud'hommes est de douze mois.");
        // Un seul intitulé par point juridique malgré deux citations sur le point 0.
        assertThat(message.split("Point juridique : Absence d'entretien préalable", -1))
                .hasSize(2);
        assertThat(message).doesNotContain("Aucune référence de jurisprudence fournie.");
    }

    @Test
    void buildUserMessage_withoutCitations_marksJurisprudenceSectionEmpty() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans jurisprudence",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== JURISPRUDENCE À L'APPUI ===");
        assertThat(message).contains("Aucune référence de jurisprudence fournie.");
        assertThat(message).doesNotContain("Point juridique :");
    }

    @Test
    void buildUserMessage_nullCitations_marksJurisprudenceSectionEmpty() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier citations null",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), null);

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== JURISPRUDENCE À L'APPUI ===");
        assertThat(message).contains("Aucune référence de jurisprudence fournie.");
    }

    // --- SF-98-55 — garde de qualité rédactionnelle + nettoyage du jargon interne ---

    @Test
    void buildSystemPrompt_includesRedactionQualityGuard_onEveryCell() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // anti-jargon
        assertThat(system).contains("Aucun jargon interne");
        assertThat(system).contains("F-DT-08");        // cité comme exemple à NE PAS reproduire
        assertThat(system).contains("matière première");
        // syllogisme + visa
        assertThat(system).contains("VISANT l'article");
        // dispositif complet
        assertThat(system).contains("article 700");
        assertThat(system).contains("1343-2");
    }

    @Test
    void buildUserMessage_toolJurisprudence_usesHumanLabelNotRawToolId() {
        var citation = new fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationResponse(
                java.util.UUID.randomUUID(),
                "Cass. soc. 13 oct. 2021, n° 18-18.022",
                "Cour de cassation",
                java.time.LocalDate.of(2021, 10, 13),
                "18-18.022",
                "https://www.courdecassation.fr/decision/18-18022",
                "Les dispositions conventionnelles s'imposent à l'employeur.",
                null, null);
        var byTool = new fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool(
                "f-dt-08-licenciement-validite", "default", List.of(citation));
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier jargon", "Conseil de prud'hommes", "Bureau de jugement (fond)",
                "Demandeur (salarié)", "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(byTool));

        String message = builder.buildUserMessage(input);

        // le libellé lisible est présent, jamais le code brut
        assertThat(message).contains("Sujet : Licenciement validite");
        assertThat(message).doesNotContain("f-dt-08-licenciement-validite");
        // la citation elle-même reste fournie
        assertThat(message).contains("Cass. soc. 13 oct. 2021, n° 18-18.022");
    }

    // --- SF-98-56 — jurisprudence adverse à réfuter ---

    @Test
    void buildSystemPrompt_jurisprudenceGuardCitesAdverseSectionAndRefutation() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // 3e source autorisée + consigne de réfutation.
        assertThat(system).contains("JURISPRUDENCE ADVERSE À RÉFUTER");
        assertThat(system).contains("réfut");
        assertThat(system).contains("avec autorité");
        // non-régression : la garde mentionne toujours les 2 sources d'appui historiques.
        assertThat(system).contains("JURISPRUDENCE À L'APPUI");
        assertThat(system).contains("JURISPRUDENCE APPLICABLE PAR OUTIL");
        // non-régression SF-98-55 : garde rédactionnelle toujours présente.
        assertThat(system).contains("Aucun jargon interne");
    }

    @Test
    void buildUserMessage_withAdverseToRefute_addsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(
                        new ConclusionPromptInput.AdverseCitationToRefute(
                                "Cass. soc. 1 jan. 2099, n° 99-99.999",
                                "Référence introuvable y compris après recherche.",
                                "L'adversaire prétend que cet arrêt fonde la nullité."),
                        new ConclusionPromptInput.AdverseCitationToRefute(
                                "Cass. soc. 3 mai 2018, n° 16-26.796",
                                "L'arrêt existe mais ne dit pas ce que l'adversaire prétend.",
                                null)));

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== JURISPRUDENCE ADVERSE À RÉFUTER ===");
        assertThat(message).contains("démontre, pour chacune, pourquoi l'adversaire se trompe");
        assertThat(message).contains("Cass. soc. 1 jan. 2099, n° 99-99.999");
        assertThat(message).contains("Référence introuvable y compris après recherche.");
        assertThat(message).contains("Position prêtée par l'adversaire : L'adversaire prétend que cet arrêt fonde la nullité.");
        assertThat(message).contains("Cass. soc. 3 mai 2018, n° 16-26.796");
    }

    @Test
    void buildUserMessage_withoutAdverseToRefute_omitsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans adverse",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("JURISPRUDENCE ADVERSE À RÉFUTER");
    }

    @Test
    void buildUserMessage_emptyAdverseToRefute_omitsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier liste vide",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("JURISPRUDENCE ADVERSE À RÉFUTER");
    }

    @Test
    void humanizeToolId_stripsPrefixAndCapitalises() {
        assertThat(CaseConclusionPromptBuilder.humanizeToolId("f-dt-08-licenciement-validite"))
                .isEqualTo("Licenciement validite");
        assertThat(CaseConclusionPromptBuilder.humanizeToolId("f-im-52-vpf-jeune-majeur"))
                .isEqualTo("Vpf jeune majeur");
        assertThat(CaseConclusionPromptBuilder.humanizeToolId(null)).isEmpty();
        assertThat(CaseConclusionPromptBuilder.humanizeToolId("  ")).isEmpty();
    }
}

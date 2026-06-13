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

    // F-270 : la garde rédactionnelle interdit tout pronostic chiffré de succès
    @Test
    void buildSystemPrompt_containsPronosticPrudenceGuard() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("Prudence du pronostic");
        assertThat(system).contains("chance de gagner");
        assertThat(system).contains("juridiction saisie et de la formation de jugement");
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
        // SF-98-60 — demandes subsidiaires
        assertThat(system).contains("À titre subsidiaire");
        assertThat(system).contains("Demandes subsidiaires");
        assertThat(system).contains("N'invente AUCUN chef");
        // SF-98-61 — 3a identité/adresse des parties + 3b signature neutre
        assertThat(system).contains("IDENTITÉ DES PARTIES");
        assertThat(system).contains("n'invente JAMAIS une adresse");
        assertThat(system).contains("[Nom et qualité de l'avocat]");
        // F-275 / SF-275-01 — en-tête POUR / CONTRE auto-rempli, [à compléter] honnête
        assertThat(system).contains("POUR");
        assertThat(system).contains("CONTRE");
        assertThat(system).contains("[à compléter]");
    }

    // --- SF-98-61 — finitions rédactionnelles (identité parties, signature, jurisprudence topique) ---

    @Test
    void buildSystemPrompt_jurisprudenceGuardRequiresTopicality() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // 3c — pertinence : un arrêt d'outil n'est utilisé que s'il est topique.
        assertThat(system).contains("ne plaque pas un arrêt non topique");
        assertThat(system).contains("JURISPRUDENCE APPLICABLE PAR OUTIL");
    }

    @Test
    void buildUserMessage_withTravailIdentities_addsPartiesIdentitySection() {
        String analysisJson = """
                {
                  "faits": [],
                  "points_juridiques": [],
                  "risques": [],
                  "travail_extracted_data": {
                    "prenom_salarie": "Jean",
                    "nom_salarie": "DUPONT",
                    "adresse_salarie": "12 rue des Lilas, 75011 Paris",
                    "nom_employeur": "SARL Martin",
                    "adresse_employeur": "5 avenue de la République, 75011 Paris"
                  }
                }
                """;
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                analysisJson,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).contains("Salarié : Jean DUPONT — Adresse : 12 rue des Lilas, 75011 Paris");
        assertThat(message).contains("Employeur : SARL Martin — Adresse : 5 avenue de la République, 75011 Paris");
    }

    @Test
    void buildUserMessage_withoutTravailIdentities_omitsPartiesIdentitySection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans identités",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("=== IDENTITÉ DES PARTIES ===");
    }

    @Test
    void buildUserMessage_partialTravailIdentity_rendersOnlyPresentFields() {
        String analysisJson = """
                {
                  "travail_extracted_data": {
                    "nom_salarie": "DUPONT",
                    "adresse_salarie": "12 rue des Lilas, 75011 Paris"
                  }
                }
                """;
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier identité partielle",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                analysisJson,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).contains("Salarié : DUPONT — Adresse : 12 rue des Lilas, 75011 Paris");
        // Aucune ligne Employeur quand ses champs sont absents.
        assertThat(message).doesNotContain("Employeur :");
    }

    @Test
    void buildUserMessage_malformedJson_omitsPartiesIdentitySectionWithoutThrowing() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier JSON cassé identités",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{ ceci n'est pas du JSON",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("=== IDENTITÉ DES PARTIES ===");
    }

    // --- F-275 / SF-275-01 — orientation POUR / CONTRE de l'en-tête depuis la position ---

    private static final String TRAVAIL_IDENTITIES_JSON = """
            {
              "travail_extracted_data": {
                "prenom_salarie": "Jean",
                "nom_salarie": "DUPONT",
                "adresse_salarie": "12 rue des Lilas, 75011 Paris",
                "nom_employeur": "SARL Martin",
                "adresse_employeur": "5 avenue de la République, 75011 Paris"
              }
            }
            """;

    @Test
    void buildUserMessage_demandeurSalarie_orientsSalarieAsClientPour() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                TRAVAIL_IDENTITIES_JSON,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).contains("le SALARIÉ est client de l'avocat (POUR)");
        assertThat(message).contains("l'EMPLOYEUR est partie adverse (CONTRE)");
        // Les données identités (SF-98-61) restent inchangées.
        assertThat(message).contains("Salarié : Jean DUPONT");
        assertThat(message).contains("Employeur : SARL Martin");
    }

    @Test
    void buildUserMessage_defendeurEmployeur_orientsEmployeurAsClientPour() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Défendeur (employeur)",
                TRAVAIL_IDENTITIES_JSON,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).contains("l'EMPLOYEUR est client de l'avocat (POUR)");
        assertThat(message).contains("le SALARIÉ est partie adverse (CONTRE)");
    }

    @Test
    void buildUserMessage_unmappablePosition_omitsPourContreOrientationButKeepsIdentities() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans rôle explicite",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Appelant",                       // pas de mention salarié / employeur
                TRAVAIL_IDENTITIES_JSON,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        // La section identités reste (SF-98-61) mais sans orientation devinée.
        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).contains("Salarié : Jean DUPONT");
        assertThat(message).doesNotContain("client de l'avocat (POUR)");
        assertThat(message).doesNotContain("partie adverse (CONTRE)");
    }

    @Test
    void buildUserMessage_nullPosition_omitsPourContreOrientation() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier position null",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                null,
                TRAVAIL_IDENTITIES_JSON,
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).doesNotContain("client de l'avocat (POUR)");
    }

    @Test
    void buildUserMessage_noIdentities_omitsSectionAndOrientation() {
        // Hors travail (immigration / famille) : aucune identité extraite → no-op total.
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier immigration",
                "Tribunal administratif",
                "Recours",
                "Requérant",
                "{\"faits\": [], \"immigration_extracted_data\": {}}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("=== IDENTITÉ DES PARTIES ===");
        assertThat(message).doesNotContain("client de l'avocat (POUR)");
    }

    @Test
    void resolveTravailClientRole_mapsRoleFromPositionLabel() {
        assertThat(CaseConclusionPromptBuilder.resolveTravailClientRole("Demandeur (salarié)"))
                .isEqualTo(CaseConclusionPromptBuilder.ClientRole.SALARIE);
        assertThat(CaseConclusionPromptBuilder.resolveTravailClientRole("Défendeur (employeur)"))
                .isEqualTo(CaseConclusionPromptBuilder.ClientRole.EMPLOYEUR);
        assertThat(CaseConclusionPromptBuilder.resolveTravailClientRole("Appelant"))
                .isEqualTo(CaseConclusionPromptBuilder.ClientRole.UNKNOWN);
        assertThat(CaseConclusionPromptBuilder.resolveTravailClientRole(null))
                .isEqualTo(CaseConclusionPromptBuilder.ClientRole.UNKNOWN);
        assertThat(CaseConclusionPromptBuilder.resolveTravailClientRole(""))
                .isEqualTo(CaseConclusionPromptBuilder.ClientRole.UNKNOWN);
    }

    @Test
    void buildSystemPrompt_redactionGuard_keepsAntiInventionAndOtherPoints() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // F-275 ne régresse pas l'anti-invention ni les autres points du guard.
        assertThat(system).contains("n'invente JAMAIS une adresse");
        assertThat(system).contains("[à compléter]");
        // Points voisins conservés (5 subsidiaires, 8 moyens adverses, 10 récapitulatives).
        assertThat(system).contains("Demandes subsidiaires");
        assertThat(system).contains("MOYENS ADVERSES À RÉFUTER");
        assertThat(system).contains("Conclusions récapitulatives");
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

    // ── SF-261-02 — moyens adverses à réfuter ────────────────────────────────

    @Test
    void buildSystemPrompt_redactionGuardIncludesAdverseMoyensRefutation() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("MOYENS ADVERSES À RÉFUTER");
        assertThat(system).contains("réfute-le explicitement");
        assertThat(system).contains("N'invente AUCUN moyen adverse non listé");
        // non-régression : les gardes SF-98-55/56 cohabitent.
        assertThat(system).contains("Aucun jargon interne");
    }

    @Test
    void buildUserMessage_withAdverseMoyens_addsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(
                        new AdverseMoyen(
                                "Le licenciement repose sur une faute grave.",
                                List.of("art. L. 1234-1 C. trav.", "art. L. 1234-9 C. trav."),
                                List.of("Lettre de licenciement", "Attestations")),
                        new AdverseMoyen(
                                "Les rappels de salaire sont prescrits.",
                                List.of("art. L. 3245-1 C. trav."),
                                List.of())));

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("=== MOYENS ADVERSES À RÉFUTER ===");
        assertThat(message).contains("réfute chacun explicitement");
        assertThat(message).contains("Moyen 1 — Thèse adverse : Le licenciement repose sur une faute grave.");
        assertThat(message).contains("Fondements invoqués : art. L. 1234-1 C. trav., art. L. 1234-9 C. trav.");
        assertThat(message).contains("Pièces invoquées : Lettre de licenciement, Attestations");
        assertThat(message).contains("Moyen 2 — Thèse adverse : Les rappels de salaire sont prescrits.");
    }

    @Test
    void buildUserMessage_withoutAdverseMoyens_omitsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans moyen adverse",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("MOYENS ADVERSES À RÉFUTER");
    }

    @Test
    void buildUserMessage_emptyAdverseMoyens_omitsRefutationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier liste vide",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("MOYENS ADVERSES À RÉFUTER");
    }

    @Test
    void buildUserMessage_adverseMoyensWithoutFondementsOrPieces_rendersTheseOnly() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier moyen minimal",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new AdverseMoyen("Thèse seule.", List.of(), List.of())));

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Moyen 1 — Thèse adverse : Thèse seule.");
        // sans fondements / pièces, on n'ajoute pas les segments correspondants
        int line = message.indexOf("Moyen 1");
        String moyenLine = message.substring(line, message.indexOf('\n', line));
        assertThat(moyenLine).doesNotContain("Fondements invoqués");
        assertThat(moyenLine).doesNotContain("Pièces invoquées");
    }

    @Test
    void buildUserMessage_moyensSectionPrecedesAdverseJurisprudence() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier mixte",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new ConclusionPromptInput.AdverseCitationToRefute(
                        "Cass. soc. 1 jan. 2099, n° 99-99.999",
                        "Référence introuvable.", null)),
                List.of(new AdverseMoyen("Moyen adverse.", List.of(), List.of())));

        String message = builder.buildUserMessage(input);

        assertThat(message.indexOf("=== MOYENS ADVERSES À RÉFUTER ==="))
                .isLessThan(message.indexOf("=== JURISPRUDENCE ADVERSE À RÉFUTER ==="));
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

    // ===== F-271 / SF-271-02 — conclusions récapitulatives (texte à préserver) =====

    @Test
    void buildSystemPrompt_containsRecapitulatifGuard() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());
        assertThat(system).contains("Conclusions récapitulatives");
        assertThat(system).contains("art. 768 CPC");
        // SF-271-02 — section renommée + consigne de préservation.
        assertThat(system).contains("TEXTE ACTUEL À PRÉSERVER");
        assertThat(system).contains("PRÉSERVE");
        assertThat(system).contains("Ne REFORMULE PAS");
        assertThat(system).contains("réputé abandonné");
    }

    @Test
    void buildUserMessage_withPreviousRecap_includesPreservationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier Dupont c/ SARL Martin",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                "## PAR CES MOTIFS\nCondamner la SARL Martin à 18 000 € de dommages et intérêts.");

        String message = builder.buildUserMessage(input);

        // SF-271-02 — section renommée + consigne « PRÉSERVE … Ne reformule PAS ».
        assertThat(message).contains("=== TEXTE ACTUEL À PRÉSERVER (jeu de conclusions de l'avocat) ===");
        assertThat(message).contains("PRÉSERVE-le tel quel");
        assertThat(message).contains("Ne reformule PAS");
        assertThat(message).contains("Condamner la SARL Martin à 18 000 €");
        assertThat(message).contains("art. 768");
    }

    @Test
    void buildUserMessage_withoutPreviousRecap_omitsPreservationSection() {
        // Constructeur back-compat (pré-F-271) → previousRecapContent == null.
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier minimal",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("TEXTE ACTUEL À PRÉSERVER");
    }

    @Test
    void buildUserMessage_blankPreviousRecap_omitsPreservationSection() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier blanc",
                "Conseil de prud'hommes",
                "Bureau de jugement (fond)",
                "Demandeur (salarié)",
                "{\"faits\": [], \"points_juridiques\": [], \"risques\": []}",
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                "   ");

        String message = builder.buildUserMessage(input);

        assertThat(message).doesNotContain("TEXTE ACTUEL À PRÉSERVER");
    }

    // ===== F-273 / SF-273-01 — actualisation « sauf à parfaire » des montants & intérêts =====

    @Test
    void buildSystemPrompt_containsSaufAParfaireGuard() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("sauf à parfaire");
        assertThat(system).contains("à la date de l'audience");
        // point de départ des intérêts visé par les articles du Code civil
        assertThat(system).contains("1231-6");
        assertThat(system).contains("1231-7");
        // anti-faux-positif : pas de réserve sur un montant définitivement arrêté
        assertThat(system).contains("définitivement arrêté");
    }

    @Test
    void buildSystemPrompt_saufAParfaire_doesNotRegressDispositifPostes() {
        // Non-régression SF-98-55 point 3 : les postes systématiques restent imposés.
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("article 700");
        assertThat(system).contains("1343-2"); // capitalisation des intérêts
        assertThat(system).contains("Dispositif complet");
    }

    @Test
    void buildSystemPrompt_saufAParfaire_uniformOnDemandeurAndDefendeur() {
        // Portée uniforme : présent demandeur ET défendeur (aucun conditionnement).
        String demandeur = procedureBuilder.buildSystemPrompt(DEMANDEUR_KEY, List.of());
        String defendeur = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        assertThat(demandeur).contains("sauf à parfaire");
        assertThat(defendeur).contains("sauf à parfaire");
    }

    @Test
    void buildSystemPrompt_saufAParfaireGuard_doesNotLeakToolCode() {
        // Anti-jargon : la consigne « sauf à parfaire » elle-même ne nomme aucun code
        // d'outil interne. (Le point 1 cite « F-DT-08 » comme contre-exemple à ne pas
        // reproduire ; on isole donc la seule phrase ajoutée par F-273.)
        String guard = CaseConclusionPromptBuilder.REDACTION_QUALITY_GUARD;
        int start = guard.indexOf("Actualisation « sauf à parfaire »");
        assertThat(start).isGreaterThan(0);
        String saufAParfaireSentence = guard.substring(start, guard.indexOf("\n4.", start));
        assertThat(saufAParfaireSentence)
                .contains("sauf à parfaire")
                .doesNotContainIgnoringCase("F-DT-")
                .doesNotContain("critères sur");
    }

    // ── F-272 / SF-272-01 — garde d'ordre in limine litis (art. 74 CPC) ──────

    /** Cellule CPH / FOND / DEFENDEUR — droit du travail FR (position de défense). */
    private static final CombinationKey DEFENDEUR_FR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CPH", "FOND", "DEFENDEUR");

    /** Cellule CA_SOC / APPEL / INTIME — défendeur en appel FR. */
    private static final CombinationKey INTIME_FR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CA_SOC", "APPEL", "INTIME");

    /** Cellule CASS_SOC / POURVOI / DEFENDEUR_POURVOI — défendeur au pourvoi FR. */
    private static final CombinationKey DEFENDEUR_POURVOI_FR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CASS_SOC", "POURVOI", "DEFENDEUR_POURVOI");

    /** Cellule TT / FOND / DEFENDEUR — défendeur BE (régime du Code judiciaire, hors garde). */
    private static final CombinationKey DEFENDEUR_BE_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.BELGIQUE,
            "TT", "FOND", "DEFENDEUR");

    private final ConclusionPromptRegistry procedureRegistry = new ConclusionPromptRegistry(List.of(
            new CphFondDemandeurPromptProvider(),
            new CphFondDefendeurPromptProvider(),
            new CaSocAppelIntimePromptProvider(),
            new CassSocPourvoiDefendeurPromptProvider(),
            new TtFondDefendeurPromptProvider()));

    private final CaseConclusionPromptBuilder procedureBuilder =
            new CaseConclusionPromptBuilder(new ObjectMapper(), procedureRegistry);

    @Test
    void buildSystemPrompt_defendeurFr_containsInLimineLitisGuard() {
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        assertThat(system).containsIgnoringCase("in limine litis");
        assertThat(system).contains("article 74 du Code de procédure civile");
        assertThat(system).contains("EXCEPTIONS DE PROCÉDURE");
        assertThat(system).contains("FINS DE NON-RECEVOIR");
    }

    @Test
    void buildSystemPrompt_demandeurFr_omitsInLimineLitisGuard() {
        String system = procedureBuilder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).doesNotContainIgnoringCase("in limine litis");
        assertThat(system).doesNotContain("article 74 du Code de procédure civile");
    }

    @Test
    void buildSystemPrompt_intimeFr_containsInLimineLitisGuard() {
        String system = procedureBuilder.buildSystemPrompt(INTIME_FR_KEY, List.of());

        assertThat(system).containsIgnoringCase("in limine litis");
    }

    @Test
    void buildSystemPrompt_defendeurPourvoiFr_containsInLimineLitisGuard() {
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_POURVOI_FR_KEY, List.of());

        assertThat(system).containsIgnoringCase("in limine litis");
    }

    @Test
    void buildSystemPrompt_defendeurBe_omitsInLimineLitisGuard() {
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_BE_KEY, List.of());

        assertThat(system).doesNotContainIgnoringCase("in limine litis");
    }

    @Test
    void buildSystemPrompt_defendeurFr_stillContainsRedactionQualityGuard() {
        // Non-régression SF-98-55 : la garde rédactionnelle commune reste présente,
        // la nouvelle garde s'ajoute sans la remplacer.
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        assertThat(system).contains("Garde de qualité rédactionnelle");
        assertThat(system).contains("Aucun jargon interne");
        assertThat(system).containsIgnoringCase("in limine litis");
    }

    @Test
    void buildSystemPrompt_inLimineLitisGuard_doesNotLeakToolCode() {
        // Anti-jargon : la garde ne nomme jamais un code d'outil interne (ex. « F-DT-36 »).
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        assertThat(CaseConclusionPromptBuilder.PROCEDURE_ORDER_GUARD)
                .doesNotContainIgnoringCase("F-DT-36")
                .doesNotContainIgnoringCase("F-DT-");
        assertThat(system).contains("vices de procédure");
    }

    @Test
    void appliesProcedureOrderGuard_truthTable() {
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(DEFENDEUR_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(INTIME_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(DEFENDEUR_POURVOI_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(DEMANDEUR_KEY)).isFalse();
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(DEFENDEUR_BE_KEY)).isFalse();
        assertThat(CaseConclusionPromptBuilder.appliesProcedureOrderGuard(null)).isFalse();
    }

    // ── F-274 / SF-274-01 — garde « pièces adverses » (communication / rejet, art. 132-135 CPC) ──

    @Test
    void buildSystemPrompt_demandeurFr_containsAdversePiecesGuard() {
        // CA1 — présente sur une cellule FR demandeur (la garde s'applique des deux côtés).
        String system = procedureBuilder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("article 132 du Code de procédure civile");
        assertThat(system).contains("article 135 du Code de procédure civile");
        assertThat(system).containsIgnoringCase("communiqu");
        assertThat(system).containsIgnoringCase("écarter des débats");
    }

    @Test
    void buildSystemPrompt_defendeurFr_containsAdversePiecesGuard() {
        // CA2 — présente sur une cellule FR défendeur.
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        assertThat(system).contains("Pièces de la partie adverse");
        assertThat(system).contains("article 132 du Code de procédure civile");
        assertThat(system).contains("article 135 du Code de procédure civile");
    }

    @Test
    void buildSystemPrompt_defendeurBe_omitsAdversePiecesGuard() {
        // CA3 — absente sur une cellule BE (régime du Code judiciaire, hors périmètre).
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_BE_KEY, List.of());

        assertThat(system).doesNotContain("Pièces de la partie adverse");
        assertThat(system).doesNotContain("article 132 du Code de procédure civile");
    }

    @Test
    void adversePiecesGuard_carriesAutoConditioningAndAntiInvention() {
        // CA4 — auto-conditionnement sur la présence de pièces adverses + anti-invention.
        String guard = CaseConclusionPromptBuilder.ADVERSE_PIECES_GUARD;

        assertThat(guard).contains("LORSQUE la partie adverse a communiqué");
        assertThat(guard).contains("MOYENS ADVERSES À RÉFUTER");
        assertThat(guard).contains("n'invente aucune pièce");
        assertThat(guard).containsIgnoringCase("n'invente AUCUNE pièce, AUCUNE date");
        assertThat(guard).contains("n'ajoute pas de rubrique vide");
    }

    @Test
    void adversePiecesGuard_doesNotLeakToolCodeOrFileName() {
        // CA5 — anti-jargon (non-régression SF-98-55) : aucun code outil, aucun nom de fichier.
        String guard = CaseConclusionPromptBuilder.ADVERSE_PIECES_GUARD;

        assertThat(guard)
                .doesNotContainIgnoringCase("F-DT-")
                .doesNotContainIgnoringCase("F-274")
                .doesNotContain(".pdf")
                .doesNotContain("nom de fichier interne");
        assertThat(guard).contains("sans exposer de libellé interne ni de nom de fichier");
    }

    @Test
    void buildSystemPrompt_defendeurFr_adversePieces_doesNotRegressOtherGuards() {
        // CA6 — coexistence : la garde s'ajoute sans remplacer les gardes existantes.
        String system = procedureBuilder.buildSystemPrompt(DEFENDEUR_FR_KEY, List.of());

        // Garde rédactionnelle commune (SF-98-55) + point 8 réfutation des moyens (SF-261-02)
        assertThat(system).contains("Garde de qualité rédactionnelle");
        assertThat(system).contains("Réfutation des moyens adverses");
        // Ossature in limine litis (F-272)
        assertThat(system).containsIgnoringCase("in limine litis");
        // Garde jurisprudence (F-242)
        assertThat(system).contains("Garde jurisprudence");
        // Nouvelle garde F-274
        assertThat(system).contains("Pièces de la partie adverse");
    }

    @Test
    void appliesAdversePiecesGuard_truthTable() {
        // CA7 — FR (toute position) → true ; BE → false ; null → false.
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(DEMANDEUR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(DEFENDEUR_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(INTIME_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(DEFENDEUR_POURVOI_FR_KEY)).isTrue();
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(DEFENDEUR_BE_KEY)).isFalse();
        assertThat(CaseConclusionPromptBuilder.appliesAdversePiecesGuard(null)).isFalse();
    }

    // ── F-291 / SF-291-02 — garde « citations & chiffres » ───────────────────

    @Test
    void buildSystemPrompt_includesCitationsFiguresGuard() {
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        // En-tête de la garde + principe « silence > erreur » (marqueur [à vérifier]).
        assertThat(system).contains("Garde citations & chiffres");
        assertThat(system).contains("[à vérifier]");
        // Anti-affirmation non sourcée : article / durée conventionnelle / montant.
        assertThat(system).contains("durée conventionnelle");
        // Exécution provisoire prud'homale épinglée sur les bons articles.
        assertThat(system).contains("article 514 du Code de procédure civile");
        assertThat(system).contains("R. 1454-28");
        assertThat(system).contains("N'utilise JAMAIS l'article 515");
        // Solde de tout compte : article L. 1234-20.
        assertThat(system).contains("L. 1234-20");
    }

    @Test
    void buildSystemPrompt_citationsFiguresGuard_coexistsWithOtherGuards() {
        // Non-régression : la garde s'ajoute sans remplacer les gardes existantes.
        String system = builder.buildSystemPrompt(DEMANDEUR_KEY, List.of());

        assertThat(system).contains("Garde citations & chiffres");
        assertThat(system).contains("Garde jurisprudence");          // F-242
        assertThat(system).contains("Aucun jargon interne");          // SF-98-55
        assertThat(system).contains("Garde de qualité rédactionnelle");
    }

    @Test
    void citationsFiguresGuard_doesNotLeakToolCodeOrFeatureId() {
        // Anti-jargon (non-régression SF-98-55) : aucun code outil ni identifiant de feature.
        assertThat(CaseConclusionPromptBuilder.CITATIONS_FIGURES_GUARD)
                .doesNotContainIgnoringCase("F-291")
                .doesNotContainIgnoringCase("F-DT-")
                .doesNotContainIgnoringCase("SF-291");
    }
}

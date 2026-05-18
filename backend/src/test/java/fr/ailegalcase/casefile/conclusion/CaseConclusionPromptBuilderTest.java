package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.conclusion.CaseConclusionPromptBuilder.ConclusionPromptInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-01 — tests unitaires de l'assemblage du prompt : présence des intrants,
 * tolérance d'un dossier sans piste / sans outil.
 */
class CaseConclusionPromptBuilderTest {

    private final CaseConclusionPromptBuilder builder =
            new CaseConclusionPromptBuilder(new ObjectMapper());

    @Test
    void buildSystemPrompt_describesProsecutorRoleAndStructure() {
        String system = builder.buildSystemPrompt();

        assertThat(system).contains("avocat du demandeur");
        assertThat(system).contains("Conseil de prud'hommes");
        assertThat(system).contains("PROJET DE CONCLUSIONS");
        assertThat(system).contains("PAR CES MOTIFS");
        assertThat(system).contains("Pièce n°");
    }

    // ── SF-98-47 — consigne d'adaptation de style ────────────────────────────

    @Test
    void buildSystemPrompt_withStyleSignatures_includesStyleInstruction() {
        String system = builder.buildSystemPrompt(List.of(
                "Phrases courtes, registre assertif, transitions « En conséquence ».",
                "Argumentation faits puis droit, paragraphes denses."));

        assertThat(system).contains("avocat du demandeur");
        assertThat(system).contains("Adopte le style rédactionnel suivant");
        assertThat(system).contains("Phrases courtes, registre assertif");
        assertThat(system).contains("Argumentation faits puis droit");
    }

    @Test
    void buildSystemPrompt_withoutStyleSignatures_isUnchanged() {
        String generic = builder.buildSystemPrompt();

        assertThat(builder.buildSystemPrompt(List.of())).isEqualTo(generic);
        assertThat(builder.buildSystemPrompt(null)).isEqualTo(generic);
        assertThat(generic).doesNotContain("Adopte le style rédactionnel suivant");
    }

    @Test
    void buildSystemPrompt_blankSignaturesOnly_isUnchanged() {
        String generic = builder.buildSystemPrompt();

        String result = builder.buildSystemPrompt(java.util.Arrays.asList(null, "", "   "));

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
                        "Demander la requalification en licenciement sans cause", "Art. L.1235-3 C. trav.")));

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
                List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Aucune pièce numérotée identifiée.");
        assertThat(message).contains("Aucun outil décisionnel rempli sur ce dossier.");
        assertThat(message).contains("Aucune piste stratégique retenue.");
        assertThat(message).doesNotContain("null");
    }

    @Test
    void buildUserMessage_nullAnalysisJson_marksSynthesisUnavailable() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier sans synthèse", "Conseil de prud'hommes",
                "Bureau de jugement (fond)", "Demandeur (salarié)",
                null, null, null, null);

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Synthèse indisponible.");
        assertThat(message).contains("Aucune pièce numérotée identifiée.");
    }

    @Test
    void buildUserMessage_malformedAnalysisJson_doesNotThrow() {
        ConclusionPromptInput input = new ConclusionPromptInput(
                "Dossier JSON cassé", "Conseil de prud'hommes",
                "Bureau de jugement (fond)", "Demandeur (salarié)",
                "{ ceci n'est pas du JSON", List.of(), List.of(), List.of());

        String message = builder.buildUserMessage(input);

        assertThat(message).contains("Synthèse indisponible (format inattendu).");
    }
}

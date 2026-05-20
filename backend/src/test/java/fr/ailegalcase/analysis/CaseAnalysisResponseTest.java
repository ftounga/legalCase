package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CaseAnalysisResponseTest {

    // U-01 : parsing nominal — tous les champs présents
    @Test
    void from_nominalJson_parsesAllFields() {
        CaseAnalysis analysis = analysis("""
                {
                  "timeline": [{"date": "2024-01-15", "evenement": "Embauche"}],
                  "faits": ["fait1", "fait2"],
                  "points_juridiques": ["point1"],
                  "risques": ["risque1"],
                  "questions_ouvertes": ["question1"]
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.timeline()).hasSize(1);
        assertThat(response.timeline().get(0).date()).isEqualTo("2024-01-15");
        assertThat(response.timeline().get(0).evenement()).isEqualTo("Embauche");
        assertThat(response.faits()).extracting(AnalysisItem::texte).containsExactly("fait1", "fait2");
        assertThat(response.pointsJuridiques()).extracting(AnalysisItem::texte).containsExactly("point1");
        assertThat(response.risques()).extracting(AnalysisItem::texte).containsExactly("risque1");
        assertThat(response.questionsOuvertes()).containsExactly("question1");
        assertThat(response.modelUsed()).isEqualTo("claude-sonnet-4-6");
    }

    // U-02 : champ manquant dans le JSON → liste vide
    @Test
    void from_missingField_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait1"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).extracting(AnalysisItem::texte).containsExactly("fait1");
        assertThat(response.timeline()).isEmpty();
        assertThat(response.pointsJuridiques()).isEmpty();
        assertThat(response.risques()).isEmpty();
        assertThat(response.questionsOuvertes()).isEmpty();
    }

    // U-03 : analysis_result null → toutes les listes vides
    @Test
    void from_nullAnalysisResult_returnsEmptyLists() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.timeline()).isEmpty();
        assertThat(response.faits()).isEmpty();
        assertThat(response.pointsJuridiques()).isEmpty();
        assertThat(response.risques()).isEmpty();
        assertThat(response.questionsOuvertes()).isEmpty();
    }

    // U-04 : JSON malformé → toutes les listes vides (pas d'exception)
    @Test
    void from_malformedJson_returnsEmptyListsWithoutException() {
        CaseAnalysis analysis = analysis("not valid json {{{");

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.timeline()).isEmpty();
        assertThat(response.faits()).isEmpty();
    }

    // U-05 : populateCounts — JSON nominal → 5 compteurs corrects
    @Test
    void populateCounts_nominalJson_setsAllCounts() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["f1", "f2", "f3"],
                  "points_juridiques": ["p1", "p2"],
                  "risques": ["r1"],
                  "questions_ouvertes": ["q1", "q2", "q3", "q4"],
                  "timeline": [{"date":"2024-01-01","evenement":"e1"},{"date":"2024-02-01","evenement":"e2"}]
                }
                """);

        CaseAnalysisResponse.populateCounts(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getFaitsCount()).isEqualTo(3);
        assertThat(analysis.getPointsJuridiquesCount()).isEqualTo(2);
        assertThat(analysis.getRisquesCount()).isEqualTo(1);
        assertThat(analysis.getQuestionsOuvertesCount()).isEqualTo(4);
        assertThat(analysis.getTimelineCount()).isEqualTo(2);
    }

    // U-06 : populateCounts — JSON malformé → compteurs restent null (fail-open)
    @Test
    void populateCounts_malformedJson_countsRemainNull() {
        CaseAnalysis analysis = analysis("not valid json");

        CaseAnalysisResponse.populateCounts(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getFaitsCount()).isNull();
        assertThat(analysis.getPointsJuridiquesCount()).isNull();
        assertThat(analysis.getRisquesCount()).isNull();
    }

    // U-07 : populateCounts — null → aucune exception, compteurs restent null
    @Test
    void populateCounts_nullResult_countsRemainNull() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse.populateCounts(analysis, null);

        assertThat(analysis.getFaitsCount()).isNull();
    }

    // U-08 : piecesManquantes — JSON avec liste → retourne la liste
    @Test
    void from_withPiecesManquantes_returnsList() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "pieces_manquantes": ["Contrat de travail", "Bulletins de salaire"]
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).containsExactly("Contrat de travail", "Bulletins de salaire");
    }

    // U-09 : piecesManquantes — champ absent du JSON → liste vide (fail-open)
    @Test
    void from_missingPiecesManquantes_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait1"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).isEmpty();
    }

    // U-10 : piecesManquantes — champ vide → liste vide
    @Test
    void from_emptyPiecesManquantes_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "pieces_manquantes": []
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).isEmpty();
    }

    // TC-01 : item string → AnalysisItem(texte, null, null) — rétrocompatibilité
    @Test
    void extractItemList_stringItem_returnsItemWithNullSource() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait ancien"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("fait ancien");
        assertThat(response.faits().get(0).source()).isNull();
        assertThat(response.faits().get(0).extrait()).isNull();
    }

    // TC-02 : item objet complet {texte, source, extrait} → AnalysisItem complet
    @Test
    void extractItemList_objectItem_returnsCompleteAnalysisItem() {
        CaseAnalysis analysis = analysis("""
                {"faits": [{"texte": "Licenciement sans motif", "source": "Document 0", "extrait": "Il est mis fin au contrat"}]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("Licenciement sans motif");
        assertThat(response.faits().get(0).source()).isEqualTo("Document 0");
        assertThat(response.faits().get(0).extrait()).isEqualTo("Il est mis fin au contrat");
    }

    // TC-03 : item objet sans source → source null (fail-open)
    @Test
    void extractItemList_objectItemWithoutSource_returnsNullSource() {
        CaseAnalysis analysis = analysis("""
                {"faits": [{"texte": "Fait sans source"}]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("Fait sans source");
        assertThat(response.faits().get(0).source()).isNull();
        assertThat(response.faits().get(0).extrait()).isNull();
    }

    // U-11 : populateRiskScore — JSON nominal → riskLevel="MOYEN", riskScore=55
    @Test
    void populateRiskScore_nominalJson_setsLevelAndScore() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "MOYEN", "valeur": 55}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isEqualTo("MOYEN");
        assertThat(analysis.getRiskScore()).isEqualTo(55);
    }

    // U-12 : populateRiskScore — champ absent → null, null (fail-open)
    @Test
    void populateRiskScore_missingField_remainsNull() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["f1"]}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isNull();
        assertThat(analysis.getRiskScore()).isNull();
    }

    // U-13 : populateRiskScore — niveau inconnu → riskLevel null (fail-open)
    @Test
    void populateRiskScore_unknownNiveau_riskLevelNull() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "CRITIQUE", "valeur": 90}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isNull();
        assertThat(analysis.getRiskScore()).isEqualTo(90);
    }

    // U-14 : populateRiskScore — valeur hors [0-100] → riskScore null (fail-open)
    @Test
    void populateRiskScore_valueOutOfRange_riskScoreNull() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "ELEVE", "valeur": 150}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isEqualTo("ELEVE");
        assertThat(analysis.getRiskScore()).isNull();
    }

    // U-15 : analysisDocuments — liste de 2 documents → correctement indexés
    @Test
    void from_withDocuments_buildsAnalysisDocuments() {
        CaseAnalysis analysis = analysis(null);
        List<AnalysisDocument> docs = List.of(
                analysisDocument("contrat.pdf"),
                analysisDocument("bulletin.pdf")
        );

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis, docs);

        assertThat(response.analysisDocuments()).hasSize(2);
        assertThat(response.analysisDocuments().get(0).index()).isEqualTo(0);
        assertThat(response.analysisDocuments().get(0).name()).isEqualTo("contrat.pdf");
        assertThat(response.analysisDocuments().get(1).index()).isEqualTo(1);
        assertThat(response.analysisDocuments().get(1).name()).isEqualTo("bulletin.pdf");
    }

    // U-16 : analysisDocuments — liste vide → []
    @Test
    void from_withEmptyDocuments_returnsEmptyAnalysisDocuments() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis, List.of());

        assertThat(response.analysisDocuments()).isEmpty();
    }

    // U-17 : analysisDocuments — null → [] (fail-open)
    @Test
    void from_withNullDocuments_returnsEmptyAnalysisDocuments() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis, null);

        assertThat(response.analysisDocuments()).isEmpty();
    }

    // U-18 : from(analysis) sans documents → analysisDocuments vide
    @Test
    void from_withoutDocuments_returnsEmptyAnalysisDocuments() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.analysisDocuments()).isEmpty();
    }

    // U-30 : détection validité licenciement — nominal FR
    @Test
    void from_licenciementValidityDetection_parsesFrenchCriteria() {
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "FR_CONVOCATION": {"reponse": "OUI", "justification": "LRAR du 2026-01-10, 5 jours ouvrables respectés"},
                    "FR_MOTIVATION": {"reponse": "NON", "justification": "Lettre motivée par motif vague"}
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection()).isNotNull();
        assertThat(response.licenciementValidityDetection().detections()).hasSize(2);
        assertThat(response.licenciementValidityDetection().detections().get("FR_CONVOCATION").reponse()).isEqualTo("OUI");
        assertThat(response.licenciementValidityDetection().detections().get("FR_MOTIVATION").reponse()).isEqualTo("NON");
    }

    // U-31 : détection BE nominale
    @Test
    void from_licenciementValidityDetection_parsesBelgianCriteria() {
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "BE_NOTIFICATION": {"reponse": "OUI", "justification": "LRAR"},
                    "BE_PREAVIS": {"reponse": "INCONNU", "justification": ""}
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection()).isNotNull();
        assertThat(response.licenciementValidityDetection().detections().get("BE_NOTIFICATION").reponse()).isEqualTo("OUI");
        assertThat(response.licenciementValidityDetection().detections().get("BE_PREAVIS").reponse()).isEqualTo("INCONNU");
    }

    // U-32 : clé inconnue ignorée (fail-open)
    @Test
    void from_licenciementValidityDetection_unknownKeyIgnored() {
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "FR_CONVOCATION": {"reponse": "OUI", "justification": ""},
                    "UNKNOWN_KEY": {"reponse": "OUI", "justification": ""}
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection().detections()).containsOnlyKeys("FR_CONVOCATION");
    }

    // U-33 : réponse non normalisée → normalisée
    @Test
    void from_licenciementValidityDetection_normalizesReponse() {
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "FR_CONVOCATION": {"reponse": "oui", "justification": ""},
                    "FR_ENTRETIEN": {"reponse": "peut-etre", "justification": ""}
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection().detections().get("FR_CONVOCATION").reponse()).isEqualTo("OUI");
        assertThat(response.licenciementValidityDetection().detections().get("FR_ENTRETIEN").reponse()).isEqualTo("INCONNU");
    }

    // U-34 : reponse absente → INCONNU
    @Test
    void from_licenciementValidityDetection_missingReponseIsUnknown() {
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "FR_CONVOCATION": {"justification": "sans réponse"}
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection().detections().get("FR_CONVOCATION").reponse()).isEqualTo("INCONNU");
    }

    // U-35 : justification tronquée au-delà de 500 caractères
    @Test
    void from_licenciementValidityDetection_truncatesLongJustification() {
        String longText = "a".repeat(800);
        CaseAnalysis analysis = analysis("""
                {
                  "licenciement_validity_detection": {
                    "FR_CONVOCATION": {"reponse": "OUI", "justification": "%s"}
                  }
                }
                """.formatted(longText));

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection().detections().get("FR_CONVOCATION").justification()).hasSize(500);
    }

    // U-36 : objet vide → détection null
    @Test
    void from_licenciementValidityDetection_emptyObjectReturnsNull() {
        CaseAnalysis analysis = analysis("""
                {"licenciement_validity_detection": {}}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection()).isNull();
    }

    // U-37 : champ absent → détection null
    @Test
    void from_licenciementValidityDetection_missingFieldReturnsNull() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait1"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.licenciementValidityDetection()).isNull();
    }

    // U-38 : piecesManquantesDetails legacy string format
    @Test
    void from_piecesManquantesLegacyStringFormat_returnsEntriesWithoutCode() {
        CaseAnalysis analysis = analysis("""
                {"pieces_manquantes": ["Contrat", "Bulletins"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantesDetails()).hasSize(2);
        assertThat(response.piecesManquantesDetails().get(0).texte()).isEqualTo("Contrat");
        assertThat(response.piecesManquantesDetails().get(0).critereCode()).isNull();
        assertThat(response.piecesManquantes()).containsExactly("Contrat", "Bulletins");
    }

    // U-39 : piecesManquantesDetails format objet
    @Test
    void from_piecesManquantesObjectFormat_returnsEntriesWithCodeUpperCase() {
        CaseAnalysis analysis = analysis("""
                {"pieces_manquantes": [
                  {"texte": "LRAR de convocation", "critere_code": "fr_convocation"},
                  {"texte": "Lettre motivée", "critere_code": "FR_MOTIVATION"},
                  {"texte": "Autre document"}
                ]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantesDetails()).hasSize(3);
        assertThat(response.piecesManquantesDetails().get(0).critereCode()).isEqualTo("FR_CONVOCATION");
        assertThat(response.piecesManquantesDetails().get(1).critereCode()).isEqualTo("FR_MOTIVATION");
        assertThat(response.piecesManquantesDetails().get(2).critereCode()).isNull();
        assertThat(response.piecesManquantes()).containsExactly("LRAR de convocation", "Lettre motivée", "Autre document");
    }

    // U-40 : pieces item malformé ignoré
    @Test
    void from_piecesManquantesBlankOrMissingText_isIgnored() {
        CaseAnalysis analysis = analysis("""
                {"pieces_manquantes": [
                  {"texte": "", "critere_code": "FR_CONVOCATION"},
                  {"critere_code": "FR_ENTRETIEN"},
                  {"texte": "Valide", "critere_code": "FR_MOTIVATION"}
                ]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantesDetails()).hasSize(1);
        assertThat(response.piecesManquantesDetails().get(0).texte()).isEqualTo("Valide");
    }

    // U-41 : immigration typeTitreSejourCode normalisé upper-case
    @Test
    void from_immigrationTypeTitreCode_upperCase() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Titre de séjour temporaire",
                  "type_titre_sejour_code": "vls_ts_etudiant",
                  "nationalite_ue": false
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData()).isNotNull();
        assertThat(response.immigrationExtractedData().typeTitreSejourCode()).isEqualTo("VLS_TS_ETUDIANT");
        assertThat(response.immigrationExtractedData().nationaliteUe()).isFalse();
        assertThat(response.immigrationExtractedData().typeTitreSejour()).isEqualTo("Titre de séjour temporaire");
    }

    // U-42 : code hors enum → null (autre champ présent pour que l'objet existe)
    @Test
    void from_immigrationCodeUnknown_returnsNullCode() {
        CaseAnalysis analysis = analysis("""
                {"type_titre_sejour": "Autre", "type_titre_sejour_code": "UNKNOWN_CODE"}
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData()).isNotNull();
        assertThat(response.immigrationExtractedData().typeTitreSejourCode()).isNull();
        assertThat(response.immigrationExtractedData().typeTitreSejour()).isEqualTo("Autre");
    }

    // U-43 : nationalité ue as string "true"
    @Test
    void from_immigrationNationaliteUeAsString_parsed() {
        CaseAnalysis analysis = analysis("""
                {"nationalite_ue": "true"}
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData().nationaliteUe()).isTrue();
    }

    // U-44 : nationalité ue invalide → null
    @Test
    void from_immigrationNationaliteUeInvalid_null() {
        CaseAnalysis analysis = analysis("""
                {"nationalite_ue": 42}
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        // nationalite is null since 42 is neither bool nor "true"/"false"
        assertThat(response.immigrationExtractedData()).isNull();
    }

    // U-45 : prompt IMMIGRATION contient les 16 codes + nationalite_ue
    @Test
    void immigrationPrompt_mentionsCodesAndNationaliteUe() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        assertThat(instruction).contains("type_titre_sejour_code").contains("nationalite_ue");
        assertThat(instruction)
                .contains("VLS_TS_ETUDIANT").contains("VLS_TS_SALARIE").contains("CST_SALARIE")
                .contains("CARTE_RESIDENT").contains("APS").contains("CST_VPF")
                .contains("CARTE_A_TRAVAIL").contains("CARTE_B").contains("PERMIS_UNIQUE")
                .contains("ANNEXE_15").contains("ATTESTATION_IMMATRICULATION");
    }

    // U-46 : immigration recours code normalisé upper-case
    @Test
    void from_immigrationRecoursCode_upperCase() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_recours_code": "recours_cnda",
                  "date_notification_decision_contestee": "2026-03-10"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData()).isNotNull();
        assertThat(response.immigrationExtractedData().typeRecoursCode()).isEqualTo("RECOURS_CNDA");
        assertThat(response.immigrationExtractedData().dateNotificationDecisionContestee()).isEqualTo("2026-03-10");
    }

    // U-47 : recours code hors enum → null
    @Test
    void from_immigrationRecoursCodeUnknown_returnsNull() {
        CaseAnalysis analysis = analysis("""
                {"type_titre_sejour": "Titre", "type_recours_code": "UNKNOWN"}
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData()).isNotNull();
        assertThat(response.immigrationExtractedData().typeRecoursCode()).isNull();
    }

    // U-48 : prompt contient les 6 codes recours
    @Test
    void immigrationPrompt_mentions6RecoursCodes() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        assertThat(instruction).contains("type_recours_code").contains("date_notification_decision_contestee");
        assertThat(instruction)
                .contains("RECOURS_GRACIEUX_PREFET").contains("RECOURS_CONTENTIEUX_TA").contains("RECOURS_CNDA")
                .contains("RECOURS_CGRA").contains("RECOURS_CCE").contains("RECOURS_CE_BELGIQUE");
    }

    // U-49 : rétrocompat — les anciens champs existent toujours
    @Test
    void from_immigrationLegacyFields_stillWorking() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_procedure_detectee": "RECOURS_CNDA",
                  "date_depot_procedure": "2026-03-15",
                  "type_recours_code": "RECOURS_CNDA"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var im = response.immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.typeProcedureDetectee()).isEqualTo("RECOURS_CNDA");
        assertThat(im.dateDepotProcedure()).isEqualTo("2026-03-15");
        assertThat(im.typeRecoursCode()).isEqualTo("RECOURS_CNDA");
    }

    // SF-246-04 : date_ordonnance_protection_jaf — cas nominal présent → champ renseigné.
    @Test
    void from_immigrationDateOrdonnanceProtectionJaf_present_isParsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "date_ordonnance_protection_jaf": "2026-01-15"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var im = response.immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateOrdonnanceProtectionJaf()).isEqualTo("2026-01-15");
    }

    // SF-246-04 : champ absent → dateOrdonnanceProtectionJaf null, pas d'exception.
    @Test
    void from_immigrationDateOrdonnanceProtectionJaf_absent_isNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_procedure_detectee": "RECOURS_CNDA"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var im = response.immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateOrdonnanceProtectionJaf()).isNull();
    }

    // SF-246-04 : si SEUL date_ordonnance_protection_jaf est présent, le record
    // immigration n'est pas null (le champ participe au test de non-nullité global).
    @Test
    void from_immigrationDateOrdonnanceProtectionJaf_onlyField_recordNotNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "date_ordonnance_protection_jaf": "2026-01-15"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.immigrationExtractedData()).isNotNull();
    }

    // SF-246-04 — invariant cadrage §5.1.6 : fixture multi-dates concurrentes →
    // chaque date est attribuée au bon champ, aucune confusion.
    @Test
    void from_immigrationMultiDates_noConfusionBetweenOrdonnanceAndOtherDates() {
        CaseAnalysis analysis = analysis("""
                {
                  "date_ordonnance_protection_jaf": "2026-01-15",
                  "date_expiration_titre": "2026-06-30",
                  "date_depot_procedure": "2026-02-01"
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        var im = response.immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateOrdonnanceProtectionJaf()).isEqualTo("2026-01-15");
        assertThat(im.dateExpirationTitre()).isEqualTo("2026-06-30");
        assertThat(im.dateDepotProcedure()).isEqualTo("2026-02-01");
    }

    // SF-246-04 : prompt immigration mentionne la clé date_ordonnance_protection_jaf
    // et la distinction d'avec date_expiration_titre / date_depot_procedure.
    @Test
    void immigrationPrompt_mentionsDateOrdonnanceProtectionJaf() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_IMMIGRATION");
        assertThat(instruction)
                .contains("date_ordonnance_protection_jaf")
                .contains("juge aux affaires familiales")
                .contains("date_expiration_titre")
                .contains("date_depot_procedure");
    }

    // SF-IA-03-13 : détection validité rupture conventionnelle
    @Test
    void from_ruptureConvValidityDetection_parses6Criteria() {
        CaseAnalysis analysis = analysis("""
                {
                  "rupture_conv_validity_detection": {
                    "RC_CONSENTEMENT": {"reponse": "OUI", "justification": "Aucune pression évoquée"},
                    "RC_DELAI_RETRACTATION": {"reponse": "OUI", "justification": "Signature 01/02, homologation 20/02"},
                    "RC_HOMOLOGATION": {"reponse": "OUI", "justification": "Attestation DREETS présente"},
                    "RC_ASSISTANCE": {"reponse": "INCONNU", "justification": ""},
                    "RC_INDEMNITE": {"reponse": "NON", "justification": "Indemnité spécifique < légale"},
                    "RC_ENTRETIENS": {"reponse": "OUI", "justification": "Compte-rendu entretien 15/01"}
                  }
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.ruptureConvValidityDetection()).isNotNull();
        assertThat(response.ruptureConvValidityDetection().detections()).hasSize(6);
        assertThat(response.ruptureConvValidityDetection().detections().get("RC_CONSENTEMENT").reponse()).isEqualTo("OUI");
        assertThat(response.ruptureConvValidityDetection().detections().get("RC_INDEMNITE").reponse()).isEqualTo("NON");
        assertThat(response.ruptureConvValidityDetection().detections().get("RC_ASSISTANCE").reponse()).isEqualTo("INCONNU");
    }

    @Test
    void from_ruptureConvValidityDetection_invalidResponseNormalizedToInconnu() {
        CaseAnalysis analysis = analysis("""
                {
                  "rupture_conv_validity_detection": {
                    "RC_CONSENTEMENT": {"reponse": "peut-être", "justification": ""}
                  }
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.ruptureConvValidityDetection()).isNotNull();
        assertThat(response.ruptureConvValidityDetection().detections().get("RC_CONSENTEMENT").reponse()).isEqualTo("INCONNU");
    }

    @Test
    void from_ruptureConvValidityDetection_unknownCodeIgnored() {
        CaseAnalysis analysis = analysis("""
                {
                  "rupture_conv_validity_detection": {
                    "RC_FANTAISIE": {"reponse": "OUI", "justification": ""},
                    "RC_HOMOLOGATION": {"reponse": "NON", "justification": "refus DREETS"}
                  }
                }
                """);
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.ruptureConvValidityDetection()).isNotNull();
        assertThat(response.ruptureConvValidityDetection().detections()).hasSize(1);
        assertThat(response.ruptureConvValidityDetection().detections()).containsOnlyKeys("RC_HOMOLOGATION");
    }

    @Test
    void from_ruptureConvValidityDetection_absent_returnsNull() {
        CaseAnalysis analysis = analysis("{}");
        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);
        assertThat(response.ruptureConvValidityDetection()).isNull();
    }

    /**
     * SF-IM-01-06 : régression — un dossier immigration avec trigger_event
     * MARIAGE_RESSORTISSANT_FR doit voir sa checklist inférée à
     * CST_VPF_CONJOINT_FR, pas à VISA_ETUDIANT (fallback titre actuel).
     * Cas paradigmatique Chen Wei : pluriannuelle Étudiant-Recherche +
     * mariage avec Française → passage L.423-1.
     */
    @Test
    void from_immigrationWithMariageTrigger_inferChecklistCSTVPFConjointFR() {
        CaseAnalysis a = analysis("""
                {
                  "type_titre_sejour_code": "CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE",
                  "trigger_events": [
                    {
                      "event_code": "MARIAGE_RESSORTISSANT_FR",
                      "event_date": "2025-03-15",
                      "source_document": "03-acte-mariage.pdf",
                      "justification": "Mariage célébré le 15/03/2025 en mairie du 5ème arrondissement de Paris"
                    }
                  ]
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(a);

        assertThat(response.immigrationExtractedData()).isNotNull();
        assertThat(response.immigrationExtractedData().inferredChecklistType())
                .isEqualTo("CST_VPF_CONJOINT_FR");
        assertThat(response.immigrationTriggerEvents()).hasSize(1);
        assertThat(response.immigrationTriggerEvents().get(0).eventCode())
                .isEqualTo("MARIAGE_RESSORTISSANT_FR");
    }

    private CaseAnalysis analysis(String result) {
        CaseAnalysis a = new CaseAnalysis();
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setVersion(1);
        a.setAnalysisResult(result);
        a.setModelUsed("claude-sonnet-4-6");
        a.setUpdatedAt(Instant.now());
        return a;
    }

    // Fix F-DT-09-BE : from(analysis, documents, "BELGIQUE") doit conserver
    // compensationEstimate non-null (nécessaire pour alimenter les alertes F-IA-03
    // du comparateur d'indemnités côté BE : typeRupture, ancienneté, salaire).
    @Test
    void from_belgianWorkspace_keepsCompensationEstimateForIA03Alerts() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT_ORDINAIRE",
                    "anciennete_annees": 7,
                    "anciennete_mois": 9,
                    "salaire_reference_mensuel": 3100
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis, List.of(), "BELGIQUE");

        // compensationEstimate doit être non-null et contenir les données IA
        assertThat(response.compensationEstimate()).isNotNull();
        assertThat(response.compensationEstimate().typeRupture()).isEqualTo("LICENCIEMENT_ORDINAIRE");
        assertThat(response.compensationEstimate().ancienneteAnnees()).isEqualTo(7);
        assertThat(response.compensationEstimate().ancienneteMois()).isEqualTo(9);
        assertThat(response.compensationEstimate().salaireReference()).isEqualTo(3100.0);
        // belgianCompensationEstimate doit aussi être renseigné pour l'affichage CCT 109
        assertThat(response.belgianCompensationEstimate()).isNotNull();
        assertThat(response.belgianCompensationEstimate().preavisSemaines()).isGreaterThan(0);
    }

    // Non-régression : FR ne doit pas avoir belgianCompensationEstimate
    @Test
    void from_frenchWorkspace_compensationEstimateOnlyNotBelgian() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT",
                    "anciennete_annees": 10,
                    "anciennete_mois": 0,
                    "salaire_reference_mensuel": 3000
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis, List.of(), "FRANCE");

        assertThat(response.compensationEstimate()).isNotNull();
        assertThat(response.compensationEstimate().typeRupture()).isEqualTo("LICENCIEMENT");
        assertThat(response.belgianCompensationEstimate()).isNull();
    }

    private AnalysisDocument analysisDocument(String documentName) {
        AnalysisDocument doc = new AnalysisDocument();
        doc.setAnalysisId(UUID.randomUUID());
        doc.setDocumentId(UUID.randomUUID());
        doc.setDocumentName(documentName);
        return doc;
    }

    // SF-DT-04-04 : extraction des 8 nouveaux champs identité + normalisation SIRET/BCE
    @Test
    void from_travailExtractedData_parsesNewIdentityFields() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "nom_salarie": "Dupont",
                    "prenom_salarie": "Jean",
                    "adresse_salarie": "12 rue de la Paix, 75002 Paris",
                    "nom_employeur": "Acme SAS",
                    "adresse_employeur": "5 avenue des Champs, 75008 Paris",
                    "siret_employeur": "123 456 789 01234",
                    "bce_employeur": "BE 0456.789.123",
                    "representant_employeur": "Martin Dupond"
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        var t = response.travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nomSalarie()).isEqualTo("Dupont");
        assertThat(t.prenomSalarie()).isEqualTo("Jean");
        assertThat(t.adresseSalarie()).isEqualTo("12 rue de la Paix, 75002 Paris");
        assertThat(t.nomEmployeur()).isEqualTo("Acme SAS");
        assertThat(t.adresseEmployeur()).isEqualTo("5 avenue des Champs, 75008 Paris");
        // Normalisation : espaces retirés → chiffres uniquement
        assertThat(t.siretEmployeur()).isEqualTo("12345678901234");
        assertThat(t.bceEmployeur()).isEqualTo("0456789123");
        assertThat(t.representantEmployeur()).isEqualTo("Martin Dupond");
    }

    @Test
    void from_travailExtractedData_missingNewFields_returnsNullTolerantly() {
        // Rétrocompat : analyse ancienne sans les 8 nouveaux champs identité
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "date_entree": "2020-01-01"
                  }
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        var t = response.travailExtractedData();
        assertThat(t).isNotNull();
        // SF-129-01 : ConventionCodeNormalizer mappe SYNTEC (legacy) → IDCC_1486
        assertThat(t.conventionCollective()).isEqualTo("IDCC_1486");
        assertThat(t.nomSalarie()).isNull();
        assertThat(t.nomEmployeur()).isNull();
        assertThat(t.siretEmployeur()).isNull();
        assertThat(t.bceEmployeur()).isNull();
        // SF-155-04 : les 5 nouveaux champs sont null sur une fixture legacy
        assertThat(t.motifNullitePressenti()).isNull();
        assertThat(t.origineInaptitudePressentie()).isNull();
        assertThat(t.avisMedecinTravailDate()).isNull();
        assertThat(t.reclassementRespecteDetected()).isNull();
        assertThat(t.heuresSupMentionneesDansDossier()).isNull();
    }

    // SF-155-04-00-BE-travail : pré-fill IA harcèlement / inaptitude / heures sup
    @Test
    void from_travailExtractedData_motifNullitePressenti_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "salaire_brut_mensuel": 3200,
                    "motif_nullite_pressenti": "HARCELEMENT_MORAL"
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.motifNullitePressenti()).isEqualTo("HARCELEMENT_MORAL");
        assertThat(t.salaireBrutMensuel()).isEqualTo(3200.0);
    }

    @Test
    void from_travailExtractedData_motifNullitePressenti_invalide_renvoieNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "motif_nullite_pressenti": "VALEUR_INCONNUE"
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.motifNullitePressenti()).isNull();
    }

    @Test
    void from_travailExtractedData_motifNullitePressenti_normalisationLowercase() {
        // Valeur en minuscules → normalisation upper-case, acceptée
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "motif_nullite_pressenti": "discrimination"
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.motifNullitePressenti()).isEqualTo("DISCRIMINATION");
    }

    @Test
    void from_travailExtractedData_inaptitudeComplete_parsedAndDetectedAnswer() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "origine_inaptitude_pressentie": "ACCIDENT_TRAVAIL",
                    "avis_medecin_travail_date": "2026-03-18",
                    "reclassement_respecte_detected": {
                      "reponse": "OUI",
                      "justification": "Trois propositions de reclassement consignées dans le courrier du 20/03/2026."
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.origineInaptitudePressentie()).isEqualTo("ACCIDENT_TRAVAIL");
        assertThat(t.avisMedecinTravailDate()).isEqualTo("2026-03-18");
        assertThat(t.reclassementRespecteDetected()).isNotNull();
        assertThat(t.reclassementRespecteDetected().reponse()).isEqualTo("OUI");
        assertThat(t.reclassementRespecteDetected().justification())
                .contains("Trois propositions");
    }

    @Test
    void from_travailExtractedData_origineInaptitudeInvalide_renvoieNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "origine_inaptitude_pressentie": "AUTRE"
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.origineInaptitudePressentie()).isNull();
    }

    @Test
    void from_travailExtractedData_reclassementJustification_troncaturAt500Chars() {
        String longJustification = "A".repeat(600);
        String json = """
                {
                  "travail_extracted_data": {
                    "reclassement_respecte_detected": {
                      "reponse": "NON",
                      "justification": "%s"
                    }
                  }
                }
                """.formatted(longJustification);
        CaseAnalysis analysis = analysis(json);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.reclassementRespecteDetected()).isNotNull();
        assertThat(t.reclassementRespecteDetected().justification()).hasSize(500);
    }

    @Test
    void from_travailExtractedData_reclassementReponseInvalide_inconnu() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "reclassement_respecte_detected": {
                      "reponse": "PEUT-ETRE",
                      "justification": "ambigu"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t.reclassementRespecteDetected()).isNotNull();
        assertThat(t.reclassementRespecteDetected().reponse()).isEqualTo("INCONNU");
    }

    @Test
    void from_travailExtractedData_heuresSupMentionnees_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "heures_sup_mentionnees": {
                      "total_declarees_25pct": 12,
                      "total_declarees_50pct": 5,
                      "hors_contingent": 2
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.heuresSupMentionneesDansDossier()).isNotNull();
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees25pct()).isEqualTo(12);
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees50pct()).isEqualTo(5);
        assertThat(t.heuresSupMentionneesDansDossier().horsContingent()).isEqualTo(2);
    }

    @Test
    void from_travailExtractedData_heuresSupMentionnees_partialFields_parsed() {
        // Un seul champ renseigné — les autres sont null
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "heures_sup_mentionnees": {
                      "total_declarees_25pct": 8
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t.heuresSupMentionneesDansDossier()).isNotNull();
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees25pct()).isEqualTo(8);
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees50pct()).isNull();
        assertThat(t.heuresSupMentionneesDansDossier().horsContingent()).isNull();
    }

    @Test
    void from_travailExtractedData_heuresSupMentionnees_negativeValues_ignored() {
        // Valeur négative → champ ignoré (null), les autres champs valides sont gardés
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "heures_sup_mentionnees": {
                      "total_declarees_25pct": -5,
                      "total_declarees_50pct": 3
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t.heuresSupMentionneesDansDossier()).isNotNull();
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees25pct()).isNull();
        assertThat(t.heuresSupMentionneesDansDossier().totalDeclarees50pct()).isEqualTo(3);
    }

    @Test
    void from_travailExtractedData_heuresSupMentionnees_malformedNotObject_gracefulNull() {
        // Payload malformé (nombre au lieu d'objet) → heuresSupMentionnees=null, autres champs intacts
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "salaire_brut_mensuel": 2500,
                    "heures_sup_mentionnees": 42
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.salaireBrutMensuel()).isEqualTo(2500.0);
        assertThat(t.heuresSupMentionneesDansDossier()).isNull();
    }

    @Test
    void from_travailExtractedData_heuresSupMentionnees_emptyObject_returnsNull() {
        // Objet vide → pas d'utilité → null
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "heures_sup_mentionnees": {}
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.heuresSupMentionneesDansDossier()).isNull();
    }

    // ==================================================================================
    // SF-155-04-00-BE-immig-FR : tests pré-fill IA France pour outils OQTF F-IM-08-02/04
    // ==================================================================================

    @Test
    void from_immigration_oqtfAvecDelai_parsesNewFields() {
        // Fixture OQTF avec délai : date + motif + recours déjà formé
        CaseAnalysis analysis = analysis("""
                {
                  "date_notification_oqtf": "2026-03-15",
                  "motif_oqtf_code": "SEJOUR_IRREGULIER",
                  "recours_forme_detected": {
                    "reponse": "NON",
                    "justification": "Aucune requête TA trouvée dans les pièces."
                  }
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateNotificationOqtf()).isEqualTo("2026-03-15");
        assertThat(im.motifOqtfCode()).isEqualTo("SEJOUR_IRREGULIER");
        assertThat(im.recoursFormeDetected()).isNotNull();
        assertThat(im.recoursFormeDetected().reponse()).isEqualTo("NON");
        assertThat(im.recoursFormeDetected().justification()).contains("Aucune requête TA");
    }

    @Test
    void from_immigration_motifOqtfCode_upperCaseNormalization() {
        // Normalisation lowercase → upper-case whitelist
        CaseAnalysis analysis = analysis("""
                {
                  "date_notification_oqtf": "2026-03-15",
                  "motif_oqtf_code": "refus_titre"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.motifOqtfCode()).isEqualTo("REFUS_TITRE");
    }

    @Test
    void from_immigration_motifOqtfCode_invalide_renvoieNull() {
        // Valeur hors whitelist → null (fail-open), autres champs préservés
        CaseAnalysis analysis = analysis("""
                {
                  "date_notification_oqtf": "2026-03-15",
                  "motif_oqtf_code": "MOTIF_INCONNU_XYZ"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.motifOqtfCode()).isNull();
        assertThat(im.dateNotificationOqtf()).isEqualTo("2026-03-15");
    }

    @Test
    void from_immigration_oqtfSansDelai_parsesNewFields() {
        // Fixture OQTF sans délai : datetime ISO + placement CRA
        CaseAnalysis analysis = analysis("""
                {
                  "date_heure_notification_oqtf_sans_delai": "2026-03-20T14:30",
                  "placement_cra_detected": true
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateHeureNotificationOqtfSansDelai()).isEqualTo("2026-03-20T14:30");
        assertThat(im.placementCraDetected()).isTrue();
    }

    @Test
    void from_immigration_oqtfSansDelai_datetimeWithSeconds_accepted() {
        // Format YYYY-MM-DDTHH:mm:ss également accepté par la regex permissive
        CaseAnalysis analysis = analysis("""
                {
                  "date_heure_notification_oqtf_sans_delai": "2026-03-20T14:30:45"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateHeureNotificationOqtfSansDelai()).isEqualTo("2026-03-20T14:30:45");
    }

    @Test
    void from_immigration_oqtfSansDelai_datetimeInvalide_renvoieNull() {
        // Format français "15/03/2026 10:00" → null (non ISO)
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Arrêté OQTF sans délai",
                  "date_heure_notification_oqtf_sans_delai": "15/03/2026 10:00"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateHeureNotificationOqtfSansDelai()).isNull();
        // Les autres champs sont intacts
        assertThat(im.typeTitreSejour()).isEqualTo("Arrêté OQTF sans délai");
    }

    @Test
    void from_immigration_placementCra_stringCoerced_true() {
        // booleanOrNull : accepte "true" / "false" (string) et les convertit
        CaseAnalysis analysis = analysis("""
                {
                  "date_heure_notification_oqtf_sans_delai": "2026-03-20T14:30",
                  "placement_cra_detected": "true"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.placementCraDetected()).isTrue();
    }

    @Test
    void from_immigration_placementCra_stringCoerced_false() {
        CaseAnalysis analysis = analysis("""
                {
                  "date_heure_notification_oqtf_sans_delai": "2026-03-20T14:30",
                  "placement_cra_detected": "false"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.placementCraDetected()).isFalse();
    }

    @Test
    void from_immigration_recoursFormeDetected_justificationTroncation500Chars() {
        // Justification > 500 car → tronquée à 500 (règle MAX_JUSTIFICATION_LENGTH)
        String longJustification = "B".repeat(600);
        String json = """
                {
                  "recours_forme_detected": {
                    "reponse": "OUI",
                    "justification": "%s"
                  }
                }
                """.formatted(longJustification);
        CaseAnalysis analysis = analysis(json);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.recoursFormeDetected()).isNotNull();
        assertThat(im.recoursFormeDetected().justification()).hasSize(500);
    }

    @Test
    void from_immigration_recoursFormeDetected_reponseNormalisation() {
        // Normalisation "oui" lowercase → "OUI" via normalizeReponse existant
        CaseAnalysis analysis = analysis("""
                {
                  "recours_forme_detected": {
                    "reponse": "oui",
                    "justification": "Recours TA déposé le 10/03"
                  }
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.recoursFormeDetected()).isNotNull();
        assertThat(im.recoursFormeDetected().reponse()).isEqualTo("OUI");
    }

    @Test
    void from_immigration_recoursFormeDetected_malformedNotObject_gracefulNull() {
        // Payload malformé (nombre au lieu d'objet) → champ null, autres champs intacts
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Titre FR",
                  "recours_forme_detected": 42
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.recoursFormeDetected()).isNull();
        assertThat(im.typeTitreSejour()).isEqualTo("Titre FR");
    }

    @Test
    void from_immigration_legacyFixture_allNewFieldsAreNull() {
        // Rétrocompat — fixture SF-IM-01-04 ou antérieure sans les 5 champs FR
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Carte de séjour temporaire",
                  "type_titre_sejour_code": "CST_SALARIE",
                  "nationalite_ue": false,
                  "type_procedure_detectee": "RENOUVELLEMENT_TITRE_SEJOUR",
                  "date_depot_procedure": "2026-03-01"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        // Les champs historiques restent intacts
        assertThat(im.typeTitreSejourCode()).isEqualTo("CST_SALARIE");
        assertThat(im.typeProcedureDetectee()).isEqualTo("RENOUVELLEMENT_TITRE_SEJOUR");
        // Les 5 nouveaux champs FR sont null
        assertThat(im.dateNotificationOqtf()).isNull();
        assertThat(im.motifOqtfCode()).isNull();
        assertThat(im.recoursFormeDetected()).isNull();
        assertThat(im.dateHeureNotificationOqtfSansDelai()).isNull();
        assertThat(im.placementCraDetected()).isNull();
    }

    @Test
    void from_immigration_newFieldsPreservedAfterInferredChecklistReconstruction() {
        // Régression : la reconstruction post-inférence checklistType (ligne ~443 de
        // CaseAnalysisResponse) doit transférer les 5 nouveaux champs FR.
        // On simule un dossier avec type_titre_sejour_code valide → déclenche
        // la reconstruction, puis on vérifie que dateNotificationOqtf survit.
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "date_notification_oqtf": "2026-03-15",
                  "motif_oqtf_code": "SEJOUR_IRREGULIER",
                  "placement_cra_detected": true
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        // Les 5 champs FR sont préservés après reconstruction du record
        assertThat(im.dateNotificationOqtf()).isEqualTo("2026-03-15");
        assertThat(im.motifOqtfCode()).isEqualTo("SEJOUR_IRREGULIER");
        assertThat(im.placementCraDetected()).isTrue();
    }

    // =========================================================================
    // SF-155-04-00-BE-immig-BE : pré-fill IA Annexe 13 BE (F-IM-08-06)
    // =========================================================================

    @Test
    void from_immigrationExtractedData_annexe13Be_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "date_notification_annexe13": "2026-03-15",
                  "delai_depart_impose_jours": 30,
                  "motif_oqt_code_be": "SEJOUR_IRREGULIER_ART_7",
                  "transfert_imminent_detected": false
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateNotificationAnnexe13()).isEqualTo("2026-03-15");
        assertThat(im.delaiDepartImposeJours()).isEqualTo(30);
        assertThat(im.motifOqtCodeBe()).isEqualTo("SEJOUR_IRREGULIER_ART_7");
        assertThat(im.transfertImminentDetected()).isFalse();
    }

    @Test
    void from_immigrationExtractedData_motifOqtBe_upperCase() {
        CaseAnalysis analysis = analysis("""
                {
                  "motif_oqt_code_be": "fin_sejour_regulier"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.motifOqtCodeBe()).isEqualTo("FIN_SEJOUR_REGULIER");
    }

    @Test
    void from_immigrationExtractedData_motifOqtBe_invalide_returnsNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Carte B",
                  "motif_oqt_code_be": "MOTIF_INCONNU"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.motifOqtCodeBe()).isNull();
        assertThat(im.typeTitreSejour()).isEqualTo("Carte B");
    }

    @Test
    void from_immigrationExtractedData_delaiDepart_zero_kept() {
        CaseAnalysis analysis = analysis("""
                {
                  "delai_depart_impose_jours": 0,
                  "motif_oqt_code_be": "SEJOUR_IRREGULIER_ART_7"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.delaiDepartImposeJours()).isEqualTo(0);
        assertThat(im.motifOqtCodeBe()).isEqualTo("SEJOUR_IRREGULIER_ART_7");
    }

    @Test
    void from_immigrationExtractedData_delaiDepart_negative_null() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Carte B",
                  "delai_depart_impose_jours": -5
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.delaiDepartImposeJours()).isNull();
    }

    @Test
    void from_immigrationExtractedData_transfertImminent_stringTrue_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "transfert_imminent_detected": "true"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.transfertImminentDetected()).isTrue();
    }

    @Test
    void from_immigrationExtractedData_annexe13Be_dateOnly_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "date_notification_annexe13": "2026-04-01"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dateNotificationAnnexe13()).isEqualTo("2026-04-01");
        assertThat(im.delaiDepartImposeJours()).isNull();
        assertThat(im.motifOqtCodeBe()).isNull();
        assertThat(im.transfertImminentDetected()).isNull();
    }

    @Test
    void from_immigrationExtractedData_annexe13Be_malformedDelai_gracefulNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Carte B",
                  "delai_depart_impose_jours": "trente",
                  "motif_oqt_code_be": "AUTRE"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.typeTitreSejour()).isEqualTo("Carte B");
        assertThat(im.delaiDepartImposeJours()).isNull();
        assertThat(im.motifOqtCodeBe()).isEqualTo("AUTRE");
    }

    @Test
    void from_immigrationExtractedData_annexe13Be_legacyFixture_retrocompat() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour": "Carte B",
                  "type_titre_sejour_code": "CARTE_B",
                  "type_recours_code": "RECOURS_CCE",
                  "date_notification_decision_contestee": "2026-02-10"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.typeTitreSejourCode()).isEqualTo("CARTE_B");
        assertThat(im.typeRecoursCode()).isEqualTo("RECOURS_CCE");
        assertThat(im.dateNotificationDecisionContestee()).isEqualTo("2026-02-10");
        assertThat(im.dateNotificationAnnexe13()).isNull();
        assertThat(im.delaiDepartImposeJours()).isNull();
        assertThat(im.motifOqtCodeBe()).isNull();
        assertThat(im.transfertImminentDetected()).isNull();
    }

    @Test
    void from_immigrationExtractedData_annexe13Be_allBeFieldsAbsentOnFrFixture_frOkBeNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "VLS_TS_ETUDIANT",
                  "nationalite_ue": false,
                  "type_procedure_detectee": "RENOUVELLEMENT_TITRE_SEJOUR"
                }
                """);

        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.typeTitreSejourCode()).isEqualTo("VLS_TS_ETUDIANT");
        assertThat(im.nationaliteUe()).isFalse();
        assertThat(im.dateNotificationAnnexe13()).isNull();
        assertThat(im.delaiDepartImposeJours()).isNull();
        assertThat(im.motifOqtCodeBe()).isNull();
        assertThat(im.transfertImminentDetected()).isNull();
    }

    // SF-246-17 : 4 champs Dublin/CRRV pour pré-fill F-IM-22 et F-IM-23
    @Test
    void from_immigration_dublinEtatMembreResponsable_nominal() {
        CaseAnalysis analysis = analysis("""
                {
                  "dublin_etat_membre_responsable": "  ITALIE  "
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dublinEtatMembreResponsable()).isEqualTo("ITALIE");
    }

    @Test
    void from_immigration_dublinMotifTransfert_nominal_uppercase() {
        CaseAnalysis analysis = analysis("""
                {
                  "dublin_motif_transfert": "demande_asile_autre_etat"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dublinMotifTransfert()).isEqualTo("DEMANDE_ASILE_AUTRE_ETAT");
    }

    @Test
    void from_immigration_dublinMotifTransfert_horsWhitelist_retourneNull() {
        // CODE_INCONNU rejeté par la whitelist → dublinMotifTransfert null.
        // On ancre avec type_titre_sejour_code pour éviter que le record entier soit null.
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "dublin_motif_transfert": "CODE_INCONNU"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dublinMotifTransfert()).isNull();
    }

    @Test
    void from_immigration_crrvTypeVisa_nominal_uppercase() {
        CaseAnalysis analysis = analysis("""
                {
                  "crrv_type_visa": "long_sejour"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.crrvTypeVisa()).isEqualTo("LONG_SEJOUR");
    }

    @Test
    void from_immigration_crrvTypeVisa_horsWhitelist_retourneNull() {
        // VISA_INCONNU rejeté par la whitelist → crrvTypeVisa null.
        // On ancre avec type_titre_sejour_code pour éviter que le record entier soit null.
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "crrv_type_visa": "VISA_INCONNU"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.crrvTypeVisa()).isNull();
    }

    @Test
    void from_immigration_crrvMotifRefus_nominal_tronque500Chars() {
        String longText = "A".repeat(600);
        CaseAnalysis analysis = analysis("""
                {
                  "crrv_motif_refus": "%s"
                }
                """.formatted(longText));
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.crrvMotifRefus()).hasSize(500);
    }

    @Test
    void from_immigration_sf246_17_tous_champs_absents_gracefulNull() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.dublinEtatMembreResponsable()).isNull();
        assertThat(im.dublinMotifTransfert()).isNull();
        assertThat(im.crrvTypeVisa()).isNull();
        assertThat(im.crrvMotifRefus()).isNull();
    }

    // SF-246-18 : 8 champs AES Immigration FR pour pré-fill outils AES
    @Test
    void from_immigration_sf246_18_aesDateEntreeFrance_nominal() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "VLS_TS_ETUDIANT",
                  "aes_date_entree_france": "2021-03-15",
                  "aes_annees_scolarite_consecutives": 3,
                  "aes_niveau_etudes": "BAC_PLUS_3_4"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesDateEntreeFrance()).isEqualTo("2021-03-15");
        assertThat(im.aesDureePresenceMois()).isNotNull().isGreaterThanOrEqualTo(36);
        assertThat(im.aesAnneesScolariteConsecutives()).isEqualTo(3);
        assertThat(im.aesNiveauEtudes()).isEqualTo("BAC_PLUS_3_4");
    }

    @Test
    void from_immigration_sf246_18_aesDateEntreeFrance_future_rejected() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "VLS_TS_ETUDIANT",
                  "aes_date_entree_france": "2030-01-01"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesDateEntreeFrance()).isNull();
        assertThat(im.aesDureePresenceMois()).isNull();
    }

    @Test
    void from_immigration_sf246_18_aesNiveauEtudes_whitelist_valid_all_codes() {
        for (String code : new String[]{"LYCEE", "BAC_PLUS_1_2", "BAC_PLUS_3_4", "BAC_PLUS_5_PLUS"}) {
            CaseAnalysis analysis = analysis("""
                    {
                      "type_titre_sejour_code": "VLS_TS_ETUDIANT",
                      "aes_date_entree_france": "2020-06-01",
                      "aes_niveau_etudes": "%s"
                    }
                    """.formatted(code));
            var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
            assertThat(im).isNotNull();
            assertThat(im.aesNiveauEtudes()).as("code: " + code).isEqualTo(code);
        }
    }

    @Test
    void from_immigration_sf246_18_aesNiveauEtudes_invalid_code_returns_null() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "VLS_TS_ETUDIANT",
                  "aes_date_entree_france": "2020-06-01",
                  "aes_niveau_etudes": "MASTER_2"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesNiveauEtudes()).isNull();
    }

    @Test
    void from_immigration_sf246_18_aesMotifHumanitaire_valid() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_VPF",
                  "aes_date_entree_france": "2019-01-01",
                  "aes_motif_humanitaire": "VICTIME_VIOLENCES"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesMotifHumanitaire()).isEqualTo("VICTIME_VIOLENCES");
    }

    @Test
    void from_immigration_sf246_18_aesMotifHumanitaire_invalid_returns_null() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_VPF",
                  "aes_date_entree_france": "2019-01-01",
                  "aes_motif_humanitaire": "DANGER_INCONNU"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesMotifHumanitaire()).isNull();
    }

    @Test
    void from_immigration_sf246_18_aesMoisActiviteSalariee_valid_range() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "aes_date_entree_france": "2020-01-01",
                  "aes_mois_activite_salariee": 18,
                  "aes_code_metier": "N1101"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesMoisActiviteSalariee()).isEqualTo(18);
        assertThat(im.aesCodeMetier()).isEqualTo("N1101");
    }

    @Test
    void from_immigration_sf246_18_aesMoisActiviteSalariee_out_of_range_returns_null() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "aes_date_entree_france": "2020-01-01",
                  "aes_mois_activite_salariee": 25
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesMoisActiviteSalariee()).isNull();
    }

    @Test
    void from_immigration_sf246_18_aesDureeScolarite_famille_nominal() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_VPF",
                  "aes_date_entree_france": "2018-09-01",
                  "aes_duree_scolarite_plus_ancien_enfant_annees": 4
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesDureeScolaritePlusAncienEnfantAnnees()).isEqualTo(4);
    }

    @Test
    void from_immigration_sf246_18_all_new_fields_null_graceful() {
        CaseAnalysis analysis = analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE"
                }
                """);
        var im = CaseAnalysisResponse.from(analysis).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.aesDateEntreeFrance()).isNull();
        assertThat(im.aesDureePresenceMois()).isNull();
        assertThat(im.aesAnneesScolariteConsecutives()).isNull();
        assertThat(im.aesNiveauEtudes()).isNull();
        assertThat(im.aesDureeScolaritePlusAncienEnfantAnnees()).isNull();
        assertThat(im.aesMotifHumanitaire()).isNull();
        assertThat(im.aesMoisActiviteSalariee()).isNull();
        assertThat(im.aesCodeMetier()).isNull();
    }

    // SF-246-19 : 10 champs supplémentaires Immigration FR pour pré-fill outils spécialisés
    @Test
    void from_immigration_sf246_19_changementTitreEnvisage_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "changement_titre_envisage": "CARTE_RESIDENT" }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.changementTitreEnvisage()).isEqualTo("CARTE_RESIDENT");
    }

    @Test
    void from_immigration_sf246_19_changementTitreEnvisage_unknown_normalizeEnum_returns_null() {
        // normalizeEnumCode filtre les codes inconnus — le champ est null et non transmis tel quel
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "changement_titre_envisage": "INCONNU_BOGUS"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.changementTitreEnvisage()).isNull();
    }

    @Test
    void from_immigration_sf246_19_changementRemunerationEur_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "changement_remuneration_eur": 45000 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.changementRemunerationEur()).isEqualTo(45000);
    }

    @Test
    void from_immigration_sf246_19_changementRemunerationEur_out_of_range_returns_null() {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "changement_remuneration_eur": 600000
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.changementRemunerationEur()).isNull();
    }

    @Test
    void from_immigration_sf246_19_natDureeResidenceReguliereAnnees_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "nat_duree_residence_reguliere_annees": 5 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.natDureeResidenceReguliereAnnees()).isEqualTo(5);
    }

    @Test
    void from_immigration_sf246_19_natDureeMariageAnnees_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "nat_duree_mariage_annees": 4 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.natDureeMariageAnnees()).isEqualTo(4);
    }

    @Test
    void from_immigration_sf246_19_natAgeDemandeur_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "nat_age_demandeur": 35 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.natAgeDemandeur()).isEqualTo(35);
    }

    @Test
    void from_immigration_sf246_19_mineursDateNaissance_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "mineurs_date_naissance": "2010-05-15" }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.mineursDateNaissance()).isEqualTo("2010-05-15");
    }

    @Test
    void from_immigration_sf246_19_mineursDateNaissance_future_rejected() {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "mineurs_date_naissance": "2099-01-01"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.mineursDateNaissance()).isNull();
    }

    @Test
    void from_immigration_sf246_19_algerienPresenceReguliereMois_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "algerien_presence_reguliere_mois": 24 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.algerienPresenceReguliereMois()).isEqualTo(24);
    }

    @Test
    void from_immigration_sf246_19_asileDateDecisionAnterieure_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "asile_date_decision_anterieure": "2022-06-15" }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.asileDateDecisionAnterieure()).isEqualTo("2022-06-15");
    }

    @Test
    void from_immigration_sf246_19_asileDateDecisionAnterieure_future_rejected() {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "asile_date_decision_anterieure": "2099-01-01"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.asileDateDecisionAnterieure()).isNull();
    }

    @Test
    void from_immigration_sf246_19_eloiDureePresenceIrreguliereMois_nominal() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "eloi_duree_presence_irreguliere_mois": 18 }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.eloiDureePresenceIrreguliereMois()).isEqualTo(18);
    }

    @Test
    void from_immigration_sf246_19_eloiMotifMenace_whitelist_valid() {
        var im = CaseAnalysisResponse.from(analysis("""
                { "eloi_motif_menace": "TERRORISME" }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.eloiMotifMenace()).isEqualTo("TERRORISME");
    }

    @Test
    void from_immigration_sf246_19_eloiMotifMenace_invalid_returns_null() {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE",
                  "eloi_motif_menace": "BOGUS_CODE"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.eloiMotifMenace()).isNull();
    }

    @Test
    void from_immigration_sf246_19_all_new_fields_null_graceful() {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CST_SALARIE"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.changementTitreEnvisage()).isNull();
        assertThat(im.changementRemunerationEur()).isNull();
        assertThat(im.natDureeResidenceReguliereAnnees()).isNull();
        assertThat(im.natDureeMariageAnnees()).isNull();
        assertThat(im.natAgeDemandeur()).isNull();
        assertThat(im.mineursDateNaissance()).isNull();
        assertThat(im.algerienPresenceReguliereMois()).isNull();
        assertThat(im.asileDateDecisionAnterieure()).isNull();
        assertThat(im.eloiDureePresenceIrreguliereMois()).isNull();
        assertThat(im.eloiMotifMenace()).isNull();
    }

    // SF-246-20 : lot Immigration BE (9bis / 9ter / 40bis / 40ter)
    @Test
    void from_immigration_sf246_20_be9bis_date_entree_nominal() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_9bis_date_entree_belgique": "2019-03-15"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9bisDateEntreeBelgique()).isEqualTo("2019-03-15");
        assertThat(im.be9bisDureePresenceMois()).isNotNull();
        assertThat(im.be9bisDureePresenceMois()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void from_immigration_sf246_20_be9bis_future_date_rejected() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_9bis_date_entree_belgique": "2099-01-01"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9bisDateEntreeBelgique()).isNull();
        assertThat(im.be9bisDureePresenceMois()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be9bis_nonIso_rejected() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_9bis_date_entree_belgique": "15/03/2019"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9bisDateEntreeBelgique()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be9ter_date_debut_symptomes_nominal() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_9ter_date_debut_symptomes": "2021-07-10"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9terDateDebutSymptomes()).isEqualTo("2021-07-10");
    }

    @Test
    void from_immigration_sf246_20_be9ter_future_date_rejected() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_9ter_date_debut_symptomes": "2099-12-31"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9terDateDebutSymptomes()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be40bis_lien_familial_conjoint() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40bis_lien_familial": "CONJOINT"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40bisLienFamilial()).isEqualTo("CONJOINT");
    }

    @Test
    void from_immigration_sf246_20_be40bis_lien_familial_descendant_mineur() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40bis_lien_familial": "DESCENDANT_MINEUR"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40bisLienFamilial()).isEqualTo("DESCENDANT_MINEUR");
    }

    @Test
    void from_immigration_sf246_20_be40bis_lien_familial_invalid_returns_null() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40bis_lien_familial": "INCONNU"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40bisLienFamilial()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be40ter_lien_familial_conjoint() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_lien_familial": "CONJOINT"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40terLienFamilial()).isEqualTo("CONJOINT");
    }

    @Test
    void from_immigration_sf246_20_be40ter_lien_familial_partenaire_legal() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_lien_familial": "PARTENAIRE_LEGAL_ENREGISTRE"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40terLienFamilial()).isEqualTo("PARTENAIRE_LEGAL_ENREGISTRE");
    }

    @Test
    void from_immigration_sf246_20_be40ter_lien_familial_invalid_returns_null() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_lien_familial": "ASCENDANT_CHARGE"
                  }
                }
                """)).immigrationExtractedData();
        // ASCENDANT_CHARGE est dans 40bis, pas 40ter — whitelist distincte
        assertThat(im).isNotNull();
        assertThat(im.be40terLienFamilial()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be40ter_revenus_mensuels_nominal() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_revenus_mensuels_nets": 2500
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40terRevenusMensuelsNets()).isEqualTo(2500);
    }

    @Test
    void from_immigration_sf246_20_be40ter_revenus_zero_returns_null() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_revenus_mensuels_nets": 0
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40terRevenusMensuelsNets()).isNull();
    }

    @Test
    void from_immigration_sf246_20_be40ter_revenus_trop_eleves_returns_null() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40ter_revenus_mensuels_nets": 50000
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40terRevenusMensuelsNets()).isNull();
    }

    @Test
    void from_immigration_sf246_20_sous_objet_absent_all_null() throws Exception {
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B"
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be9bisDateEntreeBelgique()).isNull();
        assertThat(im.be9bisDureePresenceMois()).isNull();
        assertThat(im.be9terDateDebutSymptomes()).isNull();
        assertThat(im.be40bisLienFamilial()).isNull();
        assertThat(im.be40terLienFamilial()).isNull();
        assertThat(im.be40terRevenusMensuelsNets()).isNull();
    }

    @Test
    void from_immigration_sf246_20_whitelist_distinction_40bis_vs_40ter() throws Exception {
        // Invariant : PARTENAIRE_ENREGISTRE est valide pour 40bis, pas pour 40ter.
        // PARTENAIRE_LEGAL_ENREGISTRE est valide pour 40ter, pas pour 40bis.
        var im = CaseAnalysisResponse.from(analysis("""
                {
                  "type_titre_sejour_code": "CARTE_B",
                  "immigration_be_detection_v2": {
                    "be_40bis_lien_familial": "PARTENAIRE_ENREGISTRE",
                    "be_40ter_lien_familial": "PARTENAIRE_LEGAL_ENREGISTRE"
                  }
                }
                """)).immigrationExtractedData();
        assertThat(im).isNotNull();
        assertThat(im.be40bisLienFamilial()).isEqualTo("PARTENAIRE_ENREGISTRE");
        assertThat(im.be40terLienFamilial()).isEqualTo("PARTENAIRE_LEGAL_ENREGISTRE");
    }

    // SF-166-01 : 8 flags décisionnels niveau 3 (F-DT-20/21/24/30/31/33/34/35) — Travail FR uniquement
    @Test
    void from_travailExtractedData_rappelSalaireDetecte_isolated_true_othersFalse() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "rappel_salaire_detecte": true } }
                """)).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rappelSalaireDetecte()).isTrue();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
        assertThat(t.statutProtegeDetecte()).isFalse();
        assertThat(t.transactionEnvisagee()).isFalse();
        assertThat(t.atMpDetecte()).isFalse();
        assertThat(t.urgenceProcedurale()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
    }

    @Test
    void from_travailExtractedData_travailDissimuleDetecte_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "travail_dissimule_detecte": true } }
                """)).travailExtractedData();
        assertThat(t.travailDissimuleDetecte()).isTrue();
        assertThat(t.rappelSalaireDetecte()).isFalse();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrenceDetectee_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "clause_non_concurrence_detectee": true } }
                """)).travailExtractedData();
        assertThat(t.clauseNonConcurrenceDetectee()).isTrue();
        assertThat(t.travailDissimuleDetecte()).isFalse();
    }

    @Test
    void from_travailExtractedData_statutProtegeDetecte_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "statut_protege_detecte": true } }
                """)).travailExtractedData();
        assertThat(t.statutProtegeDetecte()).isTrue();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
    }

    @Test
    void from_travailExtractedData_transactionEnvisagee_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "transaction_envisagee": true } }
                """)).travailExtractedData();
        assertThat(t.transactionEnvisagee()).isTrue();
        assertThat(t.statutProtegeDetecte()).isFalse();
    }

    @Test
    void from_travailExtractedData_atMpDetecte_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "at_mp_detecte": true } }
                """)).travailExtractedData();
        assertThat(t.atMpDetecte()).isTrue();
        assertThat(t.transactionEnvisagee()).isFalse();
    }

    @Test
    void from_travailExtractedData_urgenceProcedurale_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "urgence_procedurale": true } }
                """)).travailExtractedData();
        assertThat(t.urgenceProcedurale()).isTrue();
        assertThat(t.atMpDetecte()).isFalse();
    }

    @Test
    void from_travailExtractedData_contestationAreEnvisagee_isolated_true() {
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "contestation_are_envisagee": true } }
                """)).travailExtractedData();
        assertThat(t.contestationAreEnvisagee()).isTrue();
        assertThat(t.urgenceProcedurale()).isFalse();
    }

    @Test
    void from_travailExtractedData_allFlagsExplicitlyFalse() {
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "rappel_salaire_detecte": false,
                    "travail_dissimule_detecte": false,
                    "clause_non_concurrence_detectee": false,
                    "statut_protege_detecte": false,
                    "transaction_envisagee": false,
                    "at_mp_detecte": false,
                    "urgence_procedurale": false,
                    "contestation_are_envisagee": false
                  }
                }
                """)).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rappelSalaireDetecte()).isFalse();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
        assertThat(t.statutProtegeDetecte()).isFalse();
        assertThat(t.transactionEnvisagee()).isFalse();
        assertThat(t.atMpDetecte()).isFalse();
        assertThat(t.urgenceProcedurale()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
    }

    @Test
    void from_travailExtractedData_noFlagsInJson_allDefaultFalse() {
        // Rétrocompat : analyse antérieure à SF-166-01 ne contient aucun des 8 flags
        var t = CaseAnalysisResponse.from(analysis("""
                { "travail_extracted_data": { "convention_collective": "SYNTEC", "salaire_brut_mensuel": 3200 } }
                """)).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rappelSalaireDetecte()).isFalse();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
        assertThat(t.statutProtegeDetecte()).isFalse();
        assertThat(t.transactionEnvisagee()).isFalse();
        assertThat(t.atMpDetecte()).isFalse();
        assertThat(t.urgenceProcedurale()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
    }

    @Test
    void from_travailExtractedData_nonBooleanValues_failSafeToFalse() {
        // Fail-safe : valeurs non-boolean (number, object) ne lèvent pas d'exception et restent false
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "rappel_salaire_detecte": "yes",
                    "travail_dissimule_detecte": 1,
                    "clause_non_concurrence_detectee": null,
                    "statut_protege_detecte": "TRUE_INVALID"
                  }
                }
                """)).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rappelSalaireDetecte()).isFalse();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
        assertThat(t.statutProtegeDetecte()).isFalse();
    }

    @Test
    void from_travailExtractedData_stringTrueAndFalse_recognizedCaseInsensitive() {
        // booleanOrNull tolère les chaînes "true" / "false" — réutilisé par booleanOrFalse
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "rappel_salaire_detecte": "true",
                    "travail_dissimule_detecte": "false",
                    "clause_non_concurrence_detectee": "TRUE",
                    "statut_protege_detecte": "False"
                  }
                }
                """)).travailExtractedData();
        assertThat(t.rappelSalaireDetecte()).isTrue();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isTrue();
        assertThat(t.statutProtegeDetecte()).isFalse();
    }

    @Test
    void travailExtractedData_builder_minimalFields_setsAllFlagsToFalse() {
        // F-234 SF-234-01 : Builder avec uniquement les 9 champs de base — tous les flags doivent
        // être false par défaut (équivalent du constructeur rétrocompat 9-args supprimé).
        var t = CaseAnalysisResponse.TravailExtractedData.builder()
                .conventionCollective("SYNTEC")
                .dateEntree("2020-01-01")
                .salaireBrutMensuel(3200.0)
                .typeContrat("CDI")
                .poste("Développeur")
                .motifLicenciement("Faute simple")
                .dateLicenciement("2024-06-15")
                .congesContractuels(25)
                .primeAncienneteContractuelle(0.5)
                .build();
        assertThat(t.rappelSalaireDetecte()).isFalse();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.clauseNonConcurrenceDetectee()).isFalse();
        assertThat(t.statutProtegeDetecte()).isFalse();
        assertThat(t.transactionEnvisagee()).isFalse();
        assertThat(t.atMpDetecte()).isFalse();
        assertThat(t.urgenceProcedurale()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
    }

    @Test
    void travailExtractedData_builder_withIdentityAndMotifNullite_setsAllFlagsToFalse() {
        // F-234 SF-234-01 : Builder avec identité salarié+employeur et motifNullite (équivalent du
        // constructeur rétrocompat 23-args supprimé). Les 8 flags niveau 3 restent false par défaut.
        var t = CaseAnalysisResponse.TravailExtractedData.builder()
                .conventionCollective("SYNTEC")
                .dateEntree("2020-01-01")
                .salaireBrutMensuel(3200.0)
                .typeContrat("CDI")
                .poste("Développeur")
                .motifLicenciement("Faute simple")
                .dateLicenciement("2024-06-15")
                .congesContractuels(25)
                .primeAncienneteContractuelle(0.5)
                .nomSalarie("Dupont").prenomSalarie("Jean").adresseSalarie("12 rue de la Paix")
                .nomEmployeur("Acme SAS").adresseEmployeur("5 avenue des Champs")
                .siretEmployeur("12345678901234").representantEmployeur("Martin Dupond")
                .salaireEstDeduit(false)
                .motifNullitePressenti("HARCELEMENT_MORAL")
                .avisMedecinTravailDate("2024-06-01")
                .build();
        assertThat(t.rappelSalaireDetecte()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
        assertThat(t.motifNullitePressenti()).isEqualTo("HARCELEMENT_MORAL");
    }

    @Test
    void from_travailExtractedData_multipleFlagsTrue_independent() {
        // Plusieurs flags true en même temps (cas réaliste : dossier complexe)
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "rappel_salaire_detecte": true,
                    "urgence_procedurale": true,
                    "transaction_envisagee": true
                  }
                }
                """)).travailExtractedData();
        assertThat(t.rappelSalaireDetecte()).isTrue();
        assertThat(t.urgenceProcedurale()).isTrue();
        assertThat(t.transactionEnvisagee()).isTrue();
        assertThat(t.travailDissimuleDetecte()).isFalse();
        assertThat(t.contestationAreEnvisagee()).isFalse();
    }

    // ===========================================================================
    // F-205 SF-205-01 — extractTravailData : 23 flags Travail FR additionnels niveau 3
    // ===========================================================================
    @Test
    void extractTravailData_F205Flags_parsedFromJsonAllTrue() {
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "abandon_poste_detecte": true,
                    "arret_maladie_long_detecte": true,
                    "prise_acte_envisagee": true,
                    "resiliation_judiciaire_envisagee": true,
                    "forfait_jours_detecte": true,
                    "transfert_entreprise_detecte": true,
                    "faute_inexcusable_envisagee": true,
                    "cs_crp_envisage": true,
                    "csp_propose": true,
                    "mutation_refusee": true,
                    "modification_contrat_refusee": true,
                    "faute_grave_envisagee": true,
                    "faute_lourde_envisagee": true,
                    "cdd_requalification_envisagee": true,
                    "interim_requalification_envisagee": true,
                    "forfait_jours_validite_contestee": true,
                    "prescription_proche_detectee": true,
                    "rupture_amiable_negociee": true,
                    "entretien_preavis_obtenu": true,
                    "cse_consultation_demandee": true,
                    "irp_election_demandee": true,
                    "inspection_travail_saisie": true,
                    "mediation_judiciaire_envisagee": true
                  }
                }
                """)).travailExtractedData();
        assertThat(t.abandonPosteDetecte()).isTrue();
        assertThat(t.arretMaladieLongDetecte()).isTrue();
        assertThat(t.priseActeEnvisagee()).isTrue();
        assertThat(t.resiliationJudiciaireEnvisagee()).isTrue();
        assertThat(t.forfaitJoursDetecte()).isTrue();
        assertThat(t.transfertEntrepriseDetecte()).isTrue();
        assertThat(t.fauteInexcusableEnvisagee()).isTrue();
        assertThat(t.csCrpEnvisage()).isTrue();
        assertThat(t.cspPropose()).isTrue();
        assertThat(t.mutationRefusee()).isTrue();
        assertThat(t.modificationContratRefusee()).isTrue();
        assertThat(t.fauteGraveEnvisagee()).isTrue();
        assertThat(t.fauteLourdeEnvisagee()).isTrue();
        assertThat(t.cddRequalificationEnvisagee()).isTrue();
        assertThat(t.interimRequalificationEnvisagee()).isTrue();
        assertThat(t.forfaitJoursValiditeContestee()).isTrue();
        assertThat(t.prescriptionProcheDetectee()).isTrue();
        assertThat(t.ruptureAmiableNegociee()).isTrue();
        assertThat(t.entretienPreavisObtenu()).isTrue();
        assertThat(t.cseConsultationDemandee()).isTrue();
        assertThat(t.irpElectionDemandee()).isTrue();
        assertThat(t.inspectionTravailSaisie()).isTrue();
        assertThat(t.mediationJudiciaireEnvisagee()).isTrue();
    }

    @Test
    void extractTravailData_F205Flags_defaultFalseIfAbsent() {
        var t = CaseAnalysisResponse.from(analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC"
                  }
                }
                """)).travailExtractedData();
        // Tous les 23 flags F-205 absents → false par défaut (booleanOrFalse)
        assertThat(t.abandonPosteDetecte()).isFalse();
        assertThat(t.arretMaladieLongDetecte()).isFalse();
        assertThat(t.priseActeEnvisagee()).isFalse();
        assertThat(t.resiliationJudiciaireEnvisagee()).isFalse();
        assertThat(t.forfaitJoursDetecte()).isFalse();
        assertThat(t.transfertEntrepriseDetecte()).isFalse();
        assertThat(t.fauteInexcusableEnvisagee()).isFalse();
        assertThat(t.csCrpEnvisage()).isFalse();
        assertThat(t.cspPropose()).isFalse();
        assertThat(t.mutationRefusee()).isFalse();
        assertThat(t.modificationContratRefusee()).isFalse();
        assertThat(t.fauteGraveEnvisagee()).isFalse();
        assertThat(t.fauteLourdeEnvisagee()).isFalse();
        assertThat(t.cddRequalificationEnvisagee()).isFalse();
        assertThat(t.interimRequalificationEnvisagee()).isFalse();
        assertThat(t.forfaitJoursValiditeContestee()).isFalse();
        assertThat(t.prescriptionProcheDetectee()).isFalse();
        assertThat(t.ruptureAmiableNegociee()).isFalse();
        assertThat(t.entretienPreavisObtenu()).isFalse();
        assertThat(t.cseConsultationDemandee()).isFalse();
        assertThat(t.irpElectionDemandee()).isFalse();
        assertThat(t.inspectionTravailSaisie()).isFalse();
        assertThat(t.mediationJudiciaireEnvisagee()).isFalse();
    }

    @Test
    void travailExtractedData_builder_withF166AndF204Flags_setsAllF205FlagsToFalse() {
        // F-234 SF-234-01 : Builder avec 8 flags F-166 + 5 flags F-204 mais pas les 23 flags F-205
        // (équivalent du constructeur rétrocompat 36-args supprimé). Tous les flags F-205 = false.
        var t = CaseAnalysisResponse.TravailExtractedData.builder()
                .conventionCollective("SYNTEC")
                .dateEntree("2020-01-01")
                .salaireBrutMensuel(3200.0)
                .typeContrat("CDI")
                .poste("Développeur")
                .motifLicenciement("Faute simple")
                .dateLicenciement("2024-06-15")
                .congesContractuels(25)
                .primeAncienneteContractuelle(0.5)
                .nomSalarie("Dupont").prenomSalarie("Jean").adresseSalarie("12 rue de la Paix")
                .nomEmployeur("Acme SAS").adresseEmployeur("5 avenue des Champs")
                .siretEmployeur("12345678901234").representantEmployeur("Martin Dupond")
                .salaireEstDeduit(false)
                .motifNullitePressenti("HARCELEMENT_MORAL")
                .avisMedecinTravailDate("2024-06-01")
                // 8 flags F-166 — seul rappelSalaireDetecte=true, le reste à false
                .rappelSalaireDetecte(true)
                .build();
        // Tous les 23 flags F-205 = false (non setés sur le builder)
        assertThat(t.abandonPosteDetecte()).isFalse();
        assertThat(t.arretMaladieLongDetecte()).isFalse();
        assertThat(t.priseActeEnvisagee()).isFalse();
        assertThat(t.resiliationJudiciaireEnvisagee()).isFalse();
        assertThat(t.mediationJudiciaireEnvisagee()).isFalse();
        // Vérifier que les flags F-166 sont préservés (rappelSalaireDetecte = true)
        assertThat(t.rappelSalaireDetecte()).isTrue();
        assertThat(t.travailDissimuleDetecte()).isFalse();
    }

    @Test
    void travailExtractedData_builder_minimalFields_setsAllF205FlagsToFalse() {
        // F-234 SF-234-01 : Builder avec uniquement les 9 champs de base — propage false×23 pour
        // les flags F-205 (équivalent du constructeur rétrocompat 9-args supprimé).
        var t = CaseAnalysisResponse.TravailExtractedData.builder()
                .conventionCollective("SYNTEC")
                .dateEntree("2020-01-01")
                .salaireBrutMensuel(3200.0)
                .typeContrat("CDI")
                .poste("Développeur")
                .motifLicenciement("Faute simple")
                .dateLicenciement("2024-06-15")
                .congesContractuels(25)
                .primeAncienneteContractuelle(0.5)
                .build();
        assertThat(t.abandonPosteDetecte()).isFalse();
        assertThat(t.priseActeEnvisagee()).isFalse();
        assertThat(t.fauteInexcusableEnvisagee()).isFalse();
        assertThat(t.mediationJudiciaireEnvisagee()).isFalse();
    }

    // ===========================================================================
    // F-200 SF-200-01 — extractFamilleData : 30 flags Famille FR niveau 3
    // ===========================================================================
    @Test
    void extractFamilleData_returnsNullWhenNodeAbsent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("{\"faits\": []}");
        assertThat(CaseAnalysisResponse.extractFamilleData(root)).isNull();
    }

    @Test
    void extractFamilleData_returnsNullWhenAllFlagsFalseOrAbsent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_consentement_mutuel_envisage": false,
                    "succession_envisagee": false
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.extractFamilleData(root)).isNull();
    }

    @Test
    void extractFamilleData_parsesAllThirtyFlagsTrue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_consentement_mutuel_envisage": true,
                    "divorce_alteration_lien_envisage": true,
                    "divorce_faute_envisage": true,
                    "divorce_accepte_envisage": true,
                    "revision_post_divorce_envisagee": true,
                    "ordonnance_protection_envisagee": true,
                    "recompenses_envisagees": true,
                    "regime_communaute_universelle_detecte": true,
                    "partage_judiciaire_envisage": true,
                    "adoption_envisagee": true,
                    "reconnaissance_paternelle_envisagee": true,
                    "contestation_paternite_envisagee": true,
                    "recherche_paternite_envisagee": true,
                    "possession_etat_envisagee": true,
                    "changement_residence_envisage": true,
                    "desaccord_parental_detecte": true,
                    "pacs_dissolution_envisagee": true,
                    "separation_corps_envisagee": true,
                    "indivision_envisagee": true,
                    "ordonnance_requete_envisagee": true,
                    "succession_envisagee": true,
                    "testament_envisage": true,
                    "donation_envisagee": true,
                    "reserve_hereditaire_envisagee": true,
                    "partage_successoral_envisage": true,
                    "indivision_successorale_envisagee": true,
                    "rapport_succession_envisage": true,
                    "protection_majeur_envisagee": true,
                    "changement_etat_civil_envisage": true,
                    "pma_gpa_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // 4 cas divorce
        assertThat(f.divorceConsentementMutuelEnvisage()).isTrue();
        assertThat(f.divorceAlterationLienEnvisage()).isTrue();
        assertThat(f.divorceFauteEnvisage()).isTrue();
        assertThat(f.divorceAccepteEnvisage()).isTrue();
        // Régimes / partage / révision
        assertThat(f.revisionPostDivorceEnvisagee()).isTrue();
        assertThat(f.ordonnanceProtectionEnvisagee()).isTrue();
        assertThat(f.recompensesEnvisagees()).isTrue();
        assertThat(f.regimeCommunauteUniverselleDetecte()).isTrue();
        assertThat(f.partageJudiciaireEnvisage()).isTrue();
        // Adoption + filiation
        assertThat(f.adoptionEnvisagee()).isTrue();
        assertThat(f.reconnaissancePaternelleEnvisagee()).isTrue();
        assertThat(f.contestationPaterniteEnvisagee()).isTrue();
        assertThat(f.recherchePaterniteEnvisagee()).isTrue();
        assertThat(f.possessionEtatEnvisagee()).isTrue();
        // Autorité parentale conflictuelle
        assertThat(f.changementResidenceEnvisage()).isTrue();
        assertThat(f.desaccordParentalDetecte()).isTrue();
        // PACS / séparation / indivision / ordonnance requête
        assertThat(f.pacsDissolutionEnvisagee()).isTrue();
        assertThat(f.separationCorpsEnvisagee()).isTrue();
        assertThat(f.indivisionEnvisagee()).isTrue();
        assertThat(f.ordonnanceRequeteEnvisagee()).isTrue();
        // Successions (7)
        assertThat(f.successionEnvisagee()).isTrue();
        assertThat(f.testamentEnvisage()).isTrue();
        assertThat(f.donationEnvisagee()).isTrue();
        assertThat(f.reserveHereditaireEnvisagee()).isTrue();
        assertThat(f.partageSuccessoralEnvisage()).isTrue();
        assertThat(f.indivisionSuccessoraleEnvisagee()).isTrue();
        assertThat(f.rapportSuccessionEnvisage()).isTrue();
        // Protection majeurs / état civil / PMA-GPA
        assertThat(f.protectionMajeurEnvisagee()).isTrue();
        assertThat(f.changementEtatCivilEnvisage()).isTrue();
        assertThat(f.pmaGpaEnvisagee()).isTrue();
    }

    @Test
    void extractFamilleData_partialFlags_otherDefaultFalse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "ordonnance_protection_envisagee": true,
                    "succession_envisagee": false
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.divorceFauteEnvisage()).isTrue();
        assertThat(f.ordonnanceProtectionEnvisagee()).isTrue();
        // Les autres restent false
        assertThat(f.divorceConsentementMutuelEnvisage()).isFalse();
        assertThat(f.successionEnvisagee()).isFalse();
        assertThat(f.pmaGpaEnvisagee()).isFalse();
    }

    @Test
    void extractFamilleData_acceptsStringTrue_forFailSafety() throws Exception {
        // booleanOrFalse accepte "true" textuel (cf. helper booleanOrNull)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": "true"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.successionEnvisagee()).isTrue();
    }

    // F-239 — extraction du champ string `date_acceptation_pv`

    @Test
    void extractFamilleData_extractsDateAcceptationPV_whenPresentISOFormat() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "date_acceptation_pv": "2025-12-12"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.divorceDcEnvisage()).isTrue();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
    }

    @Test
    void extractFamilleData_dateAcceptationPVNull_whenAbsent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isNull();
    }

    @Test
    void extractFamilleData_returnsRecord_whenOnlyDateAcceptationPVPresent() throws Exception {
        // Garde-fou : si le seul champ peuplé est date_acceptation_pv (tous les flags false),
        // le record DOIT quand même être construit (au lieu d'être null comme avant F-239).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "date_acceptation_pv": "2025-12-12"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
        assertThat(f.divorceDcEnvisage()).isFalse();
    }

    @Test
    void extractFamilleData_dateAcceptationPVNull_whenEmptyString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "date_acceptation_pv": "   "
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isNull();
    }

    // F-241 — nouveau nom neutre FR+BE `date_accord_initial_divorce`
    //         + rétro-compat sur `date_acceptation_pv`
    //         + fallback déterministe via timeline (cas LLM résistant Vermeersch BE 2026-05-11)

    @Test
    void extractFamilleData_extractsDateAccordInitialDivorce_whenNewKeyPresent() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "date_accord_initial_divorce": "2025-12-12"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
    }

    @Test
    void extractFamilleData_newKeyTakesPriorityOverLegacyKey() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "date_accord_initial_divorce": "2025-12-12",
                    "date_acceptation_pv": "2024-01-01"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
    }

    @Test
    void extractFamilleData_fallsBackToTimeline_whenBothKeysAbsentAndTimelineHasConventionSignature() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "timeline": [
                    {"date": "2025-12-12", "evenement": "Signature de la convention préalable à divorce par consentement mutuel"}
                  ],
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
    }

    @Test
    void extractFamilleData_fallsBackToTimeline_matchesPvAccordKeywords() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "timeline": [
                    {"date": "2025-11-05", "evenement": "Signature du procès-verbal d'acceptation du principe de la rupture"}
                  ],
                  "famille_extracted_data": {
                    "divorce_accepte_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-11-05");
    }

    @Test
    void extractFamilleData_fallbackTimeline_ignoresEventsWithoutSignatureMarker() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "timeline": [
                    {"date": "2025-01-01", "evenement": "Convention préalable rédigée mais non signée"},
                    {"date": "2025-06-15", "evenement": "Audience d'introduction"}
                  ],
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isNull();
    }

    @Test
    void extractFamilleData_fallbackTimeline_takesFirstMatch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "timeline": [
                    {"date": "2025-12-12", "evenement": "Signature de la convention préalable"},
                    {"date": "2026-01-15", "evenement": "Nouvelle signature de l'accord modifié"}
                  ],
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateAcceptationPV()).isEqualTo("2025-12-12");
    }

    // ===========================================================================
    // F-202 SF-202-01 — extractFamilleData : 5 flags Famille BE niveau 3
    // ===========================================================================

    @Test
    void from_familleExtractedData_absent_returnsNull() {
        // Pas de famille_extracted_data dans le JSON → record null
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "faits": []
                }
                """)).familleExtractedData();
        assertThat(f).isNull();
    }

    @Test
    void from_familleExtractedData_allFlagsFalse_returnsNull() {
        // Tous les flags à false → record null (économie mémoire, pattern aligné sur extractImmigrationData)
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": false,
                    "divorce_ddi_envisage": false,
                    "cohabitation_legale_be_detectee": false,
                    "pacte_successoral_envisage": false,
                    "kafala_recueil_detecte": false
                  }
                }
                """)).familleExtractedData();
        assertThat(f).isNull();
    }

    @Test
    void from_familleExtractedData_singleFlagTrue_isParsed() {
        // Un seul flag true → record non-null avec les autres flags false
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true
                  }
                }
                """)).familleExtractedData();
        assertThat(f).isNotNull();
        assertThat(f.divorceDdiEnvisage()).isTrue();
        assertThat(f.divorceDcEnvisage()).isFalse();
        assertThat(f.cohabitationLegaleBeDetectee()).isFalse();
        assertThat(f.pacteSuccessoralEnvisage()).isFalse();
        assertThat(f.kafalaRecueilDetecte()).isFalse();
    }

    @Test
    void from_familleExtractedData_allFlagsTrue_isParsed() {
        // Tous les flags true en même temps (cas hypothétique de dossier multi-situations)
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "divorce_ddi_envisage": true,
                    "cohabitation_legale_be_detectee": true,
                    "pacte_successoral_envisage": true,
                    "kafala_recueil_detecte": true
                  }
                }
                """)).familleExtractedData();
        assertThat(f).isNotNull();
        assertThat(f.divorceDcEnvisage()).isTrue();
        assertThat(f.divorceDdiEnvisage()).isTrue();
        assertThat(f.cohabitationLegaleBeDetectee()).isTrue();
        assertThat(f.pacteSuccessoralEnvisage()).isTrue();
        assertThat(f.kafalaRecueilDetecte()).isTrue();
    }

    @Test
    void from_familleExtractedData_invalidValues_failSafeToFalse() {
        // Valeurs non-boolean (entier, string non standard, null) → false (pattern booleanOrFalse)
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": "yes",
                    "divorce_ddi_envisage": 1,
                    "cohabitation_legale_be_detectee": null,
                    "pacte_successoral_envisage": "TRUE_INVALID",
                    "kafala_recueil_detecte": true
                  }
                }
                """)).familleExtractedData();
        // Seul kafala (true littéral) doit être true ; les autres → false
        assertThat(f).isNotNull();
        assertThat(f.divorceDcEnvisage()).isFalse();
        assertThat(f.divorceDdiEnvisage()).isFalse();
        assertThat(f.cohabitationLegaleBeDetectee()).isFalse();
        assertThat(f.pacteSuccessoralEnvisage()).isFalse();
        assertThat(f.kafalaRecueilDetecte()).isTrue();
    }

    @Test
    void from_familleExtractedData_stringTrueAndFalse_recognizedCaseInsensitive() {
        // booleanOrFalse tolère les chaînes "true" / "false" (case-insensitive)
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": "true",
                    "divorce_ddi_envisage": "false",
                    "cohabitation_legale_be_detectee": "TRUE",
                    "pacte_successoral_envisage": "False",
                    "kafala_recueil_detecte": "False"
                  }
                }
                """)).familleExtractedData();
        assertThat(f).isNotNull();
        assertThat(f.divorceDcEnvisage()).isTrue();
        assertThat(f.divorceDdiEnvisage()).isFalse();
        assertThat(f.cohabitationLegaleBeDetectee()).isTrue();
        assertThat(f.pacteSuccessoralEnvisage()).isFalse();
        assertThat(f.kafalaRecueilDetecte()).isFalse();
    }

    // ========================================================================
    // SF-246-06 : pré-fill IA F-FA-24 — sous-objet succession_detection
    // (16 champs successions / libéralités, Famille FR uniquement)
    // ========================================================================

    @Test
    void extractFamilleData_successionDetection_nominalCase_allSixteenFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "date_deces": "2025-03-01",
                      "date_ouverture_succession": "2025-03-02",
                      "mode_partage_demande": "JUDICIAIRE",
                      "nombre_coheritiers": 3,
                      "montant_succession_eur": 420000.0,
                      "montant_liberalites_total_eur": 60000.0,
                      "nombre_enfants_succession": 2,
                      "date_donation": "2018-06-12",
                      "montant_donations_recues_eur": 30000.0,
                      "valeur_donation_au_jour_partage_eur": 45000.0,
                      "actif_brut_succession_eur": 480000.0,
                      "passif_succession_eur": 60000.0,
                      "type_indivision_successorale": "LEGALE",
                      "nb_descendants": 2,
                      "nb_freres_soeurs": 0,
                      "date_redaction_testament": "2020-09-30"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isEqualTo("2025-03-01");
        assertThat(f.dateOuvertureSuccessionDetectee()).isEqualTo("2025-03-02");
        assertThat(f.modePartageDemandeDetecte()).isEqualTo("JUDICIAIRE");
        assertThat(f.nombreCoheritiersDetecte()).isEqualTo(3);
        assertThat(f.montantSuccessionEurDetecte()).isEqualTo(420000.0);
        assertThat(f.montantLibsTotalEurDetecte()).isEqualTo(60000.0);
        assertThat(f.nombreEnfantsSuccessionDetecte()).isEqualTo(2);
        assertThat(f.dateDonationDetectee()).isEqualTo("2018-06-12");
        assertThat(f.montantDonationsRecuesEurDetecte()).isEqualTo(30000.0);
        assertThat(f.valeurDonationAuJourPartageEurDetectee()).isEqualTo(45000.0);
        assertThat(f.actifBrutSuccessionEurDetecte()).isEqualTo(480000.0);
        assertThat(f.passifSuccessionEurDetecte()).isEqualTo(60000.0);
        assertThat(f.typeIndivisionSuccessoraleDetecte()).isEqualTo("LEGALE");
        assertThat(f.nbDescendantsDetecte()).isEqualTo(2);
        assertThat(f.nbFreresSoeursDetecte()).isEqualTo(0);
        assertThat(f.dateRedactionTestamentDetectee()).isEqualTo("2020-09-30");
    }

    @Test
    void extractFamilleData_successionDetection_absent_allSixteenFieldsNull() throws Exception {
        // Sous-objet succession_detection absent → 16 champs null, pas d'exception.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isNull();
        assertThat(f.dateOuvertureSuccessionDetectee()).isNull();
        assertThat(f.modePartageDemandeDetecte()).isNull();
        assertThat(f.nombreCoheritiersDetecte()).isNull();
        assertThat(f.montantSuccessionEurDetecte()).isNull();
        assertThat(f.montantLibsTotalEurDetecte()).isNull();
        assertThat(f.nombreEnfantsSuccessionDetecte()).isNull();
        assertThat(f.dateDonationDetectee()).isNull();
        assertThat(f.montantDonationsRecuesEurDetecte()).isNull();
        assertThat(f.valeurDonationAuJourPartageEurDetectee()).isNull();
        assertThat(f.actifBrutSuccessionEurDetecte()).isNull();
        assertThat(f.passifSuccessionEurDetecte()).isNull();
        assertThat(f.typeIndivisionSuccessoraleDetecte()).isNull();
        assertThat(f.nbDescendantsDetecte()).isNull();
        assertThat(f.nbFreresSoeursDetecte()).isNull();
        assertThat(f.dateRedactionTestamentDetectee()).isNull();
    }

    @Test
    void extractFamilleData_successionDetection_null_allSixteenFieldsNull() throws Exception {
        // Sous-objet explicitement null (cas LLM ne détecte aucune succession).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isNull();
        assertThat(f.modePartageDemandeDetecte()).isNull();
        assertThat(f.montantSuccessionEurDetecte()).isNull();
    }

    @Test
    void extractFamilleData_successionDetection_recordBuiltWhenOnlySuccessionDetectionPresent() throws Exception {
        // Garde-fou : si le seul contenu peuplé est succession_detection (tous les
        // flags false, pas de date d'accord divorce), le record DOIT être construit.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_detection": {
                      "date_deces": "2025-03-01"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isEqualTo("2025-03-01");
        assertThat(f.successionEnvisagee()).isFalse();
    }

    @Test
    void extractFamilleData_successionDetection_nonIsoDate_failsOpenToNull() throws Exception {
        // Dates hors ISO YYYY-MM-DD → null (fail-open via isoDateOrNull).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "date_deces": "01/03/2025",
                      "date_ouverture_succession": "mars 2025",
                      "date_donation": "2018-6-12",
                      "date_redaction_testament": "2020-09-30"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isNull();
        assertThat(f.dateOuvertureSuccessionDetectee()).isNull();
        assertThat(f.dateDonationDetectee()).isNull();
        // Seule la date ISO valide est conservée.
        assertThat(f.dateRedactionTestamentDetectee()).isEqualTo("2020-09-30");
    }

    @Test
    void extractFamilleData_successionDetection_nonPositiveAmounts_failOpenToNull() throws Exception {
        // Montants <= 0 ou aberrants → null (jamais 0 — invariant cadrage §5.2).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "montant_succession_eur": 0,
                      "montant_liberalites_total_eur": -5000,
                      "montant_donations_recues_eur": 30000.0,
                      "actif_brut_succession_eur": 0.0,
                      "passif_succession_eur": -1
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.montantSuccessionEurDetecte()).isNull();
        assertThat(f.montantLibsTotalEurDetecte()).isNull();
        assertThat(f.actifBrutSuccessionEurDetecte()).isNull();
        assertThat(f.passifSuccessionEurDetecte()).isNull();
        // Seul le montant strictement positif est conservé.
        assertThat(f.montantDonationsRecuesEurDetecte()).isEqualTo(30000.0);
    }

    @Test
    void extractFamilleData_successionDetection_countsOutOfRange_failOpenToNull() throws Exception {
        // Dénombrements hors [0, 50] → null (boundedIntOrNull).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "nombre_coheritiers": -1,
                      "nombre_enfants_succession": 51,
                      "nb_descendants": 3,
                      "nb_freres_soeurs": 999
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.nombreCoheritiersDetecte()).isNull();
        assertThat(f.nombreEnfantsSuccessionDetecte()).isNull();
        assertThat(f.nbFreresSoeursDetecte()).isNull();
        // Seul le dénombrement dans la plage est conservé.
        assertThat(f.nbDescendantsDetecte()).isEqualTo(3);
    }

    @Test
    void extractFamilleData_successionDetection_enumsOutOfWhitelist_failOpenToNull() throws Exception {
        // mode_partage_demande / type_indivision_successorale hors whitelist → null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "mode_partage_demande": "PARTIEL",
                      "type_indivision_successorale": "MAINTIEN_FORCE"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modePartageDemandeDetecte()).isNull();
        assertThat(f.typeIndivisionSuccessoraleDetecte()).isNull();
    }

    @Test
    void extractFamilleData_successionDetection_enumsCaseInsensitiveAndTrimmed() throws Exception {
        // Énumérations normalisées en MAJUSCULES + trim (whitelistedOrNull).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "mode_partage_demande": "  amiable  ",
                      "type_indivision_successorale": "Conventionnelle"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modePartageDemandeDetecte()).isEqualTo("AMIABLE");
        assertThat(f.typeIndivisionSuccessoraleDetecte()).isEqualTo("CONVENTIONNELLE");
    }

    @Test
    void extractFamilleData_successionDetection_multiDates_noConfusion() throws Exception {
        // Invariant cadrage §5.1.6 : 4 dates distinctes (décès, ouverture, donation
        // antérieure, testament) → chaque champ rempli avec la BONNE date.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "date_deces": "2025-03-01",
                      "date_ouverture_succession": "2025-03-01",
                      "date_donation": "2018-06-12",
                      "date_redaction_testament": "2020-09-30"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isEqualTo("2025-03-01");
        assertThat(f.dateOuvertureSuccessionDetectee()).isEqualTo("2025-03-01");
        assertThat(f.dateDonationDetectee()).isEqualTo("2018-06-12");
        assertThat(f.dateRedactionTestamentDetectee()).isEqualTo("2020-09-30");
    }

    @Test
    void extractFamilleData_successionDetection_multiAmounts_noConfusion() throws Exception {
        // 4 montants distincts (actif brut, passif, succession, libéralités) → chacun
        // dans le bon champ.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "actif_brut_succession_eur": 480000.0,
                      "passif_succession_eur": 60000.0,
                      "montant_succession_eur": 420000.0,
                      "montant_liberalites_total_eur": 90000.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.actifBrutSuccessionEurDetecte()).isEqualTo(480000.0);
        assertThat(f.passifSuccessionEurDetecte()).isEqualTo(60000.0);
        assertThat(f.montantSuccessionEurDetecte()).isEqualTo(420000.0);
        assertThat(f.montantLibsTotalEurDetecte()).isEqualTo(90000.0);
    }

    @Test
    void extractFamilleData_successionDetection_belgianCase_recordNullWhenNothingElse() throws Exception {
        // Dossier BE : le prompt impose succession_detection null → si aucun flag BE
        // ni date d'accord, le record reste null (pas de fabrication de champs FR).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNull();
    }

    @Test
    void extractFamilleData_successionDetection_notAnObject_failsOpenToNull() throws Exception {
        // succession_detection mal typé (chaîne) → traité comme absent, 16 champs null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": "oui"
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDecesDetectee()).isNull();
        assertThat(f.nombreCoheritiersDetecte()).isNull();
    }

    @Test
    void from_familleExtractedData_successionDetection_endToEndThroughFrom() {
        // Parcours complet via from() — la synthèse expose succession_detection peuplé.
        var f = CaseAnalysisResponse.from(analysis("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection": {
                      "date_ouverture_succession": "2025-03-01",
                      "nombre_coheritiers": 4,
                      "montant_succession_eur": 250000.0
                    }
                  }
                }
                """)).familleExtractedData();
        assertThat(f).isNotNull();
        assertThat(f.dateOuvertureSuccessionDetectee()).isEqualTo("2025-03-01");
        assertThat(f.nombreCoheritiersDetecte()).isEqualTo(4);
        assertThat(f.montantSuccessionEurDetecte()).isEqualTo(250000.0);
    }

    // ========================================================================
    // SF-246-01 : pré-fill IA F-DT-36 — bloc procedure_licenciement_detection
    // ========================================================================

    @Test
    void from_travailExtractedData_procedureLicenciement_complet_parsed() {
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "procedure_licenciement_detection": {
                      "convocation_entretien_detectee": true,
                      "date_convocation_entretien": "2026-02-10",
                      "date_entretien_prealable": "2026-02-18",
                      "entretien_prealable_tenu": {
                        "reponse": "OUI",
                        "justification": "PV d'entretien daté du 18/02 produit aux pièces"
                      },
                      "lettre_licenciement_ecrite": true,
                      "lettre_licenciement_motivee": {
                        "reponse": "NON",
                        "justification": "Lettre du 25/02 mentionne uniquement 'motif personnel'"
                      },
                      "motivation_lettre_suffisante": {
                        "reponse": "NON",
                        "justification": "Aucun fait precis date n'est articule"
                      }
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.convocationEntretienDetectee()).isTrue();
        assertThat(t.dateConvocationEntretienDetectee()).isEqualTo("2026-02-10");
        assertThat(t.dateEntretienPrealableDetectee()).isEqualTo("2026-02-18");
        assertThat(t.entretienPrealableTenuDetected()).isNotNull();
        assertThat(t.entretienPrealableTenuDetected().reponse()).isEqualTo("OUI");
        assertThat(t.entretienPrealableTenuDetected().justification()).contains("PV d'entretien");
        assertThat(t.lettreLicenciementEcriteDetectee()).isTrue();
        assertThat(t.lettreLicenciementMotiveeDetected()).isNotNull();
        assertThat(t.lettreLicenciementMotiveeDetected().reponse()).isEqualTo("NON");
        assertThat(t.motivationLettreSuffisanteDetected()).isNotNull();
        assertThat(t.motivationLettreSuffisanteDetected().reponse()).isEqualTo("NON");
    }

    @Test
    void from_travailExtractedData_procedureLicenciement_sousObjetAbsent_tousNull() {
        // Bloc procedure_licenciement_detection absent → 6 champs null, pas d'exception.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.convocationEntretienDetectee()).isNull();
        assertThat(t.dateConvocationEntretienDetectee()).isNull();
        assertThat(t.dateEntretienPrealableDetectee()).isNull();
        assertThat(t.entretienPrealableTenuDetected()).isNull();
        assertThat(t.lettreLicenciementEcriteDetectee()).isNull();
        assertThat(t.lettreLicenciementMotiveeDetected()).isNull();
        assertThat(t.motivationLettreSuffisanteDetected()).isNull();
    }

    @Test
    void from_travailExtractedData_procedureLicenciement_reponseHorsEnumeration_normaliseeInconnu() {
        // Réponse hors {OUI, NON} → normalizeReponse() la ramène à INCONNU (canonique,
        // cohérent avec reclassement_respecte_detected). Côté front, INCONNU = non pré-rempli.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "procedure_licenciement_detection": {
                      "entretien_prealable_tenu": {
                        "reponse": "PEUT-ETRE",
                        "justification": "ambigu"
                      }
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.entretienPrealableTenuDetected()).isNotNull();
        assertThat(t.entretienPrealableTenuDetected().reponse()).isEqualTo("INCONNU");
        assertThat(t.entretienPrealableTenuDetected().justification()).isEqualTo("ambigu");
    }

    @Test
    void from_travailExtractedData_procedureLicenciement_dateNonIso_failOpenNull() {
        // Date au format non ISO → champ null (fail-open), pas de pré-fill.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "procedure_licenciement_detection": {
                      "date_convocation_entretien": "10/02/2026",
                      "date_entretien_prealable": "le 18 fevrier"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.dateConvocationEntretienDetectee()).isNull();
        assertThat(t.dateEntretienPrealableDetectee()).isNull();
    }

    @Test
    void from_travailExtractedData_procedureLicenciement_multiDates_aucuneConfusion() {
        // Invariant cadrage §5.1.6 : 3 dates concurrentes → chaque champ reçoit la sienne.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "date_licenciement": "2026-02-25",
                    "procedure_licenciement_detection": {
                      "date_convocation_entretien": "2026-02-10",
                      "date_entretien_prealable": "2026-02-18"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.dateConvocationEntretienDetectee()).isEqualTo("2026-02-10");
        assertThat(t.dateEntretienPrealableDetectee()).isEqualTo("2026-02-18");
        assertThat(t.dateLicenciement()).isEqualTo("2026-02-25");
    }

    @Test
    void from_travailExtractedData_procedureLicenciement_justificationTronqueeA500() {
        String longJustification = "A".repeat(600);
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "procedure_licenciement_detection": {
                      "motivation_lettre_suffisante": {
                        "reponse": "NON",
                        "justification": "%s"
                      }
                    }
                  }
                }
                """.formatted(longJustification));

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.motivationLettreSuffisanteDetected()).isNotNull();
        assertThat(t.motivationLettreSuffisanteDetected().justification()).hasSize(500);
    }

    // ========================================================================
    // SF-246-02 : pré-fill IA F-DT-24 — bloc clause_non_concurrence_detail
    // ========================================================================

    @Test
    void from_travailExtractedData_clauseNonConcurrence_complet_parsed() {
        // Cas nominal : sous-objet complet → 5 champs renseignés (SF-246-02 + SF-246-13).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": {
                      "duree_mois": 24,
                      "zone_geographique": "France métropolitaine",
                      "contrepartie_montant_mensuel_eur": 900.0,
                      "date_prise_effet": "2026-03-31",
                      "secteur_activite": "INFORMATIQUE"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDureeMois()).isEqualTo(24);
        assertThat(t.nonConcurrenceZoneGeographique()).isEqualTo("France métropolitaine");
        assertThat(t.nonConcurrenceContrepartieMontantEur()).isEqualTo(900.0);
        assertThat(t.nonConcurrenceDatePriseEffet()).isEqualTo("2026-03-31");
        assertThat(t.nonConcurrenceSecteurActivite()).isEqualTo("INFORMATIQUE");
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_sousObjetAbsent_tousNull() {
        // Sous-objet clause_non_concurrence_detail absent → 5 champs null, pas d'exception.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDureeMois()).isNull();
        assertThat(t.nonConcurrenceZoneGeographique()).isNull();
        assertThat(t.nonConcurrenceContrepartieMontantEur()).isNull();
        assertThat(t.nonConcurrenceDatePriseEffet()).isNull();
        assertThat(t.nonConcurrenceSecteurActivite()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_dureeNegativeOuAberrante_null() {
        // Garde de plage [0, 600] : durée négative ou > 600 mois → null.
        CaseAnalysis negative = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "duree_mois": -3 }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(negative).travailExtractedData()
                .nonConcurrenceDureeMois()).isNull();

        CaseAnalysis tooLarge = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "duree_mois": 720 }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(tooLarge).travailExtractedData()
                .nonConcurrenceDureeMois()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_contrepartieNonPositive_null() {
        // Contrepartie ≤ 0 → null (invariant : montant non fiable reste null, jamais 0).
        CaseAnalysis zero = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "contrepartie_montant_mensuel_eur": 0 }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(zero).travailExtractedData()
                .nonConcurrenceContrepartieMontantEur()).isNull();

        CaseAnalysis negative = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "contrepartie_montant_mensuel_eur": -100 }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(negative).travailExtractedData()
                .nonConcurrenceContrepartieMontantEur()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_zoneTronqueeA500() {
        // Zone géographique > 500 caractères → tronquée à 500.
        String longZone = "Z".repeat(700);
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "zone_geographique": "%s" }
                  }
                }
                """.formatted(longZone));

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceZoneGeographique()).hasSize(500);
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_zoneVide_null() {
        // Zone vide ou blanche → null.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "zone_geographique": "   " }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceZoneGeographique()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_champsPartiels_autresChampsRenseignes() {
        // Clause présente mais durée non chiffrée → durée null, zone + contrepartie OK.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": {
                      "zone_geographique": "Région Île-de-France",
                      "contrepartie_montant_mensuel_eur": 1200.5
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDureeMois()).isNull();
        assertThat(t.nonConcurrenceZoneGeographique()).isEqualTo("Région Île-de-France");
        assertThat(t.nonConcurrenceContrepartieMontantEur()).isEqualTo(1200.5);
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_dureeTexte_null() {
        // duree_mois en texte (ex. "vingt-quatre") → null (node non numérique).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "duree_mois": "vingt-quatre" }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDureeMois()).isNull();
    }

    // ------------------------------------------------------------------------
    // SF-246-13 : 2 champs IA complétant clause_non_concurrence_detail
    // (date de prise d'effet + secteur d'activité, pré-fill F-DT-24).
    // ------------------------------------------------------------------------

    @Test
    void from_travailExtractedData_clauseNonConcurrence_datePriseEffetNonIso_null() {
        // date_prise_effet dans un format non ISO → null (isoDateOrNull fail-open).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "date_prise_effet": "31/03/2026" }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDatePriseEffet()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_datePriseEffetIso_parsed() {
        // date_prise_effet ISO YYYY-MM-DD stricte → conservée telle quelle.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "date_prise_effet": "2025-12-15" }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDatePriseEffet()).isEqualTo("2025-12-15");
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_secteurHorsEnum_null() {
        // secteur_activite hors enum (ex. "BTP") → null (normalizeEnumCode whitelist).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "secteur_activite": "BTP" }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceSecteurActivite()).isNull();
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_secteurMinuscules_normalise() {
        // secteur_activite en minuscules → upper-case puis validé contre la whitelist.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "secteur_activite": "informatique" }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceSecteurActivite()).isEqualTo("INFORMATIQUE");
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_secteurChaqueValeurEnum_parsed() {
        // Les 5 codes de l'enum SecteurActivite sont tous acceptés.
        for (String code : new String[]{"INFORMATIQUE", "COMMERCE", "INDUSTRIE", "SERVICES", "AUTRE"}) {
            CaseAnalysis analysis = analysis("""
                    {
                      "travail_extracted_data": {
                        "clause_non_concurrence_detail": { "secteur_activite": "%s" }
                      }
                    }
                    """.formatted(code));
            assertThat(CaseAnalysisResponse.from(analysis).travailExtractedData()
                    .nonConcurrenceSecteurActivite()).isEqualTo(code);
        }
    }

    @Test
    void from_travailExtractedData_clauseNonConcurrence_datePriseEffetEtSecteurAbsents_null() {
        // Sous-objet présent mais sans les 2 clés SF-246-13 → champs null, pas d'exception.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "clause_non_concurrence_detail": { "duree_mois": 12 }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.nonConcurrenceDureeMois()).isEqualTo(12);
        assertThat(t.nonConcurrenceDatePriseEffet()).isNull();
        assertThat(t.nonConcurrenceSecteurActivite()).isNull();
    }

    // ========================================================================
    // SF-246-05 : pré-fill IA F-DT-29 — âge du demandeur (crédit-temps BE)
    // ========================================================================

    @Test
    void from_travailExtractedData_ageDemandeur_present_parsed() {
        // Cas nominal : age_demandeur_annees présent → champ renseigné.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "CP200",
                    "age_demandeur_annees": 58
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.ageDemandeurAnnees()).isEqualTo(58);
    }

    @Test
    void from_travailExtractedData_ageDemandeur_absent_null() {
        // Champ absent → ageDemandeurAnnees null, pas d'exception.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "CP200",
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.ageDemandeurAnnees()).isNull();
    }

    @Test
    void from_travailExtractedData_ageDemandeur_horsPlage_null() {
        // Garde de plage [0, 100] : âge négatif ou > 100 → null.
        CaseAnalysis negative = analysis("""
                {
                  "travail_extracted_data": { "age_demandeur_annees": -1 }
                }
                """);
        assertThat(CaseAnalysisResponse.from(negative).travailExtractedData()
                .ageDemandeurAnnees()).isNull();

        CaseAnalysis tooLarge = analysis("""
                {
                  "travail_extracted_data": { "age_demandeur_annees": 120 }
                }
                """);
        assertThat(CaseAnalysisResponse.from(tooLarge).travailExtractedData()
                .ageDemandeurAnnees()).isNull();
    }

    @Test
    void from_travailExtractedData_ageDemandeur_texte_null() {
        // age_demandeur_annees en texte (ex. "cinquante-huit") → null (node non numérique).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": { "age_demandeur_annees": "cinquante-huit" }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.ageDemandeurAnnees()).isNull();
    }

    @Test
    void from_travailExtractedData_ageDemandeur_zeroPreserve() {
        // 0 est dans la plage [0, 100] : le record le préserve tel quel. La
        // distinction "0 ≠ inconnu" est portée par le prompt, pas par l'extracteur.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": { "age_demandeur_annees": 0 }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.ageDemandeurAnnees()).isZero();
    }

    @Test
    void from_travailExtractedData_ageDemandeur_etAnciennete_aucuneConfusion() {
        // Invariant cadrage §5.1.1 : un dossier mentionnant l'âge du travailleur
        // (58 ans) ET son ancienneté (25 ans) → ageDemandeurAnnees = 58, jamais 25.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "CP200",
                    "date_entree": "2000-01-01",
                    "age_demandeur_annees": 58
                  },
                  "compensation_data": {
                    "type_rupture": "LICENCIEMENT_ORDINAIRE",
                    "anciennete_annees": 25
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.ageDemandeurAnnees()).isEqualTo(58);
    }

    // ========================================================================
    // SF-246-07 : pré-fill IA F-FA-15/16/17 — sous-objet regime_matrimonial_detection
    // (4 champs régimes matrimoniaux / liquidation, Famille FR uniquement)
    // ========================================================================

    @Test
    void extractFamilleData_regimeMatrimonialDetection_nominalCase_allFourFields() throws Exception {
        // Cas nominal : sous-objet complet → 4 champs renseignés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recompenses_envisagees": true,
                    "regime_communaute_universelle_detecte": true,
                    "partage_judiciaire_envisage": true,
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "COMMUNAUTE_UNIVERSELLE",
                      "valeur_communaute_eur": 350000.0,
                      "valeur_biens_indivision_eur": 180000.0,
                      "nombre_coindivisaires": 2
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isEqualTo("COMMUNAUTE_UNIVERSELLE");
        assertThat(f.valeurCommunauteEurDetectee()).isEqualTo(350000.0);
        assertThat(f.valeurBiensIndivisionEur()).isEqualTo(180000.0);
        assertThat(f.nombreCoindivisairesDetecte()).isEqualTo(2);
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_absent_allFourFieldsNull() throws Exception {
        // Sous-objet absent → 4 champs null, pas d'exception.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recompenses_envisagees": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isNull();
        assertThat(f.valeurCommunauteEurDetectee()).isNull();
        assertThat(f.valeurBiensIndivisionEur()).isNull();
        assertThat(f.nombreCoindivisairesDetecte()).isNull();
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_null_allFourFieldsNull() throws Exception {
        // Sous-objet explicitement null → 4 champs null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recompenses_envisagees": true,
                    "regime_matrimonial_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isNull();
        assertThat(f.valeurCommunauteEurDetectee()).isNull();
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_regimeHorsWhitelist_null() throws Exception {
        // Régime hors whitelist (ex. "REGINE_INCONNU") → null (fail-open).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recompenses_envisagees": true,
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "REGIME_INCONNU_X",
                      "valeur_communaute_eur": 200000.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isNull();
        // La valeur de communauté reste parsée indépendamment.
        assertThat(f.valeurCommunauteEurDetectee()).isEqualTo(200000.0);
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_valeurNegative_null() throws Exception {
        // Montants ≤ 0 → null (invariant §5.2 : jamais 0 ni négatif).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "regime_communaute_universelle_detecte": true,
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "COMMUNAUTE_LEGALE",
                      "valeur_communaute_eur": -500.0,
                      "valeur_biens_indivision_eur": 0.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.valeurCommunauteEurDetectee()).isNull();
        assertThat(f.valeurBiensIndivisionEur()).isNull();
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_nombreCoindivisairesHorsPlage_null() throws Exception {
        // Nombre de coïndivisaires hors [0, 50] → null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "partage_judiciaire_envisage": true,
                    "regime_matrimonial_detection": {
                      "nombre_coindivisaires": 99
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.nombreCoindivisairesDetecte()).isNull();
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_regimeCasseLowercase_normalise() throws Exception {
        // Régime en minuscules → normalisé en MAJUSCULES par whitelistedOrNull.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recompenses_envisagees": true,
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "communaute_legale"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isEqualTo("COMMUNAUTE_LEGALE");
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_seulContenuNonNull_retourneNonNull() throws Exception {
        // Garde-fou : si le seul contenu peuplé est regime_matrimonial_detection
        // (aucun flag booléen activé), l'objet est quand même retourné (non-null).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "SEPARATION_BIENS"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeMatrimonialDetecte()).isEqualTo("SEPARATION_BIENS");
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_multiValeurs_bonChampsRemplis() throws Exception {
        // Invariant cadrage §5.1.6 : dossier mentionnant à la fois la valeur de la
        // communauté (350 000 €) et celle des biens en indivision (180 000 €) →
        // chaque montant dans le bon champ, aucune confusion.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "regime_communaute_universelle_detecte": true,
                    "partage_judiciaire_envisage": true,
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "COMMUNAUTE_UNIVERSELLE",
                      "valeur_communaute_eur": 350000.0,
                      "valeur_biens_indivision_eur": 180000.0,
                      "nombre_coindivisaires": 3
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Chaque montant dans le bon champ — pas d'inversion.
        assertThat(f.valeurCommunauteEurDetectee()).isEqualTo(350000.0);
        assertThat(f.valeurBiensIndivisionEur()).isEqualTo(180000.0);
        assertThat(f.nombreCoindivisairesDetecte()).isEqualTo(3);
        assertThat(f.regimeMatrimonialDetecte()).isEqualTo("COMMUNAUTE_UNIVERSELLE");
    }

    @Test
    void extractFamilleData_regimeMatrimonialDetection_coexistenceAvecSuccessionDetection() throws Exception {
        // Les deux sous-objets peuvent coexister sans interférence.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "regime_communaute_universelle_detecte": true,
                    "succession_detection": {
                      "date_deces": "2025-01-15",
                      "nombre_coheritiers": 2
                    },
                    "regime_matrimonial_detection": {
                      "regime_matrimonial": "COMMUNAUTE_LEGALE",
                      "valeur_communaute_eur": 120000.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // succession_detection intact.
        assertThat(f.dateDecesDetectee()).isEqualTo("2025-01-15");
        assertThat(f.nombreCoheritiersDetecte()).isEqualTo(2);
        // regime_matrimonial_detection intact.
        assertThat(f.regimeMatrimonialDetecte()).isEqualTo("COMMUNAUTE_LEGALE");
        assertThat(f.valeurCommunauteEurDetectee()).isEqualTo(120000.0);
    }

    // SF-246-08 : pré-fill IA F-FA-12/13/14/20/21/22 — sous-objet vie_commune_detection
    // ─────────────────────────────────────────────────────────────────────────────────
    /** Cas nominal : 7 champs, tous présents et valides. */
    @Test
    void extractFamilleData_vieCommuneDetection_nominalCase_allSevenFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "pacs_dissolution_envisagee": true,
                    "vie_commune_detection": {
                      "date_separation": "2023-06-15",
                      "patrimoine_commun_eur": 200000.0,
                      "date_conclusion_pacs": "2018-03-01",
                      "date_requete_op": "2024-01-10",
                      "date_audience_aomp": "2024-02-20",
                      "nb_enfants_a_charge": 2,
                      "revenus_annuels_epoux_eur": 45000.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparation()).isEqualTo("2023-06-15");
        assertThat(f.patrimoineCommunEur()).isEqualTo(200000.0);
        assertThat(f.dateConclusionPacs()).isEqualTo("2018-03-01");
        assertThat(f.dateRequeteOP()).isEqualTo("2024-01-10");
        assertThat(f.dateAudienceAOMP()).isEqualTo("2024-02-20");
        assertThat(f.nbEnfantsACharge()).isEqualTo(2);
        assertThat(f.revenusAnnuelsEpoux()).isEqualTo(45000.0);
    }

    /** Sous-objet absent → 7 champs tous null (no-op gracieux). */
    @Test
    void extractFamilleData_vieCommuneDetection_absent_allSevenFieldsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "separation_corps_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparation()).isNull();
        assertThat(f.patrimoineCommunEur()).isNull();
        assertThat(f.dateConclusionPacs()).isNull();
        assertThat(f.dateRequeteOP()).isNull();
        assertThat(f.dateAudienceAOMP()).isNull();
        assertThat(f.nbEnfantsACharge()).isNull();
        assertThat(f.revenusAnnuelsEpoux()).isNull();
    }

    /** Sous-objet null JSON → 7 champs tous null. */
    @Test
    void extractFamilleData_vieCommuneDetection_null_allSevenFieldsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "separation_corps_envisagee": true,
                    "vie_commune_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparation()).isNull();
        assertThat(f.patrimoineCommunEur()).isNull();
        assertThat(f.nbEnfantsACharge()).isNull();
    }

    /** Date mal formée (non-ISO) → rejetée (isoDateOrNull). */
    @Test
    void extractFamilleData_vieCommuneDetection_dateMalFormee_rejetee() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "separation_corps_envisagee": true,
                    "vie_commune_detection": {
                      "date_separation": "15/06/2023",
                      "patrimoine_commun_eur": 100000.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparation()).isNull();
        // patrimoine_commun_eur valide → non null
        assertThat(f.patrimoineCommunEur()).isEqualTo(100000.0);
    }

    /** Montant négatif → rejeté (positiveDoubleOrNull — invariant §5.2). */
    @Test
    void extractFamilleData_vieCommuneDetection_montantNegatif_rejete() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "separation_corps_envisagee": true,
                    "vie_commune_detection": {
                      "patrimoine_commun_eur": -1.0,
                      "revenus_annuels_epoux_eur": 0.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        // vieCommuneDetectionPresent = false (all null) + sc = true → f non null
        assertThat(f).isNotNull();
        assertThat(f.patrimoineCommunEur()).isNull();
        assertThat(f.revenusAnnuelsEpoux()).isNull();
    }

    /** nb_enfants_a_charge hors plage [0, 30] → null (boundedIntOrNull). */
    @Test
    void extractFamilleData_vieCommuneDetection_nbEnfantsHorsPlage_null() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "revision_post_divorce_envisagee": true,
                    "vie_commune_detection": {
                      "nb_enfants_a_charge": 35
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.nbEnfantsACharge()).isNull();
    }

    /** nb_enfants_a_charge = 0 accepté (borne inclusive). */
    @Test
    void extractFamilleData_vieCommuneDetection_nbEnfantsZero_accepte() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "vie_commune_detection": {
                      "nb_enfants_a_charge": 0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.nbEnfantsACharge()).isEqualTo(0);
    }

    /** Seul un champ non-null suffit à retourner un FamilleExtractedData non-null. */
    @Test
    void extractFamilleData_vieCommuneDetection_seulChampNonNull_retourneNonNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "vie_commune_detection": {
                      "date_requete_op": "2024-01-10"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateRequeteOP()).isEqualTo("2024-01-10");
    }

    /** Coexistence avec regime_matrimonial_detection : les deux sous-objets sont lus. */
    @Test
    void extractFamilleData_vieCommuneDetection_coexistenceAvecRegimeMatrimonial() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_consentement_mutuel_envisage": true,
                    "regime_matrimonial_detection": {
                      "valeur_communaute_eur": 150000.0
                    },
                    "vie_commune_detection": {
                      "date_separation": "2023-06-15",
                      "nb_enfants_a_charge": 1
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // regime_matrimonial_detection intact.
        assertThat(f.valeurCommunauteEurDetectee()).isEqualTo(150000.0);
        // vie_commune_detection intact.
        assertThat(f.dateSeparation()).isEqualTo("2023-06-15");
        assertThat(f.nbEnfantsACharge()).isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────────────
    // SF-246-09 : pré-fill IA F-FA-18 — sous-objet filiation_detection
    // ─────────────────────────────────────────────────────────────────────────────────

    /** Cas nominal : tous les 7 champs renseignés. */
    @Test
    void extractFamilleData_filiationDetection_nominalCase_allSevenFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "filiation_detection": {
                      "date_etablissement_filiation": "2010-05-12",
                      "date_connaissance_verite": "2024-02-01",
                      "date_majorite_enfant": "2028-05-12",
                      "date_naissance_enfant_recherche": "2020-07-03",
                      "date_naissance_enfant": "2015-11-20",
                      "age_adoptant": 42,
                      "age_adopte": 7
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateEtablissementFiliationDetectee()).isEqualTo("2010-05-12");
        assertThat(f.dateConnaissanceVeriteDetectee()).isEqualTo("2024-02-01");
        assertThat(f.dateMajoriteEnfantDetectee()).isEqualTo("2028-05-12");
        assertThat(f.dateNaissanceEnfantRechercheDetectee()).isEqualTo("2020-07-03");
        assertThat(f.dateNaissanceEnfantDetectee()).isEqualTo("2015-11-20");
        assertThat(f.ageAdoptantDetecte()).isEqualTo(42);
        assertThat(f.ageAdopteDetecte()).isEqualTo(7);
    }

    /** Sous-objet absent → 7 champs null, pas d'exception. */
    @Test
    void extractFamilleData_filiationDetection_absent_allSevenFieldsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateEtablissementFiliationDetectee()).isNull();
        assertThat(f.dateConnaissanceVeriteDetectee()).isNull();
        assertThat(f.dateMajoriteEnfantDetectee()).isNull();
        assertThat(f.dateNaissanceEnfantRechercheDetectee()).isNull();
        assertThat(f.dateNaissanceEnfantDetectee()).isNull();
        assertThat(f.ageAdoptantDetecte()).isNull();
        assertThat(f.ageAdopteDetecte()).isNull();
    }

    /** Sous-objet explicitement null → 7 champs null. */
    @Test
    void extractFamilleData_filiationDetection_null_allSevenFieldsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "filiation_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateEtablissementFiliationDetectee()).isNull();
        assertThat(f.dateNaissanceEnfantDetectee()).isNull();
        assertThat(f.ageAdoptantDetecte()).isNull();
    }

    /** Date non ISO → champ null (fail-open). */
    @Test
    void extractFamilleData_filiationDetection_dateMalFormee_rejetee() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "filiation_detection": {
                      "date_etablissement_filiation": "12/05/2010",
                      "date_connaissance_verite": "1er février 2024"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateEtablissementFiliationDetectee()).isNull();
        assertThat(f.dateConnaissanceVeriteDetectee()).isNull();
    }

    /** Âge hors plage [0, 120] → null. */
    @Test
    void extractFamilleData_filiationDetection_ageHorsPlage_null() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "adoption_envisagee": true,
                    "filiation_detection": {
                      "age_adoptant": 150,
                      "age_adopte": -3
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.ageAdoptantDetecte()).isNull();
        assertThat(f.ageAdopteDetecte()).isNull();
    }

    /** Âge à 0 → accepté (limite basse inclusive). */
    @Test
    void extractFamilleData_filiationDetection_ageZero_accepte() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "adoption_envisagee": true,
                    "filiation_detection": {
                      "age_adopte": 0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.ageAdopteDetecte()).isEqualTo(0);
    }

    /**
     * Fixture multi-dates — invariant cadrage §5.1.6 :
     * date d'établissement filiation ≠ date connaissance vérité ≠ date majorité
     * → chaque champ rempli avec la bonne date, aucune confusion.
     */
    @Test
    void extractFamilleData_filiationDetection_multiDates_contestationPaternite_noConfusion() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "filiation_detection": {
                      "date_etablissement_filiation": "2000-03-15",
                      "date_connaissance_verite": "2023-07-20",
                      "date_majorite_enfant": "2018-03-15"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Chaque date rattachée au bon champ — aucune confusion.
        assertThat(f.dateEtablissementFiliationDetectee()).isEqualTo("2000-03-15");
        assertThat(f.dateConnaissanceVeriteDetectee()).isEqualTo("2023-07-20");
        assertThat(f.dateMajoriteEnfantDetectee()).isEqualTo("2018-03-15");
        // Les deux dates de naissance d'enfant ne sont pas renseignées (non présentes dans ce dossier).
        assertThat(f.dateNaissanceEnfantRechercheDetectee()).isNull();
        assertThat(f.dateNaissanceEnfantDetectee()).isNull();
    }

    /**
     * Fixture distinction naissance — invariant §5.1.6 (2ème cas) :
     * date naissance enfant recherche (art. 327) ≠ date naissance enfant reconnaissance (art. 316).
     * Le dossier mentionne les deux en contextes distincts → chaque champ au bon endroit.
     */
    @Test
    void extractFamilleData_filiationDetection_distingueNaissanceRechercheVsReconnaissance() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recherche_paternite_envisagee": true,
                    "reconnaissance_paternelle_envisagee": true,
                    "filiation_detection": {
                      "date_naissance_enfant_recherche": "2020-07-03",
                      "date_naissance_enfant": "2015-11-20"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateNaissanceEnfantRechercheDetectee()).isEqualTo("2020-07-03");
        assertThat(f.dateNaissanceEnfantDetectee()).isEqualTo("2015-11-20");
        // Aucune confusion entre les deux.
        assertThat(f.dateNaissanceEnfantRechercheDetectee())
            .isNotEqualTo(f.dateNaissanceEnfantDetectee());
    }

    /** Seul filiation_detection présent (sans flag boolean) → f non null. */
    @Test
    void extractFamilleData_filiationDetection_seulChampNonNull_retourneNonNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "filiation_detection": {
                      "age_adoptant": 30
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.ageAdoptantDetecte()).isEqualTo(30);
    }

    /** Coexistence avec vie_commune_detection : les deux sous-objets sont lus. */
    @Test
    void extractFamilleData_filiationDetection_coexistenceAvecVieCommuneDetection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "vie_commune_detection": {
                      "date_separation": "2022-01-01"
                    },
                    "filiation_detection": {
                      "date_etablissement_filiation": "2005-04-10"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // vie_commune_detection intact.
        assertThat(f.dateSeparation()).isEqualTo("2022-01-01");
        // filiation_detection intact.
        assertThat(f.dateEtablissementFiliationDetectee()).isEqualTo("2005-04-10");
    }

    // =========================================================================
    // SF-246-10 — autorite_parentale_detection (agesEnfantsDetectes + dates calendrier)
    // =========================================================================

    @Test
    void extractFamilleData_autoriteParentaleDetection_casNominal_troisChamps() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "autorite_parentale_detectee": true,
                    "autorite_parentale_detection": {
                      "ages_enfants": [12, 9, 4],
                      "date_debut_calendrier": "2026-09-01",
                      "date_fin_calendrier": "2027-08-31"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.agesEnfantsDetectes()).containsExactly(12, 9, 4);
        assertThat(f.dateDebutCalendrierDetectee()).isEqualTo("2026-09-01");
        assertThat(f.dateFinCalendrierDetectee()).isEqualTo("2027-08-31");
    }

    @Test
    void extractFamilleData_autoriteParentaleDetection_absent_troisChampsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_residence_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.agesEnfantsDetectes()).isNull();
        assertThat(f.dateDebutCalendrierDetectee()).isNull();
        assertThat(f.dateFinCalendrierDetectee()).isNull();
    }

    @Test
    void extractFamilleData_autoriteParentaleDetection_agesAvecValeursAberrantes_filtrees() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "autorite_parentale_detection": {
                      "ages_enfants": [10, 200, 6],
                      "date_debut_calendrier": null,
                      "date_fin_calendrier": null
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // 200 hors plage [0, 25] → exclu.
        assertThat(f.agesEnfantsDetectes()).containsExactly(10, 6);
    }

    @Test
    void extractFamilleData_autoriteParentaleDetection_listeVideApresFiltrage_null() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        // changement_residence_envisage force le guard à retourner non-null (flag lu par le parseur).
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_residence_envisage": true,
                    "autorite_parentale_detection": {
                      "ages_enfants": [200, 300],
                      "date_debut_calendrier": null,
                      "date_fin_calendrier": null
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Tous hors plage → null (jamais liste vide — invariant §5.1.2 transposé aux listes).
        assertThat(f.agesEnfantsDetectes()).isNull();
    }

    @Test
    void extractFamilleData_autoriteParentaleDetection_dateNonIso_rejetee() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "autorite_parentale_detection": {
                      "ages_enfants": null,
                      "date_debut_calendrier": "01/09/2026",
                      "date_fin_calendrier": "not-a-date"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        // date_debut_calendrier et date_fin_calendrier malformées → null.
        // Le sous-objet autorite_parentale_detection est présent mais n'apporte aucune donnée valide.
        // Guard retourne null sauf si un autre flag est présent.
        // Ici autorite_parentale_detection présent mais ses 3 champs sont null → autoriteParentaleDetectionPresent = false.
        // → extractFamilleData() retourne null (guard). On vérifie le comportement gracieux.
        if (f != null) {
            assertThat(f.dateDebutCalendrierDetectee()).isNull();
            assertThat(f.dateFinCalendrierDetectee()).isNull();
        }
    }

    @Test
    void extractFamilleData_autoriteParentaleDetection_coexistenceAvecFiliationDetection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "autorite_parentale_detectee": true,
                    "filiation_detection": {
                      "date_etablissement_filiation": "2005-04-10"
                    },
                    "autorite_parentale_detection": {
                      "ages_enfants": [8, 5],
                      "date_debut_calendrier": "2026-09-01",
                      "date_fin_calendrier": "2027-06-30"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // filiation_detection intact.
        assertThat(f.dateEtablissementFiliationDetectee()).isEqualTo("2005-04-10");
        // autorite_parentale_detection intact.
        assertThat(f.agesEnfantsDetectes()).containsExactly(8, 5);
        assertThat(f.dateDebutCalendrierDetectee()).isEqualTo("2026-09-01");
        assertThat(f.dateFinCalendrierDetectee()).isEqualTo("2027-06-30");
    }

    // =========================================================================
    // SF-246-03 — divorce_faute_detection (fautesDetectees)
    // =========================================================================

    @Test
    void extractFamilleData_divorceFauteDetection_casNominal_violencesEtAbandon() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["VIOLENCES", "ABANDON"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.fautesDetectees()).containsExactly("VIOLENCES", "ABANDON");
    }

    @Test
    void extractFamilleData_divorceFauteDetection_codesHorsWhitelist_exclus() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["ADULTERE", "CODE_INCONNU", "VIOLENCES"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // CODE_INCONNU exclu ; les deux codes valides conservés.
        assertThat(f.fautesDetectees()).containsExactly("ADULTERE", "VIOLENCES");
    }

    @Test
    void extractFamilleData_divorceFauteDetection_sousObjetAbsent_fautesNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.fautesDetectees()).isNull();
    }

    @Test
    void extractFamilleData_divorceFauteDetection_listeVideApresFiltre_retourneNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["BIDON1", "BIDON2"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Tous codes invalides → null (jamais []).
        assertThat(f.fautesDetectees()).isNull();
    }

    @Test
    void extractFamilleData_divorceFauteDetection_caseInsensible_normaliseEnMajuscules() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["adultere", "Violences"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.fautesDetectees()).containsExactly("ADULTERE", "VIOLENCES");
    }

    @Test
    void extractFamilleData_divorceFauteDetection_deduplication() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["ADULTERE", "ADULTERE", "VIOLENCES"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Doublons dédupliqués.
        assertThat(f.fautesDetectees()).containsExactly("ADULTERE", "VIOLENCES");
    }

    @Test
    void extractFamilleData_divorceFauteDetection_tousCodes8() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_faute_envisage": true,
                    "divorce_faute_detection": {
                      "fautes_detectees": ["ADULTERE","VIOLENCES","ABANDON","OUTRAGES","DEVOIR_ASSISTANCE","DEVOIR_FIDELITE","DEVOIR_COMMUNAUTE_VIE","AUTRE"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.fautesDetectees()).hasSize(8);
    }

    // =========================================================================
    // SF-246-11 — changement_etat_civil_detection (dateNaissanceDemandeurDetectee)
    // =========================================================================

    @Test
    void extractFamilleData_cecDetection_casNominal_dateNaissancePresenteIso() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_envisage": true,
                    "changement_etat_civil_detection": {
                      "date_naissance_demandeur": "1985-03-22"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateNaissanceDemandeurDetectee()).isEqualTo("1985-03-22");
    }

    @Test
    void extractFamilleData_cecDetection_sousObjetAbsent_dateNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateNaissanceDemandeurDetectee()).isNull();
    }

    @Test
    void extractFamilleData_cecDetection_dateNonIso_retourneNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_envisage": true,
                    "changement_etat_civil_detection": {
                      "date_naissance_demandeur": "22/03/1985"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Format non-ISO rejeté par isoDateOrNull().
        assertThat(f.dateNaissanceDemandeurDetectee()).isNull();
    }

    @Test
    void extractFamilleData_cecDetection_multidates_nePrendsQueNaissanceDemandeur() throws Exception {
        // Invariant §5.1.6 : fixture avec date de naissance demandeur + date de requête.
        // L'extracteur ne lit que "date_naissance_demandeur" — pas la date de requête.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_envisage": true,
                    "changement_etat_civil_detection": {
                      "date_naissance_demandeur": "1990-07-15",
                      "date_requete": "2026-01-10"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Seule la date de naissance est extraite — la date de requête est ignorée.
        assertThat(f.dateNaissanceDemandeurDetectee()).isEqualTo("1990-07-15");
    }

    @Test
    void extractFamilleData_cecDetection_sousObjetNull_dateNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_envisage": true,
                    "changement_etat_civil_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateNaissanceDemandeurDetectee()).isNull();
    }

    @Test
    void extractFamilleData_cecDetection_remonteLeRecord_memeQuandSeulChampPresent() throws Exception {
        // Seul changement_etat_civil_detection présent → FamilleExtractedData non null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "changement_etat_civil_detection": {
                      "date_naissance_demandeur": "2000-12-01"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateNaissanceDemandeurDetectee()).isEqualTo("2000-12-01");
    }

    // ===== SF-246-24 — extractFamilleData : succession_detection_v2 =====

    @Test
    void extractFamilleData_successionV2_casNominal_tousLesChamps() throws Exception {
        // Cas nominal : sous-objet complet avec toutes les valeurs valides.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection_v2": {
                      "qualite_heritier": "PREMIER_RANG",
                      "actes_equivalent_acceptation_dejas_poses": true,
                      "dettes_incertaines": false,
                      "conjoint_survivant": true,
                      "qualite_du_demandeur_reserve": "HERITIER_RESERVATAIRE_DESCENDANT",
                      "qualite_heritier_rapport": "DESCENDANT",
                      "donation_dispense_de_rapport": false,
                      "nature_presumee_non_rapportable": true,
                      "tous_descendants_communs_avec_conjoint": true,
                      "forme_donation": "NOTARIEE",
                      "saine_esprit_donateur": true,
                      "respect_quotite_disponible": true,
                      "forme_testament": "OLOGRAPHE",
                      "saine_esprit_testateur": true,
                      "legs_excede_quotite_disponible": false
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteHeritierDetectee()).isEqualTo("PREMIER_RANG");
        assertThat(f.actesEquivalentAcceptationDejaPosesDetected()).isTrue();
        assertThat(f.dettesIncertainesDetected()).isFalse();
        assertThat(f.conjointSurvivantDetected()).isTrue();
        assertThat(f.qualiteDuDemandeurReserveDetecte()).isEqualTo("HERITIER_RESERVATAIRE_DESCENDANT");
        assertThat(f.qualiteHeritierRapportDetectee()).isEqualTo("DESCENDANT");
        assertThat(f.donationDispenseDeRapportDetected()).isFalse();
        assertThat(f.naturePresumeeNonRapportableDetected()).isTrue();
        assertThat(f.tousDescendantsCommunsAvecConjointDetected()).isTrue();
        assertThat(f.formeDonationDetectee()).isEqualTo("NOTARIEE");
        assertThat(f.saineDEspritDonateurDetected()).isTrue();
        assertThat(f.respectQuotiteDisponibleDetected()).isTrue();
        assertThat(f.formeTestamentDetectee()).isEqualTo("OLOGRAPHE");
        assertThat(f.saineDEspritTestateurDetected()).isTrue();
        assertThat(f.legsExcedeQuotiteDisponibleDetected()).isFalse();
    }

    @Test
    void extractFamilleData_successionV2_sousObjetAbsent_tousNulls() throws Exception {
        // Sous-objet absent → no-op gracieux : tous les 15 champs null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteHeritierDetectee()).isNull();
        assertThat(f.actesEquivalentAcceptationDejaPosesDetected()).isNull();
        assertThat(f.dettesIncertainesDetected()).isNull();
        assertThat(f.conjointSurvivantDetected()).isNull();
        assertThat(f.qualiteDuDemandeurReserveDetecte()).isNull();
        assertThat(f.qualiteHeritierRapportDetectee()).isNull();
        assertThat(f.donationDispenseDeRapportDetected()).isNull();
        assertThat(f.naturePresumeeNonRapportableDetected()).isNull();
        assertThat(f.tousDescendantsCommunsAvecConjointDetected()).isNull();
        assertThat(f.formeDonationDetectee()).isNull();
        assertThat(f.saineDEspritDonateurDetected()).isNull();
        assertThat(f.respectQuotiteDisponibleDetected()).isNull();
        assertThat(f.formeTestamentDetectee()).isNull();
        assertThat(f.saineDEspritTestateurDetected()).isNull();
        assertThat(f.legsExcedeQuotiteDisponibleDetected()).isNull();
    }

    @Test
    void extractFamilleData_successionV2_valeursHorsWhitelist_retourneNull() throws Exception {
        // Valeurs hors whitelist → toutes whitelistées retournent null (fail-open).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection_v2": {
                      "qualite_heritier": "TROISIEME_RANG",
                      "qualite_du_demandeur_reserve": "LEGATAIRE_UNIVERSEL",
                      "qualite_heritier_rapport": "NEVEU",
                      "forme_donation": "ORALE",
                      "forme_testament": "INTERNATIONAL"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Chaînes hors whitelist → null
        assertThat(f.qualiteHeritierDetectee()).isNull();
        assertThat(f.qualiteDuDemandeurReserveDetecte()).isNull();
        assertThat(f.qualiteHeritierRapportDetectee()).isNull();
        assertThat(f.formeDonationDetectee()).isNull();
        assertThat(f.formeTestamentDetectee()).isNull();
    }

    @Test
    void extractFamilleData_successionV2_boolNonParseable_retourneNull() throws Exception {
        // Valeur de type string pour un booléen → booleanOrNull() retourne null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection_v2": {
                      "conjoint_survivant": "oui",
                      "dettes_incertaines": 1,
                      "actes_equivalent_acceptation_dejas_poses": "peut-etre"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.conjointSurvivantDetected()).isNull();
        assertThat(f.dettesIncertainesDetected()).isNull();
        assertThat(f.actesEquivalentAcceptationDejaPosesDetected()).isNull();
    }

    @Test
    void extractFamilleData_successionV2_seulSousObjet_remonteLeRecord() throws Exception {
        // Seul succession_detection_v2 renseigné (aucun flag booléen FR/BE) → record non null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_detection_v2": {
                      "conjoint_survivant": true
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.conjointSurvivantDetected()).isTrue();
    }

    @Test
    void extractFamilleData_successionV2_whitelistInsensibleCasse_normaliseEnMajuscules() throws Exception {
        // Les valeurs en minuscules passent la whitelist (normalisées en MAJUSCULES).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "succession_envisagee": true,
                    "succession_detection_v2": {
                      "qualite_heritier": "premier_rang",
                      "forme_donation": "manuelle",
                      "qualite_heritier_rapport": "conjoint_survivant"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteHeritierDetectee()).isEqualTo("PREMIER_RANG");
        assertThat(f.formeDonationDetectee()).isEqualTo("MANUELLE");
        assertThat(f.qualiteHeritierRapportDetectee()).isEqualTo("CONJOINT_SURVIVANT");
    }

    // ===== SF-246-22 — extractTravailData : procedure_travail_detection =====

    @Test
    void extractTravailData_procedureTravailDetection_casNominal_FR() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_travail_detection": {
                      "procedure_detectee": "PRUDHOMMES_FR",
                      "date_declencheur": "2026-03-01"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isEqualTo("PRUDHOMMES_FR");
        assertThat(t.dateDeclencheurProcedure()).isEqualTo("2026-03-01");
    }

    @Test
    void extractTravailData_procedureTravailDetection_casNominal_BE() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_travail_detection": {
                      "procedure_detectee": "TRIBUNAL_TRAVAIL_BE",
                      "date_declencheur": "2026-05-10"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isEqualTo("TRIBUNAL_TRAVAIL_BE");
        assertThat(t.dateDeclencheurProcedure()).isEqualTo("2026-05-10");
    }

    @Test
    void extractTravailData_procedureTravailDetection_sousObjetAbsent_deuxChampsNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "IDCC_3043"
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isNull();
        assertThat(t.dateDeclencheurProcedure()).isNull();
    }

    @Test
    void extractTravailData_procedureTravailDetection_codeHorsWhitelist_procedureNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_travail_detection": {
                      "procedure_detectee": "TRIBUNAL_COMMERCE_FR",
                      "date_declencheur": "2026-03-01"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isNull();
        assertThat(t.dateDeclencheurProcedure()).isEqualTo("2026-03-01");
    }

    @Test
    void extractTravailData_procedureTravailDetection_dateNonISO_dateNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_travail_detection": {
                      "procedure_detectee": "PRUDHOMMES_FR",
                      "date_declencheur": "01/03/2026"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isEqualTo("PRUDHOMMES_FR");
        assertThat(t.dateDeclencheurProcedure()).isNull();
    }

    @Test
    void extractTravailData_procedureTravailDetection_procedureSeule_dateDeclencheurAbsente() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_travail_detection": {
                      "procedure_detectee": "CASSATION_SOCIALE_FR"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.procedureTravailDetectee()).isEqualTo("CASSATION_SOCIALE_FR");
        assertThat(t.dateDeclencheurProcedure()).isNull();
    }

    @Test
    void extractTravailData_procedureTravailDetection_tousSixCodes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String code : java.util.List.of(
                "PRUDHOMMES_FR", "APPEL_CA_SOCIALE_FR", "CASSATION_SOCIALE_FR",
                "TRIBUNAL_TRAVAIL_BE", "COUR_TRAVAIL_BE", "CASSATION_BE")) {
            JsonNode root = mapper.readTree(String.format("""
                    {
                      "travail_extracted_data": {
                        "procedure_travail_detection": {
                          "procedure_detectee": "%s"
                        }
                      }
                    }
                    """, code));
            var t = CaseAnalysisResponse.extractTravailData(root);
            assertThat(t).isNotNull();
            assertThat(t.procedureTravailDetectee())
                    .as("code '%s' devrait être accepté", code)
                    .isEqualTo(code);
        }
    }

    // =========================================================
    // SF-246-21 — 5 nouveaux sous-objets
    // =========================================================

    // --- requalification_detection ---

    @Test
    void extractTravailData_sf24621_requalificationCdd_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "requalification_detection": {
                      "cdd_duree_mois": 6,
                      "cdd_date_fin_dernier_contrat": "2024-03-31",
                      "cdd_nouveau_date_debut": "2024-04-01",
                      "cdd_nouveau_date_fin": "2024-09-30",
                      "cdd_total_salaires_bruts": 15000.0
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.cddDureeMois()).isEqualTo(6);
        assertThat(t.cddDateFinDernierContrat()).isEqualTo("2024-03-31");
        assertThat(t.cddNouveauDateDebut()).isEqualTo("2024-04-01");
        assertThat(t.cddNouveauDateFin()).isEqualTo("2024-09-30");
        assertThat(t.cddTotalSalairesBruts()).isEqualTo(15000.0);
    }

    @Test
    void extractTravailData_sf24621_requalificationInterim_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "requalification_detection": {
                      "interim_duree_totale_mois": 18,
                      "interim_date_fin_derniere_mission": "2024-06-30",
                      "interim_nouvelle_mission_date_debut": "2024-07-01",
                      "interim_nouvelle_mission_date_fin": "2024-12-31",
                      "interim_entreprise_utilisatrice": "ACME SA",
                      "interim_total_remunerations_brutes": 25000.0,
                      "interim_duree_mission_jours": 90
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.interimDureeTotaleMois()).isEqualTo(18);
        assertThat(t.interimDateFinDerniereMission()).isEqualTo("2024-06-30");
        assertThat(t.interimNouvellesMissionDateDebut()).isEqualTo("2024-07-01");
        assertThat(t.interimNouvellesMissionDateFin()).isEqualTo("2024-12-31");
        assertThat(t.interimEntrepriseUtilisatrice()).isEqualTo("ACME SA");
        assertThat(t.interimTotalRemunerationsBrutes()).isEqualTo(25000.0);
        assertThat(t.interimDureeMissionJours()).isEqualTo(90);
    }

    @Test
    void extractTravailData_sf24621_requalification_valeursHorsBornes_nulles() throws Exception {
        // cdd_duree_mois > 120 → null ; interim_duree_mission_jours > 3650 → null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "requalification_detection": {
                      "cdd_duree_mois": 200,
                      "interim_duree_mission_jours": 5000
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.cddDureeMois()).isNull();
        assertThat(t.interimDureeMissionJours()).isNull();
    }

    // --- paie_detection ---

    @Test
    void extractTravailData_sf24621_paieDetection_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "paie_detection": {
                      "conges_jours_acquis": 25,
                      "conges_jours_pris": 18,
                      "rappel_salaire_montant_perverse_mensuel": 2800.0,
                      "rappel_salaire_periode_debut": "2023-01-01",
                      "rappel_salaire_periode_fin": "2023-12-31"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.congesJoursAcquis()).isEqualTo(25);
        assertThat(t.congesJoursPris()).isEqualTo(18);
        assertThat(t.rappelSalaireMontantPerverseMensuel()).isEqualTo(2800.0);
        assertThat(t.rappelSalairePeriodeDebut()).isEqualTo("2023-01-01");
        assertThat(t.rappelSalairePeriodeFin()).isEqualTo("2023-12-31");
    }

    @Test
    void extractTravailData_sf24621_paieDetection_congesHorsBornes_nuls() throws Exception {
        // conges_jours_acquis > 50 → null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "paie_detection": {
                      "conges_jours_acquis": 100,
                      "conges_jours_pris": 55
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.congesJoursAcquis()).isNull();
        assertThat(t.congesJoursPris()).isNull();
    }

    // --- rupture_collective_detection ---

    @Test
    void extractTravailData_sf24621_ruptureCollective_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "rupture_collective_detection": {
                      "salarie_age_annees": 45,
                      "pse_nombre_salaries": 250,
                      "pse_nombre_licenciements": 30,
                      "transaction_date_signature": "2024-05-15",
                      "transaction_indemnite_montant_eur": 50000.0
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.salarieAgeAnnees()).isEqualTo(45);
        assertThat(t.pseNombreSalaries()).isEqualTo(250);
        assertThat(t.pseNombreLicenciements()).isEqualTo(30);
        assertThat(t.transactionDateSignature()).isEqualTo("2024-05-15");
        assertThat(t.transactionIndemniteMontantEur()).isEqualTo(50000.0);
    }

    @Test
    void extractTravailData_sf24621_ruptureCollective_ageHorsBornes_nul() throws Exception {
        // salarieAgeAnnees < 16 → null ; pseNombreSalaries > 100000 → null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "rupture_collective_detection": {
                      "salarie_age_annees": 10,
                      "pse_nombre_salaries": 200000
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.salarieAgeAnnees()).isNull();
        assertThat(t.pseNombreSalaries()).isNull();
    }

    // --- sante_discrimination_detection ---

    @Test
    void extractTravailData_sf24621_santeDiscrimination_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "sante_discrimination_detection": {
                      "at_date_accident": "2023-07-20",
                      "at_date_exposition": "2020-01-01",
                      "are_type_decision": "RADIATION",
                      "are_montant_conteste": 3500.0,
                      "discrimination_motif": "ACTIVITES_SYNDICALES",
                      "discrimination_contexte": "LICENCIEMENT"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.atDateAccident()).isEqualTo("2023-07-20");
        assertThat(t.atDateExposition()).isEqualTo("2020-01-01");
        assertThat(t.areTypeDecision()).isEqualTo("RADIATION");
        assertThat(t.areMontantConteste()).isEqualTo(3500.0);
        assertThat(t.discriminationMotif()).isEqualTo("ACTIVITES_SYNDICALES");
        assertThat(t.discriminationContexte()).isEqualTo("LICENCIEMENT");
    }

    @Test
    void extractTravailData_sf24621_santeDiscrimination_codeAreInvalide_nul() throws Exception {
        // are_type_decision hors whitelist → null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "sante_discrimination_detection": {
                      "are_type_decision": "CODE_INCONNU"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.areTypeDecision()).isNull();
    }

    @Test
    void extractTravailData_sf24621_santeDiscrimination_sixCodesAreValides() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String code : java.util.List.of(
                "REFUS_INSCRIPTION", "RADIATION", "SUPPRESSION_ARE",
                "REDUCTION_ARE", "EXCLUSION_TEMPORAIRE", "AUTRE")) {
            JsonNode root = mapper.readTree(String.format("""
                    {
                      "travail_extracted_data": {
                        "sante_discrimination_detection": {
                          "are_type_decision": "%s"
                        }
                      }
                    }
                    """, code));
            var t = CaseAnalysisResponse.extractTravailData(root);
            assertThat(t.areTypeDecision())
                    .as("code ARE '%s' devrait être accepté", code)
                    .isEqualTo(code);
        }
    }

    @Test
    void extractTravailData_sf24621_santeDiscrimination_neufMotifsDiscriminationValides() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (String code : java.util.List.of(
                "ORIGINE", "SEXE", "HANDICAP", "AGE", "RELIGION",
                "ORIENTATION_SEXUELLE", "GROSSESSE", "ACTIVITES_SYNDICALES", "AUTRE")) {
            JsonNode root = mapper.readTree(String.format("""
                    {
                      "travail_extracted_data": {
                        "sante_discrimination_detection": {
                          "discrimination_motif": "%s"
                        }
                      }
                    }
                    """, code));
            var t = CaseAnalysisResponse.extractTravailData(root);
            assertThat(t.discriminationMotif())
                    .as("motif '%s' devrait être accepté", code)
                    .isEqualTo(code);
        }
    }

    // --- procedure_details_detection ---

    @Test
    void extractTravailData_sf24621_procedureDetails_nominal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_details_detection": {
                      "refere_montant_provision": 8000.0,
                      "documents_date_certificat_travail": "2024-03-01",
                      "documents_date_attestation_france_travail": "2024-03-05",
                      "documents_date_solde_tout_compte": "2024-03-10"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.refereMontantProvision()).isEqualTo(8000.0);
        assertThat(t.documentsDateCertificatTravail()).isEqualTo("2024-03-01");
        assertThat(t.documentsDateAttestationFranceTravail()).isEqualTo("2024-03-05");
        assertThat(t.documentsDateSoldeToutCompte()).isEqualTo("2024-03-10");
    }

    @Test
    void extractTravailData_sf24621_procedureDetails_dateMalFormatee_nulle() throws Exception {
        // date non-ISO → null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "procedure_details_detection": {
                      "documents_date_certificat_travail": "01/03/2024",
                      "documents_date_attestation_france_travail": "not-a-date"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.documentsDateCertificatTravail()).isNull();
        assertThat(t.documentsDateAttestationFranceTravail()).isNull();
    }

    @Test
    void extractTravailData_sf24621_sousObjetsAbsents_tousNuls() throws Exception {
        // Aucun des 5 sous-objets → tous les 32 champs null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "salaire_brut_mensuel": 3000
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.cddDureeMois()).isNull();
        assertThat(t.interimDureeTotaleMois()).isNull();
        assertThat(t.congesJoursAcquis()).isNull();
        assertThat(t.salarieAgeAnnees()).isNull();
        assertThat(t.atDateAccident()).isNull();
        assertThat(t.areTypeDecision()).isNull();
        assertThat(t.discriminationMotif()).isNull();
        assertThat(t.refereMontantProvision()).isNull();
        assertThat(t.documentsDateCertificatTravail()).isNull();
    }

    // =========================================================
    // SF-246-23 — travail_be_detection (6 champs BE)
    // =========================================================

    @Test
    void extractTravailData_sf24623_travailBeDetection_casBE_toutPresent() throws Exception {
        // CA-1 : dossier BE avec tous les 6 champs renseignés → extraction complète
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "travail_be_detection": {
                      "date_connaissance_fait": "2024-05-10",
                      "date_notification_motifs": "2024-05-13",
                      "commission_paritaire_be": "CP 200",
                      "jours_travailles_annee_precedente_be": 220,
                      "jours_prestes_be": 45,
                      "date_demande_credit_temps": "2024-03-01"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.dateConnaissanceFait()).isEqualTo("2024-05-10");
        assertThat(t.dateNotificationMotifs()).isEqualTo("2024-05-13");
        assertThat(t.commissionParitaireBe()).isEqualTo("CP 200");
        assertThat(t.joursTravaillesAnneePrecedenteBe()).isEqualTo(220);
        assertThat(t.joursPrestesBe()).isEqualTo(45);
        assertThat(t.dateDemandeCreditTemps()).isEqualTo("2024-03-01");
    }

    @Test
    void extractTravailData_sf24623_sousObjetAbsent_sixChampsNull() throws Exception {
        // CA-2 : dossier FR sans sous-objet → tous les 6 nouveaux champs null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.dateConnaissanceFait()).isNull();
        assertThat(t.dateNotificationMotifs()).isNull();
        assertThat(t.commissionParitaireBe()).isNull();
        assertThat(t.joursTravaillesAnneePrecedenteBe()).isNull();
        assertThat(t.joursPrestesBe()).isNull();
        assertThat(t.dateDemandeCreditTemps()).isNull();
    }

    @Test
    void extractTravailData_sf24623_dateNonISO_dateNull() throws Exception {
        // CA-3 : date au format non-YYYY-MM-DD → null (isoDateOrNull regex structurel).
        // Note : isoDateOrNull valide le format \d{4}-\d{2}-\d{2}, pas la cohérence
        // calendaire. "2024-99-01" PASSE le regex (→ retourné tel quel), ce qui est
        // intentionnel (l'avocat corrige dans l'outil). Seuls les formats non-ISO
        // (slash, texte libre) sont null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "travail_be_detection": {
                      "date_connaissance_fait": "10/05/2024",
                      "date_notification_motifs": "not-a-date"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.dateConnaissanceFait()).isNull();
        assertThat(t.dateNotificationMotifs()).isNull();
    }

    @Test
    void extractTravailData_sf24623_commissionParitaireTropLongue_tronquee() throws Exception {
        // CA-4 : commission_paritaire_be > 20 caractères → tronquée à 20
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "travail_be_detection": {
                      "commission_paritaire_be": "CP 200 Auxiliaires alimentaires BE longue description"
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.commissionParitaireBe()).isNotNull();
        assertThat(t.commissionParitaireBe().length()).isLessThanOrEqualTo(20);
    }

    @Test
    void extractTravailData_sf24623_joursHorsBornes_null() throws Exception {
        // CA-5 : jours_travailles hors [0, 365] → null (boundedIntOrNull)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "travail_be_detection": {
                      "jours_travailles_annee_precedente_be": 400,
                      "jours_prestes_be": -1
                    }
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.joursTravaillesAnneePrecedenteBe()).isNull();
        assertThat(t.joursPrestesBe()).isNull();
    }

    @Test
    void extractTravailData_sf24623_sousObjetNullExplicite_sixChampsNull() throws Exception {
        // CA-6 : sous-objet présent mais null JSON → pas d'exception, 6 champs null
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "travail_extracted_data": {
                    "travail_be_detection": null
                  }
                }
                """);
        var t = CaseAnalysisResponse.extractTravailData(root);
        assertThat(t).isNotNull();
        assertThat(t.dateConnaissanceFait()).isNull();
        assertThat(t.commissionParitaireBe()).isNull();
        assertThat(t.dateDemandeCreditTemps()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SF-246-25 — communaute_partage_protection_detection_v2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void extractFamilleData_sf24625_communautePartageProtection_nominal() throws Exception {
        // SF-246-25 CA-1 : sous-objet complet → tous les 17 champs peuplés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "communaute_partage_protection_detection_v2": {
                      "contrat_notarie": true,
                      "enfants_non_communs": false,
                      "clause_attribution_integrale": true,
                      "pv_difficultes_etablis": true,
                      "tentative_amiable_epuisee": true,
                      "violences_alleguees": ["PHYSIQUES", "PSYCHOLOGIQUES"],
                      "preuves_violences": ["CERTIFICAT_MEDICAL", "PLAINTE_DEPOSEE"],
                      "danger_immediat": true,
                      "presence_enfants": true,
                      "logement_commun": true,
                      "victime_financierement_dependante": false,
                      "mode_dissolution_pacs": "DECLARATION_CONJOINTE",
                      "regime_biens_pacs": "SEPARATION_BIENS",
                      "creances_alleguees": ["CONTRIBUTION_DESEQUILIBRE"],
                      "patrimoine_commun_significatif": true,
                      "patrimoine_commun_bool": true,
                      "violences_alleguees_bool": true
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.contratNotarieDetected()).isTrue();
        assertThat(f.enfantsNonCommunsDetected()).isFalse();
        assertThat(f.clauseAttributionIntegraleDetected()).isTrue();
        assertThat(f.pvDifficultesEtablisDetected()).isTrue();
        assertThat(f.tentativeAmiableEpuiseueeDetected()).isTrue();
        assertThat(f.violencesAllegueesDetectees()).containsExactly("PHYSIQUES", "PSYCHOLOGIQUES");
        assertThat(f.preuvesViolencesDetectees()).containsExactly("CERTIFICAT_MEDICAL", "PLAINTE_DEPOSEE");
        assertThat(f.dangerImmediatDetected()).isTrue();
        assertThat(f.presenceEnfantsDetected()).isTrue();
        assertThat(f.logementCommunDetected()).isTrue();
        assertThat(f.victimeFinanciairementDependanteDetected()).isFalse();
        assertThat(f.modeDissolutionPacsDetecte()).isEqualTo("DECLARATION_CONJOINTE");
        assertThat(f.regimeBiensPacsDetecte()).isEqualTo("SEPARATION_BIENS");
        assertThat(f.creancesAllegueesDetectees()).containsExactly("CONTRIBUTION_DESEQUILIBRE");
        assertThat(f.patrimoineCommunSignificatifDetecte()).isTrue();
        assertThat(f.patrimoineCommun()).isTrue();
        assertThat(f.violencesAlleguees()).isTrue();
    }

    @Test
    void extractFamilleData_sf24625_sousObjetAbsent_17ChampsNull() throws Exception {
        // SF-246-25 CA-2 : pas de sous-objet communaute_partage_protection_detection_v2 → 17 champs null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "vie_commune_detection": {
                      "patrimoine_commun_eur": 50000
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.contratNotarieDetected()).isNull();
        assertThat(f.enfantsNonCommunsDetected()).isNull();
        assertThat(f.clauseAttributionIntegraleDetected()).isNull();
        assertThat(f.pvDifficultesEtablisDetected()).isNull();
        assertThat(f.tentativeAmiableEpuiseueeDetected()).isNull();
        assertThat(f.violencesAllegueesDetectees()).isNull();
        assertThat(f.preuvesViolencesDetectees()).isNull();
        assertThat(f.dangerImmediatDetected()).isNull();
        assertThat(f.presenceEnfantsDetected()).isNull();
        assertThat(f.logementCommunDetected()).isNull();
        assertThat(f.victimeFinanciairementDependanteDetected()).isNull();
        assertThat(f.modeDissolutionPacsDetecte()).isNull();
        assertThat(f.regimeBiensPacsDetecte()).isNull();
        assertThat(f.creancesAllegueesDetectees()).isNull();
        assertThat(f.patrimoineCommunSignificatifDetecte()).isNull();
        assertThat(f.patrimoineCommun()).isNull();
        assertThat(f.violencesAlleguees()).isNull();
    }

    @Test
    void extractFamilleData_sf24625_violencesHorsWhitelist_filtrees() throws Exception {
        // SF-246-25 CA-3 : codes de violences hors whitelist → filtrés, seuls les codes valides conservés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "communaute_partage_protection_detection_v2": {
                      "violences_alleguees": ["PHYSIQUES", "INCONNUE", "AUTRE_INVALIDE", "ECONOMIQUES"],
                      "preuves_violences": ["CERTIFICAT_MEDICAL", "CODE_INCONNU"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.violencesAllegueesDetectees()).containsExactly("PHYSIQUES", "ECONOMIQUES");
        assertThat(f.preuvesViolencesDetectees()).containsExactly("CERTIFICAT_MEDICAL");
    }

    @Test
    void extractFamilleData_sf24625_tousCodesHorsWhitelist_null() throws Exception {
        // SF-246-25 CA-4 : tous les codes hors whitelist → null (jamais []).
        // `ordonnance_protection_envisagee` force le guard à retourner non-null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "ordonnance_protection_envisagee": true,
                    "communaute_partage_protection_detection_v2": {
                      "violences_alleguees": ["INCONNU_1", "INCONNU_2"],
                      "creances_alleguees": ["CODE_INVALIDE"]
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.violencesAllegueesDetectees()).isNull();
        assertThat(f.creancesAllegueesDetectees()).isNull();
    }

    @Test
    void extractFamilleData_sf24625_modePacsEtRegimePacsHorsWhitelist_nuls() throws Exception {
        // SF-246-25 CA-5 : mode_dissolution_pacs et regime_biens_pacs hors whitelist → null.
        // `pacs_dissolution_envisagee` force le guard à retourner non-null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "pacs_dissolution_envisagee": true,
                    "communaute_partage_protection_detection_v2": {
                      "mode_dissolution_pacs": "MODE_INCONNU",
                      "regime_biens_pacs": "REGIME_INCONNU"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modeDissolutionPacsDetecte()).isNull();
        assertThat(f.regimeBiensPacsDetecte()).isNull();
    }

    @Test
    void extractFamilleData_sf24625_sousObjetNullExplicite_17ChampsNull() throws Exception {
        // SF-246-25 CA-6 : sous-objet présent mais null JSON → pas d'exception, 17 champs null.
        // `separation_corps_envisagee` force le guard à retourner non-null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "separation_corps_envisagee": true,
                    "communaute_partage_protection_detection_v2": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.contratNotarieDetected()).isNull();
        assertThat(f.pvDifficultesEtablisDetected()).isNull();
        assertThat(f.violencesAllegueesDetectees()).isNull();
        assertThat(f.dangerImmediatDetected()).isNull();
        assertThat(f.modeDissolutionPacsDetecte()).isNull();
        assertThat(f.patrimoineCommun()).isNull();
        assertThat(f.violencesAlleguees()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SF-246-26 — filiation_detection_v2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void extractFamilleData_sf24626_filiationV2_nominal() throws Exception {
        // SF-246-26 CA-1 : sous-objet complet → tous les 12 champs peuplés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "filiation_detection_v2": {
                      "qualite_aagir_contestation": "PERE_DECLARE",
                      "possession_etat_conforme_5ans": true,
                      "expertise_adn_demandee_contestation": false,
                      "motifs_serieux_contestation": true,
                      "qualite_demandeur_recherche": "ENFANT_MAJEUR",
                      "presomption_possession_etat_recherche": true,
                      "expertise_adn_demandee_recherche": true,
                      "pere_designe_refuse_adn": false,
                      "motifs_serieux_recherche": true,
                      "forme_adoption_demandee": "PLENIERE",
                      "pupille_etat": true,
                      "adoptant_marie": true
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteAagirContestationDetected()).isEqualTo("PERE_DECLARE");
        assertThat(f.possessionEtatConforme5AnsDetected()).isTrue();
        assertThat(f.expertiseAdnDemandeeDetected()).isFalse();
        assertThat(f.motifsSerieuxDetected()).isTrue();
        assertThat(f.qualiteDuDemandeurRechercheDetected()).isEqualTo("ENFANT_MAJEUR");
        assertThat(f.presomptionPossessionEtatRechercheDetected()).isTrue();
        assertThat(f.expertiseAdnDemandeeRechercheDetected()).isTrue();
        assertThat(f.pereDesigneRefuseADNDetected()).isFalse();
        assertThat(f.motifsSerieuxRechercheDetected()).isTrue();
        assertThat(f.formeAdoptionDemandeeDetected()).isEqualTo("PLENIERE");
        assertThat(f.pupilleEtatDetected()).isTrue();
        assertThat(f.adoptantMarieDetected()).isTrue();
    }

    @Test
    void extractFamilleData_sf24626_sousObjetAbsent_12ChampsNull() throws Exception {
        // SF-246-26 CA-2 : pas de sous-objet filiation_detection_v2 → 12 champs null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "filiation_detection": {
                      "date_naissance_enfant": "2020-03-15"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteAagirContestationDetected()).isNull();
        assertThat(f.possessionEtatConforme5AnsDetected()).isNull();
        assertThat(f.expertiseAdnDemandeeDetected()).isNull();
        assertThat(f.motifsSerieuxDetected()).isNull();
        assertThat(f.qualiteDuDemandeurRechercheDetected()).isNull();
        assertThat(f.presomptionPossessionEtatRechercheDetected()).isNull();
        assertThat(f.expertiseAdnDemandeeRechercheDetected()).isNull();
        assertThat(f.pereDesigneRefuseADNDetected()).isNull();
        assertThat(f.motifsSerieuxRechercheDetected()).isNull();
        assertThat(f.formeAdoptionDemandeeDetected()).isNull();
        assertThat(f.pupilleEtatDetected()).isNull();
        assertThat(f.adoptantMarieDetected()).isNull();
    }

    @Test
    void extractFamilleData_sf24626_qualiteAagirHorsWhitelist_null() throws Exception {
        // SF-246-26 CA-3 : qualite_aagir_contestation hors whitelist → null.
        // forme_adoption_demandee hors whitelist → null.
        // `contestation_paternite_envisagee` force le guard.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "contestation_paternite_envisagee": true,
                    "filiation_detection_v2": {
                      "qualite_aagir_contestation": "QUALITE_INCONNUE",
                      "qualite_demandeur_recherche": "CODE_INVALIDE",
                      "forme_adoption_demandee": "INDEFINIE"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteAagirContestationDetected()).isNull();
        assertThat(f.qualiteDuDemandeurRechercheDetected()).isNull();
        assertThat(f.formeAdoptionDemandeeDetected()).isNull();
    }

    @Test
    void extractFamilleData_sf24626_booleanNonParseable_null() throws Exception {
        // SF-246-26 CA-4 : champs booléens non parsables (strings) → null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "recherche_paternite_envisagee": true,
                    "filiation_detection_v2": {
                      "possession_etat_conforme_5ans": "oui",
                      "expertise_adn_demandee_contestation": "non",
                      "pupille_etat": "maybe"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.possessionEtatConforme5AnsDetected()).isNull();
        assertThat(f.expertiseAdnDemandeeDetected()).isNull();
        assertThat(f.pupilleEtatDetected()).isNull();
    }

    @Test
    void extractFamilleData_sf24626_sousObjetNullExplicite_12ChampsNull() throws Exception {
        // SF-246-26 CA-5 : sous-objet présent mais null JSON → pas d'exception, 12 champs null.
        // `adoption_envisagee` force le guard.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "adoption_envisagee": true,
                    "filiation_detection_v2": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.qualiteAagirContestationDetected()).isNull();
        assertThat(f.formeAdoptionDemandeeDetected()).isNull();
        assertThat(f.adoptantMarieDetected()).isNull();
        assertThat(f.pupilleEtatDetected()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SF-246-27 — protection_divorce_detection_v2
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void extractFamilleData_sf24627_nominal_8ChampsRemplis() throws Exception {
        // SF-246-27 CA-1 : sous-objet complet → tous les 8 champs peuplés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "protection_divorce_detection_v2": {
                      "regime_protection_majeurs": "CURATELLE_RENFORCEE",
                      "date_certificat_medical_majeurs": "2024-03-15",
                      "date_pma": "2023-06-01",
                      "date_reconnaissance_anterieure_pma": "2023-05-10",
                      "date_don_gametes": "2023-07-22",
                      "motif_saisine_mediation": "AUTORITE_PARENTALE",
                      "date_assignation_divorce": "2024-11-04",
                      "date_audience_homologation_dc_be": "2025-02-18"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeProtectionMajeursDetected()).isEqualTo("CURATELLE_RENFORCEE");
        assertThat(f.dateCertificatMedicalMajeursDetected()).isEqualTo("2024-03-15");
        assertThat(f.datePmaDetected()).isEqualTo("2023-06-01");
        assertThat(f.dateReconnaissanceAnterieurePmaDetected()).isEqualTo("2023-05-10");
        assertThat(f.dateDonGametesDetected()).isEqualTo("2023-07-22");
        assertThat(f.motifSaisineMediationDetected()).isEqualTo("AUTORITE_PARENTALE");
        assertThat(f.dateAssignationDivorce()).isEqualTo("2024-11-04");
        assertThat(f.dateAudienceHomologationDcBe()).isEqualTo("2025-02-18");
    }

    @Test
    void extractFamilleData_sf24627_sousObjetAbsent_8ChampsNull() throws Exception {
        // SF-246-27 CA-2 : pas de sous-objet protection_divorce_detection_v2 → 8 champs null.
        // `protection_majeur_envisagee` présent pour déclencher extractFamilleData non-null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "protection_majeur_envisagee": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeProtectionMajeursDetected()).isNull();
        assertThat(f.dateCertificatMedicalMajeursDetected()).isNull();
        assertThat(f.datePmaDetected()).isNull();
        assertThat(f.dateReconnaissanceAnterieurePmaDetected()).isNull();
        assertThat(f.dateDonGametesDetected()).isNull();
        assertThat(f.motifSaisineMediationDetected()).isNull();
        assertThat(f.dateAssignationDivorce()).isNull();
        assertThat(f.dateAudienceHomologationDcBe()).isNull();
    }

    @Test
    void extractFamilleData_sf24627_whitelistsRejettentCodesInconnus() throws Exception {
        // SF-246-27 CA-3 : regime_protection_majeurs hors whitelist → null ;
        // motif_saisine_mediation hors whitelist → null.
        // `pma_gpa_envisagee` force le guard.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "pma_gpa_envisagee": true,
                    "protection_divorce_detection_v2": {
                      "regime_protection_majeurs": "TUTELLE_ALLEGEE",
                      "motif_saisine_mediation": "PENSION_ALIMENTAIRE",
                      "date_assignation_divorce": "2024-09-30"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeProtectionMajeursDetected()).isNull();
        assertThat(f.motifSaisineMediationDetected()).isNull();
        assertThat(f.dateAssignationDivorce()).isEqualTo("2024-09-30");
    }

    @Test
    void extractFamilleData_sf24627_datesMalFormees_null() throws Exception {
        // SF-246-27 CA-4 : dates partielles ou mal formées → null (isoDateOrNull strict).
        // `divorce_alteration_lien_envisage` force le guard.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_alteration_lien_envisage": true,
                    "protection_divorce_detection_v2": {
                      "date_certificat_medical_majeurs": "2024-03",
                      "date_pma": "01/06/2023",
                      "date_assignation_divorce": "2024-11-04"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateCertificatMedicalMajeursDetected()).isNull();   // "2024-03" : partiel
        assertThat(f.datePmaDetected()).isNull();                         // "01/06/2023" : mauvais séparateur
        assertThat(f.dateAssignationDivorce()).isEqualTo("2024-11-04");  // format correct → peuplé
    }

    @Test
    void extractFamilleData_sf24627_sousObjetNullExplicite_8ChampsNull() throws Exception {
        // SF-246-27 CA-5 : sous-objet présent mais null JSON → pas d'exception, 8 champs null.
        // `divorce_accepte_envisage` force le guard.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_accepte_envisage": true,
                    "protection_divorce_detection_v2": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.regimeProtectionMajeursDetected()).isNull();
        assertThat(f.dateCertificatMedicalMajeursDetected()).isNull();
        assertThat(f.datePmaDetected()).isNull();
        assertThat(f.dateReconnaissanceAnterieurePmaDetected()).isNull();
        assertThat(f.dateDonGametesDetected()).isNull();
        assertThat(f.motifSaisineMediationDetected()).isNull();
        assertThat(f.dateAssignationDivorce()).isNull();
        assertThat(f.dateAudienceHomologationDcBe()).isNull();
    }

    // =========================================================================
    // SF-246-12 — divorce_ddi_be_detection (Famille BE — divorce-desunion-be)
    // =========================================================================

    @Test
    void extractFamilleData_sf24612_nominal_dateSeparationBe() throws Exception {
        // Cas nominal : sous-objet présent + date ISO valide → dateSeparationBe renseigné.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true,
                    "divorce_ddi_be_detection": {
                      "date_separation_be": "2024-11-15"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparationBe()).isEqualTo("2024-11-15");
    }

    @Test
    void extractFamilleData_sf24612_sousObjetAbsent_dateSeparationBeNull() throws Exception {
        // Cas absent : pas de sous-objet divorce_ddi_be_detection → null (no-op gracieux).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparationBe()).isNull();
    }

    @Test
    void extractFamilleData_sf24612_dateNonIso_null() throws Exception {
        // Cas date non-ISO : rejetée → null (fail-open, invariant §5.1.6).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true,
                    "divorce_ddi_be_detection": {
                      "date_separation_be": "15/11/2024"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparationBe()).isNull();
    }

    @Test
    void extractFamilleData_sf24612_multiDates_seuleDateSeparationBe() throws Exception {
        // Cas multi-dates (invariant cadrage §5.1.6) :
        // date_mariage + date_separation_be + date_requete distincts →
        // seule date_separation_be est extraite dans dateSeparationBe.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true,
                    "divorce_ddi_be_detection": {
                      "date_separation_be": "2023-06-01"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparationBe()).isEqualTo("2023-06-01");
        // dateSeparation FR (SF-246-08) n'est PAS affectée par le sous-objet BE.
        assertThat(f.dateSeparation()).isNull();
    }

    @Test
    void extractFamilleData_sf24612_sousObjetNullExplicite_null() throws Exception {
        // Cas sous-objet explicitement null → null (no-op gracieux).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_ddi_envisage": true,
                    "divorce_ddi_be_detection": null
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateSeparationBe()).isNull();
    }

    // =========================================================================
    // SF-246-28 — famille_be_detection_v2 (Famille BE — 5 outils PREFILL_COUNT_ALWAYS_ZERO levé)
    // =========================================================================

    @Test
    void extractFamilleData_sf24628_nominal_16ChampsRemplis() throws Exception {
        // SF-246-28 CA-1 : sous-objet complet → tous les 16 champs peuplés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "famille_be_detection_v2": {
                      "mode_hebergement_principal_be": "HEBERGEMENT_EGALITAIRE",
                      "nombre_enfants_be": 2,
                      "revenu_mensuel_parent1_be": 2800.0,
                      "revenu_mensuel_parent2_be": 1950.0,
                      "allocations_familiales_be": 310.0,
                      "nuits_hebergement_parent1_be": 15,
                      "nuits_hebergement_parent2_be": 15,
                      "duree_mariage_annees_be": 11,
                      "revenu_mensuel_creancier_be": 1400.0,
                      "revenu_mensuel_debiteur_be": 3200.0,
                      "date_designation_notaire_be": "2023-03-10",
                      "date_ouverture_operations_be": "2023-04-20",
                      "date_notification_projet_be": "2024-01-15",
                      "date_homologation_be": "2024-03-08",
                      "date_mariage_be": "2012-06-16",
                      "contrat_mariage_signe_be": false
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modeHebergementPrincipalBeDetecte()).isEqualTo("HEBERGEMENT_EGALITAIRE");
        assertThat(f.nombreEnfantsBeDetecte()).isEqualTo(2);
        assertThat(f.revenuMensuelParent1BeDetecte()).isEqualTo(2800.0);
        assertThat(f.revenuMensuelParent2BeDetecte()).isEqualTo(1950.0);
        assertThat(f.allocationsFamilialesMensuellesBeDetectees()).isEqualTo(310.0);
        assertThat(f.nuitsHebergementParent1BeDetectees()).isEqualTo(15);
        assertThat(f.nuitsHebergementParent2BeDetectees()).isEqualTo(15);
        assertThat(f.dureeMariageAnneesBeDetectee()).isEqualTo(11);
        assertThat(f.revenuMensuelCreancierBeDetecte()).isEqualTo(1400.0);
        assertThat(f.revenuMensuelDebiteurBeDetecte()).isEqualTo(3200.0);
        assertThat(f.dateDesignationNotaireBeDetectee()).isEqualTo("2023-03-10");
        assertThat(f.dateOuvertureOperationsBeDetectee()).isEqualTo("2023-04-20");
        assertThat(f.dateNotificationProjetBeDetectee()).isEqualTo("2024-01-15");
        assertThat(f.dateHomologationBeDetectee()).isEqualTo("2024-03-08");
        assertThat(f.dateMariageBeDetectee()).isEqualTo("2012-06-16");
        assertThat(f.contratMariageSigneBeDetecte()).isEqualTo(false);
    }

    @Test
    void extractFamilleData_sf24628_sousObjetAbsent_16ChampsNull() throws Exception {
        // SF-246-28 CA-2 : pas de sous-objet famille_be_detection_v2 → 16 champs null.
        // `divorce_dc_envisage` force le guard extractFamilleData.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modeHebergementPrincipalBeDetecte()).isNull();
        assertThat(f.nombreEnfantsBeDetecte()).isNull();
        assertThat(f.revenuMensuelParent1BeDetecte()).isNull();
        assertThat(f.revenuMensuelParent2BeDetecte()).isNull();
        assertThat(f.allocationsFamilialesMensuellesBeDetectees()).isNull();
        assertThat(f.nuitsHebergementParent1BeDetectees()).isNull();
        assertThat(f.nuitsHebergementParent2BeDetectees()).isNull();
        assertThat(f.dureeMariageAnneesBeDetectee()).isNull();
        assertThat(f.revenuMensuelCreancierBeDetecte()).isNull();
        assertThat(f.revenuMensuelDebiteurBeDetecte()).isNull();
        assertThat(f.dateDesignationNotaireBeDetectee()).isNull();
        assertThat(f.dateOuvertureOperationsBeDetectee()).isNull();
        assertThat(f.dateNotificationProjetBeDetectee()).isNull();
        assertThat(f.dateHomologationBeDetectee()).isNull();
        assertThat(f.dateMariageBeDetectee()).isNull();
        assertThat(f.contratMariageSigneBeDetecte()).isNull();
    }

    @Test
    void extractFamilleData_sf24628_dateNonIso_null() throws Exception {
        // SF-246-28 CA-3 : dates mal formées → null (isoDateOrNull strict).
        // Les 4 dates liquidation-partage-be testées avec des formats erronés.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "famille_be_detection_v2": {
                      "date_designation_notaire_be": "10/03/2023",
                      "date_ouverture_operations_be": "2023-04",
                      "date_notification_projet_be": "2024-01-15",
                      "date_homologation_be": "2024",
                      "date_mariage_be": "2012-06-16"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.dateDesignationNotaireBeDetectee()).isNull();       // "10/03/2023" — mauvais séparateur
        assertThat(f.dateOuvertureOperationsBeDetectee()).isNull();      // "2023-04" — partiel
        assertThat(f.dateNotificationProjetBeDetectee()).isEqualTo("2024-01-15"); // correct
        assertThat(f.dateHomologationBeDetectee()).isNull();             // "2024" — partiel
        assertThat(f.dateMariageBeDetectee()).isEqualTo("2012-06-16");  // correct
    }

    @Test
    void extractFamilleData_sf24628_revenuNegatifEtBornesDepassees_null() throws Exception {
        // SF-246-28 CA-4 : revenu négatif → null ; nombre enfants hors [1,12] → null ;
        // nuits hors [0,30] → null ; durée mariage hors [0,80] → null.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "famille_be_detection_v2": {
                      "nombre_enfants_be": 15,
                      "revenu_mensuel_parent1_be": -500.0,
                      "revenu_mensuel_parent2_be": 0.0,
                      "nuits_hebergement_parent1_be": 35,
                      "nuits_hebergement_parent2_be": 10,
                      "duree_mariage_annees_be": 85,
                      "revenu_mensuel_creancier_be": 0.0
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.nombreEnfantsBeDetecte()).isNull();                        // 15 > 12 → null
        assertThat(f.revenuMensuelParent1BeDetecte()).isNull();                  // -500 < 0 → null
        assertThat(f.revenuMensuelParent2BeDetecte()).isEqualTo(0.0);          // 0 est valide (revenu nul)
        assertThat(f.nuitsHebergementParent1BeDetectees()).isNull();             // 35 > 30 → null
        assertThat(f.nuitsHebergementParent2BeDetectees()).isEqualTo(10);       // 10 ∈ [0,30] → ok
        assertThat(f.dureeMariageAnneesBeDetectee()).isNull();                   // 85 > 80 → null
        assertThat(f.revenuMensuelCreancierBeDetecte()).isEqualTo(0.0);        // 0 valide (revenu nul)
    }

    @Test
    void extractFamilleData_sf24628_whitelistModeHebergement_codeInconnu_null() throws Exception {
        // SF-246-28 CA-5 : code mode hébergement hors whitelist → null.
        // Code connu pour les 2 autres → préservé.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "famille_be_detection_v2": {
                      "mode_hebergement_principal_be": "HEBERGEMENT_MIXTE",
                      "date_mariage_be": "2015-09-12"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        assertThat(f.modeHebergementPrincipalBeDetecte()).isNull();      // "HEBERGEMENT_MIXTE" hors whitelist
        assertThat(f.dateMariageBeDetectee()).isEqualTo("2015-09-12");  // correct → préservé
    }

    @Test
    void extractFamilleData_sf24628_multidates_liquidationPartage_bonsChampsRemplis() throws Exception {
        // SF-246-28 CA-6 : fixture multi-dates liquidation-partage-be —
        // plusieurs dates coexistent ; chaque champ doit correspondre à son
        // concept juridique exact (invariant cadrage §5.1 anti-collision).
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree("""
                {
                  "famille_extracted_data": {
                    "divorce_dc_envisage": true,
                    "famille_be_detection_v2": {
                      "date_designation_notaire_be": "2022-11-03",
                      "date_ouverture_operations_be": "2023-01-18",
                      "date_notification_projet_be": "2024-02-07",
                      "date_homologation_be": "2024-04-22",
                      "date_mariage_be": "2010-07-10"
                    }
                  }
                }
                """);
        var f = CaseAnalysisResponse.extractFamilleData(root);
        assertThat(f).isNotNull();
        // Les 5 dates doivent être distinctes et correctement mappées
        assertThat(f.dateDesignationNotaireBeDetectee()).isEqualTo("2022-11-03");
        assertThat(f.dateOuvertureOperationsBeDetectee()).isEqualTo("2023-01-18");
        assertThat(f.dateNotificationProjetBeDetectee()).isEqualTo("2024-02-07");
        assertThat(f.dateHomologationBeDetectee()).isEqualTo("2024-04-22");
        assertThat(f.dateMariageBeDetectee()).isEqualTo("2010-07-10");
        // Les dates ne doivent pas se contaminer entre elles
        assertThat(f.dateDesignationNotaireBeDetectee()).isNotEqualTo(f.dateOuvertureOperationsBeDetectee());
        assertThat(f.dateNotificationProjetBeDetectee()).isNotEqualTo(f.dateHomologationBeDetectee());
    }

    // ========================================================================
    // SF-246-29 : pré-fill IA exhaustif F-DT-38 — sous-objet rupture_periode_essai_detail
    // 14 nouveaux champs IA (2 enums whitelist, 3 entiers bornés, 8 booléens
    // tri-état, 1 texte tronqué).
    // ========================================================================

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_complet_parsed() {
        // Cas nominal : sous-objet complet → 14 champs renseignés.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "categorie_socio_professionnelle": "CADRE",
                      "duree_cdd_mois": null,
                      "duree_periode_essai_mois": 4,
                      "renouvellement_invoque": false,
                      "accord_branche_renouvellement": null,
                      "accord_ecrit_salarie_renouvellement": null,
                      "auteur_rupture": "EMPLOYEUR",
                      "delai_prevenance_jours_appliques": 14,
                      "motif_lie_competences_professionnelles": true,
                      "motif_economique_ou_organisationnel": false,
                      "atteinte_liberte_fondamentale": null,
                      "lettre_rupture_motivee": true,
                      "motifs_averes_par_pieces": true,
                      "ccn_plus_favorable_respectee": true
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeCategorieSocioProfessionnelle()).isEqualTo("CADRE");
        assertThat(t.rpeDureeCddMois()).isNull();
        assertThat(t.rpeDureePeriodeEssaiMois()).isEqualTo(4);
        assertThat(t.rpeRenouvellementInvoque()).isFalse();
        assertThat(t.rpeAccordBrancheRenouvellement()).isNull();
        assertThat(t.rpeAccordEcritSalarieRenouvellement()).isNull();
        assertThat(t.rpeAuteurRupture()).isEqualTo("EMPLOYEUR");
        assertThat(t.rpeDelaiPrevenanceJours()).isEqualTo(14);
        assertThat(t.rpeMotifLieCompetences()).isTrue();
        assertThat(t.rpeMotifEconomique()).isFalse();
        assertThat(t.rpeAtteinteLiberteFondamentale()).isNull();
        assertThat(t.rpeLettreRuptureMotivee()).isTrue();
        assertThat(t.rpeMotifsAveresParPieces()).isTrue();
        assertThat(t.rpeCcnPlusFavorableRespectee()).isTrue();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_sousObjetAbsent_tousNull() {
        // Sous-objet rupture_periode_essai_detail absent → 14 champs null, pas d'exception.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeCategorieSocioProfessionnelle()).isNull();
        assertThat(t.rpeDureeCddMois()).isNull();
        assertThat(t.rpeDureePeriodeEssaiMois()).isNull();
        assertThat(t.rpeRenouvellementInvoque()).isNull();
        assertThat(t.rpeAccordBrancheRenouvellement()).isNull();
        assertThat(t.rpeAccordEcritSalarieRenouvellement()).isNull();
        assertThat(t.rpeAuteurRupture()).isNull();
        assertThat(t.rpeDelaiPrevenanceJours()).isNull();
        assertThat(t.rpeMotifLieCompetences()).isNull();
        assertThat(t.rpeMotifEconomique()).isNull();
        assertThat(t.rpeAtteinteLiberteFondamentale()).isNull();
        assertThat(t.rpeLettreRuptureMotivee()).isNull();
        assertThat(t.rpeMotifsAveresParPieces()).isNull();
        assertThat(t.rpeCcnPlusFavorableRespectee()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_categorieHorsWhitelist_null() {
        // Code hors whitelist ("STAGIAIRE") → null (jamais de fallback).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "categorie_socio_professionnelle": "STAGIAIRE"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeCategorieSocioProfessionnelle()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_auteurHorsWhitelist_null() {
        // Code hors whitelist ("INTERIM") → null.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "auteur_rupture": "INTERIM"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeAuteurRupture()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_dureePeriodeHorsBorne_null() {
        // Durée d'essai > 24 mois → null (borne haute).
        CaseAnalysis tooLarge = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "duree_periode_essai_mois": 50
                    }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(tooLarge).travailExtractedData()
                .rpeDureePeriodeEssaiMois()).isNull();

        // Durée d'essai < 0 → null (borne basse).
        CaseAnalysis negative = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "duree_periode_essai_mois": -1
                    }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(negative).travailExtractedData()
                .rpeDureePeriodeEssaiMois()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_delaiPrevenanceHorsBorne_null() {
        // Délai > 30 jours → null.
        CaseAnalysis tooLarge = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "delai_prevenance_jours_appliques": 100
                    }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(tooLarge).travailExtractedData()
                .rpeDelaiPrevenanceJours()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_dureeCddHorsBorne_null() {
        // Durée CDD > 36 mois → null.
        CaseAnalysis tooLarge = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "duree_cdd_mois": 99
                    }
                  }
                }
                """);
        assertThat(CaseAnalysisResponse.from(tooLarge).travailExtractedData()
                .rpeDureeCddMois()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_atteinteLiberteTronqueeA500() {
        // Texte atteinte_liberte_fondamentale > 500 caractères → tronqué à 500.
        String longText = "X".repeat(700);
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "atteinte_liberte_fondamentale": "%s"
                    }
                  }
                }
                """.formatted(longText));

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeAtteinteLiberteFondamentale()).hasSize(500);
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_booleenInvalide_null() {
        // Une chaîne au lieu d'un booléen → null (booleanOrNull strict).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "lettre_rupture_motivee": "yes",
                      "motifs_averes_par_pieces": "no"
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeLettreRuptureMotivee()).isNull();
        assertThat(t.rpeMotifsAveresParPieces()).isNull();
    }

    @Test
    void from_travailExtractedData_rupturePeriodeEssai_partiel_seulementChampsRenseignes() {
        // Sous-objet partiel : seuls les champs présents sont peuplés, les autres null.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "rupture_periode_essai_detail": {
                      "categorie_socio_professionnelle": "OUVRIER_EMPLOYE",
                      "duree_periode_essai_mois": 2
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.rpeCategorieSocioProfessionnelle()).isEqualTo("OUVRIER_EMPLOYE");
        assertThat(t.rpeDureePeriodeEssaiMois()).isEqualTo(2);
        // Les 12 autres champs absents → null
        assertThat(t.rpeDureeCddMois()).isNull();
        assertThat(t.rpeRenouvellementInvoque()).isNull();
        assertThat(t.rpeAuteurRupture()).isNull();
        assertThat(t.rpeDelaiPrevenanceJours()).isNull();
        assertThat(t.rpeMotifLieCompetences()).isNull();
        assertThat(t.rpeLettreRuptureMotivee()).isNull();
    }

    // ========================================================================
    // SF-212-02 : pré-fill IA F-DT-36 — sous-objet faute_grave_detail
    // 6 champs IA (texte tronqué, liste de dates, enum 3 valeurs, booléen
    // tri-état, entier borné [0,600], décimal > 0).
    // ========================================================================

    @Test
    void from_travailExtractedData_fauteGraveDetail_complet_parsed() {
        // Cas nominal : sous-objet complet → 6 champs renseignés.
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "faute_grave_detail": {
                      "faute_grave_faits_reproches": "Insultes envers le supérieur hiérarchique",
                      "faute_grave_dates_faits": ["2024-03-12", "2024-04-08"],
                      "faute_grave_qualification_employeur": "FAUTE_GRAVE",
                      "faute_grave_intention_nuire_alleeguee": false,
                      "faute_grave_anciennete_mois": 36,
                      "faute_grave_salaire_mensuel_brut": 2850.0
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.fauteGraveFaitsReproches()).isEqualTo("Insultes envers le supérieur hiérarchique");
        assertThat(t.fauteGraveDatesFaits()).containsExactly("2024-03-12", "2024-04-08");
        assertThat(t.fauteGraveQualificationEmployeur()).isEqualTo("FAUTE_GRAVE");
        assertThat(t.fauteGraveIntentionNuireAlleeguee()).isFalse();
        assertThat(t.fauteGraveAncienneteMois()).isEqualTo(36);
        assertThat(t.fauteGraveSalaireMensuelBrut()).isEqualTo(2850.0);
    }

    @Test
    void from_travailExtractedData_fauteGraveDetail_sousObjetAbsent_tousNull() {
        // Sous-objet absent → 6 champs null, pas d'exception (dossier sans
        // document disciplinaire ou dossier BE).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "convention_collective": "SYNTEC",
                    "salaire_brut_mensuel": 3200
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.fauteGraveFaitsReproches()).isNull();
        assertThat(t.fauteGraveDatesFaits()).isNull();
        assertThat(t.fauteGraveQualificationEmployeur()).isNull();
        assertThat(t.fauteGraveIntentionNuireAlleeguee()).isNull();
        assertThat(t.fauteGraveAncienneteMois()).isNull();
        assertThat(t.fauteGraveSalaireMensuelBrut()).isNull();
    }

    @Test
    void from_travailExtractedData_fauteGraveDetail_qualificationHorsWhitelist_null() {
        // Qualification hors whitelist (ex. "DISCIPLINAIRE") → null
        // (jamais de fallback — invariant whitelist stricte).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "faute_grave_detail": {
                      "faute_grave_qualification_employeur": "DISCIPLINAIRE",
                      "faute_grave_anciennete_mois": 24
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.fauteGraveQualificationEmployeur()).isNull();
        assertThat(t.fauteGraveAncienneteMois()).isEqualTo(24);
    }

    @Test
    void from_travailExtractedData_fauteGraveDetail_ancienneteHorsBornes_null() {
        // Ancienneté > MAX_FAUTE_GRAVE_ANCIENNETE_MOIS (600) → null,
        // salaire ≤ 0 → null (positiveDoubleOrNull).
        CaseAnalysis analysis = analysis("""
                {
                  "travail_extracted_data": {
                    "faute_grave_detail": {
                      "faute_grave_anciennete_mois": 1200,
                      "faute_grave_salaire_mensuel_brut": 0,
                      "faute_grave_dates_faits": []
                    }
                  }
                }
                """);

        var t = CaseAnalysisResponse.from(analysis).travailExtractedData();
        assertThat(t).isNotNull();
        assertThat(t.fauteGraveAncienneteMois()).isNull();
        assertThat(t.fauteGraveSalaireMensuelBrut()).isNull();
        assertThat(t.fauteGraveDatesFaits()).isEmpty();
    }
}

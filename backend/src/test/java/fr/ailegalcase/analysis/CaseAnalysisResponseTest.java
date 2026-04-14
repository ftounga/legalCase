package fr.ailegalcase.analysis;

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

    private AnalysisDocument analysisDocument(String documentName) {
        AnalysisDocument doc = new AnalysisDocument();
        doc.setAnalysisId(UUID.randomUUID());
        doc.setDocumentId(UUID.randomUUID());
        doc.setDocumentName(documentName);
        return doc;
    }
}

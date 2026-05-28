package fr.ailegalcase.casefile;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-219-27 : tests unitaires de
 * {@link InastriStatutTravailleurIndependantChecker} — couvre les
 * branches verdict (INDEPENDANT_CONFIRME, SALARIE_REQUALIFIE,
 * FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE, PRESUMPTION_GENERALE_SALARIAT,
 * A_QUALIFIER), le calcul des scores, la cohérence DIMONA / statut
 * déclaré, le drapeau mono-client, et toutes les validations
 * d'entrée.
 */
class InastriStatutTravailleurIndependantCheckerTest {

    private static InastriStatutTravailleurIndependantRequest reqIndependant() {
        return new InastriStatutTravailleurIndependantRequest(
                InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                InastriStatutTravailleurIndependantSecteur.AUTRE,
                0,
                InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                false,
                true);
    }

    private static InastriStatutTravailleurIndependantRequest reqSalarie() {
        return new InastriStatutTravailleurIndependantRequest(
                InastriStatutTravailleurIndependantVolonte.SALARIE_EXPLICITE,
                InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                InastriStatutTravailleurIndependantSecteur.AUTRE,
                0,
                InastriStatutTravailleurIndependantStatutDeclare.SALARIE_DIMONA,
                true,
                false);
    }

    @Nested
    @DisplayName("Cas nominaux")
    class NominalCases {

        @Test
        void independantConfirme_scoresIndependantMajoritaires() {
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(
                            reqIndependant());

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME);
            // 1 (vol) + 1 (temps) + 1 (travail) + 1 (controle AUCUNE = indep) = 4
            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(4);
            assertThat(result.scoreCriteresGenerauxSalarie()).isEqualTo(0);
            assertThat(result.presumptionSectorielleApplicable()).isFalse();
            assertThat(result.presumptionSectorielleDeclenchee()).isFalse();
            assertThat(result.requalificationSalariatRecommandee()).isFalse();
            assertThat(result.raison()).isEqualTo(
                    "CRITERES_GENERAUX_MAJORITAIRES_INDEPENDANCE");
            assertThat(result.baseJuridique()).contains(
                    "Loi du 27/06/1969");
            assertThat(result.baseJuridique()).contains("AR n° 38");
            assertThat(result.baseJuridique()).contains("art. 333");
        }

        @Test
        void salarieRequalifie_scoresSalarieMajoritaires() {
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(
                            reqSalarie());

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.SALARIE_REQUALIFIE);
            // 1 (vol) + 1 (temps AUCUNE) + 1 (travail AUCUNE) + 1 (controle TOTALE = sal) = 4
            assertThat(result.scoreCriteresGenerauxSalarie()).isEqualTo(4);
            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(0);
            assertThat(result.requalificationSalariatRecommandee()).isTrue();
            assertThat(result.raison()).isEqualTo(
                    "CRITERES_GENERAUX_MAJORITAIRES_SALARIAT");
        }

        @Test
        void presomptionSectorielleDeclenchee_construction_5sur9() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.CONSTRUCTION,
                            5,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            // Présomption sectorielle prime sur les scores généraux
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
            assertThat(result.presumptionSectorielleApplicable()).isTrue();
            assertThat(result.presumptionSectorielleDeclenchee()).isTrue();
            assertThat(result.requalificationSalariatRecommandee()).isTrue();
            assertThat(result.raison()).isEqualTo(
                    "PRESUMPTION_SECTORIELLE_DECLENCHEE_ART_337_2");
            assertThat(result.synthese()).contains("construction");
            assertThat(result.synthese()).contains("art. 337/2");
        }

        @Test
        void presomptionSectorielleNonDeclenchee_construction_4sur9() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.CONSTRUCTION,
                            4,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            // 4 < 5 → présomption applicable mais non déclenchée →
            // scores critères généraux décident
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME);
            assertThat(result.presumptionSectorielleApplicable()).isTrue();
            assertThat(result.presumptionSectorielleDeclenchee()).isFalse();
        }

        @Test
        void presomptionGeneraleSalariat_dimonaAbsenteSansDeclaration() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.IMPLICITE_OU_AMBIGUE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.SANS_DECLARATION,
                            false,
                            false);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.PRESUMPTION_GENERALE_SALARIAT);
            assertThat(result.presumptionGeneraleSalariatMobilisable()).isTrue();
            assertThat(result.raison()).isEqualTo(
                    "PRESUMPTION_GENERALE_ART_328_MOBILISABLE");
            assertThat(result.synthese()).contains("art. 328 § 1");
            assertThat(result.requalificationSalariatRecommandee()).isTrue();
        }

        @Test
        void aQualifier_egaliteParfaite() {
            // volonté IMPLICITE = 0/0
            // temps PARTIELLE = 0/0, travail PARTIELLE = 0/0
            // controle PARTIELLE = 0/0 → tout 0/0 mais pas de présomption générale
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.IMPLICITE_OU_AMBIGUE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.A_QUALIFIER);
            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(0);
            assertThat(result.scoreCriteresGenerauxSalarie()).isEqualTo(0);
            assertThat(result.raison()).isEqualTo(
                    "CRITERES_GENERAUX_NON_CONCLUANTS");
        }
    }

    @Nested
    @DisplayName("Drapeaux complémentaires")
    class FlagsCases {

        @Test
        void monoClientDetecte_lorsqueAutreClientAbsent() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            false);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.monoClientIndiceSalariat()).isTrue();
            assertThat(result.synthese()).contains("mono-client");
        }

        @Test
        void dimonaIncoherente_independantAvecDimona() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            true,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.dimonaCoherenteAvecStatutDeclare()).isFalse();
            assertThat(result.synthese()).contains("incohérence");
        }

        @Test
        void dimonaCoherente_salarieAvecDimona() {
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(reqSalarie());

            assertThat(result.dimonaCoherenteAvecStatutDeclare()).isTrue();
        }

        @Test
        void presomptionSectorielleNonApplicableAutreSecteur() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            9,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            // 9 critères sectoriels mais secteur AUTRE → présomption non applicable
            assertThat(result.presumptionSectorielleApplicable()).isFalse();
            assertThat(result.presumptionSectorielleDeclenchee()).isFalse();
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME);
        }

        @Test
        void presomptionSectorielleApplicableTousSecteursListes() {
            for (InastriStatutTravailleurIndependantSecteur s :
                    InastriStatutTravailleurIndependantSecteur.values()) {
                if (s == InastriStatutTravailleurIndependantSecteur.AUTRE) {
                    continue;
                }
                InastriStatutTravailleurIndependantRequest req =
                        new InastriStatutTravailleurIndependantRequest(
                                InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                                InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                                InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                                InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                                s, 0,
                                InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                                false, true);
                InastriStatutTravailleurIndependantResult result =
                        InastriStatutTravailleurIndependantChecker.analyze(req);
                assertThat(result.presumptionSectorielleApplicable())
                        .as("Secteur %s", s)
                        .isTrue();
            }
        }

        @Test
        void presomptionSectorielleDeclencheeTransport() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.TRANSPORT_DE_CHOSES,
                            7,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
            assertThat(result.synthese()).contains("transport");
        }

        @Test
        void presomptionSectorielleDeclencheeGardiennage() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.GARDIENNAGE,
                            6,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
            assertThat(result.synthese()).contains("gardiennage");
        }

        @Test
        void presomptionSectorielleDeclencheeNettoyage() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.NETTOYAGE,
                            5,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
            assertThat(result.synthese()).contains("nettoyage");
        }

        @Test
        void presomptionSectorielleDeclencheeAgriculture() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.AGRICULTURE_HORTICULTURE,
                            8,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false,
                            true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
            assertThat(result.synthese()).contains("agriculture");
        }
    }

    @Nested
    @DisplayName("Couverture verdicts détaillée")
    class VerdictsCoverage {

        @Test
        void volonteImpliciteCompte0_pourLesDeuxScores() {
            // volonté IMPLICITE = 0/0
            // temps TOTALE = 1/0
            // travail AUCUNE = 0/1
            // controle PARTIELLE = 0/0
            // → 1 vs 1 → égalité, mais présomption générale non mobilisable
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.IMPLICITE_OU_AMBIGUE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(1);
            assertThat(result.scoreCriteresGenerauxSalarie()).isEqualTo(1);
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.A_QUALIFIER);
        }

        @Test
        void presomptionGeneralePrimeSurEgalite_dimonaAbsente() {
            // score 0/0 + DIMONA absente + sans déclaration → présomption générale
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.IMPLICITE_OU_AMBIGUE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.SANS_DECLARATION,
                            false, false);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.PRESUMPTION_GENERALE_SALARIAT);
        }

        @Test
        void presomptionSectoriellePrimeSurScoresGeneraux() {
            // Tous critères généraux indépendant + secteur visé + 5/9
            // → la présomption sectorielle doit l'emporter
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.NETTOYAGE,
                            5,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(4);
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
        }

        @Test
        void scoresMixteIndependantMajoritaire() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);

            assertThat(result.scoreCriteresGenerauxIndependant()).isEqualTo(2);
            assertThat(result.scoreCriteresGenerauxSalarie()).isEqualTo(0);
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME);
        }

        @Test
        void avertissementContientAvocatBE() {
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(
                            reqIndependant());
            assertThat(result.avertissement()).contains("avocat");
            assertThat(result.avertissement()).contains("art. 333");
            assertThat(result.avertissement()).contains("art. 337/2");
        }
    }

    @Nested
    @DisplayName("Validations d'entrée")
    class ValidationCases {

        @Test
        void requeteNulle_throws() {
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Requête requise");
        }

        @Test
        void volonteNulle_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            null,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("volonteParties");
        }

        @Test
        void liberteTempsNulle_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            null,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("liberteOrganisationTemps");
        }

        @Test
        void liberteTravailNulle_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            null,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("liberteOrganisationTravail");
        }

        @Test
        void controleHierarchiqueNulle_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            null,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("controleHierarchique");
        }

        @Test
        void secteurNul_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            null,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("secteur");
        }

        @Test
        void nbCriteresSectorielsNegatif_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            -1,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nbCriteresSectorielsSalarie");
        }

        @Test
        void nbCriteresSectorielsTropGrand_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            10,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nbCriteresSectorielsSalarie");
        }

        @Test
        void nbCriteresSectorielsNul_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            null,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("nbCriteresSectorielsSalarie");
        }

        @Test
        void statutDeclareNul_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            null,
                            false, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("statutDeclare");
        }

        @Test
        void dimonaPresenteNulle_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            null, true);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dimonaPresente");
        }

        @Test
        void autreClientPresentNul_throws() {
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.AUTRE,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, null);
            assertThatThrownBy(() ->
                    InastriStatutTravailleurIndependantChecker.analyze(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("autreClientPresent");
        }

        @Test
        void nbCriteresSectoriels9Valide() {
            // 9 = borne max, doit être accepté
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantLiberte.PARTIELLE,
                            InastriStatutTravailleurIndependantSecteur.CONSTRUCTION,
                            9,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            // Ne doit pas lever
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE);
        }

        @Test
        void nbCriteresSectoriels0Valide() {
            // 0 = borne min, doit être accepté
            InastriStatutTravailleurIndependantRequest req =
                    new InastriStatutTravailleurIndependantRequest(
                            InastriStatutTravailleurIndependantVolonte.INDEPENDANT_EXPLICITE,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.TOTALE_INDEPENDANT,
                            InastriStatutTravailleurIndependantLiberte.AUCUNE_SALARIE,
                            InastriStatutTravailleurIndependantSecteur.CONSTRUCTION,
                            0,
                            InastriStatutTravailleurIndependantStatutDeclare.INDEPENDANT_INASTI,
                            false, true);
            InastriStatutTravailleurIndependantResult result =
                    InastriStatutTravailleurIndependantChecker.analyze(req);
            assertThat(result.presumptionSectorielleApplicable()).isTrue();
            assertThat(result.presumptionSectorielleDeclenchee()).isFalse();
            assertThat(result.verdict()).isEqualTo(
                    InastriStatutTravailleurIndependantVerdict.INDEPENDANT_CONFIRME);
        }
    }
}

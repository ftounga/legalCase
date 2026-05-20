# SF-252-01 — Bundle backend + frontend : 6 protections additives manquantes F-DT-38

> **Scope V1 (cette SF, 2026-05-20)** : 6 gaps "protections additives" (pattern uniforme : new boolean + new CodeAnomalie + verdict NULLE).
> **Scope V2 (F-252b à backloger)** : 8 gaps restants (CDD/INTERIM barème exact, prévenance refactor, suspensions arithmétique jours, reprise ancienneté stage/CDD, renouvellement clarification, apprentissage régime spécial, lettre motifs liants).

---

# SF-252-01 — Bundle backend + frontend : combler 6 protections nullité manquantes F-DT-38

## Identifiant
`F-252 / SF-252-01`

## Feature parente
`F-252` — Compléter F-DT-38 (rupture période d'essai)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-252-backend-gaps`

---

## Objectif

Combler les 10 angles morts juridiques de F-DT-38 identifiés par l'audit 2026-05-20 (4 critiques + 6 importants). 4 gaps "moyens" (11-14) renvoyés en follow-up F-252b si Marjolaine confirme.

Bundle backend + frontend en 1 SF (pattern PR #1135 F-DT-38) — couplage tight : `RupturePeriodeEssaiInput` ↔ `RupturePeriodeEssaiRequest` ↔ template HTML + records `TravailExtractedData` ↔ prompt ↔ DTO TS.

---

## 10 gaps couverts

### Critiques

1. **CDD essai L.1242-10** — barème exact : 1 jour/semaine, max 2 semaines (CDD ≤ 6 mois) ou 1 mois (CDD > 6 mois). Calculator actuel = 1 mois pour tout CDD ≤ 6 mois.
2. **Intérim essai L.1251-14** — barème exact : 2 jours (mission ≤ 1 mois) / 3 jours (1-2 mois) / 5 jours (> 2 mois). Calculator actuel = même règle que CDD.
3. **Salarié protégé L.2411-1** — élus CSE, DS, membres CSSCT, conseillers prud'homaux : rupture nécessite autorisation inspection du travail, même en période d'essai. Sans autorisation = NULLE.
4. **Délai prévenance ≠ abus** — Cass. soc. 23/01/2013 : non-respect donne droit à indemnité compensatrice de préavis (= salaire × jours manquants / 30), pas à D&I pour abus. Séparer la logique : verdict reste celui des autres critères, mais une indemnité spécifique `indemnitePrevenance` s'ajoute.

### Importants

5. **Discrimination L.1132-1 enum exhaustif** — extension de 6 à 25 motifs (origine, sexe, mœurs, orientation sexuelle, identité de genre, âge, situation famille, grossesse, caractéristiques génétiques, appartenance vraie ou supposée à ethnie/nation/race, opinions politiques, activités syndicales/mutualistes, convictions religieuses, apparence physique, nom de famille, état de santé, perte d'autonomie, handicap, vulnérabilité économique, capacité s'exprimer langue autre que français, exercice fonctions juridictionnelles, lieu de résidence, domiciliation bancaire — Code du travail à jour 2026).
6. **Lanceur d'alerte L.1132-3-3** — nouveau booléen `lanceurAlerte` → NULLE.
7. **Témoin/victime harcèlement** — L.1132-3-1 (témoignage faits discrimination) + L.1152-2 (harcèlement moral) + L.1153-2/3 (harcèlement sexuel). Nouveau booléen `temoinOuVictimeHarcelement` → NULLE.
8. **Droit de retrait L.4131-3** — danger grave et imminent. Nouveau booléen `droitDeRetraitExerce` → NULLE.
9. **Grossesse déclarée post-rupture L.1225-5** — salariée a 15 jours pour notifier la grossesse à l'employeur (certificat médical recommandé) et obtenir la nullité rétroactive. Nouveaux champs : `grossesseDeclareePostRupture` (boolean) + `dateNotificationGrossesse` (LocalDate, dans les 15j de la rupture) → NULLE si conditions remplies.
10. **Apprentissage L.6222-18** — régime spécial : 45 jours de présence effective en milieu de travail, rupture libre. Nouvelle valeur enum `TypeContrat.APPRENTISSAGE` → message explicit "régime hors-scope F-DT-38" + verdict `REGULIERE` neutre + recommandation outil dédié à backloger.

### Reportés follow-up F-252b

- 10b. Suspensions du contrat prolongent l'essai (arithmétique jours) — Cass. soc. 31/01/2018
- 11. Reprise d'ancienneté stage > 2 mois ou CDD précédent (L.1243-11)
- 12. Renouvellement durée doublée — clarification UX
- 13. ✗ déjà couvert ci-dessus (apprentissage = gap 13 audit, ici inclus en gap #10)
- 14. Lettre motivée — motifs énoncés lient l'employeur (Cass. soc. 23/01/1996) — note UX

---

## Architecture des modifications

### Backend

| Fichier | Modification |
|---------|--------------|
| `RupturePeriodeEssaiCalculator.java` | +10 anomalies, +constantes barème CDD précis / barème INTERIM L.1251-14, +`DiscriminationMotif` 25 valeurs, +verdict NULLE pour gaps 6/7/8/9, +verdict neutre APPRENTISSAGE, refonte `dureeLegaleMaximaleMois`, refonte `indemniteEstimee` avec sous-champ `indemnitePrevenance` |
| `RupturePeriodeEssaiInput.java` | +12 champs : `dureeMissionMois`, `salarieProtege`, `autorisationInspectionTravail`, `lanceurAlerte`, `temoinOuVictimeHarcelement`, `droitDeRetraitExerce`, `grossesseDeclareePostRupture`, `dateNotificationGrossesse`, `dateDebutContratPrecedent` (placeholder gap 11), `dureeStagePrealableMois` (placeholder gap 11), `motifsEnoncesLettre` (string, gap 14) |
| `RupturePeriodeEssaiRequest.java` | miroir Input + Jackson |
| `RupturePeriodeEssaiResult.java` | +champ `indemnitePrevenance` |
| `RupturePeriodeEssaiResponse.java` | +mappers nouveaux champs |
| `RupturePeriodeEssaiAnalysis.java` (JPA) | +colonnes nouvelles |
| `RupturePeriodeEssaiService.java` | +mapping Input ↔ Analysis (persistance) |
| `CaseAnalysisResponse.java` (record `TravailExtractedData`) | +12 rpe* fields |
| `CaseAnalysisResponse.java` (`extractTravailData`) | +12 parser branches, +whitelists discrim étendue |
| `LegalDomainPromptBuilder.java` | +12 keys dans `rupture_periode_essai_detail` |
| Migration `280-add-rupture-periode-essai-gaps-columns.xml` | +12 colonnes table `rupture_periode_essai_analyses` |

### Frontend

| Fichier | Modification |
|---------|--------------|
| `case-analysis.model.ts` (`TravailExtractedData`) | +12 champs TS |
| `rupture-periode-essai.model.ts` | +enum `TypeContrat.APPRENTISSAGE`, +enum `DiscriminationMotif` 25 valeurs |
| `rupture-periode-essai-section-prefill-rules.ts` | +12 fonctions `compute*` + `computePrefillCount` recalculé sur 35 champs |
| `rupture-periode-essai-section.component.ts` | +12 form controls, +12 signaux `provenance*`, +12 handlers reset, conditional rendering APPRENTISSAGE |
| `rupture-periode-essai-section.component.html` | +sections form champs nouveaux + badges `auto_awesome` |

### Tests

- Backend UT : ~40 nouveaux tests Calculator (cas par gap)
- Backend IT : ~6 nouveaux tests Controller + parser
- Frontend Jest : ~25 nouveaux tests prefill + component

---

## Critères d'acceptation

- [ ] Calculator : 10 nouvelles anomalies détectées correctement
- [ ] Verdict NULLE déclenché par : salarié protégé sans autorisation, lanceur alerte, témoin/victime harcèlement, droit de retrait, grossesse post-rupture notifiée ≤ 15j
- [ ] CDD ≤ 6 mois : essai max calculé = min(1 jour/semaine × durée_CDD_semaines, 14 jours)
- [ ] Intérim : essai max = 2/3/5 jours selon `dureeMissionMois`
- [ ] Prévenance non respecté : verdict reste celui des autres critères ; `indemnitePrevenance` calculée = salaire × jours_manquants / 30
- [ ] Apprentissage : verdict REGULIERE neutre + message "régime hors-scope F-DT-38"
- [ ] Discrimination enum 25 valeurs propagée backend → prompt → parser → DTO frontend → composant
- [ ] Prompt IA enrichi : 12 nouvelles clés dans `rupture_periode_essai_detail` avec définitions juridiques
- [ ] `extractTravailData()` parse les 12 nouveaux champs avec validators
- [ ] Pré-fill IA exhaustif : prefillFromAi() renseigne les 35 champs (23 existants + 12 nouveaux)
- [ ] Tests : `RupturePeriodeEssaiCalculatorTest` (40+ nouveaux), `RupturePeriodeEssaiControllerIT` (6+ nouveaux), Jest (25+)
- [ ] Migration 280 propre + rollback testé
- [ ] Aucune régression sur les 161 tests backend + 50 tests Jest existants
- [ ] Builder pattern enforcement IT vert

---

## Périmètre

### Hors scope (explicite, follow-up F-252b)

- Suspensions du contrat prolongeant la durée d'essai (arithmétique jours d'arrêt)
- Reprise d'ancienneté stage / CDD précédent (déduction durée essai)
- Renouvellement durée doublée — clarification UX du champ contractuel
- Lettre motivée — analyse qualitative des motifs énoncés

---

## Notes sécurité juridique

- Toutes les sources sont les **textes du Code du travail à jour 2026** + jurisprudence Cass. soc. dominante
- Enum DiscriminationMotif aligné sur L.1132-1 (version consolidée 25 motifs)
- Décision PO : implémentation autonome, validation par Marjolaine en post-déploiement (mail récap + retest sur dossier prod)
- Verdict `APPRENTISSAGE` : neutre + redirection, pas de risque d'erreur (signal explicite hors-scope)

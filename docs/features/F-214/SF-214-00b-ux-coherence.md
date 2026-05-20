# F-214 — Cadrage cohérence écran (étape 0 bis)

**Skill** : `ai-skills/screen-coherence-challenger.md`
**Date** : 2026-05-20
**Auteur** : AI-Agent (mode autonome)

---

## Verdict : GO avec ajustements

---

## Intention métier + comportement visible attendu

F-214 ajoute **22 outils décisionnels Immigration FR P2 (fréquence haute)** dans le panneau décisionnel `app-decisional-tools-panel` (onglet **Décision** de l'écran détail du dossier). Ces outils sont en grande majorité CONTEXTUAL : ils n'apparaissent que si l'IA a détecté la situation correspondante dans les documents du dossier. Le comportement visible est identique au pattern F-208 : chaque outil est une carte expansible dans le panneau, pré-remplie depuis `ImmigrationExtractedData`, avec validation F-IA-03 et refresh dashboard.

---

## Rappel verdict feature-coherence-challenger

**GO avec ajustements** (SF-214-00-coherence.md, 2026-05-20). Tous les pré-requis fonctionnels sont livrés. Ajustements : séquencement F-246, nouveaux flags IA, découpage backend + frontend par outil.

---

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel mis à jour au 2026-05-19, 6ᵉ passage). Pratique avocat droit des étrangers.

1. L'avocat ouvre le dossier → écran **détail du dossier**, 4 onglets (depuis F-244).
2. **En-tête** : titre dossier, domaine DROIT_IMMIGRATION, stepper.
3. Onglet **Dossier** : métadonnées (nationalité, titre, date expiration), stade procédural (F-243), import / liste des pièces.
4. Onglet **Analyse** : pipeline IA asynchrone → synthèse (faits, points juridiques, risques, timeline, questions ouvertes, pièces manquantes).
5. L'IA extrait `ImmigrationExtractedData` : type de titre, date d'expiration, nationalité, flags booléens (F-201/F-246), procédure détectée, etc. Les **flags IA déclenchent l'apparition des outils CONTEXTUAL** dans l'onglet Décision.
6. Onglet **Décision** :
   - `app-decisional-tools-panel` : outils ALWAYS_ON (F-IM-01/05/06/07, renouvellement délai, VLS-TS, autorisation travail employeur) + outils CONTEXTUAL (selon flags IA détectés par l'analyse).
   - Les outils **P2 F-214** apparaissent ici, côte à côte avec les outils P1 (F-208) et les outils existants.
   - `app-case-dashboard` : verdicts agrégés.
   - `app-conclusions-section` : génération projet de conclusions (F-98).
7. L'avocat saisit / vérifie les données dans chaque outil pertinent, déclenche le calcul (POST endpoint).
8. L'avocat consulte le verdict (scoring, checklist, délais calculés).
9. L'avocat génère les conclusions.
10. Onglet **Suivi** : échéances liées aux outils (délais calculés).
11. **État terminal** : projet de conclusions généré (tranché au 3ᵉ passage F-98, 2026-05-18).

---

## État terminal du processus

✅ Tranché (F-98, 2026-05-18) : projet de conclusions généré (`app-conclusions-section`, onglet Décision). Non modifié par F-214.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| 1–3. Ouverture + Dossier | `case-file-detail`, onglet Dossier | ✅ existant |
| 4. Analyse IA | Onglet Analyse, `app-analysis-pipeline` + synthèse | ✅ existant |
| 5. Extraction ImmigrationExtractedData | Backend pipeline IA + `CaseAnalysisResponse` | ✅ existant (étendu par F-246) |
| 6. Outils décisionnels | Onglet Décision, `app-decisional-tools-panel` | ✅ existant (étendu par F-214) |
| 6b. Tableau de bord | Onglet Décision, `app-case-dashboard` | ✅ existant |
| 6c. Conclusions | Onglet Décision, `app-conclusions-section` | ✅ existant (F-98) |
| 7–8. Saisie + calcul | Formulaire dans chaque outil | ✅ pattern existant (F-208) |
| 9. Conclusions | `app-conclusions-section` | ✅ existant |
| 10. Suivi échéances | Onglet Suivi, `app-case-deadlines-section` | ✅ existant (F-69) |

---

## Position candidate de la feature (écran, zone, points d'entrée)

**Écran** : détail du dossier — onglet **Décision**.
**Zone** : `app-decisional-tools-panel` — sous-sections dynamiques pilotées par `TOOL_REGISTRY` et `DecisionToolVisibilityService`.
**Point d'entrée** : aucun nouveau point d'entrée — les outils apparaissent automatiquement dans le panneau lorsque les flags IA correspondants sont détectés (CONTEXTUAL) ou toujours (ALWAYS_ON pour VLS-TS, renouvellement, autorisation travail employeur).
**Groupement thématique** : les 22 outils P2 s'insèrent dans les groupes thématiques existants de `app-decisional-tools-panel` (F-169 groupement) :
- `DELAIS` : renouvellement délai (outil 7), VLS-TS (outil 4), appel CAA/CE (outil 17), assignation résidence (outil 18)
- `STATUT_SEJOUR` : regroupement familial (outil 2), VPF liens personnels (outil 3), carte résident (outil 12), récépissé/attestation (outil 8), UE/EEE/Suisse (outil 20)
- `AES` : AES présence prouvée (outil 6)
- `SANTE` : étranger malade L. 425-9 (outil 1)
- `ASILE` : OFPRA introduction (outil 9), AJ CNDA (outil 10)
- `VICTIMES` : victime traite L. 425-1 (outil 11)
- `CONTENTIEUX` : OQTF catégories (outil 5), ITF judiciaire (outil 19), retrait titre fraude (outil 21), recours naturalisation TJ (outil 15), recours naturalisation TA (outil 16)
- `MINEURS` : MNA évaluation âge (outil 14)
- `ADMINISTRATIF` : ANEF procédure (outil 13), autorisation travail employeur (outil 22)

---

## Challenge placement

Les 22 outils F-214 s'insèrent dans `app-decisional-tools-panel` (onglet Décision), qui est **le bon écran** : l'avocat y est après avoir consulté la synthèse IA (onglet Analyse) et avant de générer les conclusions. Le placement est cohérent avec le parcours réel.

Pas de problème de placement : le pattern CONTEXTUAL garantit que seuls les outils pertinents pour le dossier en cours sont visibles. Si aucun flag IA n'est détecté, l'avocat voit uniquement les ALWAYS_ON (renouvellement, VLS-TS, autorisation travail employeur = 3 outils) — pas de surcharge.

---

## Challenge lisibilité de la séquence

✅ La séquence est lisible grâce aux 4 onglets (F-244) : l'avocat consulte l'analyse (onglet 1) avant les outils décisionnels (onglet 2). Pas de nouveau problème de séquence introduit par F-214. Les outils P2 n'apparaissent que si les flags IA ont été calculés (étape 5 = après analyse IA).

**Point dur** : un outil qui produit un **délai daté** (renouvellement, VLS-TS, appel CAA/CE, assignation résidence) doit matérialiser l'échéance dans l'onglet **Suivi** (`app-case-deadlines-section` F-69) — conformément à l'invariant du 6ᵉ passage (F-206, 2026-05-19). Ce point est intégré dans les mini-specs comme critère d'acceptation.

---

## Challenge charge écran

### Charge avant F-214 (onglet Décision)

- Bloc 1 : `app-decisional-tools-panel` (4 transversaux ALWAYS_ON + 17 outils existants dont 13 CONTEXTUAL)
- Bloc 2 : `app-case-dashboard`
- Bloc 3 : `app-conclusions-section`

**→ 3 blocs primaires**, conformes au seuil de l'onglet Décision.

### Charge après F-214

F-214 n'ajoute **aucun bloc primaire nouveau**. Les 22 outils s'insèrent comme sous-sections dynamiques dans `app-decisional-tools-panel` (Bloc 1). L'onglet Décision reste à 3 blocs primaires.

La charge visible pour l'avocat dépend des flags IA : en l'absence de flag détecté, seuls les 3 outils ALWAYS_ON F-214 s'ajoutent aux 4 transversaux existants = 7 outils visibles au maximum sans flag. Avec flags, chaque flag déclenche +1 outil. Le mécanisme CONTEXTUAL de F-201/F-IA-04 régule la charge.

**Pas de surcharge structurelle.** La charge est pilotée par les flags IA.

---

## Challenge état final / continuité

Après l'usage d'un outil P2 :
1. L'avocat voit le verdict (scoring / checklist / délais calculés) dans la carte de l'outil.
2. `CaseDashboardRefreshService.triggerRefresh()` met à jour le tableau de bord agrégé (Bloc 2).
3. Si l'outil produit un délai daté → `app-case-deadlines-section` (onglet Suivi) est mis à jour.
4. L'avocat peut passer à l'outil suivant ou générer les conclusions (Bloc 3).

**Pas de dead-end.** Le fil conducteur est clair : outils → tableau de bord → conclusions.

---

## Ajustements IA requis (invariants anti-surcharge)

1. **CONTEXTUAL obligatoire** pour les 19 outils sur 22 dont la fréquence est < 30 % des dossiers. Les 3 outils ALWAYS_ON (renouvellement délai, VLS-TS, autorisation travail employeur) sont justifiés (délais irréversibles + pertinence universelle dans un dossier immigration).
2. **Groupement thématique** dans `app-decisional-tools-panel` : les outils P2 doivent s'inscrire dans les groupes thématiques existants (F-169) pour maintenir la lisibilité. Pas de groupe thématique nouveau sauf si > 3 outils dans un thème absent. À définir dans les mini-specs frontend.
3. **Délai → onglet Suivi** : tout outil qui calcule un délai daté (numéros 4, 7, 17, 18) implémente le bridge vers `app-case-deadlines-section` (F-69). Critère d'acceptation obligatoire dans les mini-specs concernées.
4. **Pré-fill IA obligatoire** : tous les champs saisissables pré-remplis depuis `ImmigrationExtractedData`. Les champs non encore disponibles dans le record sont documentés comme gaps V1 (pattern F-208 SF-208-05..08 `PREFILL_COUNT_ALWAYS_ZERO` pour les composants sans pré-fill V1).
5. **Seuil onglet Décision** : 3 blocs primaires max. Tout nouveau bloc primaire est refusé. Les outils F-214 = sous-sections dans le Bloc 1 existant.
6. **Garde-fou CI** : `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` mis à jour à chaque SF backend, avant la SF frontend.

---

## Décision finale

**Verdict : GO avec ajustements.**

Le placement dans `app-decisional-tools-panel` (onglet Décision) est le seul emplacement correct et cohérent. Les ajustements (CONTEXTUAL obligatoire, groupement thématique, bridge délai → onglet Suivi) sont tous intégrables dans les mini-specs sans refonte de l'écran.

**Aucun pré-requis UX à livrer avant F-214** : la structure 4 onglets (F-244), le panneau TOOL_REGISTRY (F-IA-04), et les groupes thématiques (F-169) sont livrés.

---

## MAJ apportée au parcours écran de référence (`docs/business/parcours-ecran-dossier.md`)

**7ᵉ passage** (F-214, 2026-05-20) : confirmation que l'onglet Décision absorbe les outils P2 sans bloc primaire nouveau. Point dur ajouté : les outils P2 qui produisent des délais datés bridgent vers l'onglet Suivi (F-69). Invariant formalisé : tout outil décisionnel produisant un délai daté = critère d'acceptation obligatoire dans sa mini-spec.

> Note : la mise à jour effective de `docs/business/parcours-ecran-dossier.md` (section historique) sera réalisée au commit de ce document.

# Mini-spec — F-DT-17 / SF-DT-17-02 Frontend indemnité précarité CDD (FR)

## Identifiant

`F-DT-17 / SF-DT-17-02`

## Feature parente

`F-DT-17` — Indemnité précarité CDD (art. L.1243-8)

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-DT-17-02-frontend-indemnite-precarite-cdd`

---

## Objectif

Livrer le composant Angular `indemnite-precarite-cdd-section` qui consomme l'API SF-DT-17-01 (POST/GET `/api/v1/case-files/{id}/cdd-indemnite-precarite`) afin que l'avocat français puisse saisir le total des salaires bruts perçus pendant un CDD, éventuellement son taux 6 % ou 10 %, et un cas d'exclusion L.1243-10 le cas échéant — et obtenir instantanément l'indemnité de précarité calculée.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur ouvre un dossier de droit du travail France. Le panel F-IA-04 affiche la section "INDEMNITÉ PRÉCARITÉ CDD" (tool_id `F-DT-17-indemnite-precarite-cdd`, règle ALWAYS_ON FR seed migration 109).
2. Section repliée par défaut (collapsed). Click/Enter → expand.
3. `ngOnInit` : GET `/api/v1/case-files/{id}/cdd-indemnite-precarite`.
   - GET 200 → affichage du résultat persisté en mode lecture (form masqué, bouton "Modifier").
   - GET 404 → mode formulaire vide ; pré-fill IA depuis `aiData.salaireBrutMensuel` (contextuel — voir section IA).
4. L'avocat saisit :
   - `salaireMensuelReference` (€/mois) — optionnel, champ d'aide UI.
   - `dureeCddMois` (mois, décimal possible 0.25 step) — optionnel, champ d'aide UI.
   - `totalSalairesBruts` (€) — obligatoire, > 0. Si salaireMensuelReference × dureeCddMois produisent une valeur, ce champ est auto-rempli (rectifiable).
   - `tauxPrecarite` (radio 10 %/6 %) — défaut 10.
   - `casExclusion` (select nullable — 6 valeurs L.1243-10 + "Aucun"). Défaut `null`.
5. Submit → POST avec body `{ totalSalairesBruts, tauxPrecarite, casExclusion }` (ne pas envoyer `salaireMensuelReference`/`dureeCddMois`).
6. Réponse 200 : affichage du bloc résultat (montant € + formule + baseJuridique + messages), snackbar succès, `CaseDashboardRefreshService.triggerRefresh()` appelé.
7. Bouton "Modifier" → retour au mode formulaire avec valeurs pré-remplies.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `totalSalairesBruts` manquant / ≤ 0 | Submit désactivé (form invalide) | — |
| `casExclusion` inconnu | Backend 400, snackbar erreur rouge | 400 |
| Dossier hors droit du travail | Backend 400, snackbar erreur | 400 |
| Dossier hors workspace | Backend 404, snackbar erreur | 404 |
| GET inexistant | Reste en mode formulaire (pas de snackbar — 404 attendu) | 404 |
| Erreur réseau POST | Snackbar rouge, form reste accessible | 5xx |
| `workspaceCountry !== 'FRANCE'` | Bannière info "BE : pas d'équivalent forfaitaire" | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-07 à F-DT-11, F-DT-15, F-DT-19, F-DT-21, F-IM-05/06/07/08, F-FA-05/06/07, F-132. Scan : les autres outils d'indemnité/chiffrage (F-DT-09 comparateur, F-DT-11 indemnité nullité, F-DT-19 heures sup) suivent le même pattern — form + calcul backend + résultat persisté. `harcelement-licenciement-nul-section` est le template canonique.
- [x] **Autres pays** : FRANCE only. Belgique : `loi du 03/07/1978` ne prévoit pas d'équivalent forfaitaire — bannière info.
- [x] **Autres domaines** : non applicable — spécifique droit du travail FR.
- [x] **Autres UI patterns** : pattern section F-IA-04 + pré-fill IA (SF-155-04) + alertes cohérence F-IA-03 via `CoherenceAlertBuilder` (SF-155-05) + refresh dashboard (SF-IA-02-03) — tous réutilisés, pas de nouveau pattern.

### Niveaux de vérification

- [x] Modèle TypeScript + interface contrat API
- [x] Service Angular wrapping HttpClient
- [x] Composant Angular consommateur
- [x] Spec Jest ≥ 15 tests
- [x] Entrée TOOL_REGISTRY symétrique

### Cas spécifique : outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : pré-fill + alerte salaire. `aiData.salaireBrutMensuel` pré-remplit `salaireMensuelReference` (champ d'aide — pas envoyé au backend). Si avocat édite avec écart > 10 % vs IA → `coherenceAlerts.SALAIRE_MENSUEL` via `CoherenceAlertBuilder`. Pas de champ IA direct pour `totalSalairesBruts` (backend ne calcule pas cumul).
- [x] **Refresh dashboard (F-IA-02)** : `triggerRefresh()` dans `next:` du POST.
- [x] **Pré-remplissage IA** : `prefillFromAi()` + signal `provenanceSalaire` — badge "Pré-rempli depuis l'analyse" + effacement sur `onSalaireChange`. Pas de mapping pour `dateEntree` (non utile au calcul ; hors scope).
- [x] **Persistance des inputs** : backend persiste `totalSalairesBruts`, `tauxPrecarite`, `casExclusion` (colonnes dédiées) + `result_data` JSON. OK.
- [x] **Masquage conditionnel** : règle F-IA-04 `decision_tool_visibility_rules` ALWAYS_ON FR+travail seed migration 109. Composant déjà gated par backend. Gate front : bannière BE info.
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` n'inclut **que** `!showForm()` (pattern canonique) — pas `|| result()`.

### Cas spécifique : nouveau pattern UI ou service partagé

Pas applicable — composant spécifique au domaine, réutilise les patterns existants (`CoherenceAlertBuilder`, `LegalCitationsPipe`, `CaseDashboardRefreshService`, `SourceExplanationService`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SFs décisionnelles frontend en cours (SF-DT-12-02, SF-DT-21-02, SF-IM-08-05/06/07 ?) | Oui | **SF parallèle** — chaque SF crée sa propre entrée TOOL_REGISTRY. Conflit anticipé dans `decisional-tools-panel.component.ts` — rebase + merge toutes les entrées au push. |
| Pré-fill IA salaire (pattern SF-155-04-A1) | Oui | **Intégré dans cette SF** — provenance badge + alerte cohérence > 10 %. |
| Pattern `CoherenceAlertBuilder` SF-155-05 | Oui | **Intégré** — construit `CoherenceAlert<'SALAIRE_MENSUEL'>`. |
| BE équivalent précarité | Non | **Non applicable** — loi belge du 03/07/1978 ne prévoit pas d'indemnité forfaitaire post-CDD. Documenté dans la bannière info. |
| Pré-fill `totalSalairesBruts` depuis aiData | Non | **Backlog** — futur SF : backend extractor pour rémunérations brutes cumulées sur période CDD (hors scope SF-DT-17-02). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Non applicable aux autres cibles (BE — pas d'équivalent forfaitaire)

---

## Critères d'acceptation

- [x] Composant standalone `IndemnitePrecariteCddSectionComponent` créé — `selector: 'app-indemnite-precarite-cdd-section'`
- [x] Service `IndemnitePrecariteCddService` (methods `calculate`, `get`) créé
- [x] Modèle TypeScript `indemnite-precarite-cdd.model.ts` avec enums `CasExclusionCdd` (6 valeurs), `IndemnitePrecariteCddRequest`, `IndemnitePrecariteCddResponse`
- [x] Entrée TOOL_REGISTRY `F-DT-17-indemnite-precarite-cdd` dans `decisional-tools-panel.component.ts`
- [x] GET 404 → mode formulaire + pré-fill IA si `aiData.salaireBrutMensuel > 0`
- [x] GET 200 → mode lecture (form masqué)
- [x] POST succès → résultat + snackbar + `triggerRefresh()`
- [x] POST erreur → snackbar rouge
- [x] Form invalide (total ≤ 0) → submit désactivé, pas d'appel HTTP
- [x] Bannière info si `workspaceCountry !== 'FRANCE'` (pas de masquage silencieux)
- [x] Palette navy/or, pas de MatDatepicker (pas de date), typo Inter + JetBrains Mono pour formule/baseJuridique/articles
- [x] Badge "Pré-rempli depuis l'analyse" sur salaire si pré-fill IA
- [x] Alerte cohérence `SALAIRE_MENSUEL` si avocat édite avec écart > 10 % vs IA
- [x] Edition manuelle salaire → `provenanceSalaire=null`
- [x] Test spec Jest ≥ 15 cases (voir plan de test)

---

## Périmètre

### Hors scope (explicite)

- Pas d'extraction backend de `totalSalairesBruts` depuis aiData (nécessiterait parsing fiches de paie multiples — future SF).
- Pas de module Belgique (pas d'équivalent légal).
- Pas d'export PDF (global dans `prudhome-fiche-section` / `tribunal-travail-fiche-section`).
- Pas de prise en compte des indemnités journalières maladie / congés payés dans le total (règle avocat — documenté dans le message résultat).

---

## Contrat API (importé de SF-DT-17-01)

### POST `/api/v1/case-files/{caseFileId}/cdd-indemnite-precarite`

Body :
```json
{
  "totalSalairesBruts": number (> 0, required),
  "tauxPrecarite": 10 | 6 | null (null défaut 10),
  "casExclusion": string (enum ci-dessous) | null
}
```

Enum `casExclusion` :
- `CDD_ETUDIANT_VACANCES`
- `CDD_SAISONNIER`
- `CDD_USAGE`
- `CDI_REFUSE_PAR_SALARIE`
- `RUPTURE_ANTICIPEE_SALARIE`
- `RUPTURE_ANTICIPEE_FAUTE_GRAVE`

Réponse 200 :
```json
{
  "caseFileId": "uuid",
  "totalSalairesBruts": number,
  "tauxPrecarite": 10 | 6,
  "casExclusion": string | null,
  "indemnitePrecarite": number,
  "formule": string,
  "baseJuridique": string,
  "messages": string[]
}
```

### GET `/api/v1/case-files/{caseFileId}/cdd-indemnite-precarite`

- 200 → même shape que POST
- 404 → "Aucune analyse d'indemnité de précarité CDD trouvée pour ce dossier"

### Erreurs

- 400 : message dans `error.message` — `Total des salaires bruts requis et strictement positif` / `Taux applicable : 10 % ou 6 %` / `Cas d'exclusion inconnu : ...`
- 404 : case file hors workspace ou inexistant
- 400 : dossier hors droit du travail

---

## Composants Angular

| Composant | Description |
|-----------|-------------|
| `IndemnitePrecariteCddSectionComponent` | Section F-IA-04, form + résultat, pré-fill IA + alertes F-IA-03 |
| `IndemnitePrecariteCddService` | wrapper HttpClient POST/GET |
| Modèle `indemnite-precarite-cdd.model.ts` | Types + liste options |

Composant modifié :
- `decisional-tools-panel.component.ts` : ajout import + entrée TOOL_REGISTRY.

---

## Plan de test

### Tests unitaires (Jest spec) — ≥ 15

1. FRANCE → 6 options de cas d'exclusion affichées
2. GET 200 → mode lecture, form masqué, valeurs hydratées
3. GET 404 → mode formulaire, pas de badge IA si pas d'aiData
4. Form valid : total > 0 requis
5. POST succès → résultat affiché + snackbar + `triggerRefresh()` appelé
6. POST erreur 400 → snackbar rouge, form reste ouvert
7. POST ignoré si form invalide (pas d'appel HTTP)
8. Pré-fill IA salaire mensuel depuis `aiData.salaireBrutMensuel > 0`
9. `aiData.salaireBrutMensuel ≤ 0` → pas de pré-fill
10. `aiData = null` → pas de pré-fill, pas de badge
11. `onSalaireChange` manuel → provenance remise à null (badge disparaît)
12. GET 200 → provenance=null même si aiData présent (persisté > IA)
13. `coherenceAlerts.SALAIRE_MENSUEL` présent si écart > 10 % vs IA
14. `coherenceAlerts.SALAIRE_MENSUEL` absent si écart ≤ 10 %
15. `ngOnChanges(aiData)` post-mount rafraîchit pré-fill si form vide
16. `workspaceCountry='BELGIQUE'` → bannière affichée, form caché
17. Cas d'exclusion sélectionné → taux garde sa valeur mais backend renvoie `indemnitePrecarite=0`, message EXCLUSIONS affiché
18. Auto-calcul `totalSalairesBruts = salaireMensuelReference × dureeCddMois` si les deux présents
19. Édition manuelle de `totalSalairesBruts` sur-écrit l'auto-calcul
20. toggleCollapse / editMode fonctionnent
21. Alertes cachées après showForm=false (pattern anti-bug SF-IA-03-12)

### Tests d'intégration

Non applicable (backend déjà couvert par SF-DT-17-01 +15 UT + 10 IT).

### Isolation workspace

- [x] Non applicable frontend — le backend isole déjà via workspaceMemberRepository (SF-DT-17-01).

---

## Impact par domaine métier

- **Droit du travail France** : oui — feature spécifique aux CDD français.
- **Droit du travail Belgique** : non applicable — pas d'équivalent légal forfaitaire. Bannière info explicite.
- **Immigration** : non applicable.
- **Famille** : non applicable.

---

## Parité des domaines métier

Niveau outil décisionnel : **3 — Calculateur** (pas scoring, pas comparateur, pas détection événement). Règle de parité non déclenchée (seuil ≥ 5). Le calculateur reste domaine droit du travail FR uniquement, justifié par absence légale BE.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Outil décisionnel métier** — nouvelle entrée TOOL_REGISTRY + composant frontend. Scan des autres outils décisionnels effectué ci-dessus (§cohérence transversale).
- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché (consomme API existante)
- [ ] Plans / limites — non touché
- [ ] Navigation / routing — non touché (section intégrée dans panel existant)

### Smoke tests E2E concernés

- Aucun smoke test concerné — pas de nouvelle route, pas de guard, pas de flow auth. Les smoke tests existants couvrent déjà la navigation dossier → panel décisionnel.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-17-01` — backend Done 2026-04-24 (PR #488)
- `SF-155-04-A1` (template canonique HLN pré-fill IA) — Done
- `SF-155-05` (CoherenceAlertBuilder partagé) — Done

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Champ d'aide `salaireMensuelReference`** : pas envoyé au backend ; sert uniquement à :
  - recevoir le pré-fill IA (`aiData.salaireBrutMensuel`) avec badge provenance
  - déclencher l'alerte cohérence F-IA-03 si l'avocat le modifie avec écart > 10 %
  - auto-calculer `totalSalairesBruts` via multiplication par `dureeCddMois` (helper UX)
- **Pas de date** : le backend F-DT-17-01 n'a pas de champ date (contrat figé). Pas de MatDatepicker.
- **Pas de mapping IA pour cas d'exclusion** : l'IA ne détecte pas les exclusions L.1243-10 (spécifique CDD, rare). L'avocat saisit manuellement.
- **Pattern de référence** : `harcelement-licenciement-nul-section` (template canonique SF-155-04-A1) — pré-fill + `CoherenceAlertBuilder` + `LegalCitationsPipe`.

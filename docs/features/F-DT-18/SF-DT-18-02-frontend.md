# Mini-spec — F-DT-18 / SF-DT-18-02 Frontend indemnité fin mission intérim (FR)

## Identifiant

`F-DT-18 / SF-DT-18-02`

## Feature parente

`F-DT-18` — Indemnité fin mission intérim (art. L.1251-32 Code du travail)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-18-02-frontend-fin-mission-interim`

---

## Objectif

Livrer le composant Angular `fin-mission-interim-section` qui consomme l'API SF-DT-18-01 (POST/GET `/api/v1/case-files/{id}/fin-mission-interim`) afin que l'avocat français puisse saisir le total des rémunérations brutes perçues pendant une mission d'intérim, la durée de la mission, la date de fin et un éventuel motif d'exclusion L.1251-33 — et obtenir instantanément l'indemnité de fin de mission (IFM 10 %) calculée.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur ouvre un dossier de droit du travail France. Le panel F-IA-04 affiche la section "INDEMNITÉ FIN MISSION INTÉRIM" (tool_id `F-DT-18-fin-mission-interim`, règle ALWAYS_ON FR seed migration backend).
2. Section repliée par défaut (collapsed). Click/Enter → expand.
3. `ngOnInit` : GET `/api/v1/case-files/{id}/fin-mission-interim`.
   - GET 200 → affichage du résultat persisté en mode lecture (form masqué, bouton "Modifier").
   - GET 404 → mode formulaire vide ; pré-fill IA depuis `aiData.salaireBrutMensuel` (champ d'aide UX).
4. L'avocat saisit :
   - `salaireMensuelReference` (€/mois) — optionnel, champ d'aide UX (auto-calc + pré-fill IA + alerte F-IA-03).
   - `dureeMissionJours` (jours, entier ≥ 1) — obligatoire (envoyé au backend).
   - `totalRemunerationsBrutesEur` (€) — obligatoire, > 0 (envoyé au backend). Auto-calculé en `salaireMensuelReference × dureeMissionJours / 30` si les deux champs d'aide sont remplis et que l'avocat n'a pas encore saisi manuellement.
   - `motifExclusion` (select nullable — 6 valeurs L.1251-33 + "Aucun"). Défaut `null`.
   - `dateFinMission` (date ISO `YYYY-MM-DD`) — obligatoire (envoyé au backend), `<input type="date">` natif.
5. Submit → POST avec body `{ totalRemunerationsBrutesEur, dureeMissionJours, motifExclusion, dateFinMission }` (ne pas envoyer `salaireMensuelReference`).
6. Réponse 200 : affichage du bloc résultat (taux + montant € + formule + baseJuridique + messages), snackbar succès, `CaseDashboardRefreshService.triggerRefresh()` appelé.
7. Bouton "Modifier" → retour au mode formulaire avec valeurs pré-remplies.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `totalRemunerationsBrutesEur` manquant / ≤ 0 | Submit désactivé (form invalide) | — |
| `dureeMissionJours` manquant / ≤ 0 | Submit désactivé | — |
| `dateFinMission` vide | Submit désactivé | — |
| `motifExclusion` inconnu | Backend 400, snackbar erreur rouge | 400 |
| Dossier hors droit du travail | Backend 400, snackbar erreur | 400 |
| Dossier hors workspace | Backend 404, snackbar erreur | 404 |
| GET inexistant | Reste en mode formulaire (pas de snackbar — 404 attendu) | 404 |
| Erreur réseau POST | Snackbar rouge, form reste accessible | 5xx |
| `workspaceCountry !== 'FRANCE'` | Bannière info "BE : pas d'équivalent forfaitaire" | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-07 à F-DT-11, F-DT-15, F-DT-17, F-DT-19, F-DT-21, F-IM-05/06/07/08, F-FA-05/06/07, F-132. Scan : F-DT-17 (indemnité précarité CDD) est le **jumeau direct** — même pattern (taux 10 %, exclusions, totalSalaires/totalRemunerations) — utilisé comme référence stricte pour cette SF. `harcelement-licenciement-nul-section` reste le template canonique global.
- [x] **Autres pays** : FRANCE only. Belgique : pas d'équivalent forfaitaire dans la loi belge sur le travail intérimaire (loi 24/07/1987) — bannière info.
- [x] **Autres domaines** : non applicable — spécifique droit du travail FR.
- [x] **Autres UI patterns** : pattern section F-IA-04 + pré-fill IA (SF-155-04) + alertes cohérence F-IA-03 via `CoherenceAlertBuilder` (SF-155-05) + refresh dashboard (SF-IA-02-03) — tous réutilisés, pas de nouveau pattern.

### Niveaux de vérification

- [x] Modèle TypeScript + interface contrat API (importé SF-DT-18-01)
- [x] Service Angular wrapping HttpClient
- [x] Composant Angular consommateur
- [x] Spec Jest ≥ 15 tests
- [x] Entrée TOOL_REGISTRY symétrique

### Cas spécifique : outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : pré-fill + alerte salaire. `aiData.salaireBrutMensuel` pré-remplit `salaireMensuelReference` (champ d'aide — pas envoyé au backend). Si avocat édite avec écart > 10 % vs IA → `coherenceAlerts.SALAIRE_MENSUEL` via `CoherenceAlertBuilder`. Pas de champ IA direct pour `totalRemunerationsBrutesEur` ni `dureeMissionJours` (pipeline IA actuel n'extrait pas ces champs spécifiques intérim).
- [x] **Refresh dashboard (F-IA-02)** : `triggerRefresh()` dans `next:` du POST.
- [x] **Pré-remplissage IA** : `prefillFromAi()` + signal `provenanceSalaire` — badge "Pré-rempli depuis l'analyse" + effacement sur `onSalaireChange`. Pas de mapping pour `dureeMissionJours` ni `dateFinMission` (backend pipeline IA n'extrait pas spécifiquement les missions intérim).
- [x] **Persistance des inputs** : backend persiste `totalRemunerationsBrutesEur`, `dureeMissionJours`, `motifExclusion`, `dateFinMission` (colonnes dédiées SF-DT-18-01) + `result_data` JSON. OK.
- [x] **Masquage conditionnel** : règle F-IA-04 `decision_tool_visibility_rules` ALWAYS_ON FR+travail seed migration backend SF-DT-18-01. Composant déjà gated par backend. Gate front : bannière BE info.
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` n'inclut **que** `!showForm()` (pattern canonique) — pas `|| result()`.

### Cas spécifique : nouveau pattern UI ou service partagé

Pas applicable — composant spécifique au domaine, réutilise les patterns existants (`CoherenceAlertBuilder`, `LegalCitationsPipe`, `CaseDashboardRefreshService`, `SourceExplanationService`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-17 jumeau | Oui | **Pattern de référence STRICT** — structure quasi-identique (form + résultat + pré-fill + alertes). Même TOOL_REGISTRY shape. |
| Pré-fill IA salaire (pattern SF-155-04-A1) | Oui | **Intégré dans cette SF** — provenance badge + alerte cohérence > 10 %. |
| Pattern `CoherenceAlertBuilder` SF-155-05 | Oui | **Intégré** — construit `CoherenceAlert<'SALAIRE_MENSUEL'>`. |
| BE équivalent indemnité fin mission intérim | Non | **Non applicable** — pas d'équivalent forfaitaire dans la loi belge. Documenté dans la bannière info. |
| Pré-fill `totalRemunerationsBrutesEur` depuis aiData | Non | **Backlog** — futur SF : backend extractor pour rémunérations cumulées sur période intérim (hors scope SF-DT-18-02). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Non applicable aux autres cibles (BE — pas d'équivalent forfaitaire)

---

## Critères d'acceptation

- [x] Composant standalone `FinMissionInterimSectionComponent` créé — `selector: 'app-fin-mission-interim-section'`
- [x] Service `FinMissionInterimService` (methods `calculate`, `get`) créé
- [x] Modèle TypeScript `fin-mission-interim.model.ts` avec enum `MotifExclusionInterim` (6 valeurs), `FinMissionInterimRequest`, `FinMissionInterimResponse`
- [x] Entrée TOOL_REGISTRY `F-DT-18-fin-mission-interim` dans `decisional-tools-panel.component.ts`
- [x] GET 404 → mode formulaire + pré-fill IA si `aiData.salaireBrutMensuel > 0`
- [x] GET 200 → mode lecture (form masqué)
- [x] POST succès → résultat + snackbar + `triggerRefresh()`
- [x] POST erreur → snackbar rouge
- [x] Form invalide (total ≤ 0, durée ≤ 0, dateFinMission vide) → submit désactivé, pas d'appel HTTP
- [x] Bannière info si `workspaceCountry !== 'FRANCE'` (pas de masquage silencieux)
- [x] Palette navy/or si pas exclusion, rouge `--danger` si exclusion retenue (pas la gradation `--danger-medium/-strong/-dark` réservée urgences 48h), pas de MatDatepicker (`<input type="date">`), typo Inter + JetBrains Mono pour formule/baseJuridique/articles
- [x] Badge "Pré-rempli depuis l'analyse" sur salaire si pré-fill IA
- [x] Alerte cohérence `SALAIRE_MENSUEL` si avocat édite avec écart > 10 % vs IA
- [x] Edition manuelle salaire → `provenanceSalaire=null`
- [x] Test spec Jest ≥ 15 cases (voir plan de test)

---

## Périmètre

### Hors scope (explicite)

- Pas d'extraction backend de `totalRemunerationsBrutesEur` ni de `dureeMissionJours` ni de `dateFinMission` depuis aiData (nécessiterait parsing fiches de paie et contrat de mission — future SF).
- Pas de module Belgique (pas d'équivalent légal).
- Pas d'export PDF (global dans `prudhome-fiche-section`).
- Pas de prise en compte des indemnités de congés payés dans le total brut (règle avocat — documenté dans le message résultat backend).

---

## Contrat API (importé de SF-DT-18-01)

### POST `/api/v1/case-files/{caseFileId}/fin-mission-interim`

Body :
```json
{
  "totalRemunerationsBrutesEur": number (> 0, required),
  "dureeMissionJours": number (> 0, required),
  "motifExclusion": string (enum ci-dessous) | null,
  "dateFinMission": "YYYY-MM-DD" (required)
}
```

Enum `motifExclusion` :
- `CONTRAT_INDETERMINEE_PROPOSE`
- `RUPTURE_ANTICIPEE_SALARIE`
- `FAUTE_GRAVE`
- `FORCE_MAJEURE`
- `MISSION_PEPINIERE_QUALIFIANTE`
- `INTERIMAIRE_REFUS_PROPOSITION_CDI`

Réponse 200 :
```json
{
  "caseFileId": "uuid",
  "totalRemunerationsBrutesEur": number,
  "dureeMissionJours": number,
  "motifExclusion": string | null,
  "dateFinMission": "YYYY-MM-DD",
  "tauxApplique": 0.10 | 0,
  "montantIndemniteEur": number,
  "exclusionRetenue": boolean,
  "baseJuridique": string,
  "formule": string,
  "messages": string[],
  "country": "FRANCE"
}
```

### GET `/api/v1/case-files/{caseFileId}/fin-mission-interim`

- 200 → même shape que POST
- 404 → "Aucune analyse d'indemnité fin de mission intérim trouvée pour ce dossier"

### Erreurs

- 400 : message dans `error.message` — `Total des rémunérations brutes requis et strictement positif` / `Durée de mission > 0` / `Date de fin de mission requise` / `Motif d'exclusion inconnu : ...`
- 404 : case file hors workspace ou inexistant
- 400 : dossier hors droit du travail

---

## Composants Angular

| Composant | Description |
|-----------|-------------|
| `FinMissionInterimSectionComponent` | Section F-IA-04, form + résultat, pré-fill IA + alertes F-IA-03 |
| `FinMissionInterimService` | wrapper HttpClient POST/GET |
| Modèle `fin-mission-interim.model.ts` | Types + liste options motif exclusion |

Composant modifié :
- `decisional-tools-panel.component.ts` : ajout import + entrée TOOL_REGISTRY.

---

## Plan de test

### Tests unitaires (Jest spec) — ≥ 15

1. FRANCE → 6 options de motif d'exclusion affichées (codes corrects)
2. GET 200 → mode lecture, form masqué, valeurs hydratées
3. GET 404 → mode formulaire, pas de badge IA si pas d'aiData
4. Form valid : total > 0, durée > 0, dateFinMission renseignée
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
16. `workspaceCountry='BELGIQUE'` → bannière affichée, pas de GET
17. Motif d'exclusion sélectionné → envoyé dans le POST, backend renvoie `exclusionRetenue=true`, taux=0
18. Auto-calcul `totalRemunerationsBrutesEur = round(salaireMensuelReference × dureeMissionJours / 30)` si les deux champs d'aide remplis
19. Édition manuelle de `totalRemunerationsBrutesEur` → l'auto-calc ne l'écrase plus
20. toggleCollapse / editMode fonctionnent
21. Alertes cachées après showForm=false (pattern anti-bug SF-IA-03-12)

### Tests d'intégration

Non applicable (backend déjà couvert par SF-DT-18-01 +UT + IT).

### Isolation workspace

- [x] Non applicable frontend — le backend isole déjà via workspaceMemberRepository (SF-DT-18-01).

---

## Impact par domaine métier

- **Droit du travail France** : oui — feature spécifique aux missions d'intérim françaises (art. L.1251-32 / L.1251-33).
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

- `SF-DT-18-01` — backend en parallèle (contrat API figé dans cette mini-spec, importé)
- `SF-155-04-A1` (template canonique HLN pré-fill IA) — Done
- `SF-155-05` (CoherenceAlertBuilder partagé) — Done

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Champ d'aide `salaireMensuelReference`** : pas envoyé au backend ; sert uniquement à :
  - recevoir le pré-fill IA (`aiData.salaireBrutMensuel`) avec badge provenance
  - déclencher l'alerte cohérence F-IA-03 si l'avocat le modifie avec écart > 10 %
  - auto-calculer `totalRemunerationsBrutesEur` via `round(salaireMensuelReference × dureeMissionJours / 30)` (helper UX, base 30 jours/mois usuelle en intérim)
- **Date** : `<input type="date">` natif (convention projet, pas MatDatepicker).
- **Pas de mapping IA pour motif d'exclusion** : l'IA ne détecte pas les exclusions L.1251-33 (spécifique intérim, rare). L'avocat saisit manuellement.
- **Pattern de référence** : `indemnite-precarite-cdd-section` (jumeau F-DT-17, structure quasi-identique) + `harcelement-licenciement-nul-section` (template canonique SF-155-04-A1) — pré-fill + `CoherenceAlertBuilder` + `LegalCitationsPipe`.
- **Bannière exclusion** : si `result.exclusionRetenue=true`, bannière rouge `--danger` pour montrer que l'indemnité est nulle (montant 0 €). Non urgence 48h → pas de gradation `--danger-medium/-strong/-dark`.

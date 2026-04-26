# SF-FA-18-02 — Frontend reconnaissance paternelle (art. 316 Cciv)

> Frontend qui ferme F-FA-18 (Filiation – reconnaissance paternelle volontaire).
> Backend SF-FA-18-01 mergé PR #652. Contrat figé importé.

## Objectif

Exposer dans le panel décisionnel F-IA-04 l'outil "Reconnaissance paternelle"
sous forme d'un composant Angular qui consomme l'API
`/api/v1/case-files/{id}/reconnaissance-paternelle-analysis` (POST + GET) et
guide l'avocat à travers les 3 sous-types prévus à l'art. 316 Cciv.

## Pattern de référence

`partage-judiciaire-section` (SF-FA-17-02 — PR #638). Tous les invariants frontend
décisionnels (palette navy/or/rouge, datepicker `type="date"`, gate country avec
bannière info, `CaseDashboardRefreshService.triggerRefresh()` post-POST,
`MatSnackBar` pour erreurs, JetBrains Mono pour `baseJuridique` et `formule`,
pré-fill IA + signaux provenance, validation F-IA-03 multi-sources, tooltips
`CoherencePopoverTriggerDirective`, helper partagé `CoherenceAlertBuilder`).

## Comportement nominal

1. Au montage : si `workspaceCountry === 'FRANCE'`, GET `…/reconnaissance-paternelle-analysis`.
   - 200 → mode résultat hydraté (`showForm = false`).
   - 404 → mode formulaire vide, fallback pré-fill IA si `aiData` présent.
2. Si `workspaceCountry !== 'FRANCE'` → bannière info (équivalent CC art. 327 BE
   au backlog jumeau), aucun appel HTTP.
3. Saisie : sous-type radio (3 options), date naissance enfant
   (input `type="date"`), date reconnaissance (input `type="date"`), 4 critères
   booléens radio, `presenceParProcuration` checkbox.
4. Validation : `dateNaissanceEnfant` requis pour POST_NATALE_*, optionnel pour
   PRENATALE. Tous les booléens requis.
5. POST → mode résultat. SnackBar succès. `triggerRefresh()` du dashboard.
6. Affiche un chip d'info `Délai contestation : 10 ans (art. 332-335)` dans le
   bandeau résultat.
7. Édition : retour au formulaire en conservant les valeurs.

## Cas d'erreur

- BE → bannière info + form caché (gate frontend symétrique au 400 backend).
- POST 400 (validation backend) → snackbar rouge avec message du backend.
- POST 404 (cross-workspace) → snackbar rouge "Dossier introuvable".
- Erreur réseau → snackbar générique "Erreur lors du calcul".

## Critères d'acceptation vérifiables

1. FR + tous champs valides → POST envoyé avec body conforme au contrat figé,
   résultat affiché avec verdict (chip ELEVEE/MOYENNE/FAIBLE), score, base
   juridique, délai contestation 10 ans en chip, formule (JetBrains Mono).
2. BE → aucun appel HTTP, bannière info "Outil français uniquement —
   équivalent BE au backlog (CC art. 327 et s.)".
3. GET 200 hydrate le résultat, `provenance*` reset à null.
4. GET 404 garde le mode formulaire et lance `prefillFromAi()` une fois.
5. Pré-fill IA : `consentementLibreDuPere`, `paterniteVraisemblable`,
   `enfantNonReconnuParAutrePere`, `procedureRespectee` → badge "Pré-rempli depuis
   l'analyse" (icône `auto_awesome`, palette or canonique).
6. Validation F-IA-03 : si avocat saisit une valeur divergente de
   `aiData.paterniteVraisemblableDetected` (ou autre source IA), badge
   d'incohérence + popover `CoherencePopoverTriggerDirective`. Multi-sources
   F96 / QUESTION_IA / IA / PIECE_MANQUANTE pris en compte via le helper
   `CoherenceAlertBuilder`.
7. Handler `onXxxChange()` reset de `provenance<Field>` au 1er edit manuel.
8. ngOnChanges(aiData) re-déclenche le pré-fill si form vierge ; n'écrase pas
   les saisies avocat.
9. POST 400 → snackbar rouge avec message backend.
10. Sous-type PRENATALE n'exige pas de date de naissance.
11. Délai 10 ans (`delaiContestationAns`) toujours rendu en chip info dans le
    bandeau résultat (même si la valeur diffère du défaut).
12. Liste `documentsRequis` rendue en `<ul>` non vide.
13. Liste `risquesContestation` rendue en `<ul>` (ou cachée si vide).
14. TOOL_REGISTRY contient `'F-FA-18-reconnaissance-paternelle'` → composant +
    bindings IA `(caseFileId, workspaceCountry, aiData=familleExtractedData,
    procedureChecks, aiQuestions, piecesManquantes)`.

## Plan de test

- **Unit** (≥ 12 specs) :
  1. FRANCE → GET au ngOnInit
  2. BELGIQUE → aucun appel HTTP (gate)
  3. GET 200 → mode résultat hydraté
  4. GET 404 → mode formulaire
  5. Pré-fill IA `consentementLibreDuPere` + provenance IA
  6. Pré-fill ignoré si pas d'aiData
  7. `onConsentementChange()` reset provenance
  8. `formValid()` true seulement quand tous les champs requis présents
  9. PRENATALE form valide sans date naissance
  10. POST nominal → résultat + snackbar succès + triggerRefresh
  11. POST erreur backend → snackbar rouge
  12. coherenceAlerts.PATERNITE_VRAISEMBLABLE divergence IA présente
  13. coherenceAlerts vides après calcul (showForm=false)
  14. `bannerClass` mappe ELEVEE/MOYENNE/FAIBLE → palette canonique
  15. `editMode()` ré-affiche le form
  16. TOOL_REGISTRY entry resolves to `ReconnaissancePaternelleSectionComponent`

## Tables / endpoints / composants impactés

- **Composant** : `frontend/src/app/case-files/reconnaissance-paternelle-section/`
- **Modèle** : `frontend/src/app/core/models/reconnaissance-paternelle.model.ts`
- **Service** : `frontend/src/app/core/services/reconnaissance-paternelle.service.ts`
- **TOOL_REGISTRY** : `decisional-tools-panel.component.ts` — entrée `F-FA-18-reconnaissance-paternelle`
- **FamilleExtractedData** : 4 champs détection IA optionnels (4 boolean detected).
- Aucune migration (tool_id déjà inséré par migration 178 backend).

## Hors périmètre

- Belgique (régime CC art. 327 et s. distinct) → backlog jumeau.
- Contestation paternité 332-335 → SF-FA-18-03.
- Action en recherche de paternité 327 → SF-FA-18-04.
- Possession d'état 317 → SF-FA-18-05.
- Adoption simple/plénière → SF-FA-18-06/07.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : aucun impact (outil masqué par
  `decision_tool_visibility_rules.legal_domain = DROIT_FAMILLE`).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : bannière info + backlog jumeau.

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- Droit du travail FR/BE : non applicable.
- Droit immigration FR/BE : non applicable.
- Droit famille FR : couvert.
- Droit famille BE : feature jumelle au backlog (régime juridique distinct CC
  art. 327 et s. — consentement maternel obligatoire). Asymétrie temporaire
  justifiée et tracée.

## Nouveau pattern UI ou service partagé

Aucun. Réutilisation stricte du pattern `partage-judiciaire-section` (SF-FA-17-02)
+ helper partagé `CoherenceAlertBuilder` + directive `CoherencePopoverTriggerDirective`.

## Préoccupations transversales

- **Auth / Principal** : aucun changement.
- **Workspace context** : gate `workspaceCountry` côté composant.
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (composant interne au panel F-IA-04).
- **Outil décisionnel métier** : nouvel outil, scan effectué — pas de mélange,
  un outil = une situation (reconnaissance volontaire art. 316 ≠ contestation
  art. 332-335 ≠ recherche de paternité art. 327 ≠ adoption art. 343).

## Contrat API (importé de SF-FA-18-01)

POST `/api/v1/case-files/{caseFileId}/reconnaissance-paternelle-analysis`

Body :
```json
{
  "sousType": "RECONNAISSANCE_PRENATALE | RECONNAISSANCE_POST_NATALE_NAISSANCE | RECONNAISSANCE_POST_NATALE_ULTERIEURE",
  "dateNaissanceEnfant": "YYYY-MM-DD | null (pour PRENATALE)",
  "dateReconnaissance": "YYYY-MM-DD | null",
  "consentementLibreDuPere": true,
  "paterniteVraisemblable": true,
  "enfantNonReconnuParAutrePere": true,
  "procedureRespectee": true,
  "presenceParProcuration": false
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "sousType": "RECONNAISSANCE_PRENATALE",
  "verdictRecevabilite": "ELEVEE | MOYENNE | FAIBLE",
  "scoreEligibilite": 90,
  "effetFiliation": "YYYY-MM-DD",
  "risquesContestation": ["..."],
  "documentsRequis": ["..."],
  "delaiContestationAns": 10,
  "baseJuridique": "Art. 316 Cciv + 332-335 + 372 Cciv",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

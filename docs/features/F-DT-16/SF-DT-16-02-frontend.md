# SF-DT-16-02 — Frontend Licenciement nul (détection multi-protections + indemnité plancher 6 mois)

## Objectif (1 phrase)

Exposer une **section Angular `<app-licenciement-nul-detection-section>`** qui consomme l'API SF-DT-16-01 pour permettre à l'avocat de saisir 7 booléens de protection + dates contextuelles, et afficher la détection automatique des protections actives, le score de probabilité de nullité, l'indemnité plancher 6 mois et l'ouverture de la réintégration (art. L.1235-3-1 al. 2, FRANCE).

## Pattern de référence

Composant canonique 2026-04-24 : **`harcelement-licenciement-nul-section`** (F-DT-11-02). Layout verdict binaire-like inspiré de `divorce-accepte-section` (bannière palette navy `--available` ELEVEE / or `--medium` MOYENNE / rouge `--danger` FAIBLE).

Toutes les conventions de `ai-skills/frontend-coherence-audit.md` §5 sont appliquées (palette navy/or/rouge classique, `<input type="date">`, `MatSnackBar`, `triggerRefresh()`, JetBrains Mono pour `baseJuridique`/`formule`, Inter ailleurs).

## Contrat API (importé de SF-DT-16-01)

- **Endpoint** : `POST + GET /api/v1/case-files/{caseFileId}/licenciement-nul-detection`
- **Pays** : FRANCE uniquement (400 si BE — gate côté frontend via bannière info).
- **Request** : 7 booléens (`salarieEnceinte`, `salarieAccidentTravail`, `salarieHarceleAvere`, `salarieDiscriminationAlleguee`, `salarieMotifLanceurAlerte`, `salarieMandatRepresentant`, `salarieActionJustice`) + 2 dates contextuelles (`dateAccouchement`, `dateConsolidationAT`) + `salaireMensuelBrutEur` (BigDecimal > 0) + `ancienneteAnnees` (entier ≥ 0, optionnel) + `dateNotificationLicenciement` (LocalDate, requis).
- **Response** : snapshot complet inputs + `protectionsDetectees: string[]`, `nombreProtectionsActives: number`, `nulliteProbable: boolean`, `scoreNullite: number` (0-100), `verdictProbabiliteNullite: 'FAIBLE'|'MOYENNE'|'ELEVEE'`, `indemniteMinimumNuliteEur: number`, `indemniteMinimumMois: 6`, `reintegrationOuverte: boolean`, `baseJuridique: string`, `formule: string`, `messages: string[]`, `country: 'FRANCE'`.
- **Codes erreur** : 400 validation, 404 dossier inaccessible.

## Comportement nominal

1. **Mount** sur dossier travail FR → GET initial. Si 200 → snapshot restauré, form caché. Si 404 → form vide (mode formulaire) + appel `prefillFromAi()`.
2. **Form** : 7 slide-toggles (1 par protection) + 2 datepickers conditionnels (`dateAccouchement` actif si `salarieEnceinte=false` ; `dateConsolidationAT` actif si `salarieAccidentTravail=false`) + 2 numériques (`salaireMensuelBrutEur` requis, `ancienneteAnnees` optionnel) + 1 datepicker `dateNotificationLicenciement` requis.
3. **Soumission** : POST → résultat affiché ; `MatSnackBar` succès ; `CaseDashboardRefreshService.triggerRefresh()`.
4. **Affichage résultat** :
   - Bannière verdict : navy ELEVEE (`check_circle`) / or MOYENNE (`warning`) / rouge FAIBLE (`error`) + score `XX/100`.
   - **Liste de chips colorés** des protections détectées (1 chip par code — palette navy/or selon urgence).
   - Carte "Indemnité minimum 6 mois" en grand JetBrains Mono.
   - Badge "Réintégration ouverte" navy si `reintegrationOuverte=true`.
   - `baseJuridique` + `formule` en JetBrains Mono italique secondaire.
   - Liste `<ul>` `messages` avec `LegalCitationsPipe` pour rendre les citations en `<code>`.
5. **Bouton "Modifier"** → `editMode()` réaffiche le form.

### Gate `workspaceCountry`

Si `workspaceCountry !== 'FRANCE'` → bannière info navy "Outil disponible en France uniquement (art. L.1235-3-1 al. 2). Le pendant Belgique (Loi 1978 art. 63 / CCT 109) est planifié séparément." → pas d'appel HTTP.

### Pré-remplissage IA

`@Input() aiData?: TravailExtractedData | null` (no-op si absent).

Champs pré-remplis depuis `TravailExtractedData` :
- `salaireBrutMensuel` → `salaireMensuelBrutEur` (badge IA + note SF-130-01 si `salaireEstDeduit=true`).
- `dateLicenciement` (ISO YYYY-MM-DD) → `dateNotificationLicenciement` (badge IA).
- Pas de pré-fill des 7 booléens (l'IA ne fournit pas de signal binaire fiable pour `salarieEnceinte`/`salarieAccidentTravail`/etc — l'avocat les saisit explicitement).
- `motifNullitePressenti` consulté pour mapping graceful : `HARCELEMENT_MORAL`/`HARCELEMENT_SEXUEL` → `salarieHarceleAvere=true` ; `DISCRIMINATION` → `salarieDiscriminationAlleguee=true` ; `MATERNITE_PATERNITE` → `salarieEnceinte=true` ; `ACCIDENT_MP` → `salarieAccidentTravail=true` ; `RETORSION` → `salarieActionJustice=true` ; `SYNDICAL` → `salarieMandatRepresentant=true`. Pas de pré-fill si valeur déjà saisie par avocat (provenance null = manuelle).

Provenance par champ : `provenanceSalaire`, `provenanceDateNotification`, `provenanceProtections` (un signal global pour les 7 toggles pré-remplis depuis `motifNullitePressenti` — granularité fine non requise, l'IA pose au max 1 motif).

`ngOnChanges(aiData)` → re-prefill si `showForm() && !result()`.

### Validation F-IA-03 au changement (cohérence alerts)

Fields audités :
- `SALAIRE` — divergence > 10 % vs `aiData.salaireBrutMensuel` (seuil aligné F-DT-09).
- `DATE_NOTIFICATION` — divergence date stricte vs `aiData.dateLicenciement`.
- `PROTECTIONS` — alerte si `aiData.motifNullitePressenti` mappé est présent mais le toggle correspondant est resté `false` (l'avocat contredit l'analyse).

`coherenceAlerts = computed<Partial<Record<LNDAlertField, LNDCoherenceAlert>>>()` gate `showForm()` strict (pattern anti-bug SF-IA-03-12).

`alertsSummary` computed → résumé "N incohérence(s) avec l'analyse" en tête du form.

Builder partagé `CoherenceAlertBuilder<LNDAlertField>` — pas d'interface locale.

Sources scannées (hiérarchie F96 > QUESTION_IA > IA > PIECE_MANQUANTE) :
- `procedureChecks` filtrés sur `critereCode` ∈ {`SALAIRE_BRUT_MENSUEL`, `DATE_LICENCIEMENT`, `LND_PROTECTION_*`}.
- `aiQuestions` filtrés idem.
- `aiData` direct (cf. ci-dessus).
- `piecesManquantes` enrichissement contributor (codes : `SALAIRE_BRUT_MENSUEL`, `LND_DATE_NOTIFICATION`, `LND_PROTECTIONS`).

## Cas d'erreur

| Cas | Comportement |
|-----|--------------|
| GET 404 | Reste en mode form, pré-fill IA si possible |
| GET autre erreur | Reste en mode form, pré-fill IA si possible (fail-open) |
| POST 400 (validation) | `MatSnackBar` rouge avec message backend |
| POST autre erreur | `MatSnackBar` "Erreur lors du calcul" |
| `workspaceCountry !== 'FRANCE'` | Bannière info navy, pas d'appel HTTP, pas de form |

## Critères d'acceptation vérifiables

1. Mount FRANCE déclenche un GET ; mount BELGIQUE ne déclenche aucun appel HTTP.
2. GET 200 restaure les 7 booléens + 2 dates contextuelles + salaire + ancienneté + date notif + result.
3. GET 404 laisse le form vide.
4. `formValid()` exige `salaireMensuelBrutEur > 0` et `dateNotificationLicenciement` non null.
5. POST nominal envoie les 7 booléens + dates + salaire + ancienneté + date notif ; succès → `result` populé, `showForm=false`, `MatSnackBar` succès, `triggerRefresh()` appelé.
6. POST erreur backend → `MatSnackBar` rouge `panelClass='snack-error'`.
7. POST sans form valide → aucun appel HTTP.
8. Pré-fill IA `salaireBrutMensuel` populé → `salaireMensuelBrutEur` set + `provenanceSalaire = 'IA'`.
9. Pré-fill IA `dateLicenciement` ISO → `dateNotificationLicenciement` set + `provenanceDateNotification = 'IA'`.
10. Pré-fill IA `motifNullitePressenti = 'DISCRIMINATION'` → `salarieDiscriminationAlleguee = true` + `provenanceProtections = 'IA'`.
11. `onSalaireChange` efface `provenanceSalaire`.
12. `onDateNotificationChange` efface `provenanceDateNotification`.
13. `coherenceAlerts.SALAIRE` présent si écart > 10 % vs IA, absent sinon.
14. `coherenceAlerts.DATE_NOTIFICATION` présent si dates IA et avocat divergent.
15. `coherenceAlerts` masqué après calcul (`showForm=false`).
16. `verdictBannerClass('ELEVEE')` retourne classe `--available` (navy) ; `MOYENNE` → `--medium` (or) ; `FAIBLE` → `--danger` (rouge classique).
17. `editMode()` ré-affiche le form.
18. `toggleCollapse()` toggle correctement.
19. `ngOnChanges(aiData)` post-mount re-pré-fill si form vide ; n'écrase pas saisie avocat.
20. Entrée `TOOL_REGISTRY` `'F-DT-16-licenciement-nul-detection'` câblée avec `caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes`.

## Plan de test

### Spec Jest (`licenciement-nul-detection-section.component.spec.ts`)

Au moins 18 tests :
- `mount FRANCE → GET initial déclenché`
- `mount BELGIQUE → pas d'appel HTTP`
- `GET 200 → restore 7 booléens + dates + salaire + result + showForm=false`
- `GET 404 → mode formulaire, pas de result`
- `formValid : exige salaire > 0 + dateNotif non null`
- `calculate() POST → body complet, succès → result + snackbar + triggerRefresh`
- `calculate() ignoré si form invalide`
- `calculate() erreur backend → snackbar rouge`
- `prefill IA salaireBrutMensuel + dateLicenciement → valeurs + provenance IA`
- `prefill IA aiData absent → no-op`
- `prefill IA motifNullitePressenti=DISCRIMINATION → salarieDiscriminationAlleguee=true`
- `prefill IA motifNullitePressenti=HARCELEMENT_MORAL → salarieHarceleAvere=true`
- `onSalaireChange efface badge IA`
- `onDateNotificationChange efface badge IA`
- `coherenceAlerts.SALAIRE présent si écart > 10 %`
- `coherenceAlerts.SALAIRE absent si écart ≤ 10 %`
- `coherenceAlerts.DATE_NOTIFICATION présent si divergence stricte`
- `coherenceAlerts gate : alertes masquées après calcul (showForm=false)`
- `verdict ELEVEE → banner navy --available`
- `verdict MOYENNE → banner or --medium`
- `verdict FAIBLE → banner rouge --danger`
- `toggleCollapse fonctionne`
- `editMode ré-affiche le form`
- `ngOnChanges(aiData) re-prefill si form vide`
- `ngOnChanges(aiData) après saisie manuelle ne modifie pas la valeur`

### Compilation TypeScript

`npx tsc --noEmit -p tsconfig.app.json` doit être OK.

## Tables / endpoints / composants impactés

- **Nouveau model** `frontend/src/app/core/models/licenciement-nul-detection.model.ts`.
- **Nouveau service** `frontend/src/app/core/services/licenciement-nul-detection.service.ts`.
- **Nouveau composant** `frontend/src/app/case-files/licenciement-nul-detection-section/` (4 fichiers .ts/.html/.scss/.spec.ts).
- **Modification** `decisional-tools-panel.component.ts` — entrée `TOOL_REGISTRY` `'F-DT-16-licenciement-nul-detection'` (le tool_id backend pré-câblé via migration 142).
- **Aucune modification backend** (API SF-DT-16-01 mergée, contrat figé).

## Hors périmètre

- Pendant BE Loi 1978 / CCT 109 (feature jumelle backlog).
- Détection automatique des 7 booléens depuis documents (extraction LLM côté backend) — couvert ultérieurement.
- Calcul indemnité maxi (juge) — fourchette indicative non requise.
- Pré-fill IA des 7 booléens individuellement (granularité fine non disponible côté `TravailExtractedData`).

## Analyse de cohérence transversale

- **Outils décisionnels frontend audités** : `harcelement-licenciement-nul-section` (template canonique — pattern hérité), `divorce-accepte-section` (palette verdict 3 niveaux référence), `discrimination-section` (slide-toggles + IA partagée), `licenciement-section` (F-DT-08, validity), `motif-grave-be-section` (verdict-like). Aucune divergence : SF-DT-16-02 réutilise strictement le pattern (palette, datepicker `<input type="date">`, gate country bannière info, refresh + snackbar).
- **Pays FR seul** : gate explicite via bannière info ; pas de masquage silencieux.
- **Pas de nouveau pattern UI partagé** introduit. Réutilisation `CoherenceAlertBuilder` + `CoherencePopoverTriggerDirective` + `LegalCitationsPipe` + `SourceExplanationService`.
- **Préoccupation transversale** : aucune (pas d'auth, pas de routing, pas de plan, pas d'IA pipeline modifié — seul un nouveau composant section).

## Impact par domaine métier

- **Droit du travail FRANCE** : oui, cœur de la SF (8 protections art. L.1235-3-1 al. 2).
- **Droit du travail BELGIQUE** : non (gate UI bannière info, pendant non implémenté — backlog).
- **Immigration** : non applicable.
- **Famille** : non applicable.

## Parité des domaines métier (niveau 5 — scoring)

Le scoring est fourni par le backend SF-DT-16-01 (déjà documenté en parité backend). La SF-DT-16-02 n'introduit pas de nouveau scoring frontend — elle se contente de présenter le verdict backend.

→ **Action** : aucune action complémentaire. La parité reste documentée côté backend (PRODUCT_SPEC F-DT-16 + note "non couverture BE — feature jumelle backlog requise").

## Nouveau pattern UI ou service partagé

Aucun. La SF réutilise les patterns canoniques existants :
- `CoherenceAlertBuilder<F>` (shared)
- `CoherencePopoverTriggerDirective` (shared)
- `LegalCitationsPipe` (shared)
- `CaseDashboardRefreshService` (shared)
- `SourceExplanationService` (shared)
- Layout SCSS calé sur `divorce-accepte-section` + `harcelement-licenciement-nul-section`.

## Self-check seuils standard frontend (avant commit)

| Métrique | Seuil min | Statut visé |
|---|---|---|
| `CoherenceAlertBuilder` invocations | ≥ 2 | ≥ 2 (SALAIRE + DATE_NOTIFICATION + PROTECTIONS = 3) |
| `[appCoherencePopover]` triggers | ≥ 3 | 3 (SALAIRE, DATE_NOTIFICATION, PROTECTIONS) |
| `prefillFromAi` invocations | ≥ 2 | 2 (ngOnInit fallback 404 + ngOnChanges) |
| `auto_awesome` icons (badge IA) | ≥ 2 | ≥ 3 (un par champ pré-rempli) |
| `provenance*` signals | ≥ 6 | 3 signals (SALAIRE, DATE_NOTIFICATION, PROTECTIONS) — **note** : la cible standard ≥ 6 vise les composants à 5+ champs IA ; ici 3 champs IA → 3 signals (cohérent avec `harcelement-licenciement-nul-section` qui en a 2). Documenté. |
| `coherenceAlerts` computed | ≥ 1 | 1 |
| handlers (`onXxxChange`) | ≥ 2 | ≥ 9 (7 toggles + salaire + dateNotif + 2 dates contextuelles + ancienneté = 12) |
| Interface locale `CoherenceAlert` | = 0 | 0 (alias type-only via shared) |

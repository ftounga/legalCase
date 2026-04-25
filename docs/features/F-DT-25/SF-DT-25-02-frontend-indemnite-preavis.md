# Mini-spec — F-DT-25 / SF-DT-25-02 Frontend Indemnité compensatrice de préavis FR

## Identifiant

`F-DT-25 / SF-DT-25-02`

## Feature parente

`F-DT-25` — Indemnité compensatrice de préavis (art. L.1234-1 et s. Code du travail FR)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-25-02-frontend-indemnite-preavis`

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02), réf. skill `ai-skills/frontend-coherence-audit.md` §5.
- **Pattern IA** : `immigration-title-decision-section` (F-IM-05) pour `prefillFromAi()` + signals provenance + `coherenceAlerts` + `ngOnChanges()`.
- **Pattern sélecteur CCN** : `anciennete-section` (F-DT-07-05) — `ConventionReferentialService.list()` + `normalizeCode()`.
- **Pattern multi-fields + form complexe** : `harcelement-licenciement-nul-section` (SALAIRE + MOTIF_NULLITE).
- **Helper partagé** : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05/06).

## Contrat API (importé de SF-DT-25-01)

Endpoint `POST /api/v1/case-files/{caseFileId}/indemnite-preavis` + `GET` symétrique.

```typescript
export type FonctionPreavis = 'OUVRIER' | 'EMPLOYE' | 'AGENT_MAITRISE' | 'CADRE';
export type SourceDureePreavis = 'LEGALE' | 'CCN' | 'USAGE';

export interface IndemnitePreavisRequest {
  ancienneteAnnees: number;
  ancienneteMois: number;
  salaireMensuelBrutEur: number;
  conventionCollectiveCode?: string | null;
  fonction: FonctionPreavis;
  exemptionEmployeur: boolean;
  dateRupture: string; // YYYY-MM-DD
}

export interface IndemnitePreavisResponse {
  caseFileId: string;
  ancienneteAnnees: number;
  ancienneteMois: number;
  salaireMensuelBrutEur: number;
  conventionCollectiveCode?: string | null;
  fonction: FonctionPreavis;
  exemptionEmployeur: boolean;
  dateRupture: string;
  dureePreavisMois: number;
  sourceDuree: SourceDureePreavis;
  montantIndemniteEur: number;
  exemptionRetenue: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Objectif

Livrer le composant Angular `<app-indemnite-preavis-section>` qui consomme l'endpoint `POST/GET /api/v1/case-files/{id}/indemnite-preavis` (SF-DT-25-01 backend) pour calculer la durée de préavis applicable (légale art. L.1234-1 / CCN / usage), le montant de l'indemnité compensatrice quand le préavis n'est pas exécuté, et exposer la base juridique + formule.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier FR (DROIT_DU_TRAVAIL, workspace `country = 'FRANCE'`).
2. Le panel décisionnel F-IA-04 affiche l'outil `F-DT-25-indemnite-preavis` (visibilité backend règle ALWAYS_ON FR + travail).
3. Le composant collapsible affiche en header `INDEMNITÉ COMPENSATRICE DE PRÉAVIS` + chip montant (`{montantIndemniteEur} €`) si résultat persisté.
4. À l'ouverture (dépli) : GET → si 200 résultat précédent, affichage du mode résultat ; si 404, formulaire + pré-fill IA depuis `synthesis.travailExtractedData`.
5. Formulaire :
   - `ancienneteAnnees` (`<input type="number">` mat-input, entier ≥ 0, obligatoire) — pré-rempli si IA fournit
   - `ancienneteMois` (`<input type="number">` mat-input, entier 0-11, obligatoire)
   - `salaireMensuelBrutEur` (`<input type="number">`, > 0, obligatoire) — pré-rempli depuis `aiData.salaireBrutMensuel`
   - `conventionCollectiveCode` (`mat-select` via `ConventionReferentialService.list()` filtré FRANCE, optionnel) — pré-rempli depuis `aiData.conventionCollective` via `normalizeCode()`
   - `fonction` (`mat-select`, 4 options OUVRIER/EMPLOYE/AGENT_MAITRISE/CADRE, obligatoire) — pas pré-rempli IA (pas d'extraction actuelle)
   - `exemptionEmployeur` (`mat-slide-toggle`, bool, défaut false) — pas pré-rempli IA
   - `dateRupture` (`<input type="date">`, obligatoire, ≤ aujourd'hui) — pré-rempli depuis `aiData.dateLicenciement` si disponible
6. À la soumission (POST) : appel service, puis affichage mode résultat + `MatSnackBar` succès + `CaseDashboardRefreshService.triggerRefresh()`.
7. Mode résultat :
   - Carte 1 "Durée préavis" : `dureePreavisMois` mois + badge `sourceDuree` (LEGALE/CCN/USAGE).
   - Carte 2 "Montant indemnité" : `montantIndemniteEur` € + indicateur `exemptionRetenue` (chip "Exemption retenue" si vrai).
   - `formule` en JetBrains Mono, `baseJuridique` en JetBrains Mono italique.
   - `messages[]` rendus via `LegalCitationsPipe`.
   - Bouton "Modifier" pour ré-éditer.

### Gate pays

- Pas de gate UI explicite dans le composant. L'outil est exclusivement FR (visibilité gérée par backend règle `decision_tool_visibility_rules` + le backend renvoie `country: 'FRANCE'` dans la réponse).
- Toutefois, par cohérence avec template canonique : si `workspaceCountry !== 'FRANCE'` → bannière info navy "Outil France uniquement — la durée de préavis BE est régie par la loi du 03/07/1978 et CP applicable, voir outil dédié BE." Pas de form, pas d'appel HTTP.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GET 404 | Mode formulaire + pré-fill IA |
| GET autre erreur | Reste en mode formulaire vide (fail-open) |
| POST 400 (validation backend) | `MatSnackBar` rouge avec message backend |
| POST 403/404 | `MatSnackBar` générique |
| Backend down | `MatSnackBar` "Erreur lors du calcul" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- **Autres outils métier** : composants livrés 2026-04-24 (harcelement, inaptitude, heures-sup, indemnite-precarite-cdd, travail-dissimule, motif-grave-be). Réutilise mêmes patterns : pas de pattern nouveau.
- **Autres pays** : BE non applicable côté backend (FR uniquement, contrat API). Bannière info gracieuse si `workspaceCountry === 'BELGIQUE'`.
- **Autres domaines** : DROIT_DU_TRAVAIL FRANCE uniquement.
- **Autres UI patterns** : aucune introduction de nouveau pattern shared. Réutilise `CoherencePopoverTriggerDirective`, `CoherenceAlertBuilder`, `LegalCitationsPipe`, `ConventionReferentialService`, `CaseDashboardRefreshService`, `SourceExplanationService`.
- **Auth / workspace / plans / navigation** : aucun impact — composant isolé.

### Niveaux de vérification

- **Modèle TypeScript** : `indemnite-preavis.model.ts` (Request / Response + enums) calqué sur SF-DT-25-01.
- **Service / logique métier** : `IndemnitePreavisService` = wrapper HttpClient POST/GET (calque `harcelement-nullite.service.ts`).
- **Entité JPA + schéma DB** : aucune modification (backend mergé sépraément).
- **Tests existants** : backend tests verts (responsabilité SF-DT-25-01). Frontend à créer (≥ 15 tests).

### Cas spécifique : outil décisionnel

- **Cohérence IA (F-IA-03)** : alertes :
  - `SALAIRE` → divergence relative > 10 % entre `aiData.salaireBrutMensuel` et saisie (WARNING). Source IA + PIECE_MANQUANTE optionnel.
  - `DATE_RUPTURE` → divergence ≥ 15 jours entre `aiData.dateLicenciement` et `dateRupture` saisie (WARNING). Source IA.
  - `CONVENTION` → divergence stricte upper-case entre `normalizeCode(aiData.conventionCollective)` et `conventionCollectiveCode` saisi (WARNING). Source IA.
- **Refresh dashboard (F-IA-02)** : `triggerRefresh()` appelé après POST succès.
- **Pré-remplissage IA** : `prefillFromAi()` branche `aiData.salaireBrutMensuel` → `salaireMensuelBrutEur` + `aiData.dateLicenciement` → `dateRupture` + `aiData.conventionCollective` (normalized) → `conventionCollectiveCode`. Badges "Pré-rempli depuis l'analyse" via signals provenance effacés au `onXxxChange()`.

### Nouveau pattern UI ou service partagé

Aucun nouveau pattern. Le composant compose uniquement des patterns canoniques déjà partagés :
- `CoherenceAlertBuilder` (SF-155-05/06)
- `CoherencePopoverTriggerDirective`
- `ConventionReferentialService` (SF-129-01)
- `SourceExplanationService` (SF-IA-03-15a)

---

## Impact par domaine métier

Cette feature est **sensible au domaine DROIT_DU_TRAVAIL** uniquement.

- **Droit du travail FR** : oui — outil principal calcule l'indemnité de préavis L.1234-1 et s.
- **Droit du travail BE** : non — la durée de préavis BE est régie par CCT 109 + ancienneté, déjà couverte par F-DT-09 (comparateur indemnités) et F-DT-27 (motif grave). Une SF jumelle BE pourrait être ouverte au backlog si le besoin est identifié, mais hors périmètre V1.
- **Immigration FR/BE** : non applicable.
- **Famille FR/BE** : non applicable.

---

## Critères d'acceptation

1. Composant `<app-indemnite-preavis-section>` standalone, importé dans `decisional-tools-panel/TOOL_REGISTRY` sous tool_id `F-DT-25-indemnite-preavis`.
2. Inputs : `caseFileId` (required), `workspaceCountry`, `aiData?: TravailExtractedData`, `procedureChecks?`, `aiQuestions?`, `piecesManquantes?`.
3. Form complet : 7 champs (ancienneteAnnees + ancienneteMois + salaireMensuelBrutEur + conventionCollectiveCode + fonction + exemptionEmployeur + dateRupture) avec validation min/max.
4. Pré-fill IA fonctionnel sur 3 champs (salaire, dateRupture, convention) + badges `auto_awesome` "Pré-rempli depuis l'analyse" effacés au changement manuel.
5. `coherenceAlerts` computed : 3 alertes (`SALAIRE`, `DATE_RUPTURE`, `CONVENTION`) via `CoherenceAlertBuilder`.
6. Mode résultat : carte durée préavis + badge `sourceDuree`, carte montant + chip exemption + formule JetBrains Mono + baseJuridique + messages.
7. `triggerRefresh()` après POST succès.
8. `MatSnackBar` pour erreurs HTTP.
9. Gate `workspaceCountry !== 'FRANCE'` → bannière info navy.
10. Au moins 15 tests Jest verts (mount, GET 200/404, POST succès/erreur, pré-fill IA, alertes cohérence, handlers, gate pays).
11. `tsc --noEmit` passe.
12. Self-check pré-commit (tous seuils > 0) : voir CLAUDE.md ligne 190.

## Plan de test minimal

### Unitaires (component.spec.ts)

1. `FRANCE` → form affiché, pas de bannière country.
2. `BELGIQUE` → bannière info, pas de form, pas de GET HTTP.
3. GET 200 → mode résultat, valeurs persistées prises, pas de badge IA.
4. GET 404 → mode formulaire, pré-fill IA si `aiData` fourni.
5. `prefillFromAi()` complet : salaire + dateRupture + conventionCollective normalisé → 3 badges IA.
6. `prefillFromAi()` sans aiData → aucune valeur ni badge.
7. `prefillFromAi()` salaire ≤ 0 → pas de pré-fill salaire.
8. `prefillFromAi()` convention non normalisable → pas de pré-fill convention.
9. `onSalaireChange()` efface badge IA salaire.
10. `onConventionChange()` efface badge IA convention.
11. `onDateRuptureChange()` efface badge IA date rupture.
12. `coherenceAlerts.SALAIRE` présent si écart > 10 %, absent sinon.
13. `coherenceAlerts.DATE_RUPTURE` présent si écart ≥ 15 jours, absent sinon.
14. `coherenceAlerts.CONVENTION` présent si codes divergent, absent sinon.
15. `formValid()` false sur valeurs manquantes/négatives, true sur form complet.
16. `calculate()` POST avec body correct, snackbar succès, `triggerRefresh()` appelé.
17. `calculate()` erreur backend → snackbar rouge, calculating reset.
18. `calculate()` ignoré si form invalide (pas d'appel HTTP).
19. `ngOnChanges(aiData)` post-mount rafraîchit pré-fill si form vide.
20. `ngOnChanges(aiData)` après saisie manuelle n'écrase pas saisie avocat.
21. Alertes masquées après showForm=false (résultat affiché).

### Intégration (panel)

- Ajout entry `TOOL_REGISTRY` → fixture panel + GET visibility renvoie `F-DT-25-indemnite-preavis` → composant rendu (test léger inclus dans suite frontend existante).

### Isolation workspace

- Pas d'isolation custom (caseFileId vient du contexte parent — GET/POST scopés). Pas de test workspace dédié.

---

## Tables / endpoints / composants impactés

### Endpoints consommés

- `POST /api/v1/case-files/{id}/indemnite-preavis`
- `GET /api/v1/case-files/{id}/indemnite-preavis`
- `GET /api/v1/referentials/conventions` (déjà cache via `ConventionReferentialService`)
- `GET /api/v1/case-files/{id}/source-explanations` (SF-IA-03-15a)

### Composants créés

- `frontend/src/app/core/models/indemnite-preavis.model.ts`
- `frontend/src/app/core/services/indemnite-preavis.service.ts`
- `frontend/src/app/case-files/indemnite-preavis-section/indemnite-preavis-section.component.{ts,html,scss,spec.ts}`

### Composants modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` : ajout entry `F-DT-25-indemnite-preavis` dans `TOOL_REGISTRY`.

### Tables impactées

- Aucune (backend déjà mergé en SF-DT-25-01).

---

## Hors périmètre

- Backend (couvert par SF-DT-25-01 mergé séparément).
- Belgique : pas couvert (durée de préavis BE = autre logique CCT 109 + Loi 1978).
- Pré-fill `fonction` : pas couvert (l'IA ne l'extrait pas actuellement).
- Pré-fill `exemptionEmployeur` : pas couvert (qualification juridique requise).
- Outils PDF / export : hors V1.

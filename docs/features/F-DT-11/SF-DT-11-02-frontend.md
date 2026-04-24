# Mini-spec — F-DT-11 / SF-DT-11-02 Harcèlement moral/sexuel + indemnité licenciement nul — FRONTEND

## Identifiant
`F-DT-11 / SF-DT-11-02`

## Feature parente
`F-DT-11`

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-11-02-harcelement-licenciement-nul-frontend`

---

## Objectif

Composant Angular standalone `<app-harcelement-licenciement-nul-section>` permettant à l'avocat de saisir le salaire mensuel de référence + le motif de nullité (FR ou BE selon le workspace), d'envoyer au backend SF-DT-11-01, et d'afficher le résultat (indemnité minimum, formule, base juridique, messages).

**Parallélisation** : cette SF est développée **en parallèle** de SF-DT-11-01 (backend). Elle se branche sur le contrat API figé dans SF-DT-11-01. Les tests unitaires utilisent un mock du service — pas besoin que le backend soit mergé pour passer les tests frontend. L'intégration réelle sera testée lorsque les deux PRs seront mergées.

---

## Contrat API (importé de SF-DT-11-01 — figé)

### Endpoint
`POST /api/v1/case-files/{caseFileId}/harcelement-licenciement-nul`
`GET /api/v1/case-files/{caseFileId}/harcelement-licenciement-nul`

### Modèle TypeScript
```typescript
// frontend/src/app/core/models/harcelement-nullite.model.ts

export type MotifNulliteFr =
  | 'HARCELEMENT_MORAL' | 'HARCELEMENT_SEXUEL' | 'DISCRIMINATION'
  | 'GROSSESSE' | 'SALARIE_PROTEGE' | 'LIBERTE_FONDAMENTALE'
  | 'ACTION_JUSTICE' | 'ALERTE_ETHIQUE';

export type MotifNulliteBe =
  | 'HARCELEMENT_MORAL_BE' | 'HARCELEMENT_SEXUEL_BE'
  | 'VIOLENCE_AU_TRAVAIL_BE' | 'DISCRIMINATION_BE';

export type MotifNullite = MotifNulliteFr | MotifNulliteBe;

export interface HarcelementNulliteRequest {
  salaireMensuelReference: number;
  motifNullite: MotifNullite;
}

export interface HarcelementNulliteResponse {
  caseFileId: string;
  salaireMensuelReference: number;
  motifNullite: MotifNullite;
  country: 'FRANCE' | 'BELGIQUE';
  indemniteMinimumNullite: number;
  formule: string;
  baseJuridique: string;
  messages: string[];
}
```

---

## Comportement attendu

### Cas nominal

1. Composant standalone avec `@Input() caseFileId: string` et `@Input() workspaceCountry: 'FRANCE'|'BELGIQUE'` (pour filtrer les motifs disponibles).
2. Au mount : `GET` pour charger une analyse existante → affichage lecture seule + bouton "Modifier".
3. En mode formulaire :
   - Champ salaire mensuel de référence (mat-form-field outline + numeric input validation `> 0`).
   - Sélecteur `motifNullite` (mat-select) filtré selon `workspaceCountry` :
     - FRANCE → 8 options (HARCELEMENT_MORAL, …, ALERTE_ETHIQUE)
     - BELGIQUE → 4 options (HARCELEMENT_MORAL_BE, …, DISCRIMINATION_BE)
   - Bouton "Calculer" (disabled si form invalide).
4. POST → persiste + affiche résultat.
5. Après succès : déclenche `CaseDashboardRefreshService.triggerRefresh()` pour rafraîchir les cards du dashboard.
6. Affichage résultat :
   - Indemnité minimum en gras, grande police.
   - Formule grise (ex. "6 mois × 3 000,00 € = 18 000,00 €").
   - Base juridique en monospace.
   - Messages en liste `ul`.

### Cas d'erreur
| Situation | UI |
|---|---|
| 400 backend | MatSnackBar "Erreur : [message]", pas de modification état |
| 404 pendant GET initial | Pas d'affichage, formulaire ouvert |

---

## Composants à créer

1. **Service** : `frontend/src/app/core/services/harcelement-nullite.service.ts` — wrapper `HttpClient` avec `getBaseUrl()` (pattern identique à `RuptureConvIndemniteService`).
2. **Modèle** : `frontend/src/app/core/models/harcelement-nullite.model.ts` — interfaces ci-dessus.
3. **Composant** : `frontend/src/app/case-files/harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component.{ts,html,scss,spec.ts}` — pattern exact de `rupture-conv-indemnite-section`.
4. **Enregistrement dans le panel F-IA-04** : `TOOL_REGISTRY` dans `decisional-tools-panel.component.ts` avec `tool_id: 'F-DT-11-harcelement-licenciement-nul'`.

---

## Tests (Angular / Jest)

- [ ] Service `HarcelementNulliteService` : `post` et `get` appellent la bonne URL avec bons headers.
- [ ] Composant — mount avec `workspaceCountry=FRANCE` → 8 options dans le select.
- [ ] Composant — mount avec `workspaceCountry=BELGIQUE` → 4 options.
- [ ] Composant — POST réussi → affichage résultat + refresh dashboard déclenché.
- [ ] Composant — erreur HTTP → snackbar affiché.
- [ ] Composant — form invalide (salaire vide) → bouton calculer disabled.

---

## Design System

- `mat-card` avec entête (icône + titre "Indemnité licenciement nul — harcèlement").
- `mat-form-field appearance="outline"`.
- `mat-error` sous chaque input.
- Couleurs : palette charte navy/or, aucune couleur hors DESIGN_SYSTEM.md.
- Police : Inter pour texte, JetBrains Mono pour base juridique et formule.

---

## Impact par domaine métier

DROIT_DU_TRAVAIL FR + BE. Non applicable immigration/famille.

## Parité

Niveau 3. Parité ≥5 non applicable.

---

## Périmètre

### Hors scope
- Intégration du composant dans `case-file-detail.component.html` — sera fait automatiquement par le panel F-IA-04 via `TOOL_REGISTRY` (une fois l'entrée ajoutée, le panel l'affiche quand la règle `ALWAYS_ON` déclenche).
- Export PDF — hors scope.
- Détection IA automatique du motif — hors scope.

---

## Dépendances
- SF-DT-11-01 (backend) — développée en **parallèle**, même jour.
- F-IA-04 (panel) — done.

---

## Notes
- Pattern de référence complet : `rupture-conv-indemnite-section` (dans même repo). Copier sa structure verbatim et adapter les champs.
- Les options de motif sont hardcodées dans le composant (pas de call API pour les récupérer).

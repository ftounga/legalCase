# Mini-spec — F-207 / SF-207-05b-frontend Outil référé tribunal du travail BE (UI)

## Identifiant

`F-207 / SF-207-05b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-05b-frontend-refere-tribunal-travail-be`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern miroir : `c4-onem-checklist-section` (#1133) — checklist + verdict + génération texte copiable.

## Objectif

Section frontend de l'outil référé tribunal du travail BE (backend SF-207-05 #1147). Formulaire 7+ champs (motif, dates, conditions, mesure provisoire) + verdict 3 états (ELIGIBLE/INCERTAIN/NON_ELIGIBLE) + score conditions + squelette de requête copiable. BE-only.

## Contrat API (figé #1147)

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/refere-tribunal-travail-be`

Inputs :
```ts
{
  motifUrgence: 'HARCELEMENT' | 'SALAIRE_IMPAYE' | 'MODIFICATION_UNILATERALE' | 'AUTRE';
  motifUrgenceDescription: string;
  dateFaitGenerateur: string;                   // ISO, requis
  dateDemarcheAmiable?: string | null;
  preuveUrgenceJointe: boolean;
  mesureProvisoireDemandee: string;
  perilEnDemeure: boolean;
  competenceTerritorialeIdentifiee: boolean;
}
```

Réponse 200 :
```ts
{
  verdict: 'REFERE_ELIGIBLE' | 'REFERE_INCERTAIN' | 'REFERE_NON_ELIGIBLE';
  conditionsNonRemplies: string[];   // enum names
  scoreConditions: number;            // 0-5
  requeteSquelette: string | null;    // null si NON_ELIGIBLE
  baseJuridique: string;
  etapeSuivante: 'DEPOSER_REQUETE' | 'RENFORCER_DOSSIER' | 'ALTERNATIVE_PROCEDURE_FOND';
}
```

## Comportement

Section `refere-tribunal-travail-be-section.component` — pattern F-IA-04 + génération texte copiable (Clipboard API).

### Formulaire

- Select `motifUrgence` (4 options humanisées).
- Textarea `motifUrgenceDescription` (placeholder ≥ 30 caractères).
- `dateFaitGenerateur` (date, requis).
- `dateDemarcheAmiable` (date, optionnel).
- Checkbox `preuveUrgenceJointe`.
- Checkbox `perilEnDemeure`.
- Textarea `mesureProvisoireDemandee` (placeholder ≥ 10 caractères).
- Checkbox `competenceTerritorialeIdentifiee`.
- Bouton « Évaluer l'éligibilité au référé ».

### Pré-fill IA

| Champ | Source | Provenance |
|---|---|---|
| `motifUrgence` | `aiData.motifUrgenceDetecte` (whitelist 4 codes) | `provenanceMotif` |
| `dateFaitGenerateur` | `aiData.dateFaitGenerateurUrgence` | `provenanceDate` |
| `perilEnDemeure` | `aiData.perilImmediatPresume` | `provenancePeril` |

`getPrefillCount` parité stricte (0-3).

### Verdict

Badge :
- Vert `REFERE_ELIGIBLE`
- Ambre `REFERE_INCERTAIN`
- Rouge `REFERE_NON_ELIGIBLE`

Affichage `scoreConditions` / 5 (barre de progression colorée).

Si `conditionsNonRemplies.length > 0` : liste à puces humanisée (mapping enum → libellé FR).

Si `requeteSquelette !== null` : `<mat-card>` avec `<textarea readonly>` + bouton « Copier la requête » (Clipboard API).

Encart `etapeSuivante` :
- ELIGIBLE → vert « Déposer la requête au greffe du tribunal du travail compétent. »
- INCERTAIN → ambre « Renforcer le dossier (preuve d'urgence, compétence) avant dépôt. »
- NON_ELIGIBLE → information « La procédure de fond est plus adaptée. »

`baseJuridique` en `JetBrains Mono`.

### Refresh dashboard, validation F-IA-03, erreurs

Standard (cf. patterns précédents).

## TOOL_REGISTRY

`refere-tribunal-travail-be` inséré après `at-fedris-declaration` dans `decisional-tools-panel.component.ts`. Theme `URGENCES` (nouveau) ou `DELAIS` (cohérence) — l'agent choisit ; recommandation : créer un thème ou utiliser `URGENCES` si déjà présent, sinon `DELAIS`.

## Visibility seed

Migration `XXX-add-refere-tribunal-travail-be-visibility.xml` (prochain après 260) :
- INSERT : `tool_id='refere-tribunal-travail-be'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `layer='ALWAYS_ON'`, priority 93.

Justification ALWAYS_ON : référé = outil transversal urgence, doit rester accessible (cf. étape 0bis F-207).

## Critères d'acceptation

- [ ] Section rend formulaire + verdict + score + squelette copiable ; gate `BELGIQUE` strict.
- [ ] Pré-fill 3 champs ; modification → provenance `null`.
- [ ] `getPrefillCount` : 0/1/2/3.
- [ ] 3 verdicts colorés correctement ; barre score / 5.
- [ ] `requeteSquelette` non null → encart avec bouton copier ; null → encart caché.
- [ ] `etapeSuivante` affichée avec encart coloré.
- [ ] `MatSnackBar` sur erreur ; refresh dashboard.
- [ ] Migration backend ALWAYS_ON visibilité ; `DecisionToolVisibilityIntegrityIT` vert.
- [ ] Tests Jest : prefill-rules (5+), component (10+).

## Composants

Sous `frontend/src/app/case-files/refere-tribunal-travail-be-section/` : `*.{ts,html,scss,spec.ts}` + prefill-rules.
Modèle + service + modifs panel + case-analysis.model.ts (3 nouveaux fields BE).
Migration backend.

## Dépendances

- Backend SF-207-05 (#1147 mergé).
- Pattern frontend `c4-onem-checklist-section` (lettre rectificative copiable proche du squelette requête).

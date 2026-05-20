# Mini-spec — F-207 / SF-207-04b-frontend Outil déclaration AT Fedris (UI)

## Identifiant

`F-207 / SF-207-04b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-04b-frontend-at-fedris-declaration`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern miroir : `contestation-c4-onem-section` (#1139, le plus récent pattern décisionnel BE livré).

## Objectif

Section frontend de l'outil déclaration AT Fedris (consommant backend SF-207-04 #1142). Formulaire 4 dates + verdict 5 états coloré + encart conséquences. Modes prospectif (3 verdicts délai) + rétrospectif (2 verdicts déclaration). BE-only.

## Contrat API (figé en #1142)

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/at-fedris-declaration`

Inputs :
```ts
{
  dateAccident: string;                          // ISO, requis
  dateConnaissanceEmployeur?: string | null;     // default = dateAccident
  dateActionEnvisagee?: string | null;           // default today
  dateDeclarationEffectuee?: string | null;      // si fourni → mode rétrospectif
}
```

Réponse 200 :
```ts
{
  verdict: 'DELAI_OUVERT' | 'DELAI_IMMINENT' | 'DELAI_DEPASSE'
         | 'DECLARATION_DANS_LES_TEMPS' | 'DECLARATION_HORS_DELAI';
  dateLimiteDeclaration: string;
  joursRestants: number;                          // signé négatif = jours de retard
  regleAppliquee: string;
  baseJuridique: string;
  formuleCalcul: string;
  consequencesNonRespect: string;
}
```

404 si workspace FR / autre workspace ; 400 si dates incohérentes.

## Comportement

Section `at-fedris-declaration-section.component` — pattern F-IA-04.

### Formulaire

- `dateAccident` (date, requis).
- `dateConnaissanceEmployeur` (date, optionnel) — placeholder « (par défaut = date d'accident) ».
- `dateActionEnvisagee` (date, optionnel) — placeholder « (par défaut = aujourd'hui) ».
- Checkbox « Déclaration déjà effectuée ? » → si coché, affiche `dateDeclarationEffectuee` (date, requis dans ce cas).
- Bouton « Calculer le délai Fedris ».

### Pré-fill IA

| Champ | Source `aiData` |
|---|---|
| `dateAccident` | `aiData.dateAccident` (livré SF-207-04 backend) |
| `dateConnaissanceEmployeur` | `aiData.dateConnaissanceAccidentEmployeur` |

`dateDeclarationEffectuee` et `dateActionEnvisagee` ne sont pas pré-remplis (info circonstancielle saisie par l'avocat).

`getPrefillCount` static — parité stricte.

### Verdict

Badge coloré :
- **Vert** : `DELAI_OUVERT` ou `DECLARATION_DANS_LES_TEMPS`
- **Ambre** : `DELAI_IMMINENT`
- **Rouge** : `DELAI_DEPASSE` ou `DECLARATION_HORS_DELAI`

Si `joursRestants < 0` (mode rétrospectif `HORS_DELAI` ou prospectif `DEPASSE`) → afficher « Retard de N jour(s) » (valeur absolue).

Encart d'information :
- `dateLimiteDeclaration` en évidence.
- `regleAppliquee` (codename).
- `baseJuridique` + `formuleCalcul` en `JetBrains Mono`.
- `consequencesNonRespect` dans un encart d'alerte (rouge si dépassé/hors délai, info ambre si imminent).

### Validation F-IA-03

2 champs pré-remplissables (`dateAccident`, `dateConnaissanceEmployeur`) → 2 alertes possibles. Pattern `coherenceAlerts` + popover.

### Refresh dashboard

`CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST.

### Erreurs

`MatSnackBar`. `mat-error` pour validation Bean.

## TOOL_REGISTRY

`at-fedris-declaration` inséré après `contestation-c4-onem` dans `decisional-tools-panel.component.ts`. Theme `DELAIS`. Inputs standard.

## Visibility seed

Migration `XXX-add-at-fedris-declaration-visibility.xml` (prochain après 258) : INSERT `decision_tool_visibility_rules` `tool_id='at-fedris-declaration'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `layer='ALWAYS_ON'`, priority 92, `trigger_field=NULL`, `trigger_value=NULL`.

Justification ALWAYS_ON : un dossier AT est rare mais quand il existe, l'outil est critique (délai 8 j). Pour des workspaces BE Travail, le rendu coût-avantage de la visibilité ALWAYS_ON est faible (l'avocat ignore l'outil si dossier non-AT). Alternative CONTEXTUAL avec trigger `at_mp_detecte` (existe dans `TravailExtractedData` via F-166) — à privilégier si on veut le panneau plus épuré. **Décision SF-207-04b : ALWAYS_ON par cohérence avec les 3 outils BE déjà livrés** ; un futur audit pourra basculer en CONTEXTUAL si feedback utilisateur.

## Conformité F-IA-04 / Critères

Standard pattern (cf. SF-207-03b). Critères :

- [ ] Section rend formulaire + verdict + encart conséquences ; gate `BELGIQUE` strict.
- [ ] Pré-fill 2 champs ; modification → provenance `null`.
- [ ] `getPrefillCount` : 0/1/2 selon `aiData`.
- [ ] Mode rétrospectif activé par checkbox → `dateDeclarationEffectuee` apparaît, devient requis.
- [ ] Verdict coloré correct sur les 5 états ; `joursRestants` négatif affiché « Retard de N j ».
- [ ] `MatSnackBar` sur erreur ; refresh dashboard appelé.
- [ ] Migration backend visibility ALWAYS_ON appliquée ; `DecisionToolVisibilityIntegrityIT` vert.
- [ ] Tests Jest : prefill-rules (4+ tests), component (8+ tests).

## Composants

Sous `frontend/src/app/case-files/at-fedris-declaration-section/` : `*.{ts,html,scss,spec.ts}` + prefill-rules `*.{ts,spec.ts}`.
Modèle : `at-fedris-declaration.model.ts`.
Service : `at-fedris-declaration.service.ts`.
Modifs : `decisional-tools-panel.component.ts` + `case-analysis.model.ts` (ajout 2 fields).
Migration backend : `XXX-add-at-fedris-declaration-visibility.xml`.

## Dépendances

- Backend SF-207-04 (#1142 mergé).
- Pattern frontend `contestation-c4-onem-section` (#1139).

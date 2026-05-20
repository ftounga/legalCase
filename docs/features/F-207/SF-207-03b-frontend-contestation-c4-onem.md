# Mini-spec — F-207 / SF-207-03b-frontend Outil contestation C4 ONEM (UI)

## Identifiant

`F-207 / SF-207-03b-frontend` · Statut : `ready` · Date : 2026-05-20 · Branche : `feat/SF-207-03b-frontend-contestation-c4-onem`

## Cadrages amont

Étape 0 / 0 bis F-207 livrées #1119. Pattern : `c4-onem-checklist-section` (SF-207-02b, vient d'être livré #1133) — modèle le plus proche (form + verdict + paliers + UI BE-only).

## Objectif

Section frontend de l'outil contestation C4 ONEM (consommant backend SF-207-03 #1137). Affiche un formulaire avec basculement Cas A (recours admin) / Cas B (recours tribunal) selon `recoursAdminDejaForme`, restitue le verdict 6 états + les 2 paliers ADMIN/TRIBUNAL. BE-only.

## Contrat API (figé)

`POST` + `GET /api/v1/case-files/{caseFileId}/decision-tools/contestation-c4-onem`

Inputs :
```ts
{
  dateNotificationDecisionOnem: string;        // ISO, requis
  dateActionEnvisagee?: string | null;          // default today Europe/Brussels
  recoursAdminDejaForme: boolean;
  dateDecisionDirecteur?: string | null;        // requis si recoursAdminDejaForme=true
}
```

Réponse 200 :
```ts
{
  verdict: 'RECOURS_ADMIN_OUVERT' | 'RECOURS_ADMIN_IMMINENT' | 'RECOURS_ADMIN_PRESCRIT'
         | 'RECOURS_TRIBUNAL_OUVERT' | 'RECOURS_TRIBUNAL_IMMINENT' | 'RECOURS_TRIBUNAL_PRESCRIT';
  paliers: { type: 'ADMIN' | 'TRIBUNAL'; dateLimite: string | null; joursRestants: number | null; baseJuridique: string }[];
  etapeSuivante: 'RECOURS_ADMIN_DIRECTEUR' | 'RECOURS_TRIBUNAL_TRAVAIL' | 'FORCLUSION_TOTALE' | 'AUCUNE';
  baseJuridique: string;
  formuleCalcul: string;
  // inputs persistés
}
```

404 si workspace FR / autre workspace.

## Comportement

Section `contestation-c4-onem-section.component` — pattern F-IA-04. Inputs : `caseFileId`, `workspaceCountry`, `aiData? : TravailExtractedData`, `procedureChecks?`, `aiQuestions?`, `piecesManquantes?`.

### Formulaire

- Toggle `recoursAdminDejaForme` (radio/switch) : « Recours administratif déjà formé ? Non / Oui ».
- Champ `dateNotificationDecisionOnem` (date, requis).
- Champ `dateActionEnvisagee` (date, optionnel, par défaut affichage today).
- Champ `dateDecisionDirecteur` (date) — visible **uniquement si** `recoursAdminDejaForme === true`. Requis dans ce cas.
- Bouton « Calculer les délais de contestation » → POST.

### Pré-fill IA

| Champ | Source `aiData` | Provenance signal |
|---|---|---|
| `dateNotificationDecisionOnem` | `aiData.dateNotificationDecisionOnem` (livré SF-207-03 backend) | `provenanceDateNotification` |
| `dateDecisionDirecteur` | `aiData.dateDecisionDirecteur` | `provenanceDateDirecteur` |
| `recoursAdminDejaForme` | `aiData.recoursAdminDejaForme` (Boolean nullable côté backend → bool côté front, défaut `false`) | `provenanceRecoursAdmin` |

`getPrefillCount(input)` parité stricte avec `prefillFromAi()`.

### Affichage du verdict

Badge coloré :
- Vert : `*_OUVERT` (admin ou tribunal)
- Ambre : `*_IMMINENT`
- **Rouge** : `*_PRESCRIT` + alerte `etapeSuivante = FORCLUSION_TOTALE` si tribunal prescrit
- `RECOURS_ADMIN_PRESCRIT` (sans tribunal entamé) : rouge mais avec alerte spéciale « Saut palier admin — voir avec le tribunal du travail directement (CJ 580 2°) ».

### Affichage des 2 paliers

Tableau ou cartes empilées :
- **Palier ADMIN** (toujours affiché) : `dateLimite` + `joursRestants` (avec couleur ambre si ≤ 7, rouge si ≤ 0) + base juridique en `JetBrains Mono`.
- **Palier TRIBUNAL** : si `dateLimite !== null` → affichage similaire (seuils 14 j) ; sinon affichage informatif « Indéterminé tant que la décision du Directeur n'est pas notifiée ».

`baseJuridique` globale + `formuleCalcul` en monospace en bas.

### Validation F-IA-03

`coherenceAlerts` computed sur les 3 champs pré-remplissables. Pattern canonique.

### Refresh dashboard

`CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST.

### Erreurs

`MatSnackBar` pour 400/500. `mat-error` pour validation Bean.

## Entrée TOOL_REGISTRY

`contestation-c4-onem` inséré **immédiatement après** `c4-onem-checklist` dans `decisional-tools-panel.component.ts` (séquence métier : C4 d'abord, sa contestation ensuite). `inputs:` standard, `TOOL_LABEL`, `TOOL_ICON`, `THEME_BY_TOOL_ID = 'DELAIS'` (calculateur de délais — cohérent avec prescription).

## Visibility seed

Migration `XXX-add-contestation-c4-onem-visibility.xml` (prochain numéro après 256) :
- INSERT `decision_tool_visibility_rules` : `tool_id='contestation-c4-onem'`, `country='BELGIQUE'`, `legal_domain='DROIT_DU_TRAVAIL'`, `layer='ALWAYS_ON'` (transversal : tous les dossiers BE Travail bénéficient de l'outil), `trigger_field=NULL`, `trigger_value=NULL`.

## Conformité F-IA-04

- [x] Palette navy/or/vert ; rouge **réservé** PRESCRIT/FORCLUSION.
- [x] `<input type="date">` (PAS `MatDatepicker`).
- [x] `JetBrains Mono` pour `baseJuridique` et `formuleCalcul`.
- [x] Gate `workspaceCountry === 'BELGIQUE'`.
- [x] `MatSnackBar` erreurs.
- [x] Refresh dashboard `next:`.
- [x] Pré-fill IA + provenance + badges.
- [x] Validation F-IA-03 `coherenceAlerts` + popover.
- [x] `getPrefillCount(input)` static, parité stricte.
- [x] Entrée TOOL_REGISTRY symétrique.

## Critères d'acceptation

- [ ] Section rend formulaire conditionnel + verdict ; gate `BELGIQUE` strict.
- [ ] Toggle `recoursAdminDejaForme` masque/affiche `dateDecisionDirecteur`.
- [ ] Pré-fill IA fonctionne sur les 3 champs ; modification → provenance `null`.
- [ ] `getPrefillCount` retourne 0/1/2/3 selon `aiData`.
- [ ] Validation F-IA-03 sur les champs pré-remplissables.
- [ ] Verdict ADMIN/TRIBUNAL coloré (vert/ambre/rouge) + alerte FORCLUSION si tribunal prescrit + alerte saut palier admin si admin prescrit en Cas A.
- [ ] Les 2 paliers affichés avec leurs dates/jours/base juridique.
- [ ] `MatSnackBar` sur erreur réseau ; refresh dashboard appelé sur succès.
- [ ] Entrée TOOL_REGISTRY après c4-onem-checklist ; intégrité visibility verte.
- [ ] Migration backend ALWAYS_ON / BELGIQUE / DROIT_DU_TRAVAIL.

## Hors scope

- Backend (livré #1137).
- Génération de la lettre de recours admin / requête tribunal (template texte) — peut être enrichi en V2.
- Pourvoi en cassation (outil distinct, hors F-207).

## Plan de test (Jest)

- [ ] `contestation-c4-onem-section-prefill-rules.spec.ts` — 5+ tests (0/1/2/3 champs, mapping booléen).
- [ ] `contestation-c4-onem-section.component.spec.ts` — 8+ tests (toggle Cas A/B, pré-fill, badges, verdict 6 états, paliers, alerte FORCLUSION, snackbar erreur, refresh dashboard).
- [ ] `DecisionToolVisibilityIntegrityIT` (backend) reste vert.

## Composants

Sous `frontend/src/app/case-files/contestation-c4-onem-section/` : `*.{ts,html,scss,spec.ts}` + prefill-rules `*.{ts,spec.ts}`.
Modèle : `frontend/src/app/core/models/contestation-c4-onem.model.ts`.
Service : `frontend/src/app/core/services/contestation-c4-onem.service.ts`.
Modifs : `decisional-tools-panel.component.ts` + `case-analysis.model.ts` (ajout 3 fields BE C4 contestation).
Migration backend : `XXX-add-contestation-c4-onem-visibility.xml`.

## Dépendances

- SF-207-03 backend (#1137 mergé) — endpoint + extension `TravailExtractedData`.
- SF-207-02b — pattern frontend canonique (mirroir le plus à jour).

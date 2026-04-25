# SF-FA-25-02 — Frontend Majeurs protégés (sauvegarde + habilitation)

> **Feature parente :** F-FA-25 — Majeurs protégés (FRANCE)
> **Type :** SF frontend (parallélisée avec SF-FA-25-01 backend)
> **Branche :** `feat/SF-FA-25-02-frontend-majeurs-proteges`
> **Pattern de référence canonique :** `ordonnance-protection-section`
> **Miroir famille (cards "recommandée") :** `autorite-parentale-section`

---

## Objectif

Exposer dans le panneau F-IA-04 un outil décisionnel "Majeurs protégés" qui
guide l'avocat sur le choix du régime de protection (sauvegarde de justice,
curatelle simple/renforcée, tutelle, habilitation familiale, mandat de
protection future) à partir des éléments médicaux, familiaux et patrimoniaux
du dossier — FRANCE uniquement (art. 425-494 Cciv et 494-1 Cciv pour
l'habilitation familiale).

---

## Contrat API (importé de SF-FA-25-01 backend, parallèle)

### Endpoint
- `POST /api/v1/case-files/{caseFileId}/majeurs-proteges` — calcul + persistance
- `GET  /api/v1/case-files/{caseFileId}/majeurs-proteges` — récupération

### Codes enum (alignés sur backend)

```typescript
export type RegimeProtection =
  | 'SAUVEGARDE_JUSTICE'
  | 'HABILITATION_FAMILIALE'
  | 'CURATELLE_SIMPLE'
  | 'CURATELLE_RENFORCEE'
  | 'TUTELLE'
  | 'MANDAT_PROTECTION_FUTURE';

export type DemandeurFamilial =
  | 'CONJOINT'
  | 'ENFANT_MAJEUR'
  | 'PARENT'
  | 'FRERE_SOEUR'
  | 'TIERS_PROCHE'
  | 'MINISTERE_PUBLIC';

export type ActeEnvisage =
  | 'GESTION_PATRIMOINE'
  | 'DECISIONS_LOGEMENT'
  | 'DECISIONS_SANTE'
  | 'DECISIONS_FAMILIALES'
  | 'ACTES_ETAT_CIVIL'
  | 'AUTRE';

export type VerdictMajeurs = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
```

### Request (POST body)

```typescript
export interface MajeursProtegesRequest {
  regimeProtectionDemande: RegimeProtection;
  altertationFacultesMentales: boolean;
  altertationFacultesPhysiques: boolean;
  certificatMedicalCirconstancie: boolean;
  /** ISO YYYY-MM-DD */
  dateCertificatMedical: string;
  consentementPersonneAProteger: boolean;
  demandeurFamilial: DemandeurFamilial;
  actesEnvisages: ActeEnvisage[];
  urgencePatrimoniale: boolean;
  patrimoineSignificatif: boolean;
  isolementSocial: boolean;
}
```

### Response (GET / POST)

```typescript
export interface MajeursProtegesResponse {
  caseFileId: string;
  // Inputs persistés (echoed)
  regimeProtectionDemande: RegimeProtection;
  altertationFacultesMentales: boolean;
  altertationFacultesPhysiques: boolean;
  certificatMedicalCirconstancie: boolean;
  dateCertificatMedical: string;
  consentementPersonneAProteger: boolean;
  demandeurFamilial: DemandeurFamilial;
  actesEnvisages: ActeEnvisage[];
  urgencePatrimoniale: boolean;
  patrimoineSignificatif: boolean;
  isolementSocial: boolean;
  // Sortie décisionnelle
  scoreEligibilite: number; // 0-100
  regimeOptimalRecommande: RegimeProtection; // peut différer du demandé
  verdictAcceptabiliteJaf: VerdictMajeurs;
  delaiProcedureMoisPrevisionnel: number;
  auditionPersonneObligatoire: boolean;
  expertisePsyComplementaireRecommandee: boolean;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

### Codes d'erreur attendus (relayés par backend)

| Code | Cas |
|------|-----|
| 400  | Validation bean (champs requis, ISO date invalide, enum hors liste) |
| 403  | Tentative depuis un workspace BELGIQUE (gate FRANCE backend) |
| 404  | GET sans calcul antérieur — le composant reste en mode formulaire |
| 500  | Erreur interne — affichage MatSnackBar rouge |

---

## Comportement nominal

1. Le composant `<app-majeurs-proteges-section>` est rendu par le panneau
   F-IA-04 quand `tool_id = F-FA-25-majeurs-proteges` est dans
   `visibility.contextual` (tool registry symétrique aux autres famille).
2. Au mount FRANCE :
   - GET 200 → mode résultat, valeurs persistées affichées, badges IA effacés.
   - GET 404 → mode formulaire, pré-fill IA depuis `aiData?.*` si dispo.
3. L'avocat saisit les 11 champs (1 régime demandé + 4 toggles médicaux/
   consentement + 1 datepicker certificat + 1 demandeur + 1 multi-select
   actes + 3 toggles contextuels).
4. Validation côté UI : régime demandé, demandeur, ≥ 1 acte, certificat
   médical daté (ISO), au moins une altération.
5. Au submit POST → snackbar succès + `dashboardRefresh.triggerRefresh()`.
6. Affichage résultat : bannière verdict (palette navy/or/rouge classique),
   score, **carte "Régime optimal recommandé"** (peut différer du régime
   demandé — point différenciant de l'outil), badges audition + expertise,
   délai procédure (mois), messages, `baseJuridique` + `formule` (JetBrains
   Mono).
7. Bouton "Modifier" → retour mode formulaire avec pré-remplissage
   persisté (badges IA effacés).

## Cas d'erreur

- POST 400 → snackbar rouge `panelClass: snack-error`, `calculating=false`.
- POST 500 → idem, message "Erreur lors du calcul" si pas de `error.message`.
- `workspaceCountry !== 'FRANCE'` → bannière info "FR uniquement", pas de
  GET, mention de l'absence d'équivalent BE direct (régimes Belgique :
  administration provisoire — backlog).
- Mismatch enum (IA renvoie un régime non listé) → ignoré silencieusement
  par le pré-fill.

---

## Critères d'acceptation

- [x] Le composant mount sans erreur en FRANCE et expose les options enums
      attendues (6 régimes, 6 demandeurs, 6 actes envisagés).
- [x] Gate `workspaceCountry`: BELGIQUE → bannière info, pas d'appel HTTP.
- [x] GET 200 → form masqué, valeurs persistées, pas de badge IA.
- [x] GET 404 → mode formulaire + pré-fill IA appliqué (si `aiData`).
- [x] POST succès → résultat affiché, snackbar succès, dashboardRefresh
      déclenché, carte "Régime optimal recommandé" rendue avec
      `regimeOptimalRecommande`.
- [x] POST 400 → snackbar rouge, `calculating=false`.
- [x] formValid faux si régime demandé null, demandeur null, actes vides,
      date certificat invalide, ou aucune altération.
- [x] `coherenceAlerts` produit une alerte multi-source IA + F96 + PIECE
      pour `DATE_CERTIFICAT`, `ALT_MENTALES`, `CONSENTEMENT`,
      `DEMANDEUR_FAMILIAL` (les 4 fields F-IA-03).
- [x] Pré-fill IA effacé sur changement manuel (`provenanceXxx → null`).
- [x] Affichage `baseJuridique` et `formule` en JetBrains Mono — palette
      navy/or/rouge classique sans gradation rouge dominante.
- [x] Tests Jest (≥ 18 cas couvrant mount, formValid, GET, POST, prefill,
      handlers, alertes F-IA-03, gate pays, ngOnChanges).
- [x] `tsc --noEmit -p tsconfig.app.json` clean.

---

## Plan de test

### Unitaires Jest (`majeurs-proteges-section.component.spec.ts`)

1. **Mount FRANCE** : composant créé, options enum (6/6/6).
2. **formValid** : couvre les 5 conditions de validité.
3. **GET 200** : applyPersistedResult, badge IA jamais posé.
4. **GET 404** : restera en mode formulaire et applique pré-fill IA.
5. **POST succès** : body construit correctement, dashboardRefresh
   triggered, snackbar succès.
6. **POST 400** : snackbar rouge, `calculating=false`.
7. **POST ignoré si form invalide** : pas d'appel HTTP.
8. **Gate BELGIQUE** : pas de GET, `isFrance() === false`.
9. **Gate FRANCE** : load() appelé.
10. **ngOnChanges(aiData)** : pré-fill rafraîchit si form vide.
11. **prefillFromAi** : applique régime, altérations, consentement, date.
12. **onXxxChange** : efface badge IA pour tous les champs IA.
13. **coherenceAlerts.DATE_CERTIFICAT** : alerte multi-source.
14. **coherenceAlerts.ALT_MENTALES** : alerte boolean.
15. **coherenceAlerts.CONSENTEMENT** : alerte boolean (sens critique).
16. **coherenceAlerts.DEMANDEUR_FAMILIAL** : alerte enum.
17. **alertes masquées si showForm=false**.
18. **alertBadgeLabel** / **alertTooltip** : prefix correct selon source.
19. **verdictBannerClass** : 3 verdicts mappés correctement.
20. **regimeProtectionLabel** / **demandeurFamilialLabel** /
    **acteEnvisageLabel** : tous codes mappés.

### Tests d'intégration

Hors périmètre SF-FA-25-02 (couvert end-to-end après merge des deux SF
dans SF-FA-25-03 si besoin).

### Isolation workspace

- Backend SF-FA-25-01 garantit le filtre `workspace_id` sur le repository.
  Côté frontend : seul `caseFileId` est passé — pas de fuite cross-tenant
  côté UI.

---

## Tables / endpoints / composants impactés

| Élément | Type | Action |
|---------|------|--------|
| `frontend/src/app/core/models/majeurs-proteges.model.ts` | Création | Types + options + helpers `*Label` |
| `frontend/src/app/core/services/majeurs-proteges.service.ts` | Création | Wrapper `calculate()` + `get()` |
| `frontend/src/app/case-files/majeurs-proteges-section/` | Création | Composant + .html + .scss + .spec |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Édition | Ajout entrée TOOL_REGISTRY `F-FA-25-majeurs-proteges` |
| `frontend/src/app/core/models/divorce-accepte.model.ts` (`FamilleExtractedData`) | Édition | Ajout champs IA pré-fill (8 champs : `regimeProtectionDemande`, `altertationFacultesMentales`, `altertationFacultesPhysiques`, `certificatMedicalCirconstancieDetected`, `dateCertificatMedicalDetected`, `consentementPersonneAProtegerDetected`, `demandeurFamilialDetected`, `actesEnvisagesDetected`) |
| Backend SF-FA-25-01 | (Hors périmètre — parallèle) | API à figer pour merger |

---

## Hors périmètre

- Backend (couvert par SF-FA-25-01 parallèle).
- Génération de la requête JAF papier (générateur de document — futur SF).
- Calcul du seuil patrimoine "significatif" — boolean tranché par avocat.
- Belgique (administration provisoire art. 488 CC) — backlog F-FA-25-BE.
- Mandat de protection future "homologué" vs "notarié" — modulé via
  `regimeOptimalRecommande` côté backend.

---

## Analyse de cohérence transversale

| Cible | Application | Action |
|-------|-------------|--------|
| Pattern UI canonique (palette navy/or/rouge, datepicker `<input type="date">`, gate workspaceCountry, MatSnackBar, JetBrains Mono pour `baseJuridique`/`formule`) | OUI | Repris à l'identique du `ordonnance-protection-section` (référence canonique 2026-04-24) |
| Pré-fill IA + provenance signal + handlers `onXxxChange` qui effacent le badge | OUI | Implémenté pour 4 champs (régime, altérations mentales, consentement, demandeur) |
| Validation F-IA-03 (CoherenceAlertBuilder + popover + multi-source IA/F96/PIECE) | OUI | 4 fields audités via `CoherenceAlertBuilder.forField<MajeursAlertField>()` |
| Entrée TOOL_REGISTRY symétrique avec `inputs: ctx => ({ caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes })` | OUI | Ajouté à `decisional-tools-panel` |
| Domaine droit du travail / immigration | NON | Outil purement famille FR — pas d'impact |
| Belgique | NON (backlog) | Bannière info pour BE — équivalent `administration provisoire` BE noté hors périmètre, à porter par F-FA-25-BE backlog |
| Outils décisionnels (invariant "1 outil = 1 situation") | OUI | L'outil porte **les 6 régimes de protection en un seul outil** car la décision est unique : choisir le régime adapté à la situation (la sortie est `regimeOptimalRecommande`). Le critère d'éclatement (cf. F-DT-08/F-DT-10) est "situations métier différentes" — ici les 6 régimes constituent un même arbre décisionnel piloté par la même collection de critères médicaux/patrimoniaux. Pas d'éclatement nécessaire. |

---

## Nouveau pattern UI ou service partagé

Aucun. Le composant réutilise :
- `CoherenceAlertBuilder` (shared)
- `CoherencePopoverTriggerDirective` (shared)
- `LegalCitationsPipe` (shared)
- `CaseDashboardRefreshService` (cross-tools)
- `SourceExplanationService` (cross-tools)
- Palette / typographie / structure inhérentes au DESIGN_SYSTEM.

---

## Impact par domaine métier

- **Droit du travail (FR / BE)** : aucun impact. L'outil est purement civil
  (protection des majeurs).
- **Immigration (FR / BE)** : aucun impact direct. Toutefois, en pratique
  un avocat peut être amené à demander une protection sur une personne
  étrangère — le composant n'a pas de gate immigration spécifique.
- **Famille (FR)** : impact direct — outil livré pour la France.
- **Famille (BE)** : non pertinent immédiatement. Le régime belge
  d'administration provisoire (art. 488 CC) suit une logique distincte —
  il faudra une feature jumelle F-FA-25-BE au backlog (pas inclus dans
  cette SF — bannière info en attendant).

---

## Parité des domaines métier

L'outil est de **niveau 5 (scoring / analyse validité)**.

- Droit du travail : N/A (concept non applicable au domaine).
- Immigration : N/A (concept non applicable — la protection des majeurs
  est neutre vis-à-vis du titre de séjour).
- Famille FR : livré par cette SF.
- Famille BE : équivalent (administration provisoire) à porter dans une
  feature jumelle F-FA-25-BE (backlog) — bannière info entretemps.

Décision : pas de feature jumelle pour les domaines droit du travail et
immigration (concept non transposable). Une note backlog F-FA-25-BE est
à créer dès que cette SF est mergée pour ne pas créer de dette de
parité FR/BE silencieuse.

---

## Préoccupations transversales

- **Outil décisionnel métier** — Liste des outils décisionnels existants :
  `licenciement-section`, `rupture-conv-indemnite-section`,
  `partage-immobilier-section`, `divorce-accepte-section`,
  `divorce-faute-section`, `divorce-alteration-section`,
  `ordonnance-protection-section`, `autorite-parentale-section`,
  `changement-residence-section`, `desaccords-parentaux-section`,
  `mesures-provisoires-section`, `revisions-post-divorce-section`,
  `harcelement-licenciement-nul-section`, `discrimination-section`,
  `inaptitude-section`, `heures-sup-section`, `oqtf-avec-delai-section`,
  `oqtf-sans-delai-section`, `annexe13-be-section`,
  `belgian-9bis/9ter/40bis/40ter-section`, `aes-metiers-tension-section`,
  `aes-famille-section`, `motif-grave-be-section`,
  `immigration-title-decision-section`, `immigration-recours-section`,
  `immigration-work-right-section`, `immigration-checklist-section`,
  `divorce-checklist-section`, `calendrier-garde-section`,
  `prudhome-fiche-section`, `tribunal-travail-fiche-section`,
  `recompenses-section`, `travail-procedure-section`. Aucun pattern de
  "switch sur situations métier" n'est introduit dans cette SF —
  l'outil porte une situation unique (choix de régime de protection).

---

## Sources externes

- art. 425-494 Cciv (régimes de protection)
- art. 494-1 et s. Cciv (habilitation familiale, depuis 2016)
- art. 477-490 Cciv (mandat de protection future)
- Loi 2007-308 du 5 mars 2007 — réforme des tutelles
- Loi 2015-177 et ord. 2015-1288 — habilitation familiale
- Code de procédure civile : art. 1217-1283 (procédure devant juge des
  contentieux de la protection)

---

## Liens

- Backend : SF-FA-25-01 (parallèle)
- Pattern référence : `ordonnance-protection-section` + `autorite-parentale-section`
- Skill audit cohérence : `ai-skills/frontend-coherence-audit.md` §5/§6

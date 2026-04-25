# SF-FA-26-02 — Frontend Changement état civil (FRANCE)

> **Feature parente :** F-FA-26 — Changement d'état civil (nom / prénom / sexe)
> **Type :** SF frontend (parallélisée avec SF-FA-26-01 backend)
> **Branche :** `feat/SF-FA-26-02-frontend-changement-etat-civil`
> **Pattern de référence canonique :** `harcelement-licenciement-nul-section`
> (template canonique 2026-04-24, cf. `ai-skills/frontend-coherence-audit.md` §5).
> **Miroir famille (multi-select + scoring + carte recommandation) :**
> `divorce-faute-section`, `majeurs-proteges-section`,
> `changement-residence-section`.

---

## Objectif

Exposer dans le panneau F-IA-04 un outil décisionnel "Changement d'état civil"
qui guide l'avocat sur la procédure compétente (mairie / juge / tribunal
judiciaire) et la probabilité d'acceptation pour un changement de nom, prénom,
sexe ou nom+prénom — FRANCE uniquement (art. 60-61-9 Cciv, loi 2016-1547,
loi 2022-301).

---

## Contrat API (importé de SF-FA-26-01 backend, parallèle)

### Endpoint
- `POST /api/v1/case-files/{caseFileId}/changement-etat-civil` — calcul + persistance
- `GET  /api/v1/case-files/{caseFileId}/changement-etat-civil` — récupération

### Codes enum (alignés sur backend)

```typescript
export type TypeChangement = 'NOM' | 'PRENOM' | 'SEXE' | 'NOM_ET_PRENOM';
export type MotifInvoque =
  | 'INTERET_LEGITIME'
  | 'MARIAGE'
  | 'RECTIFICATION_ERREUR'
  | 'IDENTIFICATION_GENRE'
  | 'AUTRE';
export type PreuveEtatCivil =
  | 'JUSTIFICATIF_USAGE_30ANS'
  | 'LIVRET_FAMILLE'
  | 'CERTIFICAT_NAISSANCE'
  | 'ACTES_CIVILS'
  | 'TEMOIGNAGES'
  | 'EXPERTISE_MEDICALE'
  | 'AUTRE';
export type CompetenceProcedure = 'MAIRIE' | 'JUGE' | 'TRIBUNAL_JUDICIAIRE';
export type VerdictEtatCivil = 'ELEVEE' | 'MOYENNE' | 'FAIBLE';
```

### Request (POST body)

```typescript
export interface ChangementEtatCivilRequest {
  typeChangement: TypeChangement;
  motifInvoque: MotifInvoque;
  preuvesProduites: PreuveEtatCivil[];
  majeurDemandeur: boolean;
  consentementParental: boolean;
  datesDocsConcordants: boolean;
  dejaChangeAuparavant: boolean;
  /** ISO YYYY-MM-DD */
  dateNaissanceDemandeur: string;
  departementDeclaration: string;
}
```

### Response

```typescript
export interface ChangementEtatCivilResponse {
  caseFileId: string;
  // Inputs persistés (echoed)
  typeChangement: TypeChangement;
  motifInvoque: MotifInvoque;
  preuvesProduites: PreuveEtatCivil[];
  majeurDemandeur: boolean;
  consentementParental: boolean;
  datesDocsConcordants: boolean;
  dejaChangeAuparavant: boolean;
  dateNaissanceDemandeur: string;
  departementDeclaration: string;
  // Sortie décisionnelle
  competenceProcedure: CompetenceProcedure;
  delaiInstructionMoisPrevisionnel: number;
  scoreAcceptabilite: number; // 0-100
  verdictAcceptabilite: VerdictEtatCivil;
  documentsRequisManquants: string[];
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

### Codes d'erreur attendus (relayés par backend)

| Code | Cas |
|------|-----|
| 400 | Validation bean (champs requis, ISO date invalide, enum hors liste, departement vide) |
| 403 | Workspace BELGIQUE (gate FRANCE backend) |
| 404 | GET sans calcul antérieur — composant reste en mode formulaire |
| 500 | Erreur interne — affichage MatSnackBar rouge |

---

## Comportement nominal

1. Le composant `<app-changement-etat-civil-section>` est rendu par le panneau
   F-IA-04 quand `tool_id = F-FA-26-changement-etat-civil` est dans
   `visibility.contextual` (TOOL_REGISTRY symétrique).
2. Au mount FRANCE :
   - GET 200 → mode résultat, valeurs persistées affichées, badges IA effacés.
   - GET 404 → mode formulaire, pré-fill IA depuis `aiData?.*` si dispo.
3. L'avocat saisit les champs (mat-select typeChangement / motifInvoque,
   mat-select multiple preuves, 4 toggles, `<input type="date">` naissance,
   input texte départment).
4. Validation côté UI : typeChangement non null, motifInvoque non null,
   ≥ 1 preuve, dateNaissanceDemandeur ISO valide, departementDeclaration non
   vide.
5. Au submit POST → snackbar succès + `dashboardRefresh.triggerRefresh()`.
6. Affichage résultat : bannière verdict (palette navy/or/rouge classique),
   score, **carte "Compétence procédure"** (MAIRIE / JUGE / TRIBUNAL —
   point différenciant), délai instruction (mois), chips documents manquants,
   messages, `baseJuridique` + `formule` (JetBrains Mono).
7. Bouton "Modifier" → retour mode formulaire avec valeurs persistées,
   badges IA effacés.

## Cas d'erreur

- POST 400 → snackbar rouge `panelClass: snack-error`, `calculating=false`.
- POST 500 → idem, message "Erreur lors du calcul" si pas de `error.message`.
- `workspaceCountry !== 'FRANCE'` → bannière info "FR uniquement", pas de GET,
  équivalent BE (procédure changement nom CCB) en backlog.
- Mismatch enum (IA renvoie un type non listé) → ignoré silencieusement par
  le pré-fill.

---

## Critères d'acceptation

- [x] Composant mount sans erreur en FRANCE et expose les options enums
      attendues (4 types changement, 5 motifs, 7 preuves).
- [x] Gate `workspaceCountry`: BELGIQUE → bannière info, pas d'appel HTTP.
- [x] GET 200 → form masqué, valeurs persistées, pas de badge IA.
- [x] GET 404 → mode formulaire + pré-fill IA appliqué (si `aiData`).
- [x] POST succès → résultat affiché, snackbar succès, dashboardRefresh
      déclenché, carte "Compétence procédure" rendue avec
      `competenceProcedure`.
- [x] POST 400 → snackbar rouge, `calculating=false`.
- [x] formValid faux si typeChangement null, motifInvoque null, preuves
      vides, dateNaissance invalide ou departement vide.
- [x] `coherenceAlerts` produit une alerte multi-source IA + F96 + PIECE
      pour `TYPE_CHANGEMENT`, `MOTIF_INVOQUE`, `MAJEUR_DEMANDEUR`,
      `CONSENTEMENT_PARENTAL`, `DATE_NAISSANCE` (5 fields F-IA-03).
- [x] Pré-fill IA effacé sur changement manuel (`provenanceXxx → null`).
- [x] Affichage `baseJuridique` et `formule` en JetBrains Mono — palette
      navy/or/rouge classique sans gradation rouge dominante.
- [x] Tests Jest (≥ 18 cas couvrant mount, formValid, GET, POST, prefill,
      handlers, alertes F-IA-03, gate pays, ngOnChanges).
- [x] `tsc --noEmit -p tsconfig.app.json` clean.

---

## Plan de test

### Unitaires Jest (`changement-etat-civil-section.component.spec.ts`)

1. Mount FRANCE : composant créé, options enum (4/5/7).
2. formValid : couvre les 5 conditions de validité.
3. GET 200 : applyPersistedResult, badge IA jamais posé.
4. GET 404 : reste en mode formulaire et applique pré-fill IA.
5. POST succès : body construit correctement, dashboardRefresh triggered,
   snackbar succès.
6. POST 400 : snackbar rouge, `calculating=false`.
7. POST ignoré si form invalide : pas d'appel HTTP.
8. Gate BELGIQUE : pas de GET, `isFrance() === false`.
9. Gate FRANCE : load() appelé.
10. ngOnChanges(aiData) : pré-fill rafraîchit si form vide.
11. prefillFromAi : applique typeChangement, motifInvoque, dateNaissance, etc.
12. onXxxChange : efface badge IA pour tous les champs IA.
13. coherenceAlerts.TYPE_CHANGEMENT : alerte multi-source.
14. coherenceAlerts.MOTIF_INVOQUE : alerte enum.
15. coherenceAlerts.DATE_NAISSANCE : alerte ISO date.
16. coherenceAlerts.CONSENTEMENT_PARENTAL : alerte boolean (mineur).
17. alertes masquées si showForm=false.
18. alertBadgeLabel / alertTooltip : prefix correct selon source.
19. verdictBannerClass : 3 verdicts mappés correctement.
20. competenceProcedureLabel / typeChangementLabel / motifInvoqueLabel /
    preuveLabel : tous codes mappés.

### Tests d'intégration

Hors périmètre SF-FA-26-02 (couvert end-to-end après merge des deux SF).

### Isolation workspace

Backend SF-FA-26-01 garantit le filtre `workspace_id` sur le repository.
Côté frontend : seul `caseFileId` est passé — pas de fuite cross-tenant.

---

## Tables / endpoints / composants impactés

| Élément | Type | Action |
|---------|------|--------|
| `frontend/src/app/core/models/changement-etat-civil.model.ts` | Création | Types + options + helpers `*Label` |
| `frontend/src/app/core/services/changement-etat-civil.service.ts` | Création | Wrapper `calculate()` + `get()` |
| `frontend/src/app/case-files/changement-etat-civil-section/` | Création | Composant + .html + .scss + .spec |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Édition | Ajout entrée TOOL_REGISTRY `F-FA-26-changement-etat-civil` |
| `frontend/src/app/core/models/divorce-accepte.model.ts` (`FamilleExtractedData`) | Édition | Ajout champs IA pré-fill (`typeChangementDetecte`, `motifChangementDetecte`, `dateNaissanceDemandeurDetectee`, `majeurDemandeurDetected`, `consentementParentalDetected`) |
| Backend SF-FA-26-01 | (Hors périmètre — parallèle) | API à figer pour merger |

---

## Hors périmètre

- Backend (couvert par SF-FA-26-01 parallèle).
- Génération de la requête papier (générateur de document — futur SF).
- Belgique (procédure changement nom CCB) — backlog F-FA-26-BE.
- Calcul du seuil d'usage 30 ans — boolean tranché par avocat.

---

## Analyse de cohérence transversale

| Cible | Application | Action |
|-------|-------------|--------|
| Pattern UI canonique (palette navy/or/rouge, datepicker `<input type="date">`, gate workspaceCountry, MatSnackBar, JetBrains Mono pour `baseJuridique`/`formule`) | OUI | Repris à l'identique du `harcelement-licenciement-nul-section` (référence canonique 2026-04-24) |
| Pré-fill IA + provenance signal + handlers `onXxxChange` qui effacent le badge | OUI | Implémenté pour 5 champs (typeChangement, motifInvoque, dateNaissance, majeurDemandeur, consentementParental) |
| Validation F-IA-03 (CoherenceAlertBuilder + popover + multi-source IA/F96/PIECE) | OUI | 5 fields audités via `CoherenceAlertBuilder.forField<ChangementEtatCivilAlertField>()` |
| Entrée TOOL_REGISTRY symétrique avec `inputs: ctx => ({ caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes })` | OUI | Ajouté à `decisional-tools-panel` |
| Domaine droit du travail / immigration | NON | Outil purement civil/famille — pas d'impact |
| Belgique | NON (backlog) | Bannière info pour BE — équivalent BE (changement nom CCB) à porter par F-FA-26-BE backlog |
| Outils décisionnels (invariant "1 outil = 1 situation") | OUI | L'outil porte **les 4 types de changement (nom/prénom/sexe/nom+prénom) en un seul outil** car la décision est unique : déterminer la compétence (mairie/juge/TJ) et la probabilité d'acceptation. Pas d'éclatement nécessaire (situation métier unique). |

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

- **Droit du travail (FR / BE)** : aucun impact. Outil purement civil.
- **Immigration (FR / BE)** : aucun impact direct (le changement d'état civil
  peut concerner un étranger mais relève d'une procédure civile distincte).
- **Famille (FR)** : impact direct — outil livré pour la France.
- **Famille (BE)** : non pertinent immédiatement. Procédure belge (changement
  nom CCB) suit une logique distincte — feature jumelle F-FA-26-BE backlog.

---

## Parité des domaines métier

L'outil est de **niveau 5 (scoring / analyse validité)**.

- Droit du travail : N/A (concept non applicable au domaine).
- Immigration : N/A (concept non applicable — état civil indépendant du
  titre de séjour).
- Famille FR : livré par cette SF.
- Famille BE : équivalent (changement nom CCB) à porter dans une feature
  jumelle F-FA-26-BE (backlog) — bannière info entretemps.

Décision : pas de feature jumelle pour les domaines droit du travail et
immigration (concept non transposable). F-FA-26-BE à inscrire au backlog
dès merge pour ne pas créer de dette de parité FR/BE silencieuse.

---

## Préoccupations transversales

- **Outil décisionnel métier** — Aucun pattern de "switch sur situations
  métier" introduit dans cette SF. L'outil porte une situation unique
  (procédure de changement d'état civil — choix de la compétence en sortie).

---

## Sources externes

- Art. 60 Cciv (changement de prénom — mairie ou juge contentieux protection)
- Art. 61-1 à 61-3-1 Cciv (changement de nom)
- Art. 61-5 à 61-8 Cciv (changement de sexe à l'état civil — TJ)
- Loi 2016-1547 du 18 novembre 2016 (déjudiciarisation prénom)
- Loi 2022-301 du 2 mars 2022 (changement de nom simplifié — mairie résidence)
- Décret 2022-1006 (modalités déclaration mairie)

---

## Liens

- Backend : SF-FA-26-01 (parallèle — contrat figé importé ici)
- Pattern référence : `harcelement-licenciement-nul-section`
- Miroir famille : `divorce-faute-section`, `majeurs-proteges-section`,
  `changement-residence-section`
- Skill audit cohérence : `ai-skills/frontend-coherence-audit.md` §5/§6

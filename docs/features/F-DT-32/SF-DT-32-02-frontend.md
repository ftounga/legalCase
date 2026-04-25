# Mini-spec — F-DT-32 / SF-DT-32-02 Frontend documents de fin de contrat (FR)

## Identifiant

`F-DT-32 / SF-DT-32-02`

## Feature parente

`F-DT-32` — Documents de fin de contrat (L.1234-19 / R.1234-9 / L.1234-20)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-32-02-frontend-documents-fin-contrat`

---

## Objectif

Composant Angular `<app-documents-fin-contrat-section>` qui consomme l'API
SF-DT-32-01 (mergée PR #595) pour vérifier la conformité des trois documents
de fin de contrat français (certificat de travail, attestation France Travail,
reçu pour solde de tout compte) avec score, verdict et indemnités de retard,
intégré au panel F-IA-04 via TOOL_REGISTRY.

---

## Comportement attendu

### Cas nominal

1. À l'ouverture de la section, GET `/api/v1/case-files/{id}/documents-fin-contrat` :
   - 200 → affiche directement le résultat (cartes documents, score, verdict, indemnités).
   - 404 → mode formulaire avec pré-fill IA si `aiData` disponible.
2. Mode formulaire (FRANCE uniquement, bannière info sinon) :
   - Datepicker `<input type="date">` `dateFinContrat` (pré-rempli depuis `aiData.dateLicenciement`).
   - Slide-toggle `certificatTravailRemis` → datepicker conditionnel `dateCertificatTravail`.
   - Slide-toggle `attestationFranceTravailRemise` → datepicker conditionnel `dateAttestationFranceTravail`.
   - Slide-toggle `souldeToutCompteSigne` → datepicker conditionnel `dateSouldeToutCompte` + slide-toggle informatif `souldeToutCompteContestableDelai6mois` (UI-side, non envoyé au backend qui le calcule).
   - Numérique `salaireMensuelBrutEur` (pré-rempli depuis `aiData.salaireBrutMensuel`).
3. POST → backend renvoie réponse complète. Affichage : 3 cartes documents (statut OK / retard / manquant), score sur 100, verdict (FAIBLE/MOYEN/ELEVE), montants indemnités retard en JetBrains Mono, messagesContentieux + messages, baseJuridique + formule.
4. `triggerRefresh()` après POST succès.
5. Bouton « Modifier » revient au form.

### Cas d'erreur

| Situation | Comportement | Notes |
|-----------|-------------|-------|
| `workspaceCountry !== 'FRANCE'` | Bannière info "Outil France uniquement, voir feature jumelle BE backlog" | pas de form |
| GET 404 | Mode formulaire (404 attendu) | fail-open + pré-fill IA |
| Erreur HTTP POST | MatSnackBar rouge | message backend |
| `dateFinContrat` manquante | Form invalide, bouton disabled | UX-side |
| `salaireMensuelBrutEur` ≤ 0 | Form invalide, bouton disabled | UX-side |
| AI data absent | Pas de pré-fill, form vide saisie manuelle | fail-open |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires (multi-fields + dates + score) :
  `conges-payes-section` (F-DT-26), `harcelement-licenciement-nul-section`
  (F-DT-11, template canonique). Patterns identiques (gate FRANCE bannière,
  prefill IA, F-IA-03, refresh, MatSnackBar).
- [x] Multi-cartes : `indemnite-comparatif-section` (F-DT-09 fourchettes) +
  `divorce-checklist-section` (F-FA-07 items multiples). Pattern carte
  documents emprunté.
- [x] Pré-fill IA : `salaireBrutMensuel` (TravailExtractedData) + `dateLicenciement`
  (mappé sur `dateFinContrat`). Champs `certificatTravailRemis`,
  `attestationFranceTravailRemise`, `souldeToutCompteSigne` non extraits par
  l'IA actuelle → saisie manuelle.
- [x] FR vs BE : feature jumelle backlog (C4 ONEM). Bannière info si BE.
- [x] Pattern canonique IA respecté : `prefillFromAi()` invoqué dans `ngOnInit()`
  + `ngOnChanges()`, signals `provenance<Field>`, badges UI `auto_awesome`,
  handlers `onXxxChange()` qui clear le badge.
- [x] F-IA-03 : 2 champs alertables (SALAIRE, DATE_FIN_CONTRAT) via
  `CoherenceAlertBuilder` partagé. `coherenceAlerts` computed gate
  `showForm()`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Template canonique `harcelement-licenciement-nul` | Oui | Référencé dans le composant |
| Pattern multi-fields + dates `conges-payes-section` | Oui | Calque structure form + DateRupture pattern |
| Pattern carte multi-items `divorce-checklist` | Oui | Calque cards 3 documents |
| Pattern card amount `indemnite-comparatif` | Oui | Calque card avec montant + ref article |
| Gate FRANCE bannière info | Oui | Implémenté |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` après POST |
| Pré-fill IA TravailExtractedData | Oui | salaireBrutMensuel + dateLicenciement |
| F-IA-03 multi-sources | Oui | 2 alertes (SALAIRE + DATE_FIN_CONTRAT) |
| TOOL_REGISTRY F-DT-32-documents-fin-contrat | Oui | Entrée ajoutée |

### Décision

- [x] Étendu à toutes les cibles applicables côté frontend
- [x] Belgique = feature jumelle backlog (UI BE non livrée)
- [x] Pas de nouveau pattern UI / service partagé introduit (réutilise CoherenceAlertBuilder, CaseDashboardRefreshService, MatSnackBar, LegalCitationsPipe).

---

## Impact par domaine métier

Cette SF est sensible au domaine **DROIT_DU_TRAVAIL FR** uniquement :
- Aucun impact sur Famille / Immigration (outil isolé sur le pays-domaine).
- Pas de pertinence pour BELGIQUE travail (feature jumelle backlog avec
  documents distincts : C4 ONEM, certificat Loi 03/07/1978 art. 22).
- Pas transversale.

## Parité des domaines métier

Niveau 5 (scoring) — jumeau backend `documents_fin_contrat_analyses` est
strictement FR. Les 2 autres domaines :
- Immigration : non applicable (pas de documents de fin de contrat).
- Famille (divorce) : non applicable (régime distinct, pas de documents employeur).

L'équivalent Belgique (C4 ONEM + certificat Loi 03/07/1978 + attestation
vacances) est bien identifié comme **feature jumelle backlog** dans la
mini-spec backend SF-DT-32-01 (§Analyse de cohérence transversale).

## Nouveau pattern UI ou service partagé

Non. Réutilise :
- `CoherenceAlertBuilder` (shared/coherence-popover/coherence-alert-builder.ts)
- `CoherencePopoverTriggerDirective`
- `CaseDashboardRefreshService`
- `LegalCitationsPipe`
- `MatSnackBar`

Pas de directive / service / DTO réutilisable nouveau.

---

## Critères d'acceptation

- [ ] Composant standalone `<app-documents-fin-contrat-section>` créé avec
      `@Input() caseFileId`, `workspaceCountry`, `aiData`, `procedureChecks`,
      `aiQuestions`, `piecesManquantes`.
- [ ] GET 200 → affiche résultat ; GET 404 → mode formulaire.
- [ ] POST → cards + score + verdict + indemnités + refresh dashboard.
- [ ] Pré-fill IA opérationnel : `salaireBrutMensuel` → `salaireMensuelBrutEur`,
      `dateLicenciement` → `dateFinContrat`. Badges `auto_awesome` "Pré-rempli
      depuis l'analyse" affichés. Handlers `onXxxChange` clear le badge.
- [ ] F-IA-03 : alertes coherence sur SALAIRE (écart > 10 %) et DATE_FIN_CONTRAT
      (toute différence). Multi-sources via `CoherenceAlertBuilder`.
- [ ] Gate FRANCE : bannière info si BELGIQUE, pas de masquage silencieux.
- [ ] Datepickers `<input type="date">` (pas MatDatepicker).
- [ ] Slide-toggles pour booléens (3 documents + 1 informatif STC contestable).
- [ ] Cards documents : palette navy/or (succès), or (retard), navy clair (manquant),
      pas de rouge dominant (pas urgence < 72h).
- [ ] JetBrains Mono pour `baseJuridique`, `formule`, montants indemnités.
- [ ] Inter pour le reste.
- [ ] Entrée TOOL_REGISTRY `F-DT-32-documents-fin-contrat`.
- [ ] ≥ 14 tests Jasmine couvrant : mount, GET 200/404, POST succès/erreur,
      form valide/invalide, prefill IA, F-IA-03 alertes, BELGIQUE bannière,
      handlers onXxxChange, ngOnChanges, mapping conditionnel datepickers.

---

## Périmètre

### Hors scope

- Génération PDF des documents (autre SF F-DT-32-03)
- Belgique frontend (feature jumelle backlog)
- Intégration France Travail API
- Modification du backend SF-DT-32-01 (déjà mergé PR #595)

---

## Contrat API (importé de SF-DT-32-01)

`POST + GET /api/v1/case-files/{caseFileId}/documents-fin-contrat`

### Request body

```ts
interface DocumentsFinContratRequest {
  dateFinContrat: string;             // YYYY-MM-DD (required)
  certificatTravailRemis: boolean | null;
  dateCertificatTravail: string | null;
  attestationFranceTravailRemise: boolean | null;
  dateAttestationFranceTravail: string | null;
  souldeToutCompteSigne: boolean | null;
  dateSouldeToutCompte: string | null;
  salaireMensuelBrutEur: number;      // > 0 (required)
}
```

Note : `souldeToutCompteContestableDelai6mois` est un toggle UI-side
informatif (ne fait pas partie du request — le backend calcule
`souldeToutCompteContestable` via la fenêtre 6 mois après signature).

### Response body

```ts
interface DocumentsFinContratResponse {
  caseFileId: string;
  // Snapshot inputs
  dateFinContrat: string;
  certificatTravailRemis: boolean | null;
  dateCertificatTravail: string | null;
  attestationFranceTravailRemise: boolean | null;
  dateAttestationFranceTravail: string | null;
  souldeToutCompteSigne: boolean | null;
  dateSouldeToutCompte: string | null;
  salaireMensuelBrutEur: number;
  // Outputs
  certificatRemisDansDelai: boolean;
  attestationRemiseDansDelai: boolean;
  souldeToutCompteValide: boolean;
  souldeToutCompteContestable: boolean;
  totalSanctionsCumulables: number;
  indemniteRetardCertificatEur: number;
  indemniteRetardAttestationEur: number;
  scoreConformiteEmployeur: number;
  verdictRisqueContentieux: 'FAIBLE' | 'MOYEN' | 'ELEVE';
  baseJuridique: string;
  formule: string;
  messages: string[];
  messagesContentieux: string[];
  country: 'FRANCE';
}
```

Codes erreur :
- 400 : dateFinContrat absente, salaireMensuelBrutEur ≤ 0, BELGIQUE,
  dossier non DROIT_DU_TRAVAIL.
- 404 : pas d'analyse persistée (mode formulaire) / autre workspace.

---

## Technique

### Fichiers créés

- `frontend/src/app/core/models/documents-fin-contrat.model.ts`
- `frontend/src/app/core/services/documents-fin-contrat.service.ts`
- `frontend/src/app/case-files/documents-fin-contrat-section/documents-fin-contrat-section.component.ts`
- `frontend/src/app/case-files/documents-fin-contrat-section/documents-fin-contrat-section.component.html`
- `frontend/src/app/case-files/documents-fin-contrat-section/documents-fin-contrat-section.component.scss`
- `frontend/src/app/case-files/documents-fin-contrat-section/documents-fin-contrat-section.component.spec.ts`

### Fichiers modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
  → import + entrée TOOL_REGISTRY `F-DT-32-documents-fin-contrat`.

---

## Plan de test

### Tests unitaires Jasmine (≥ 14)

- [ ] Mount + workspaceCountry FRANCE par défaut
- [ ] GET 200 → affiche résultat + masque form
- [ ] GET 404 → reste en mode formulaire
- [ ] formValid : salaire > 0 + dateFinContrat requis
- [ ] formValid faux si salaire absent ou ≤ 0
- [ ] POST succès → cards + score + verdict + dashboardRefresh.triggerRefresh appelé
- [ ] POST erreur → snackbar rouge
- [ ] BELGIQUE → bannière info, pas de form ni de GET
- [ ] Pré-fill IA salaire (provenance IA + handler clear)
- [ ] Pré-fill IA date fin contrat (provenance IA + handler clear)
- [ ] coherenceAlerts.SALAIRE si écart > 10 %
- [ ] coherenceAlerts.DATE_FIN_CONTRAT si dates IA et user diffèrent
- [ ] ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide et pas de result
- [ ] toggleCollapse + editMode

### Isolation workspace

Test côté backend (404). Côté frontend, le service utilise les credentials
session, le test simule le 404 pour autre workspace.

---

## Analyse d'impact

### Préoccupations transversales

- [x] Aucune (pas d'auth, pas de routing global, pas de plan/limite changée,
  pas de workspace context modifié, outil isolé).

### Smoke tests E2E

- [x] Aucun smoke test concerné — outil métier indépendant.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-32-01` (backend) — **mergée** (PR #595).

---

## Pattern de référence

- Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
- Pattern multi-fields + dates : `conges-payes-section` (F-DT-26-02).
- Pattern card multi-items : `divorce-checklist-section` (F-FA-07-02).
- Helper partagé : `CoherenceAlertBuilder` (shared/coherence-popover/).

---

## Notes

- Le toggle UI `souldeToutCompteContestableDelai6mois` est purement informatif
  côté avocat (case "STC déjà contestable ?") — il **n'est pas envoyé** au
  backend qui calcule lui-même `souldeToutCompteContestable` via la fenêtre
  6 mois L.1234-20 al. 2.
- Pas de gradation rouge — palette standard (navy/or) car aucune urgence < 72h.

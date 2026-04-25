# Mini-spec — F-DT-34 / SF-DT-34-02 Frontend Référé prud'homal (FR)

## Identifiant

`F-DT-34 / SF-DT-34-02`

## Feature parente

`F-DT-34` — Référé prud'homal (provisions, expertises, mesures conservatoires —
art. R.1454-1 et suivants du Code du travail)

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-34-02-frontend-refere-prudhomal`

---

## Objectif

Composant Angular `<app-refere-prudhomal-section>` qui consomme l'API SF-DT-34-01
(figée — voir contrat ci-dessous) pour évaluer la pertinence d'un référé
prud'homal (R.1454-1) selon le type de référé, la nature de la créance, les
preuves d'urgence, l'absence de contestation sérieuse et le dommage immédiat.
Restitue : score de succès, verdict, délai d'audience prévisionnel, délai
d'ordonnance prévisionnel, montant de provision recommandé, base juridique,
formule, messages — avec pré-fill IA + alertes F-IA-03. Intégré au panel
F-IA-04 via TOOL_REGISTRY.

---

## Comportement attendu

### Cas nominal

1. À l'ouverture de la section, GET `/api/v1/case-files/{id}/refere-prudhomal` :
   - 200 → affiche directement le résultat (verdict, score, 2 cartes délais
     audience + ordonnance, montant provision, messages).
   - 404 → mode formulaire avec pré-fill IA si `aiData` disponible.
2. Mode formulaire (FRANCE uniquement, bannière info sinon) :
   - mat-select `typeRefere` (6 options : PROVISION_SALAIRES, EXPERTISE_MEDICALE,
     EXPERTISE_TECHNIQUE, MESURES_CONSERVATOIRES, REINTEGRATION_URGENCE, AUTRE)
   - mat-select `natureCreance` (6 options : SALAIRES_NON_VERSES, INDEMNITE_RUPTURE,
     HEURES_SUPPLEMENTAIRES, PRIMES, CONGES_PAYES, AUTRE)
   - Numérique `montantProvisionDemandeeEur` (> 0 si typeRefere ∈ {PROVISION_SALAIRES})
   - Slide-toggle `absenceContestationSerieuse`
   - mat-select multiple `preuvesUrgenceProduites` (7 options : BULLETIN_PAIE,
     RELANCE_EMPLOYEUR, MISE_EN_DEMEURE, CONSTAT_HUISSIER, CERTIFICAT_MEDICAL,
     CONTRAT, AUTRE)
   - Slide-toggle `dommageImmediatCarac`
   - Slide-toggle `trésorerieEmployeurDouteuse`
   - Datepicker `<input type="date">` `dateMiseEnDemeure`
   - Numérique `ancienneteContratMois` (>= 0)
3. POST → backend renvoie réponse complète. Affichage : bannière verdict
   colorée (palette navy/or/rouge selon verdict), score sur 100, 2 cartes
   délais (audience + ordonnance) avec valeurs JetBrains Mono, montant
   provision recommandé en JetBrains Mono, messages, baseJuridique + formule.
4. `triggerRefresh()` après POST succès.
5. Bouton « Modifier » revient au form.

### Cas d'erreur

| Situation | Comportement | Notes |
|-----------|-------------|-------|
| `workspaceCountry !== 'FRANCE'` | Bannière info "Outil France uniquement" | pas de form |
| GET 404 | Mode formulaire (404 attendu) | fail-open + pré-fill IA |
| Erreur HTTP POST | MatSnackBar rouge | message backend |
| `typeRefere`, `natureCreance`, `dateMiseEnDemeure`, `montantProvisionDemandeeEur` manquants | Form invalide, bouton disabled | UX-side |
| `ancienneteContratMois` < 0 | Form invalide | UX-side |
| AI data absent | Pas de pré-fill, form vide saisie manuelle | fail-open |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires (form complexe + mat-select multiple + scoring) :
  `referes-admin-section` (F-IM-08-08, référés équivalents immigration —
  pattern principal pour 2 cartes scores parallèles + multiselect preuves) +
  `harcelement-licenciement-nul-section` (F-DT-11, template canonique).
- [x] Pattern card multi-items : `documents-fin-contrat-section` (F-DT-32 —
  multi-cards documents). Ici on a 2 cartes délais (audience + ordonnance).
- [x] Pré-fill IA : `salaireBrutMensuel` non utilisé directement (provision
  saisie). `dateLicenciement` mappé sur `dateMiseEnDemeure` quand pertinent.
  La nature de la créance peut être pré-remplie à partir de `motifLicenciement`
  ou `heuresSupMentionneesDansDossier` (mapping fail-open : si heures sup
  détectées → HEURES_SUPPLEMENTAIRES). `ancienneteContratMois` calculé depuis
  `dateEntree` IA si disponible (fallback : non pré-rempli).
- [x] FR vs BE : feature jumelle backlog (procédure d'urgence Tribunal du
  travail BE équivalente). Bannière info si BE.
- [x] Pattern canonique IA respecté : `prefillFromAi()` invoqué dans `ngOnInit()`
  + `ngOnChanges()`, signals `provenance<Field>`, badges UI `auto_awesome`,
  handlers `onXxxChange()` qui clear le badge.
- [x] F-IA-03 : 2 champs alertables (DATE_MISE_EN_DEMEURE, ANCIENNETE) via
  `CoherenceAlertBuilder` partagé. `coherenceAlerts` computed gate `showForm()`.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Template canonique `harcelement-licenciement-nul` | Oui | Référencé dans le composant |
| Pattern référés `referes-admin-section` | Oui | Calque structure form + 2 cards scores parallèles → 2 cards délais |
| Pattern multi-cards `documents-fin-contrat` | Oui | Calque cards 2 délais |
| Gate FRANCE bannière info | Oui | Implémenté |
| Refresh dashboard F-IA-02 | Oui | `triggerRefresh()` après POST |
| Pré-fill IA TravailExtractedData | Oui | dateMiseEnDemeure ← dateLicenciement, ancienneteContratMois ← dateEntree, natureCreance ← heuresSupMentionneesDansDossier |
| F-IA-03 multi-sources | Oui | 2 alertes (DATE_MISE_EN_DEMEURE + ANCIENNETE) |
| TOOL_REGISTRY F-DT-34-refere-prudhomal | Oui | Entrée ajoutée |

### Décision

- [x] Étendu à toutes les cibles applicables côté frontend
- [x] Belgique = feature jumelle backlog (UI BE non livrée)
- [x] Pas de nouveau pattern UI / service partagé introduit (réutilise
      CoherenceAlertBuilder, CaseDashboardRefreshService, MatSnackBar,
      LegalCitationsPipe).

---

## Impact par domaine métier

Cette SF est sensible au domaine **DROIT_DU_TRAVAIL FR** uniquement :
- Aucun impact sur Famille / Immigration (outil isolé sur le pays-domaine).
- Pas de pertinence pour BELGIQUE travail (feature jumelle backlog Tribunal
  du travail BE — référé).
- Pas transversale.

## Parité des domaines métier

Niveau 5 (scoring) — jumeau backend `refere_prudhomal_analyses` est strictement
FR. Les 2 autres domaines :
- Immigration : équivalent `referes-admin-section` (F-IM-08-08, L.521-1 / L.521-2 CJA) déjà livré.
- Famille (divorce) : non applicable (pas d'urgence procédurale équivalente —
  les MPU sont déjà couvertes par F-FA-12 mesures provisoires).

L'équivalent Belgique (Tribunal du travail BE — référé) est bien identifié
comme **feature jumelle backlog** dans la mini-spec backend SF-DT-34-01.

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

- [x] Composant standalone `<app-refere-prudhomal-section>` créé avec
      `@Input() caseFileId`, `workspaceCountry`, `aiData`, `procedureChecks`,
      `aiQuestions`, `piecesManquantes`.
- [x] GET 200 → affiche résultat ; GET 404 → mode formulaire.
- [x] POST → bannière verdict + score + 2 cartes délais + montant provision
      + refresh dashboard.
- [x] Pré-fill IA opérationnel : `dateLicenciement` → `dateMiseEnDemeure`,
      `dateEntree` → `ancienneteContratMois` (calcul mois entre dateEntree et
      dateLicenciement ou today), `heuresSupMentionneesDansDossier` →
      `natureCreance=HEURES_SUPPLEMENTAIRES`. Badges `auto_awesome` "Pré-rempli
      depuis l'analyse" affichés. Handlers `onXxxChange` clear le badge.
- [x] F-IA-03 : alertes coherence sur DATE_MISE_EN_DEMEURE (toute différence
      avec dateLicenciement IA, source IA) et ANCIENNETE (écart > 2 mois entre
      saisie et calcul IA). Multi-sources via `CoherenceAlertBuilder`.
- [x] Gate FRANCE : bannière info si BELGIQUE, pas de masquage silencieux.
- [x] Datepickers `<input type="date">` (pas MatDatepicker).
- [x] mat-select pour typeRefere (6) + natureCreance (6).
- [x] mat-select multiple pour preuvesUrgenceProduites (7).
- [x] 3 slide-toggles (absenceContestationSerieuse, dommageImmediatCarac,
      trésorerieEmployeurDouteuse).
- [x] Numérique pour montantProvisionDemandeeEur (>= 0) et ancienneteContratMois (>= 0).
- [x] Bannière verdict palette standard (navy, or, rouge selon verdict —
      INSUFFISAMMENT_FONDE = navy, PROVISION_PROBABLE / EXPERTISE_RECOMMANDEE
      = or, AUTRE_VOIE_RECOMMANDEE = navy clair). Pas de gradation rouge
      dominant (pas urgence < 72h — référé prud'homal délai audience typ. 15j+).
- [x] 2 cartes délais (audience + ordonnance) avec valeurs JetBrains Mono.
- [x] Montant provision recommandé en JetBrains Mono.
- [x] JetBrains Mono pour `baseJuridique`, `formule`.
- [x] Inter pour le reste.
- [x] Entrée TOOL_REGISTRY `F-DT-34-refere-prudhomal`.
- [x] ≥ 12 tests Jasmine couvrant : mount, GET 200/404, POST succès/erreur,
      form valide/invalide, prefill IA, F-IA-03 alertes, BELGIQUE bannière,
      handlers onXxxChange, ngOnChanges.

---

## Périmètre

### Hors scope

- Génération de l'assignation référé prud'homal (autre feature)
- Belgique frontend (feature jumelle backlog)
- Modification du backend SF-DT-34-01 (parallèle — contrat figé)

---

## Contrat API (importé de SF-DT-34-01)

`POST + GET /api/v1/case-files/{caseFileId}/refere-prudhomal`

### Enums

```ts
type TypeRefere = 'PROVISION_SALAIRES' | 'EXPERTISE_MEDICALE' | 'EXPERTISE_TECHNIQUE'
                | 'MESURES_CONSERVATOIRES' | 'REINTEGRATION_URGENCE' | 'AUTRE';
type NatureCreance = 'SALAIRES_NON_VERSES' | 'INDEMNITE_RUPTURE' | 'HEURES_SUPPLEMENTAIRES'
                   | 'PRIMES' | 'CONGES_PAYES' | 'AUTRE';
type PreuveUrgence = 'BULLETIN_PAIE' | 'RELANCE_EMPLOYEUR' | 'MISE_EN_DEMEURE'
                   | 'CONSTAT_HUISSIER' | 'CERTIFICAT_MEDICAL' | 'CONTRAT' | 'AUTRE';
type VerdictRefereDt = 'PROVISION_PROBABLE' | 'EXPERTISE_RECOMMANDEE'
                     | 'INSUFFISAMMENT_FONDE' | 'AUTRE_VOIE_RECOMMANDEE';
```

### Request body

```ts
interface ReferePrudhomalRequest {
  typeRefere: TypeRefere;
  natureCreance: NatureCreance;
  montantProvisionDemandeeEur: number;
  absenceContestationSerieuse: boolean;
  preuvesUrgenceProduites: PreuveUrgence[];
  dommageImmediatCarac: boolean;
  trésorerieEmployeurDouteuse: boolean;
  dateMiseEnDemeure: string; // YYYY-MM-DD
  ancienneteContratMois: number;
}
```

### Response body

```ts
interface ReferePrudhomalResponse {
  caseFileId: string;
  // Snapshot inputs reflétés.
  typeRefere: TypeRefere;
  natureCreance: NatureCreance;
  montantProvisionDemandeeEur: number;
  absenceContestationSerieuse: boolean;
  preuvesUrgenceProduites: PreuveUrgence[];
  dommageImmediatCarac: boolean;
  trésorerieEmployeurDouteuse: boolean;
  dateMiseEnDemeure: string;
  ancienneteContratMois: number;
  // Outputs
  scoreSuccess: number; // 0..100
  verdictRecommandation: VerdictRefereDt;
  delaiAudienceJoursPrevisionnel: number;
  delaiOrdonnanceJoursPrevisionnel: number;
  montantProvisionRecommandeEur: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

Codes erreur :
- 400 : champs requis absents, montant ≤ 0, ancienneté < 0, BELGIQUE,
  dossier non DROIT_DU_TRAVAIL.
- 404 : pas d'analyse persistée (mode formulaire) / autre workspace.

---

## Technique

### Fichiers créés

- `frontend/src/app/core/models/refere-prudhomal.model.ts`
- `frontend/src/app/core/services/refere-prudhomal.service.ts`
- `frontend/src/app/case-files/refere-prudhomal-section/refere-prudhomal-section.component.ts`
- `frontend/src/app/case-files/refere-prudhomal-section/refere-prudhomal-section.component.html`
- `frontend/src/app/case-files/refere-prudhomal-section/refere-prudhomal-section.component.scss`
- `frontend/src/app/case-files/refere-prudhomal-section/refere-prudhomal-section.component.spec.ts`

### Fichiers modifiés

- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
  → import + entrée TOOL_REGISTRY `F-DT-34-refere-prudhomal`.

---

## Plan de test

### Tests unitaires Jasmine (≥ 12)

- [x] Mount + workspaceCountry FRANCE par défaut
- [x] GET 200 → affiche résultat + masque form
- [x] GET 404 → reste en mode formulaire
- [x] formValid : tous champs requis renseignés
- [x] formValid faux si typeRefere absent
- [x] formValid faux si dateMiseEnDemeure absente
- [x] POST succès → bannière verdict + cards + dashboardRefresh.triggerRefresh appelé
- [x] POST erreur → snackbar rouge
- [x] BELGIQUE → bannière info, pas de form ni de GET
- [x] Pré-fill IA dateMiseEnDemeure (provenance IA + handler clear)
- [x] Pré-fill IA natureCreance (provenance IA + handler clear)
- [x] coherenceAlerts.DATE_MISE_EN_DEMEURE si dates IA et user diffèrent
- [x] coherenceAlerts.ANCIENNETE si écart > 2 mois
- [x] ngOnChanges(aiData) post-mount rafraîchit le pré-fill si form vide
- [x] toggleCollapse + editMode

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

- `SF-DT-34-01` (backend) — **parallèle** (contrat figé importé). En production
  l'outil ne sera utilisable qu'après merge du backend.

---

## Pattern de référence

- Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
- Pattern référés (form + 2 cards parallèles) : `referes-admin-section`
  (F-IM-08-08).
- Pattern multi-cards : `documents-fin-contrat-section` (F-DT-32-02).
- Helper partagé : `CoherenceAlertBuilder` (shared/coherence-popover/).

---

## Notes

- Pas de gradation rouge — palette standard (navy/or). Le référé prud'homal
  n'est pas une urgence < 72h (délai audience typ. 15+ jours, ordonnance
  ~30j).
- Le champ `montantProvisionRecommandeEur` peut différer de
  `montantProvisionDemandeeEur` (le backend recalcule selon la nature de la
  créance et l'ancienneté — typ. plafond 6 mois de salaire).

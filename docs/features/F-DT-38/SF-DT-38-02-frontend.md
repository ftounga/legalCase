# Mini-spec — F-DT-38 / SF-DT-38-02 — Frontend : section Angular rupture de période d'essai

## Identifiant
`F-DT-38 / SF-DT-38-02` — livrée **dans la même branche** que SF-DT-38-01 (PR combinée `feat/F-DT-38-rupture-periode-essai`)

**Raison du regroupement** : la tuile dashboard (`CaseFileDashboardService`) consomme directement les classes backend (`RupturePeriodeEssaiResponse`, `RupturePeriodeEssaiRepository`) ET l'entrée TOOL_REGISTRY frontend exige le seed `decision_tool_visibility_rules` couplé (garde-fou `DecisionToolVisibilityIntegrityIT`). Découpler en 2 PR créerait un état intermédiaire cassé sur master entre la PR 01 et la PR 02. La règle CLAUDE.md sur la parallélisation backend/frontend autorise cette combinaison quand le couplage tile dashboard / TOOL_REGISTRY l'impose (précédent : SF-DT-36 livré en 2 PR, puis SF-DT-36-03 nécessaire pour câbler la tile a posteriori — anti-pattern à éviter ici).

## Feature parente
`F-DT-38` — Rupture de période d'essai (qualification régulière / abusive / nulle / illégale)

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-DT-38-02-frontend`

---

## Objectif

Fournir une section Angular dans le panel décisionnel qui consomme l'API SF-DT-38-01, affiche un formulaire pré-rempli par l'IA, rend un verdict 4 niveaux (`REGULIERE` / `RISQUE_ABUSIVE` / `NULLE` / `ILLEGALE_REQUALIF_LICENCIEMENT`), met en avant la **réintégration** pour `NULLE`, expose les anomalies détectées et alimente le dashboard.

---

## Contrat API consommé (importé de SF-DT-38-01)

`POST` / `GET /api/v1/case-files/{caseFileId}/rupture-periode-essai` — voir SF-DT-38-01 pour le détail des champs.

---

## Comportement attendu

### Cas nominal

1. La section apparaît dans le panel décisionnel `decisional-tools-panel` si `decision_tool_visibility_rules` autorise l'affichage (visibilité contextuelle, flag IA `rupture_periode_essai_detectee` ou indicateur équivalent).
2. À l'ouverture, le formulaire est pré-rempli par l'IA depuis `travailExtractedData.rupture_periode_essai_detail` (sous-objet ajouté à la synthèse) — invariant F-246 pré-fill exhaustif.
3. L'avocat ajuste les champs au besoin (badge `auto_awesome` disparaît dès qu'un champ est modifié manuellement).
4. Clic « Calculer » → POST → affichage du verdict, des anomalies et de l'indemnité estimée.
5. Refresh dashboard via `CaseDashboardRefreshService`.

### Cas d'erreur

| Situation | Comportement UI |
|-----------|-----------------|
| Erreur réseau / 5xx | MatSnackBar « Erreur lors du calcul » |
| 400 backend | MatSnackBar avec message du backend |
| 404 GET initial (pas encore calculé) | mode formulaire vide / pré-rempli IA |
| Workspace BE | section non rendue (visibilité contextuelle FR uniquement) |

---

## Pré-fill IA (champs déjà extraits — F-246 invariant exhaustif différé)

**Note de cadrage** : pour cette première livraison, le pré-fill IA s'appuie **exclusivement sur les champs déjà extraits par le pipeline** (mapping sans extension du prompt ni du record `TravailExtractedData`). C'est le même pattern que F-DT-36 a suivi (SF-DT-36-02 livré sans pré-fill, complété par SF-246-01 plus tard). Le **pré-fill exhaustif F-246** (extension du prompt + record avec un sous-objet `rupture_periode_essai_detail` couvrant 10+ nouveaux champs) sera livré dans une SF F-246 dédiée post-F-DT-38, pour garder cette première livraison atomique et review-able.

Mapping de pré-fill du SF-DT-38-02 (8 champs depuis l'existant) :

| Champ formulaire | Source IA (TravailExtractedData) | Provenance badge |
|---|---|---|
| `typeContrat` | `typeContrat` (déjà extrait — CDI/CDD/INTERIM/AUTRE → fallback CDI) | IA |
| `dateDebutContrat` | `dateEntree` (déjà extrait) | IA |
| `dateRupture` | `dateLicenciement` (déjà extrait) | IA |
| `motifInvoque` | `motifLicenciement` (déjà extrait) | IA |
| `discriminationInvoquee` | `motifNullitePressenti` (mapping DISCRIMINATION/HARCELEMENT/SYNDICAL → enum F-DT-38) | IA |
| `grossesseAuMomentRupture` | mappé depuis `motifNullitePressenti = MATERNITE_PATERNITE` ou `MATERNITE` | IA |
| `arretAccidentTravailEnCours` | `atMpDetecte` (déjà extrait F-DT-33) | IA |
| `conventionCollectiveApplicable` | `conventionCollective != null` | IA |
| `salaireMensuelBrut` | `salaireBrutMensuel` (déjà extrait) | IA |

Les autres champs (`categorieSocioProfessionnelle`, `dureePeriodeEssaiContractuelleMois`, `renouvellementInvoque`, `auteurRupture`, `delaiPrevenanceJoursAppliques`, `motifLieAuxCompetencesProfessionnelles`, `motifEconomiqueOuOrganisationnel`, `lettreRuptureMotivee`, `motifsAveresParPieces`, `conventionCollectivePlusFavorableRespectee`, `atteinteLiberteFondamentale`) sont **saisie manuelle** dans cette SF — pré-fill exhaustif en SF F-246 dédiée ultérieure.

**Justification du report F-246** : extension du prompt + record + extraction + DTO frontend + helper prefill exhaustif sur 10+ nouveaux champs représente un risque de PR mastodonte (cf. PR #1124 F-246-03 qui a déjà rebondi sur ce volume). Le découpage SF-DT-38-02 (livraison outil) → SF-246-XX (pré-fill exhaustif) est conforme au précédent F-DT-36 et préserve la review-ability.

---

## Validation F-IA-03 (3 fields croisables)

| Field | Source de divergence | Action si divergence |
|---|---|---|
| `DATE_RUPTURE` | F96 critère `DT38_DATE_RUPTURE` / question IA / pièce manquante `LETTRE_RUPTURE_ESSAI` | Badge `CoherenceAlert` |
| `MOTIF_PROFESSIONNEL` | F96 critère `DT38_MOTIF_PROFESSIONNEL` / question IA | Badge |
| `DUREE_ESSAI` | F96 critère `DT38_DUREE_ESSAI` / question IA / pièce `CONTRAT_TRAVAIL` | Badge |

---

## TOOL_REGISTRY

Ajouter dans `decisional-tools-panel.component.ts` :

```ts
['F-DT-38-rupture-periode-essai', {
  displayLabel: 'Rupture de période d\'essai (FR)',
  component: RupturePeriodeEssaiSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.travailExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
    standaloneMode: ctx.standaloneMode ?? false,
  }),
}],
```

Classement catégorie (TOOL_CATEGORIES_BY_ID) : `'VALIDITE'` (cohérent avec F-DT-08 / F-DT-36).

---

## Seed `decision_tool_visibility_rules`

Migration `256-seed-f-dt-38-visibility-rules.xml` :
- `legal_domain = DROIT_DU_TRAVAIL`
- `country = FRANCE`
- `tool_id = F-DT-38-rupture-periode-essai`
- `layer = CONTEXTUAL`
- `trigger_field = type_rupture` ; trigger sur valeurs `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`, `DEMISSION` (la rupture d'essai peut être à l'initiative de l'employeur OU du salarié — donc le filtre type_rupture seul est trop restrictif ; on ajoute un second filtre IA contextuel sur la présence d'une période d'essai). Priority 58 (juste après F-DT-36 priority 57).

**Important** : la visibilité dépend aussi du flag IA `rupture_periode_essai_detectee` — la règle de visibilité simple `decision_tool_visibility_rules` ne suffira pas, mais elle évite l'affichage hors dossier travail. Le component sera additionnellement no-op si `workspaceCountry !== 'FRANCE'`.

---

## Critères d'acceptation

- [ ] Section apparaît UNIQUEMENT pour workspace FRANCE + dossier `DROIT_DU_TRAVAIL` + flag de rupture détecté.
- [ ] Pré-fill IA exhaustif : 18+ champs renseignés depuis `travailExtractedData` quand l'info est présente (invariant F-246 respecté).
- [ ] Badge `auto_awesome` visible sur tous les champs pré-remplis ; disparaît à la modification manuelle.
- [ ] POST → verdict affiché avec banner color-coded (REGULIERE = navy / RISQUE_ABUSIVE = or / NULLE = rouge avec mention « Option principale : réintégration » / ILLEGALE = rouge).
- [ ] Indemnité fourchette 1-6 mois × salaire affichée pour `RISQUE_ABUSIVE`.
- [ ] Verdict `NULLE` : mention explicite « Réintégration + rappel salaires entre rupture et réintégration » (texte Marjolaine).
- [ ] Verdict `ILLEGALE` : mention « Barème Macron L.1235-3 applicable — voir outil F-DT-08 ».
- [ ] Refresh dashboard après calcul.
- [ ] Validation F-IA-03 fonctionnelle sur 3 fields.
- [ ] Tuile dashboard F-DT-38 visible quand la section est affichable et calculée.
- [ ] Tests Jest section + helper prefill + service HTTP.

---

## Périmètre

### Hors scope
- Backend (SF-DT-38-01).
- Outil jumeau BE (F-DT-39 backlog).
- Modification des autres sections décisionnelles.

---

## Technique

### Fichiers créés / modifiés

| Fichier | Type |
|---|---|
| `frontend/src/app/case-files/rupture-periode-essai-section/rupture-periode-essai-section.component.ts` | Nouveau |
| `.../rupture-periode-essai-section.component.html` | Nouveau |
| `.../rupture-periode-essai-section.component.scss` | Nouveau |
| `.../rupture-periode-essai-section.component.spec.ts` | Nouveau |
| `.../rupture-periode-essai-section-prefill-rules.ts` | Nouveau |
| `.../rupture-periode-essai-section-prefill-rules.spec.ts` | Nouveau |
| `frontend/src/app/core/services/rupture-periode-essai.service.ts` | Nouveau |
| `frontend/src/app/core/models/rupture-periode-essai.model.ts` | Nouveau |
| `frontend/src/app/core/models/case-analysis.model.ts` | Modifié — ajout sous-objet `rupturePeriodeEssaiDetail` au record `TravailExtractedData` |
| `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` | Modifié — entrée TOOL_REGISTRY + TOOL_CATEGORIES_BY_ID + import |
| `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` | Modifié — ajout sous-objet `rupture_periode_essai_detail` au prompt `TRAVAIL_INSTRUCTION` |
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` | Modifié — record `TravailExtractedData` + nouveau record `RupturePeriodeEssaiDetail` |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisResponseTest.java` | Modifié — sérialisation snake_case du nouveau record |
| `backend/src/test/java/fr/ailegalcase/analysis/LegalDomainPromptBuilderTest.java` | Modifié — présence du sous-objet dans le prompt |
| `backend/src/main/resources/db/changelog/migrations/256-seed-f-dt-38-visibility-rules.xml` | Nouveau (seed) |
| `backend/src/main/resources/db/changelog/db.changelog-master.xml` | Modifié — référence à la nouvelle migration |
| `backend/src/test/java/fr/ailegalcase/casefile/DashboardTileToolIdIntegrityIT.java` | Modifié — ajout F-DT-38-rupture-periode-essai dans `KNOWN_FRONTEND_TOOL_IDS` |
| `backend/src/test/java/fr/ailegalcase/casefile/CaseFileDashboardService*.java` (si pertinent) | Modifié si la liste d'outils dashboard est dur-codée |

### Tests Jest (≥ 25 tests)
- Composant : pré-fill IA exhaustif (18+ champs), calculate, verdict banners, fourchette indemnité, mention réintégration, mention barème Macron, F-IA-03 alerts, refresh dashboard, mode standalone.
- Helper prefill : computePrefillCount, mappings champs un par un.
- Service : POST/GET.

---

## Préoccupations transversales

- [x] **Outil décisionnel métier** — F-DT-38 = outil décisionnel nouveau. Composants frontend impactés : `decisional-tools-panel`, dashboard tile, TOOL_REGISTRY, KNOWN_FRONTEND_TOOL_IDS du test d'intégrité. Conforme à la mémoire `feedback_pre_merge_visibility_seed_check` : entrée TOOL_REGISTRY + seed couplés dans la même SF.
- [x] **Self-check grep pré-commit** obligatoire (mémoire `feedback_self_check_grep_pre_commit`) :
  - `grep -r "F-DT-38-rupture-periode-essai" frontend/src/` — doit lister panel + visibility + tests
  - `grep -r "F-DT-38-rupture-periode-essai" backend/src/` — doit lister seed + tests integrity

### Smoke tests E2E
- [x] Aucun — feature additive frontend ; pas de changement d'auth / workspace / navigation.

---

## Dépendances
- SF-DT-38-01 backend (contrat API + endpoint déployé) — peut être parallélisé sur contrat figé puisque contrat documenté ci-dessus.

---

## Notes
- Pré-fill exhaustif F-246 = invariant non-négociable (cas Marjolaine F-DT-36 mai 2026 : section avec 0 champ pré-rempli = inacceptable).
- Mention réintégration pour `NULLE` = traduction directe du mail Marjolaine 19/05.
- Atténuation `RISQUE_ABUSIVE` pour `ILLEGALE` + lettre motivée = traduction directe du mail Marjolaine 19/05.

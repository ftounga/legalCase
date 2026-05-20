# Mini-spec — SF-246-27 / F-246 — Lot Famille FR protection & divorce

> Template basé sur `project-governance/templates/subfeature-template.md`.
> Modèle de référence : SF-246-26 (filiation & autorité parentale, commit 4d9e757c).

---

## Identifiant

`F-246 / SF-246-27`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`in-progress`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-27-lot-famille-protection-divorce`

---

## Objectif

Brancher le pré-remplissage IA pour 7 outils Famille (FR + BE) en dette D2/D3 :
`majeurs-proteges`, `pma-gpa-bioethique`, `mediation-familiale`, `ordonnance-requete`,
`divorce-accepte` (date d'assignation), `divorce-alteration` (dates), et `divorce-dc-be`
(date d'audience d'homologation — **BELGIQUE UNIQUEMENT**), via un nouveau sous-objet
backend `protection_divorce_detection_v2` exposant les champs extractibles manquants.

---

## Comportement attendu

### Cas nominal

**Backend :**
1. `CaseAnalysisResponse.FamilleExtractedData` reçoit 8 nouveaux champs via le sous-objet
   `protection_divorce_detection_v2` du JSON IA :
   - `dateCertificatMedicalMajeursDetected` (ISO date) — majeurs-proteges
   - `regimeProtectionMajeursDetected` (whitelist 6 valeurs) — majeurs-proteges
   - `datePmaDetected` (ISO date) — pma-gpa-bioethique
   - `dateReconnaissanceAnterieurePmaDetected` (ISO date) — pma-gpa-bioethique
   - `dateDonGametesDetected` (ISO date) — pma-gpa-bioethique
   - `motifSaisineMediationDetected` (whitelist) — mediation-familiale
   - `dateAssignationDivorce` (ISO date) — divorce-accepte + divorce-alteration
   - `dateAudienceHomologationDcBe` (ISO date, **Belgique uniquement**) — divorce-dc-be
2. `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION_P2` : ajout de la section SF-246-27
   pour le sous-objet `protection_divorce_detection_v2`.
3. `extractFamilleData()` : parsing du sous-objet + guard null + appel builder.

**Frontend :**
4. `divorce-accepte.model.ts` (`FamilleExtractedData`) : JSDoc mis à jour pour les 8 champs.
5. `divorce-accepte-section-prefill-rules.ts` : ajout de `computeDateAssignation()` lisant
   `aiData.dateAssignationDivorce`.
6. `divorce-alteration-section-prefill-rules.ts` : ajout de `computeDateAssignation()`
   lisant `aiData.dateAssignationDivorce`.
7. `divorce-dc-be-section-prefill-rules.ts` : ajout de `computeDateAudienceHomologation()`
   lisant `aiData.dateAudienceHomologationDcBe` (**annotation BELGIQUE UNIQUEMENT**).
8. `majeurs-proteges-section-prefill-rules.ts` : les champs existants
   `regimeProtectionDemande` et `dateCertificatMedical` lisent désormais les vrais
   champs backend (`regimeProtectionMajeursDetected`, `dateCertificatMedicalMajeursDetected`)
   au lieu des champs aspirationnels.
9. `pma-gpa-bioethique-section-prefill-rules.ts` : ajout de `computeDatePma()`,
   `computeDateReconnaissanceAnterieure()`, `computeDateDon()`.
10. `mediation-familiale-section-prefill-rules.ts` : `computeMotifSaisine()` lit
    `aiData.motifSaisineMediationDetected` (champ réel) au lieu du champ aspirationnel.
11. `ordonnance-requete-section.component.ts` : `prefillFromAi()` branche
    `motifRequete` sur un futur champ (pas encore dans ce lot — cf. §10).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Sous-objet `protection_divorce_detection_v2` absent du JSON IA | Tous les champs null, no-op gracieux côté frontend |
| Date non-ISO (ex. "20/03/2026") | `isoDateOrNull()` renvoie null — pas d'erreur |
| Code régime hors whitelist | `whitelistedOrNull()` renvoie null |
| `dateAudienceHomologationDcBe` dans un dossier FR | Le helper lit null car le composant `divorce-dc-be` n'est monté que si `workspaceCountry === 'BELGIQUE'` |
| Deux dates concurrentes présentes | Prompt distingue les concepts — champ null si ambigu (invariant §5.1 cadrage) |

---

## Analyse de cohérence transversale

### Préoccupation transversale — outil décisionnel métier (déclencheur)

- [x] Outils impactés : `majeurs-proteges-section`, `pma-gpa-bioethique-section`,
  `mediation-familiale-section`, `ordonnance-requete-section`, `divorce-accepte-section`,
  `divorce-alteration-section`, `divorce-dc-be-section`
- [x] Self-check grep pré-commit obligatoire (mémoire projet)
- [x] `TOOL_REGISTRY` frontend ↔ `decisional-tools-panel` ↔ binding `inputs(ctx)` vérifié
- [x] Smoke tests E2E `cd e2e && npm test` avant push
- [x] Garde pays `divorce-dc-be` : **BELGIQUE UNIQUEMENT** — champ annoté + prompt protégé

### Composants impactés

Backend :
- `CaseAnalysisResponse.java` (record + Builder + extractFamilleData)
- `LegalDomainPromptBuilder.java` (FAMILLE_INSTRUCTION_P2)
- `CaseAnalysisResponseTest.java` (nouveaux cas de test)

Frontend :
- `divorce-accepte.model.ts` (JSDoc FamilleExtractedData)
- `divorce-accepte-section-prefill-rules.ts` + spec
- `divorce-alteration-section-prefill-rules.ts` + spec
- `divorce-dc-be-section-prefill-rules.ts` + spec (**BELGIQUE**)
- `majeurs-proteges-section-prefill-rules.ts` + spec
- `pma-gpa-bioethique-section-prefill-rules.ts` + spec
- `mediation-familiale-section-prefill-rules.ts` + spec

---

## Champs IA à extraire (pré-remplissage) — invariant F-246

### Tableau outil → champs branchés

| Outil | Champ frontend | Clé JSON IA (sous-objet) | Type | Whitelist/Format |
|---|---|---|---|---|
| `majeurs-proteges` | `regimeProtectionDemande` | `protection_divorce_detection_v2.regime_protection_majeurs` | string | SAUVEGARDE_JUSTICE, HABILITATION_FAMILIALE, CURATELLE_SIMPLE, CURATELLE_RENFORCEE, TUTELLE, MANDAT_PROTECTION_FUTURE |
| `majeurs-proteges` | `dateCertificatMedical` | `protection_divorce_detection_v2.date_certificat_medical_majeurs` | ISO date | YYYY-MM-DD |
| `pma-gpa-bioethique` | `datePMA` | `protection_divorce_detection_v2.date_pma` | ISO date | YYYY-MM-DD |
| `pma-gpa-bioethique` | `dateReconnaissanceAnterieurePMA` | `protection_divorce_detection_v2.date_reconnaissance_anterieure_pma` | ISO date | YYYY-MM-DD |
| `pma-gpa-bioethique` | `dateDon` | `protection_divorce_detection_v2.date_don_gametes` | ISO date | YYYY-MM-DD |
| `mediation-familiale` | `motifSaisine` | `protection_divorce_detection_v2.motif_saisine_mediation` | string | AUTORITE_PARENTALE, CONTRIBUTION_ENTRETIEN, DROIT_VISITE, RESIDENCE, AUTRE |
| `divorce-accepte` | `dateAssignation` | `protection_divorce_detection_v2.date_assignation_divorce` | ISO date | YYYY-MM-DD |
| `divorce-alteration` | `dateAssignation` | `protection_divorce_detection_v2.date_assignation_divorce` | ISO date | YYYY-MM-DD (même champ partagé) |
| `divorce-dc-be` (**BE**) | `dateAudienceHomologation` | `protection_divorce_detection_v2.date_audience_homologation_dc_be` | ISO date | YYYY-MM-DD — **Belgique uniquement** |

**Extension backend :** 8 champs nouveaux dans `FamilleExtractedData` + record + Builder.
**Extension prompt :** section SF-246-27 dans `FAMILLE_INSTRUCTION_P2`.
**Distinctions clés :**
- `date_assignation_divorce` = date d'introduction de la requête en divorce devant le JAF
  DISTINCT de `date_acceptation_pv` (signature du PV) et de `date_audience_aomp` (AOMP).
- `date_audience_homologation_dc_be` = date de l'audience d'homologation de la convention
  par le tribunal de la famille belge (CJ art. 1287+ ; **Belgique uniquement**).
- `date_pma` / `date_reconnaissance_anterieure_pma` / `date_don_gametes` : trois dates
  bioéthiques distinctes (loi 2021) — ne pas confondre.

---

## Critères d'acceptation vérifiables

1. `CaseAnalysisResponseTest` : 5 nouveaux cas (CA-1 à CA-5) couvrant le sous-objet
   `protection_divorce_detection_v2` ; 0 FAIL.
2. `divorce-accepte-section-prefill-rules.spec.ts` : `computePrefillCount` inclut
   `dateAssignation` — compte 6/6 sur fixture complète.
3. `divorce-alteration-section-prefill-rules.spec.ts` : idem — compte 6/6.
4. `divorce-dc-be-section-prefill-rules.spec.ts` : `computeDateAudienceHomologation`
   retourne la date sur fixture BE, null sur fixture FR (pas de champ).
5. `majeurs-proteges-section-prefill-rules.spec.ts` : compte 14/14 sur fixture complète
   (12 anciens + `regimeProtection` + `dateCertificat` désormais issus de champs réels).
6. `pma-gpa-bioethique-section-prefill-rules.spec.ts` : count 4/4 (1 dispositif + 3 dates).
7. `mediation-familiale-section-prefill-rules.spec.ts` : `computeMotifSaisine` retourne
   le code whitelist valide, null sur code hors whitelist.
8. Smoke E2E : ~27 échecs préexistants tolérés — pas de nouveau FAIL.
9. `git grep 'aspirationnel\|motifSaisineMediationDetecte' --` : zéro occurrence dans les
   helpers modifiés.

---

## Plan de test minimal

### Tests backend (JUnit)
- CA-1 : JSON complet avec `protection_divorce_detection_v2` peuplé (8 champs) → 8 champs non-null.
- CA-2 : Sous-objet absent → tous null, no-op gracieux.
- CA-3 : Dates non-ISO dans le sous-objet → tous null.
- CA-4 : Code régime hors whitelist → null.
- CA-5 : Fixture BE avec `date_audience_homologation_dc_be` + fixture FR sans → isolation.

### Tests frontend (Jest)
- Spec divorce-accepte : fixture avec `dateAssignationDivorce` valide → count +1 ; ISO invalide → 0.
- Spec divorce-alteration : pareil.
- Spec divorce-dc-be : fixture avec `dateAudienceHomologationDcBe` → retour date ; fixture sans → null.
- Spec majeurs-proteges : `regimeProtectionMajeursDetected` + `dateCertificatMedicalMajeursDetected` → compte.
- Spec pma-gpa : `datePmaDetected` → compte ; non-ISO → 0.
- Spec mediation-familiale : `motifSaisineMediationDetected` → code whitelisté retourné.

---

## Tables / endpoints / composants impactés

- `CaseAnalysisResponse.java` : record `FamilleExtractedData` + Builder + `extractFamilleData`
- `LegalDomainPromptBuilder.java` : `FAMILLE_INSTRUCTION_P2`
- `CaseAnalysisResponseTest.java`
- `divorce-accepte.model.ts` (JSDoc)
- 6 helpers prefill-rules + leurs specs

---

## Hors périmètre

- `motifRequete` de l'outil `ordonnance-requete` : seul `presenceEnfants` est branchable via
  `communaute_partage_protection_detection_v2.presence_enfants` (SF-246-25, déjà livré) ;
  `motifRequete` est qualifié par l'avocat (info structurellement absente des pièces).
- Autres champs de `majeurs-proteges` déjà branchés en SF-FA-25-02 (altérations, actes, etc.) :
  non retouchés — seuls `regimeProtection` et `dateCertificat` migrent vers source réelle.
- `divorce-alteration.dateCessationVieCommune` : déjà branché via `vie_commune_detection.date_separation` (SF-246-08).
- Toute refonte backend de l'algorithme de calcul des outils.
- Déploiement de l'outil `ordonnance-requete` préfill complet (SF ultérieure F-246-28+).

# Mini-spec — [F-246 / SF-246-18] Pré-remplissage IA — Lot AES Immigration FR

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Pattern de référence : `docs/features/F-246/SF-246-17-lot-oqtf-recours-immigration-fr.md` (SF-246-17, commit `5e92b9b1`).

---

## Identifiant

`F-246 / SF-246-18`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-18-lot-aes-immigration-fr`

---

## Objectif

Compléter le pré-remplissage IA des 4 outils AES Immigration FR en branchant
tous les champs extractibles des pièces qui restent en no-op structurel :

- `aes-etudiant` : `dureePresenceMois` (dérivé de `dateEntreeFrance`) + `anneesScolariteEnFranceConsecutives` + `niveauEtudesActuel`
- `aes-famille` : `dureeScolaritePlusAncienEnfantAnnees` + `dateDepotDemande` (depuis `dateDepotProcedure`)
- `aes-humanitaire` : `motifHumanitaireDominant` (enum `MotifHumanitaire`)
- `aes-metiers-tension` : `dateEntreeFrance` + `moisActiviteSalarieeDernieres24Mois` + `codeMetier` (texte libre ROME)

Bar invariant F-246 (2026-05-19) : tenter l'extraction ; `null` si absent des pièces.

---

## Comportement attendu

### Cas nominal

1. **Backend — 8 nouveaux champs `ImmigrationExtractedData`** (sous-objet logique AES) :
   - `aesDateEntreeFrance` (String ISO YYYY-MM-DD) — date d'entrée en France extraite du passeport/visa.
   - `aesDureePresenceMois` (Integer) — calculé dans l'extracteur depuis `aesDateEntreeFrance` (mois entiers écoulés depuis aujourd'hui) ; pas de clé LLM dédiée.
   - `aesAnneesScolariteConsecutives` (Integer) — années d'études consécutives en France extraites du certificat de scolarité.
   - `aesNiveauEtudes` (String, whitelist 4 codes) — `LYCEE` / `BAC_PLUS_1_2` / `BAC_PLUS_3_4` / `BAC_PLUS_5_PLUS` — déduit du diplôme le plus élevé mentionné.
   - `aesDureeScolaritePlusAncienEnfantAnnees` (Integer) — durée de scolarité en France de l'enfant le plus anciennement inscrit (attestation de scolarité).
   - `aesMotifHumanitaire` (String, whitelist 6 codes) — `RISQUES_AU_RETOUR` / `ISOLEMENT_TOTAL` / `VICTIME_VIOLENCES` / `VICTIME_TRAITE` / `SITUATION_MEDICALE_PRECAIRE_HORS_L425_9` / `AUTRE_HUMANITAIRE`.
   - `aesMoisActiviteSalariee` (Integer, 0–24) — mois de travail salarié détectés dans les bulletins de paie/contrats des 24 derniers mois.
   - `aesCodeMetier` (String libre) — code ROME ou libellé du métier en tension tel qu'il figure sur l'attestation employeur / fiche de paie.

2. **Prompt `IMMIGRATION_INSTRUCTION`** — 7 nouvelles clés JSON instruites (FRANCE uniquement, null si absent des pièces) :
   - `aes_date_entree_france` : date ISO YYYY-MM-DD d'entrée en France (passeport / visa d'entrée / premier titre).
   - `aes_annees_scolarite_consecutives` : entier ≥ 0, années d'études consécutives en France.
   - `aes_niveau_etudes` : l'un des 4 codes exacts `LYCEE` / `BAC_PLUS_1_2` / `BAC_PLUS_3_4` / `BAC_PLUS_5_PLUS` ou null.
   - `aes_duree_scolarite_plus_ancien_enfant_annees` : entier ≥ 0, scolarité France de l'enfant le plus anciennement inscrit.
   - `aes_motif_humanitaire` : l'un des 6 codes exacts ou null.
   - `aes_mois_activite_salariee` : entier 0–24 (24 derniers mois de salariat).
   - `aes_code_metier` : texte libre (code ROME ou libellé du métier) ou null.

3. **Extracteur `extractImmigrationData()`** — parse et valide les 7 clés + calcule `aesDureePresenceMois`.

4. **DTO frontend `ImmigrationExtractedData`** — 8 nouveaux champs optionnels exposés.

5. **Helpers prefill-rules mis à jour** :

   | Outil | Nouvelles fonctions | `computePrefillCount` avant → après |
   |---|---|---|
   | `aes-etudiant` | `computeDureePresenceMois` + `computeAnneesScolarite` + `computeNiveauEtudes` | 2 → 5 |
   | `aes-famille` | `computeDureeScolaritePlusAncienEnfant` + `computeDateDepotDemande` | 2 → 4 |
   | `aes-humanitaire` | `computeMotifHumanitaire` | 2 → 3 |
   | `aes-metiers-tension` | `computeDateEntreeFrance` + `computeMoisActiviteSalariee` + `computeCodeMetier` | 1 → 4 |

6. **Composants `prefillFromAi()`** — appliquent les nouvelles fonctions, posent les signaux de provenance `'IA'` pour chaque champ, affichent les badges `auto_awesome`. Remise à `null` de la provenance au changement manuel.

7. **Reset on persisted result** — lors du `load()` côté composant, les provenances des nouveaux champs sont remises à `null` (les valeurs viennent du serveur, pas de l'IA).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `aes_date_entree_france` manquant | `aesDateEntreeFrance` = null ; `aesDureePresenceMois` = null |
| Date ISO future | `aesDateEntreeFrance` = null (guard non-futur dans l'extracteur) |
| `aes_niveau_etudes` hors whitelist | `aesNiveauEtudes` = null |
| `aes_motif_humanitaire` hors whitelist | `aesMotifHumanitaire` = null |
| `aes_mois_activite_salariee` hors [0–24] | `aesMoisActiviteSalariee` = null |
| Dossier BELGIQUE | Tous les 8 champs = null (prompt impose null hors FR) |
| `aiData` arrive après le premier rendu | `prefillFromAi()` ré-invoqué dans `ngOnChanges()` — no-op si la valeur est déjà présente |
| `standaloneMode = true` | `prefillFromAi()` court-circuité |

---

## Analyse de cohérence transversale

- Pas de nouvelle route, guard, workspace, ni plan — exemptés.
- **Outil décisionnel métier** : modification de 4 outils existants — invariant un outil = une situation métier respecté (pas de fusion).
- Smoke tests E2E requis avant push.

---

## Champs IA à extraire — détail outil par outil

### `aes-etudiant` (F-IM-09 voie étudiante)

| Champ formulaire | Signal composant | Provenance backend | Statut avant | Statut après |
|---|---|---|---|---|
| `dateEntreeFrance` | `dateEntreeFrance` | `aesDateEntreeFrance` | à brancher (cast `as any`) → **pré-rempli** |  pré-rempli |
| `dureePresenceMois` | `dureePresenceMois` | calculé depuis `aesDateEntreeFrance` | à brancher (dérivé) | **pré-rempli** |
| `anneesScolariteEnFranceConsecutives` | `anneesScolariteEnFranceConsecutives` | `aesAnneesScolariteConsecutives` | à brancher | **pré-rempli** |
| `niveauEtudesActuel` | `niveauEtudesActuel` | `aesNiveauEtudes` | à brancher | **pré-rempli** |
| `dateDepotDemande` | `dateDepotDemande` | `dateDepotProcedure` (existant) | pré-rempli | inchangé |
| `resultatsAcademiques` | `resultatsAcademiques` | — | info structurellement absente | inchangé |
| `moyensSubsistance` | `moyensSubsistance` | — | info structurellement absente | inchangé |
| `menaceOrdrePublic` | `menaceOrdrePublic` | — | info structurellement absente | inchangé |
| `parcoursCoherent` | `parcoursCoherent` | — | info structurellement absente | inchangé |

### `aes-famille` (F-IM-09 voie familiale L.435-1)

| Champ formulaire | Signal composant | Provenance backend | Statut avant | Statut après |
|---|---|---|---|---|
| `dateEntreeFrance` | `dateEntreeFrance` | `aesDateEntreeFrance` | pré-rempli (cast `as any`) | **pré-rempli sur champ typé** |
| `dureePresenceMois` | `dureePresenceMois` | calculé depuis `aesDateEntreeFrance` | pré-rempli (dérivé) | inchangé |
| `dureeScolaritePlusAncienEnfantAnnees` | `dureeScolaritePlusAncienEnfantAnnees` | `aesDureeScolaritePlusAncienEnfantAnnees` | à brancher | **pré-rempli** |
| `dateDepotDemande` | `dateDepotDemande` | `dateDepotProcedure` (existant) | à brancher | **pré-rempli** |
| `enfantsScolarisesFrance` | `enfantsScolarisesFrance` | — | info structurellement absente | inchangé |

### `aes-humanitaire` (F-IM-09 voie humanitaire L.435-2)

| Champ formulaire | Signal composant | Provenance backend | Statut avant | Statut après |
|---|---|---|---|---|
| `dateEntreeFrance` | `dateEntreeFrance` | `aesDateEntreeFrance` | pré-rempli (cast `as any`) | **pré-rempli sur champ typé** |
| `dateDepotDemande` | `dateDepotDemande` | `dateDepotProcedure` (existant) | pré-rempli | inchangé |
| `motifHumanitaireDominant` | `motifHumanitaire` | `aesMotifHumanitaire` | à brancher | **pré-rempli** |

### `aes-metiers-tension` (F-IM-09 métiers en tension L.435-4)

| Champ formulaire | Signal composant | Provenance backend | Statut avant | Statut après |
|---|---|---|---|---|
| `dateEntreeFrance` | `dateEntreeFrance` | `aesDateEntreeFrance` | à brancher (champ existait mais pas dans record) | **pré-rempli** |
| `dateDepotDemande` | `dateDepotDemande` | `dateDepotProcedure` (existant) | pré-rempli | inchangé |
| `moisActiviteSalarieeDernieres24Mois` | `moisActiviteSalarieeDernieres24Mois` | `aesMoisActiviteSalariee` | à brancher | **pré-rempli** |
| `codeMetier` | `codeMetier` | `aesCodeMetier` | à brancher | **pré-rempli** |
| `metierEstEnTension` | `metierEstEnTension` | — | info structurellement absente | inchangé |
| `menaceOrdrePublic` | `menaceOrdrePublic` | — | info structurellement absente | inchangé |
| `contratOuPromesseValide` | `contratOuPromesseValide` | — | info structurellement absente | inchangé |

---

## Critères d'acceptation vérifiables

1. Fixture JSON avec `aes_date_entree_france = "2021-03-15"` → `aesDureePresenceMois` ≥ 36 (calculé dynamiquement).
2. Fixture avec `aes_date_entree_france = "2030-01-01"` → `aesDateEntreeFrance` = null (date future rejetée).
3. Fixture avec `aes_niveau_etudes = "BAC_PLUS_5_PLUS"` → `aesNiveauEtudes = "BAC_PLUS_5_PLUS"`.
4. Fixture avec `aes_niveau_etudes = "MASTER_2"` (hors whitelist) → `aesNiveauEtudes` = null.
5. Fixture avec `aes_motif_humanitaire = "VICTIME_VIOLENCES"` → `aesMotifHumanitaire = "VICTIME_VIOLENCES"`.
6. Jest `AesEtudiantPrefillRules` : `computeAnneesScolarite` retourne null si champ absent ; retourne la valeur sinon.
7. Jest `AesMetiersTensionPrefillRules` : `computeDateEntreeFrance` lit `aiData.aesDateEntreeFrance` ; `computeCodeMetier` retourne null si texte vide.
8. Dossier BE → tous les 8 nouveaux champs null (vérifié par fixture `"domaine" = "BELGIQUE"`).
9. `prefillFromAi()` aes-etudiant : pose `provenanceNiveauEtudes = 'IA'` si champ vide.
10. `prefillFromAi()` aes-metiers-tension : n'écrase pas `codeMetier` si déjà saisi (provenance !== 'IA').

---

## Plan de test minimal

### Backend (JUnit)

- `CaseAnalysisResponseTest` — 8 nouveaux cas dans `extractImmigrationData_*` :
  - `aes_date_entree_france` passé → `aesDateEntreeFrance` correctement parsé + `aesDureePresenceMois` calculé.
  - `aes_date_entree_france` future → `aesDateEntreeFrance` null + `aesDureePresenceMois` null.
  - `aes_niveau_etudes` valide + invalide.
  - `aes_motif_humanitaire` valide + invalide.
  - `aes_mois_activite_salariee` valide (10) + hors range (25) → null.
  - Dossier BE (objet vide) → tous null.

### Frontend (Jest)

- `aes-etudiant-section-prefill-rules.spec.ts` — nouveaux cas :
  - `computeDureePresenceMois` avec date passée + date future + null.
  - `computeAnneesScolarite` avec valeur entière + null.
  - `computeNiveauEtudes` whitelist + hors whitelist.
- `aes-famille-section-prefill-rules.spec.ts` — nouveaux cas :
  - `computeDureeScolaritePlusAncienEnfant` avec valeur + null.
  - `computeDateDepotDemande` (ISO non futur + futur).
- `aes-humanitaire-section-prefill-rules.spec.ts` — nouveaux cas :
  - `computeMotifHumanitaire` avec code valide + invalide.
- `aes-metiers-tension-section-prefill-rules.spec.ts` — nouveaux cas :
  - `computeDateEntreeFrance` + `computeMoisActiviteSalariee` + `computeCodeMetier`.

---

## Tables / endpoints / composants impactés

**Backend :**
- `CaseAnalysisResponse.java` — `ImmigrationExtractedData` record : +8 champs + Builder + `extractImmigrationData()`.
- `LegalDomainPromptBuilder.java` — `IMMIGRATION_INSTRUCTION` : +7 clés JSON.

**Frontend :**
- `case-analysis.model.ts` — `ImmigrationExtractedData` interface : +8 champs.
- `aes-etudiant-section-prefill-rules.ts` — +3 fonctions + compteur.
- `aes-famille-section-prefill-rules.ts` — +2 fonctions + compteur.
- `aes-humanitaire-section-prefill-rules.ts` — +1 fonction + compteur.
- `aes-metiers-tension-section-prefill-rules.ts` — +3 fonctions + compteur.
- `aes-etudiant-section.component.ts` — `prefillFromAi()` + 3 provenances.
- `aes-famille-section.component.ts` — `prefillFromAi()` + 2 provenances.
- `aes-humanitaire-section.component.ts` — `prefillFromAi()` + 1 provenance.
- `aes-metiers-tension-section.component.ts` — `prefillFromAi()` + 3 provenances.
- Specs Jest correspondantes (+4 fichiers).

---

## Hors périmètre

- Outils BE (régimes différents — hors lot).
- Refactoring des flags F-201 (non touchés).
- Champs `resultatsAcademiques`, `moyensSubsistance`, `menaceOrdrePublic`, `parcoursCoherent`, `enfantsScolarisesFrance`, `contratOuPromesseValide` — info structurellement absente des pièces, exemptés.

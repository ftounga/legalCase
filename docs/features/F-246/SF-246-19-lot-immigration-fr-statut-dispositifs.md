# SF-246-19 — Lot Immigration FR statut & dispositifs

**Feature parente :** F-246 — Complétion du pré-remplissage IA des outils décisionnels  
**Branche :** `feat/SF-246-19-lot-immigration-fr-statut-dispositifs`  
**Date :** 2026-05-20

---

## Objectif

Brancher le pré-remplissage IA sur 7 outils Immigration FR dont les champs saisissables
étaient encore à 0 ou partiellement couverts : `changement-statut`, `immigration-title-decision`,
`naturalisation`, `mineurs-immigration`, `regime-algerien`, `asile-avance`, `mesures-eloignement`.

---

## Périmètre — champs par outil

### 1. Changement de statut (F-IM-11) — 2 champs nouveaux

| Champ formulaire       | Clé JSON IA                    | Règle                                        |
|------------------------|--------------------------------|----------------------------------------------|
| `titreEnvisage`        | `changement_titre_envisage`    | `mapTitreSejourFromIa()`, null si non mappable |
| `remunerationContratEur` | `changement_remuneration_eur` | entier > 0 (≤ 500 000), null sinon           |

> Provenance : `provenanceTitreEnvisage` + `provenanceRemuneration` (nouveaux signals).
> Condition : ne pas écraser si provenance !== 'IA'.

### 2. Immigration title decision (F-IM-05) — 0 champ nouveau

Les 3 champs (`nationaliteUe`, `motif`, `situationFamiliale`) sont déjà branchés via
les triggerEvents / typeTitreSejourCode / heuristique texte. Aucun ajout nécessaire.

### 3. Naturalisation (F-IM-13) — 3 champs nouveaux

| Champ formulaire               | Clé JSON IA                       | Règle                         |
|--------------------------------|-----------------------------------|-------------------------------|
| `dureeResidenceReguliereAnnees`| `nat_duree_residence_reguliere_annees` | entier ≥ 0, ≤ 70, null sinon |
| `dureeMariageAnnees`           | `nat_duree_mariage_annees`       | entier ≥ 0, ≤ 70, null sinon |
| `ageDemandeur`                 | `nat_age_demandeur`              | entier ≥ 0, ≤ 120, null sinon |

> Provenance : `provenanceDureeResidence`, `provenanceDureeMariage`, `provenanceAge` (nouveaux).

### 4. Mineurs étrangers (F-IM-19) — 1 champ nouveau + 2 casts supprimés

| Champ formulaire   | Source                             | Règle                                        |
|--------------------|------------------------------------|----------------------------------------------|
| `dateNaissance`    | `mineurs_date_naissance` (nouveau)| ISO YYYY-MM-DD, date passée, null sinon      |
| `dateEntreeFrance` | `aesDateEntreeFrance` (SF-246-18) | déjà typé, cast `any` supprimé               |
| `nationalite`      | `nationalite` (F-235, déjà typé)  | déjà typé, cast `any` supprimé               |

> `dateEntreeFrance` et `nationalite` : accès typé direct sur `ai.aesDateEntreeFrance`
> et `ai.nationalite` — les casts `as any` existants dans le helper sont supprimés.

### 5. Régime algérien (F-IM-17) — 1 champ nouveau

| Champ formulaire          | Clé JSON IA                        | Règle                    |
|---------------------------|------------------------------------|--------------------------|
| `presenceReguliereFranceMois` | `algerien_presence_reguliere_mois` | entier ≥ 0, ≤ 600, null sinon |

> Provenance : `provenancePresenceReguliere` (nouveau signal).

### 6. Asile avancé (F-IM-12) — 1 champ nouveau

| Champ formulaire        | Clé JSON IA                    | Règle                              |
|-------------------------|--------------------------------|------------------------------------|
| `dateDecisionAnterieure`| `asile_date_decision_anterieure`| ISO YYYY-MM-DD, non future, null sinon |

> Provenance : `provenanceDateDecision` (nouveau signal).

### 7. Mesures d'éloignement (F-IM-20) — 2 champs nouveaux

| Champ formulaire              | Clé JSON IA                         | Règle                              |
|-------------------------------|-------------------------------------|------------------------------------|
| `dureePresenceIrreguliereMois`| `eloi_duree_presence_irreguliere_mois` | entier ≥ 0, ≤ 600, null sinon  |
| `motifMenace`                 | `eloi_motif_menace`                 | whitelist `MotifMenaceCode`, insensible casse |

> Whitelist `motifMenace` : `ORDRE_PUBLIC`, `SECURITE_ETAT`, `TERRORISME`, `CRIMINALITE_GRAVE`, `AUTRE`.
> Provenance : `provenanceDureePresenceIrr`, `provenanceMotif` (les provenances `provenanceDispositif` et `provenanceMotif` existent déjà — seule `provenanceDureePresenceIrr` est nouvelle).

---

## Nouveaux champs backend (ImmigrationExtractedData)

```
// SF-246-19 : pré-fill statut & dispositifs Immigration FR
String changementTitreEnvisage,         // mapTitreSejourFromIa côté extracteur
Integer changementRemunerationEur,      // > 0, ≤ 500 000
Integer natDureeResidenceReguliereAnnees, // [0, 70]
Integer natDureeMariageAnnees,          // [0, 70]
Integer natAgeDemandeur,               // [0, 120]
String mineursDateNaissance,           // ISO non-future
Integer algerienPresenceReguliereMois, // [0, 600]
String asileDateDecisionAnterieure,    // ISO non-future
Integer eloiDureePresenceIrreguliereMois, // [0, 600]
String eloiMotifMenace,               // whitelist MotifMenaceCode 5 valeurs
```

---

## Comportement nominal

1. Backend extrait les 10 nouveaux champs du nœud JSON IA Immigration.
2. Prompt enrichi avec instructions ciblées FR uniquement.
3. Frontend DTO reçoit les nouveaux champs, helpers purs mis à jour.
4. `prefillFromAi()` des 7 composants branche les nouveaux champs.
5. `computePrefillCount()` reflète les nouveaux champs.

## Cas d'erreur

- Valeur hors plage → `null` (fail-open, jamais d'exception).
- Date future → `null`.
- Code enum invalide → `null` (normalizeEnumCode).
- Champ absent du JSON → `null`.
- Workspace BELGIQUE → tous les champs restent null (FR uniquement).

---

## Critères d'acceptation

1. `computePrefillCount()` retourne le nombre correct pour chaque outil.
2. `prefillFromAi()` ne pré-remplit pas si l'avocat a déjà saisi une valeur (provenance !== 'IA').
3. Aucun cast `as any` sur `dateEntreeFrance` / `nationalite` dans `mineurs-immigration`.
4. Tests Jest PASS pour tous les helpers.
5. Tests JUnit PASS pour tous les extracteurs backend.
6. Aucune régression sur les outils existants (smoke E2E).

---

## Plan de test

### Backend (JUnit — CaseAnalysisResponseTest)

- `from_immigration_sf246_19_changementTitreEnvisage_nominal`
- `from_immigration_sf246_19_changementTitreEnvisage_invalid_returns_null`
- `from_immigration_sf246_19_changementRemuneration_nominal`
- `from_immigration_sf246_19_changementRemuneration_out_of_range_returns_null`
- `from_immigration_sf246_19_nat_dureeResidence_nominal`
- `from_immigration_sf246_19_nat_dureeMariage_nominal`
- `from_immigration_sf246_19_nat_ageDemandeur_nominal`
- `from_immigration_sf246_19_mineurs_dateNaissance_nominal`
- `from_immigration_sf246_19_mineurs_dateNaissance_future_rejected`
- `from_immigration_sf246_19_algerien_presenceReguliere_nominal`
- `from_immigration_sf246_19_asile_dateDecisionAnterieure_nominal`
- `from_immigration_sf246_19_asile_dateDecisionAnterieure_future_rejected`
- `from_immigration_sf246_19_eloi_dureePresence_nominal`
- `from_immigration_sf246_19_eloi_motifMenace_whitelist_valid`
- `from_immigration_sf246_19_eloi_motifMenace_invalid_returns_null`
- `from_immigration_sf246_19_all_new_fields_null_graceful`

### Frontend (Jest — helpers purs)

- `ChangementStatutPrefillRules` : computeTitreEnvisage / computeRemuneration / computePrefillCount
- `NaturalisationPrefillRules` : computeDureeResidence / computeDureeMariage / computeAge / computePrefillCount
- `MineursImmigrationPrefillRules` : computeDateNaissance (typed) / computeDateEntreeFrance (typed) / computeNationalite (typed)
- `RegimeAlgerienPrefillRules` : computePresenceReguliere / computePrefillCount
- `AsileAvancePrefillRules` : computeDateDecisionAnterieure / computePrefillCount
- `MesuresEloignementPrefillRules` : computeDureePresenceIrr / computeMotifMenace / computePrefillCount

---

## Fichiers impactés

### Backend
- `CaseAnalysisResponse.java` — 10 champs + builder + builder setters + toBuilder + build + extracteur + whitelists + null guard
- `LegalDomainPromptBuilder.java` — `IMMIGRATION_INSTRUCTION` + 10 clés FR

### Frontend
- `case-analysis.model.ts` — 10 champs dans `ImmigrationExtractedData`
- `changement-statut-section-prefill-rules.ts` — `computeTitreEnvisage` + `computeRemuneration` + `computePrefillCount`
- `changement-statut-section-prefill-rules.spec.ts` — nouveaux cas
- `changement-statut-section.component.ts` — provenance + prefillFromAi
- `naturalisation-section-prefill-rules.ts` — 3 compute functions + `computePrefillCount`
- `naturalisation-section-prefill-rules.spec.ts` — nouveaux cas
- `naturalisation-section.component.ts` — provenances + prefillFromAi
- `mineurs-immigration-section-prefill-rules.ts` — casts `any` → typed
- `mineurs-immigration-section-prefill-rules.spec.ts` — mise à jour
- `mineurs-immigration-section.component.ts` — cast `any` dans buildDateEntreeAlert + buildDateNaissanceAlert → typed
- `regime-algerien-section-prefill-rules.ts` — `computePresenceReguliereFranceMois` + `computePrefillCount`
- `regime-algerien-section-prefill-rules.spec.ts` — nouveaux cas
- `regime-algerien-section.component.ts` — provenance + prefillFromAi
- `asile-avance-section-prefill-rules.ts` — `computeDateDecisionAnterieure` + `computePrefillCount`
- `asile-avance-section-prefill-rules.spec.ts` — nouveaux cas
- `asile-avance-section.component.ts` — provenance + prefillFromAi
- `mesures-eloignement-section-prefill-rules.ts` — `computeDureePresenceIrreguliereMois` + `computeMotifMenace` + `computePrefillCount`
- `mesures-eloignement-section-prefill-rules.spec.ts` — nouveaux cas
- `mesures-eloignement-section.component.ts` — provenance + prefillFromAi

---

## Hors périmètre

- Immigration BE (régimes distincts, feature jumelle backlog).
- `immigration-title-decision` : déjà à 3 champs couverts, aucun nouveau champ.
- Ajout de nouveaux codes enum dans les modèles frontend (les whitelists existantes sont réutilisées).

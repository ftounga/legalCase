# SF-246-22 — Pré-remplissage du « total rémunérations de la période » (congés payés)

> Extension de **F-246** (exhaustivité du pré-remplissage IA des outils décisionnels). Frontend pur, exempte étape 0/0bis (champ existant, réutilise le pattern de provenance déjà en place sur cet outil).

## Objectif (une phrase)

Pré-remplir le champ **« Total rémunérations brutes de la période (€) »** de l'outil *Indemnité compensatrice de congés payés* (F-DT-26) par une **estimation dérivée** du salaire mensuel brut, au lieu de le laisser systématiquement vide.

## Constat (pourquoi)

Test 2026-06-16 : sur cet outil, 3 champs n'étaient pas pré-remplis. 2 (jours acquis/pris) dépendent d'une détection dans les pièces (comportement correct). Le 3ᵉ — **total rémunérations de la période** — n'avait **aucune source IA** : l'IA extrait le salaire **mensuel**, pas le cumul sur la période de référence. L'avocat devait toujours le saisir, alors qu'une **estimation** raisonnable est dérivable (salaire mensuel × 12, période de référence annuelle standard de la méthode du 1/10ᵉ).

## Comportement nominal

- Nouvelle règle `computeTotalRemunerationPeriodeEur(input)` = `salaireBrutMensuel × 12` si **FRANCE** et `salaireBrutMensuel > 0`, sinon `null`.
- `prefillFromAi()` pose la valeur dans `totalRemunerationPeriodeEur` (si vide ou provenance IA) + provenance `IA`.
- Le champ affiche une **note de provenance distincte** (réutilise `.cp-provenance-note`) : « **Estimation IA (salaire mensuel × 12) — ajustez si la période diffère** » — honnête sur le caractère **dérivé** (≠ « Pré-rempli depuis l'analyse » des champs réellement extraits).
- Le champ reste **éditable** ; au 1ᵉʳ changement manuel, la provenance IA est effacée (pattern existant).
- Le **compteur de pré-fill** (`computePrefillCount`, badge `auto_awesome` du panneau) inclut ce champ → +1 quand le salaire est présent.

## Cas d'erreur / limites

1. **Pas de salaire détecté** → règle `null` → champ vide (inchangé).
2. **Workspace BELGIQUE** → `null` (régime BE distinct, hors périmètre).
3. **Estimation ≠ exacte** : le total réel peut inclure primes/heures sup et exclure des absences ; d'où le mot **« estimation »** + l'invitation à ajuster, et le caractère éditable + relecture avant calcul (F-292). « silence > erreur » respecté : la valeur est une **dérivation explicite et marquée**, pas une donnée inventée non sourcée.

## Critères d'acceptation vérifiables

- [ ] `computeTotalRemunerationPeriodeEur({salaireBrutMensuel: 2500, FRANCE})` = `30000` ; BELGIQUE / salaire absent → `null`.
- [ ] À l'ouverture de l'outil sur un dossier FR avec salaire détecté, le champ « Total rémunérations… » est **pré-rempli** (= salaire × 12) + note « Estimation IA ».
- [ ] `computePrefillCount({salaireBrutMensuel: 2500, FRANCE})` = **2** (salaire + total) ; avec date = **3**.
- [ ] Le champ reste éditable ; saisie manuelle → note de provenance retirée.
- [ ] BELGIQUE → aucun pré-fill (count 0).

## Plan de test minimal

- **Unitaires** (`conges-payes-section-prefill-rules.spec.ts`) : `computeTotalRemunerationPeriodeEur` (FR/BE/absent) ; `computePrefillCount` recalibré (2 / 3).
- **Composant** (`conges-payes-section.component.spec.ts`) : `prefillFromAi` pose le total + provenance ; reset au changement.
- **Isolation workspace** : N/A (composant de présentation, pré-fill local depuis la synthèse déjà chargée).

## Composants impactés

- `conges-payes-section-prefill-rules.ts` (nouvelle règle + count).
- `conges-payes-section.component.ts` (signal `provenanceTotal` + wiring).
- `conges-payes-section.component.html` (note de provenance).
- Specs associés.

**Aucun** : backend, endpoint, migration, contrat API (réutilise `salaireBrutMensuel` déjà extrait).

## Hors périmètre

- Dérivation fine via durée d'emploi réelle (hire date → rupture, < 12 mois) — l'estimation annuelle suffit, l'avocat ajuste.
- Pré-fill des jours acquis/pris (déjà câblé SF-246-21 ; dépend de la détection pièces).
- Tout calcul backend.

## Analyse transversale

- **Outil décisionnel** : modifie le **pré-remplissage** de F-DT-26 (pas le calcul ni le verdict) → invariant « 1 outil = 1 situation » intact. Self-check : specs rules + composant + prefill-count-integrity.
- **Pré-fill IA** : champ dérivé (pas de nouveau champ `*ExtractedData` ; réutilise `salaireBrutMensuel`).
- **Auth/workspace/navigation/plans** : aucun. **Smoke E2E** : N/A.

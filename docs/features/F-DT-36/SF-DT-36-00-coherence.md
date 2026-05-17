# F-DT-36 — Analyse des nullités de procédure de licenciement (Document de cadrage cohérence — étape 0)

**Date** : 2026-05-17
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Source signal terrain** : démo Renversez 13/05 — l'avocate attendait, sur le dossier Dupont, le signalement d'un vice de procédure côté employeur (argument qui fait gagner un dossier indépendamment du fond). Trou confirmé par audit gap 2026-05-14.

---

## Verdict global

**GO avec ajustements** — un ajustement de frontière avec F-DT-08 à intégrer dans la mini-spec.

---

## Intention métier (1 phrase)

Fournir à l'avocat un outil décisionnel qui analyse les **vices de forme de la procédure de licenciement côté employeur** (convocation, entretien, délais, lettre, CSE, convention collective) et rend un verdict de nullité — un moyen « technique » qui peut faire gagner un dossier indépendamment du fond.

---

## Workflow métier réel — position de F-DT-36

Workflow repris des cadrages F-243 / F-98 (source : pratique avocat contentieux droit du travail).

```
... → 3. Analyse IA du dossier → synthèse
    → 4. Examen des pièces
    → 5. Identification des moyens en droit (OUTILS DÉCISIONNELS)   ⬅ F-DT-36
    → 6. Calcul des indemnités
    → 7. Pistes stratégiques
    → 8. Stade procédural (F-243)
    → 9. Conclusions (F-98)
```

F-DT-36 est un **outil décisionnel de plus** à l'étape 5, dans la famille « licenciement », aux côtés de F-DT-08 et F-DT-16.

---

## Cartographie features ↔ workflow (challenge amont / aval)

| Brique | Statut | Analyse |
|---|---|---|
| Pipeline IA chunk→doc→dossier | ✅ Livré | Extrait les données du licenciement depuis les pièces — base du pré-fill IA |
| Examen / numérotation des pièces (F-145/146) | ✅ Livré | F-DT-36 cite les pièces (convocation, lettre…) |
| F-DT-08 validité licenciement | ✅ Livré | **Outil voisin — frontière à clarifier (cf. ajustement)** |
| F-DT-16 licenciement nul (protections salarié) | ✅ Livré | Outil voisin — pas de chevauchement (F-DT-16 = côté salarié, F-DT-36 = vices côté employeur) |
| Panel outils décisionnels (F-IA-04) | ✅ Livré | F-DT-36 = une section de plus, visibilité contextuelle |
| Pré-fill IA + F-IA-03 (cohérence) | ✅ Livré | Pattern standard outil décisionnel |
| **Consommateurs aval** : synthèse décisionnelle, pistes stratégiques (F-176), conclusions (F-98) | ✅ / 🟡 | Le verdict de nullité alimente le dashboard et, à terme, les conclusions |

**Challenge amont** : aucun trou. Toutes les briques d'un outil décisionnel existent (pipeline IA, pièces, pattern Calculator + section, panel).

**Challenge aval** : le verdict de F-DT-36 sera repris dans la synthèse décisionnelle et les pistes stratégiques (déjà livrées), et plus tard dans les conclusions (F-98). Pas de dead-end.

---

## Ajustement requis — frontière F-DT-08 / F-DT-36

F-DT-08 (validité du licenciement) contient déjà des critères de **forme** (convocation, entretien, délai de notification). F-DT-36 analyse les **vices de procédure** — il y a un **chevauchement partiel**.

Décision de cadrage (à respecter en mini-spec) :
- **F-DT-08 reste l'outil du « le motif tient-il ? »** — validité au fond + vérification de forme sommaire.
- **F-DT-36 est l'outil dédié et approfondi des vices de procédure côté employeur** — les 10 critères détaillés, c'est lui la référence.
- Conformément au modèle produit (outils décisionnels = simulateurs indépendants, cf. mémoire `feedback_decision_tools_are_simulators`), les deux outils coexistent sans fusion ni override. Mais la mini-spec F-DT-36 doit **expliciter cette frontière** pour éviter que l'avocat reçoive deux verdicts contradictoires sur la convocation/l'entretien. Pas de divergence non documentée sur les critères de forme communs.

---

## Invariants anti-gadget pour la mini-spec

1. **Pré-fill IA obligatoire** : les champs analysables depuis les pièces (date de convocation, date d'entretien, date de notification, présence d'une lettre, motif invoqué…) sont pré-remplis depuis l'analyse IA. L'avocat ne ressaisit pas ce que l'IA a extrait.
2. **Citation des pièces** : chaque vice détecté renvoie à la pièce concernée (la convocation, la lettre de licenciement…).
3. **Frontière F-DT-08 explicite** (cf. ajustement ci-dessus).
4. **Verdict actionnable** : `NULLITE_AVEREE` / `NULLITE_PROBABLE` / `PROCEDURE_REGULIERE`, avec pour chaque vice le fondement légal (article du Code du travail) et ce qui manque pour conclure.
5. **Périmètre FR V1** : la nullité de procédure belge a un mécanisme distinct — outil jumeau BE à ouvrir au backlog post-livraison FR si signal terrain (ne pas le bâcler en miroir).

---

## Décision finale

**GO avec ajustements.** F-DT-36 démarre. Outil décisionnel de la famille licenciement, périmètre FR V1, 10 critères de vice. Ajustement obligatoire en mini-spec : clarifier la frontière avec F-DT-08. Étape suivante : cadrage écran 0 bis, puis mini-specs.

---

## Liens
- `docs/PRODUCT_SPEC.md` — F-DT-36 (10 vices détaillés), F-DT-08, F-DT-16
- `docs/features/F-98/SF-98-00-coherence.md` — F-DT-36 identifiée comme pré-requis qualité du volet conclusions DT
- `ai-skills/feature-coherence-challenger.md` — skill appliquée

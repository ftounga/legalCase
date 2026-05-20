# F-DT-38 — Rupture de période d'essai (qualification régulière / abusive / nulle) — Document de cadrage cohérence (étape 0)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Source signal terrain** : 2ᵉ démo Marjolaine RENVERSEZ (18/05/2026, 17h30, ~30 min, visio). En fin de démo, l'avocate a sollicité explicitement un outil décisionnel qualifiant la **rupture d'un contrat pendant la période d'essai** comme régulière ou abusive. **Mail détaillé reçu le 19/05/2026 à 18:38** (`avocat@renversez.com`) — scope figé. Voir aussi `memory/project_renversez_post_demo_13_05.md` addendum 19/05.

---

## Verdict global

**GO sans ajustements** — la feature s'insère sans frottement dans le pattern outil décisionnel licenciement, situation métier juridiquement distincte des outils voisins, périmètre FR V1 strict.

---

## Intention métier (1 phrase)

Fournir à l'avocat un outil décisionnel qui qualifie une **rupture pendant la période d'essai** comme `REGULIERE`, `RISQUE_ABUSIVE`, `NULLE` ou `ILLEGALE_REQUALIF_LICENCIEMENT`, à partir des éléments factuels du dossier (durée d'essai, ancienneté, délai de prévenance, motif, état de santé, conventions collectives) et de la jurisprudence Cass. soc. + protections L.1132-1 / L.1226-9 / L.1225-1.

---

## Workflow métier réel — position de F-DT-38

Workflow repris des cadrages F-DT-36 / F-243 / F-98 (source : pratique avocat contentieux droit du travail).

```
... → 3. Analyse IA du dossier → synthèse
    → 4. Examen des pièces
    → 5. Identification des moyens en droit (OUTILS DÉCISIONNELS)   ⬅ F-DT-38
    → 6. Calcul des indemnités
    → 7. Pistes stratégiques
    → 8. Stade procédural (F-243)
    → 9. Conclusions (F-98)
```

F-DT-38 est un **outil décisionnel de plus** à l'étape 5, dans la famille « rupture du contrat de travail », à côté de F-DT-08 / F-DT-10 / F-DT-16 / F-DT-22 / F-DT-36 — mais sur une situation métier strictement distincte : **la rupture pendant la période d'essai**.

---

## Cartographie features ↔ workflow (challenge amont / aval)

| Brique | Statut | Analyse |
|---|---|---|
| Pipeline IA chunk→doc→dossier | ✅ Livré | Extrait les données du contrat et de la rupture depuis les pièces — base du pré-fill IA |
| Examen / numérotation des pièces (F-145/146) | ✅ Livré | F-DT-38 cite les pièces (contrat, lettre de rupture, certificats…) |
| F-DT-08 validité licenciement | ✅ Livré | Outil voisin — situation **post-essai** (entretien préalable, motivation écrite obligatoire) — overlap < 20 % |
| F-DT-10 rupture conventionnelle | ✅ Livré | Outil voisin — mode amiable, **pas d'application en période d'essai** — overlap nul |
| F-DT-16 licenciement nul (protections salarié) | ✅ Livré | Outil voisin — F-DT-38 invoque les régimes spéciaux (grossesse, AT/MP, discrimination) dans le contexte spécifique de l'essai — overlap conceptuel < 25 % sans recalcul d'indemnité licenciement nul classique |
| F-DT-22 requalification CDD → CDI | ✅ Livré | Outil voisin — situation hors période d'essai — overlap nul |
| F-DT-36 vices de procédure | ✅ Livré | Outil voisin — procédure licenciement post-essai (entretien préalable, lettre motivée), **pas applicable** à la rupture d'essai qui n'exige pas ces formalités — overlap nul |
| Panel outils décisionnels (F-IA-04) | ✅ Livré | F-DT-38 = une section de plus, visibilité contextuelle |
| Pré-fill IA + F-IA-03 (cohérence) | ✅ Livré | Pattern standard outil décisionnel |
| **Consommateurs aval** : synthèse décisionnelle, pistes stratégiques (F-176), conclusions (F-98) | ✅ / 🟡 | Le verdict de qualification alimente le dashboard et, à terme, les conclusions |

**Challenge amont** : aucun trou. Toutes les briques d'un outil décisionnel existent (pipeline IA, pièces, pattern Calculator + section, panel, F-IA-04 contextuel).

**Challenge aval** : le verdict alimente la synthèse décisionnelle et les pistes stratégiques (déjà livrées), et plus tard les conclusions (F-98). Pas de dead-end.

---

## Analyse d'overlap (règle CLAUDE.md « un outil décisionnel = une situation métier »)

| Outil voisin | Situation métier | Overlap | Justification |
|---|---|---|---|
| **F-DT-08** validité licenciement | Rupture après période d'essai, formalités complètes (entretien préalable L.1232-2, motivation écrite L.1232-6) | < 20 % | Régime juridique distinct L.1221-19 et s. (essai) vs L.1232-2 et s. (post-essai) — pas d'entretien préalable obligatoire, pas de motivation écrite obligatoire en essai |
| **F-DT-10** rupture conventionnelle | Mode amiable post-essai (art. L.1237-11 et s.) | 0 % | Pas d'application en période d'essai |
| **F-DT-16** licenciement nul (7 protections) | Licenciement caractérisé + cause de nullité (discrimination, AT/MP, grossesse, syndicat...) | < 25 % conceptuel | F-DT-38 invoque ces régimes spéciaux dans le contexte de l'essai (rupture nulle = discrimination, AT/MP, grossesse) mais SANS recalcul d'indemnité licenciement nul plancher 6 mois (Marjolaine 19/05 : « L'indemnité de nullité de 6 mois n'est pas applicable mais en demandant la réintégration le salarié obtient bien plus »). Le verdict `NULLE` de F-DT-38 met en avant l'option **réintégration** + rappel salaires entre rupture et réintégration, pas le plancher L.1235-3-1 |
| **F-DT-22** requalification CDD → CDI | Cas hors essai (motifs de recours, durée maximale, succession) | 0 % | Situation indépendante |
| **F-DT-36** vices de procédure | Vices de la procédure de licenciement (entretien préalable, lettre, délais) | 0 % | Procédure non applicable à la rupture d'essai (pas de convocation, pas de lettre motivée obligatoire) |

**Verdict overlap** : F-DT-38 est une **situation métier distincte au sens règle CLAUDE.md**. AUCUN outil existant ne qualifie une rupture pendant la période d'essai. Le régime juridique L.1221-19 à L.1221-25 est juridiquement autonome.

---

## Précisions juridiques apportées par Marjolaine (mail 19/05/2026 18:38) — règles à respecter en mini-spec

### Régime nominal (rupture régulière)
- Pendant la période d'essai, **les deux parties peuvent rompre sans motif** (principe de liberté de rupture).
- **Délai de prévenance employeur (Art. L.1221-25)** — échelle progressive :
  - **24 h** si présence < 8 jours
  - **48 h** si présence ≥ 8 jours et < 1 mois
  - **2 semaines** si présence ≥ 1 mois et < 3 mois
  - **1 mois** si présence ≥ 3 mois
- **Délai de prévenance salarié (Art. L.1221-25)** : **48 h** (ou 24 h si présence < 8 jours)
- → Verdict `REGULIERE` si tous les critères respectés.

### Abus (jurisprudence — ouvre droit à dommages et intérêts)
La rupture peut **dégénérer en abus** quand :
- Motif **sans rapport avec les qualités professionnelles** du salarié
- **Volonté de nuire** caractérisée
- **Légèreté blâmable** (rupture précipitée, sans évaluation effective)
- Motifs **étrangers à l'essai** (économique déguisé, organisationnel, etc.)

Indemnité indicative donnée par Marjolaine : **~1 mois de salaire** (CPH Montpellier avant barème Macron 2018) — fourchette à figer dans le calculator (entre 1 et 6 mois selon préjudice et jurisprudence locale).
→ Verdict `RISQUE_ABUSIVE`

### Nullité (discrimination ou atteinte liberté fondamentale)
La rupture est **nulle** si elle résulte :
- D'une **discrimination** au sens L.1132-1 : grossesse (L.1225-1), maladie, origine, sexe, syndicat, etc.
- D'un **accident du travail / maladie professionnelle** (L.1226-9)
- D'une **atteinte à une liberté fondamentale**

⚠️ **Précision importante de Marjolaine** : « L'indemnité de nullité de 6 mois prévue pour les licenciements **n'est pas applicable** mais en demandant la réintégration le salarié obtient bien plus ! »

→ Le calculator doit afficher :
- Verdict `NULLE`
- Option **réintégration** mise en avant comme remède principal (avec rappel salaires entre la rupture et la réintégration)
- Indemnité subsidiaire = dommages et intérêts (pas de plancher 6 mois automatique)

### Illégalité (durée ou renouvellement irrégulier)
La rupture est **illégale** (s'analyse comme un **licenciement sans cause réelle et sérieuse**) si :
- **Durée de la période d'essai > durée légale autorisée** Art. L.1221-19 (2 mois ouvriers/employés, 3 mois agents de maîtrise/techniciens, 4 mois cadres en CDI ; durée différente CDD selon Art. L.1242-10)
- **Procédure de renouvellement non respectée** (Art. L.1221-23 : accord de branche le prévoyant + accord exprès écrit du salarié, jamais tacite, dans la durée initiale)

→ Verdict `ILLEGALE_REQUALIF_LICENCIEMENT` (= barème Macron L.1235-3 applicable, sauf si lettre de rupture motivée avec motifs avérés)

Marjolaine précise : « **à moins qu'il existe une lettre de rupture motivée avec des motifs avérés** » → critère à inclure dans le calculator (présence d'une lettre motivée + motifs avérés = atténuation possible).

---

## Invariants anti-gadget pour la mini-spec

1. **Verdict 4 niveaux** : `REGULIERE` / `RISQUE_ABUSIVE` / `NULLE` / `ILLEGALE_REQUALIF_LICENCIEMENT` — pas de fusion ni de simplification arbitraire.
2. **Échelle de prévenance L.1221-25 codée par catégorie d'ancienneté** : 24h / 48h / 2 sem. / 1 mois (employeur) — pas de seuil unique simpliste.
3. **Durée légale par catégorie socio-professionnelle (L.1221-19)** : ouvrier/employé (2 mois), agent de maîtrise/technicien (3 mois), cadre (4 mois) — pour CDI ; CDD selon L.1242-10.
4. **Renouvellement régulier** : accord de branche + accord exprès écrit du salarié, jamais tacite — règle stricte L.1221-23.
5. **Régimes spéciaux de nullité** : L.1132-1 (discrimination), L.1225-1 (grossesse), L.1226-9 (AT/MP), libertés fondamentales — TOUS opposables en période d'essai.
6. **Option réintégration** : verdict `NULLE` propose **réintégration + rappel salaires** comme remède principal (précision Marjolaine 19/05) — pas le plancher 6 mois.
7. **Atténuation `ILLEGALE_REQUALIF_LICENCIEMENT`** : présence d'une lettre motivée + motifs avérés = critère pris en compte (précision Marjolaine 19/05).
8. **Pré-fill IA exhaustif** au sens invariant F-246 (`feedback_decision_tools_all_fields_prefilled`) : TOUS les champs saisissables doivent être pré-remplis par l'IA si l'information est présente dans les documents.
9. **Citation des pièces** : chaque verdict cite les pièces (contrat, lettre de rupture, certificats AT/MP, certificat grossesse, etc.).
10. **Périmètre FR V1 strict** : la rupture d'essai BE relève d'un régime distinct (phase initiale du contrat post-statut unique 2014) — outil jumeau BE = F-DT-39 backlog post-livraison FR uniquement si signal terrain BE.

---

## Décision finale

**GO sans ajustements.** F-DT-38 démarre. Outil décisionnel de la famille « rupture du contrat de travail », périmètre FR V1, **situation métier distincte au sens règle CLAUDE.md** (régime juridique L.1221-19 à L.1221-25 autonome, aucun outil existant ne couvre la rupture pendant la période d'essai). Pré-fill IA exhaustif obligatoire (F-246). Étape suivante : cadrage écran 0 bis, puis mini-specs SF-DT-38-01 (backend) + SF-DT-38-02 (frontend) parallélisables sur contrat API figé.

---

## Liens
- `docs/PRODUCT_SPEC.md` — F-DT-38 (12 critères pressentis détaillés ligne 190)
- `docs/features/F-DT-36/SF-DT-36-00-coherence.md` — pattern outil décisionnel licenciement
- `memory/project_renversez_post_demo_13_05.md` — signal terrain démo 13/05 + 18/05 + mail 19/05
- `memory/feedback_decision_tools_one_per_situation.md` — règle un outil = une situation métier
- `memory/feedback_decision_tools_all_fields_prefilled.md` — invariant pré-fill exhaustif
- `ai-skills/feature-coherence-challenger.md` — skill appliquée

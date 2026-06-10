# F-261 — Cadrage cohérence (étape 0)

> Feature : **Conclusions en réponse** — ingérer les écritures adverses et réfuter **moyen par moyen**.
> Programme « Conclusions V2 », levier fonctionnel n°1. Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements** (décision PO sur le sourcing des moyens adverses + identification du document adverse)

---

## Intention métier (1 phrase)

Quand l'avocat est **en réponse** (l'adversaire a déposé ses écritures), LegalCase ingère les **écritures adverses** et génère des conclusions qui **réfutent chaque moyen adverse** (thèse → notre réponse + fondement + jurisprudence), au lieu de produire un acte unilatéral « en demande ».

---

## Workflow métier réel de l'avocat (contentieux écrit, posture « en réponse »)

> Source : pratique standard (procédure écrite, échange de conclusions) + acte réel DURAND validé 2026-06-10.

1. L'avocat reçoit / dépose ses premières conclusions **en demande**.
2. L'**adversaire** dépose ses conclusions, articulant **ses moyens** (thèses + fondements + pièces).
3. L'avocat **lit les écritures adverses** (souvent 30-40 pages) et en **extrait les moyens** un par un.
4. Pour chaque moyen adverse, il **construit sa réponse** : le moyen est mal fondé / les faits le contredisent / la jurisprudence invoquée est inopérante.
5. Il rédige des **conclusions en réponse / récapitulatives** structurées « moyen adverse → réfutation ».
6. Dépôt. C'est l'étape la plus **chronophage** du contentieux écrit.

**F-261 couvre les étapes 3-5, et c'est là que se trouve le plus gros gain de temps avocat.**

---

## Cartographie features actuelles ↔ workflow

| Étape workflow | Feature LegalCase | Statut |
|---|---|---|
| 1. Conclusions en demande | F-98 (45 cellules) | ✅ Livrée |
| 2. Dépôt adverse (hors app) | écritures adverses **uploadées comme document** (F-43) | ✅ upload OK |
| 3a. **Identifier le document = écritures adverses** | — (SF-98-56 marque les *citations*, pas le *document*) | ❌ **Manquant** (= « Option B » différée de SF-98-56) |
| 3b. **Extraire les moyens adverses** (thèse + fondements + pièces) | — (synthèse unilatérale ; aucune extraction des arguments adverses) | ❌ **Manquant** |
| 4. Réfutation par moyen | les cellules défendeur/intimé de F-98 **instruisent déjà** « réfuter moyen par moyen » dans leur prompt système | ✅ consigne présente, **mais sans intrant structuré** |
| 4bis. Réfuter les **citations** adverses douteuses | F-179 + SF-98-56 (section « JURISPRUDENCE ADVERSE À RÉFUTER ») | ✅ Livrée |
| 5. Conclusions en réponse | F-98 génération | ✅ moteur présent |
| (posture) en demande vs en réponse | F-243 position (DEMANDEUR/DEFENDEUR/APPELANT/INTIME) | ✅ suffit |

---

## Position de la nouvelle feature

F-261 s'insère aux **étapes 3-4** : une nouvelle section **« MOYENS ADVERSES À RÉFUTER »** dans le prompt des conclusions, alimentée par l'**extraction structurée des moyens** du document marqué « écritures adverses ». Les cellules de F-98 réfutent déjà « moyen par moyen » — il leur manque l'**intrant** : la liste structurée des moyens adverses réels.

Couture technique (reconnaissance) :
- `ConclusionPromptInput` : ajouter `List<AdverseMoyenToRefute>` (thèse, fondements, pièces invoquées) — symétrique de `adverseToRefute` (citations, SF-98-56).
- `CaseConclusionPromptBuilder` : méthode `appendAdverseMoyensToRefute` + section « MOYENS ADVERSES À RÉFUTER » (avant la jurisprudence adverse).
- Source des moyens : nouvelle **extraction IA ciblée** sur le document « écritures adverses ».

---

## Challenge amont

**Question** : chaque étape avant la feature est-elle couverte ?

- Étapes 1, 2, 4 (consigne), 5, posture : ✅ couvertes.
- **Étape 3a — identification du document adverse : ❌ trou.** Aucun champ de camp/rôle sur `Document`. SF-98-56 a délibérément marqué les *citations* (Option A) et a **différé au backlog le tag de camp du document (Option B)**. F-261 **a besoin** de cette Option B : savoir quel document est les écritures adverses pour en extraire les moyens. *(Distinction sûre ici : l'avocat sait sans ambiguïté quel document est l'acte adverse — pas le risque de contresens de SF-98-56.)*
- **Étape 3b — extraction des moyens adverses : ❌ trou.** L'extraction IA (`LegalDomainPromptBuilder`, `*ExtractedData`) et la synthèse sont **unilatérales** (faits/points juridiques/risques vus du client). Aucun `adverse_extracted_data`, aucune notion de « thèse adverse / fondements invoqués ».

→ Deux briques amont à ajouter (décomposition de F-261 en SF).

## Challenge aval

**Question** : la sortie est-elle exploitable ?

- ✅ **Oui, directement** : les cellules défendeur/intimé de F-98 portent déjà la consigne « réfuter moyen par moyen » ; leur fournir les moyens structurés active immédiatement une réfutation **ancrée sur les arguments réels** de l'adversaire (vs générique). L'acte produit est relu/édité/exporté/versionné par l'existant. Cohérence avec SF-98-56 (citations) : les deux sections « adverse » se complètent (moyens + citations).

---

## Décomposition proposée (SF de F-261)

1. **SF-261-01 — Tag « écritures adverses » au niveau document** (= Option B de SF-98-56) : marquer un document uploadé comme `ECRITURES_ADVERSES` (vs client/neutre). Pré-requis de l'extraction.
2. **SF-261-02 — Extraction structurée des moyens adverses** : extraction IA ciblée sur le(s) document(s) marqué(s) adverse → liste `{thèse, fondements (articles), pièces invoquées}`. **Framework + déclinaison par domaine** (travail → immigration → famille).
3. **SF-261-03 — Injection « MOYENS ADVERSES À RÉFUTER » + réfutation** : nouvel intrant `ConclusionPromptInput` + section builder + consigne de réfutation moyen par moyen ancrée sur les moyens extraits (non-régression SF-98-55/56).

---

## STOPs / pré-requis à ajouter au backlog

- **SF-261-01 (tag document adverse)** et **SF-261-02 (extraction moyens)** sont des **pré-requis bloquants** de SF-261-03. À séquencer 01 → 02 → 03.
- L'extraction (02) est **domaine-dépendante** (les moyens travail ≠ immigration ≠ famille) → framework + sous-vagues par domaine, comme F-98.

## Décision PO requise — sourcing des moyens adverses

| Option | Principe | Fiabilité | Effort | Avis |
|---|---|---|---|---|
| **A — Tag document + extraction IA ciblée** | L'avocat marque le doc « écritures adverses » → extraction IA isolée sur ce doc → moyens structurés | Élevée (IA focalisée, périmètre net) | Moyen | ✅ **Recommandée** (s'aligne sur l'esprit SF-98-56, périmètre sûr) |
| **B — Dérivation depuis la synthèse unilatérale** | Déduire les moyens adverses des `risques`/`points_juridiques` existants | Moyenne (ambigu, risque de réfuter sa propre thèse) | Léger | ⚠️ Fallback seulement |
| **C — Saisie avocat des moyens** | Formulaire « thèse adverse / fondements » | Parfaite (humain) | Lourd (UX) | ❌ Phase 2+ |

---

## Invariants anti-gadget pour la mini-spec

1. **Réfuter les moyens réels, pas génériques** : la réfutation s'appuie sur les moyens **extraits du document adverse**, pas sur une thèse adverse devinée.
2. **Origine adverse établie** : seuls les moyens issus d'un document **marqué « écritures adverses »** alimentent la section — jamais déduits à l'aveugle (cohérent avec l'invariant anti-contresens de SF-98-56).
3. **Pas de moyen inventé** : l'extraction ne fabrique pas de moyen absent du document adverse ; à défaut de moyen identifiable, section absente.
4. **Posture cohérente** : la réfutation par moyen est pertinente surtout en posture défendeur/intimé/en réponse ; pilotée par la présence effective de moyens adverses extraits (pas forcée).
5. **Non-régression SF-98-55/56** : pas de jargon ; la section « moyens adverses » se combine avec « jurisprudence adverse à réfuter » sans doublon ni contradiction.
6. **3 domaines** : le mécanisme d'injection (SF-261-03) est commun ; l'extraction (SF-261-02) se décline par domaine.

## Décision finale

**GO avec ajustements.** La posture et le moteur de réfutation existent (cellules F-98 défendeur/intimé) ; les deux trous amont — **identification du document adverse** et **extraction structurée des moyens** — sont à combler en pré-requis (SF-261-01 puis -02), avant l'injection (SF-261-03).

### ✅ DÉCISION PO (2026-06-10) : **Option A — tag document « écritures adverses » + extraction IA ciblée.**
Séquence : **SF-261-01** (tag du document comme écritures adverses) → **SF-261-02** (extraction IA structurée des moyens, framework + vagues par domaine) → **SF-261-03** (injection « MOYENS ADVERSES À RÉFUTER » + réfutation). Étape 0 bis (cohérence écran) requise pour SF-261-01 (marquage dans la table des documents). F-261 passe de `Backlog` à `À faire`.

# F-262 — Cadrage cohérence (étape 0)

> Feature : **Filet de complétude des chefs de demande** — signaler les chefs applicables non plaidés.
> Programme « Conclusions V2 », levier fonctionnel n°2. Skill : `ai-skills/feature-coherence-challenger.md`. 2026-06-10.

## Verdict : **GO avec ajustements** — ⚠️ **chevauchement majeur avec F-258 à trancher (décision PO)**

---

## Intention métier (1 phrase)

À partir des faits, lister les **chefs de demande** mobilisables pour la situation et **signaler à l'avocat ceux qui sont applicables mais non plaidés** dans son acte, pour qu'il n'en oublie aucun.

---

## Constat central — chevauchement avec F-258

L'investigation révèle qu'il **n'existe pas de catalogue de chefs de demande**, mais que les ~60 **outils décisionnels** (F-DT/IM/FA) sont un proxy : *outils **applicables** (`DecisionToolVisibilityService`) − outils **calculés** (`CaseFileDashboardService`) = chefs potentiels manquants*.

**Or c'est EXACTEMENT ce que fait déjà F-258** : encart non bloquant dans `conclusions-section` « N outil(s) décisionnel(s) pertinent(s) ne sont pas encore calculés… ». → L'« Option A » (dérivation depuis les outils) de F-262 est un **doublon de F-258 (> 30 % d'overlap)**.

**La valeur différenciante de F-262 est donc ailleurs** : les chefs de demande qui **ne sont PAS représentés par un outil** :
- chefs transverses du dispositif : **article 700 CPC, dépens, intérêts au taux légal + capitalisation, exécution provisoire** (générés par l'IA, sans outil) ;
- chefs métier sans calculateur dédié : **dommages-intérêts pour préjudice moral / circonstances vexatoires**, rappels divers, etc. ;
- la complétude du **dispositif** (ce qui est *plaidé*), pas seulement le calcul des *outils*.

---

## Workflow métier réel de l'avocat

> Source : pratique standard + acte réel DURAND (validé 10/06).

1. L'avocat analyse les faits.
2. Il identifie **tous les chefs de demande** mobilisables (indemnités, rappels, dommages-intérêts, art. 700, dépens, intérêts…).
3. Il **rédige son dispositif** en n'oubliant aucun chef applicable.
4. Hantise : **oublier un chef** (poste indemnitaire) → perte sèche pour le client.

**F-262 couvre l'étape 2-3 : le filet anti-oubli.**

## Cartographie features actuelles ↔ workflow

| Étape | Feature LegalCase | Statut |
|---|---|---|
| 1. Analyse des faits | F-3/4/5 | ✅ |
| 2a. Chefs **calculables via outil** détectés applicables | F-IA-04 (`DecisionToolVisibilityService`) | ✅ |
| 2b. Alerte « outils applicables non calculés » | **F-258** (encart `conclusions-section`) | ✅ **déjà livré** |
| 2c. **Chefs applicables SANS outil** (art. 700, dépens, intérêts, préjudice moral, chefs métier non outillés) | — aucun catalogue, aucune détection | ❌ **trou réel = la vraie valeur F-262** |
| 3. Dispositif | F-98 (dispositif complet SF-98-55 : art. 700/intérêts/capitalisation/astreinte) | ✅ partiel (l'IA les produit, mais pas de *filet* qui garantit/signale) |

## Position de la nouvelle feature

F-262 ne doit **pas** ré-implémenter le filet « outils non calculés » (= F-258). Sa valeur = un **filet sur les chefs de demande au sens dispositif**, incluant les chefs **non outillés**. Cela suppose un **catalogue de chefs de demande par domaine/situation** + un mécanisme d'applicabilité (qui peut réutiliser les flags F-IA-04 pour les chefs outillés, et des règles pour les chefs transverses).

## Challenge amont

- Étapes 1, 2a, 2b : ✅ couvertes (dont F-258 pour les outils).
- **Étape 2c : ❌ trou.** Pas de catalogue de chefs de demande, pas de détection des chefs **non outillés** applicables. C'est le seul périmètre qui distingue F-262 de F-258.

## Challenge aval

- ✅ La sortie (liste de chefs applicables non plaidés) s'exploite dans `conclusions-section` (encart non bloquant, **même pattern que F-258**) et guide l'avocat avant/après génération. Aval clair.

## STOPs / pré-requis

- **Catalogue des chefs de demande** (par domaine) = brique amont à créer pour la valeur différenciante. **Framework + vagues par domaine** (travail → immigration → famille), comme F-98/F-261.
- ⚠️ **Anti-doublon F-258** : F-262 ne doit pas refaire le filet des outils non calculés ; soit il **étend F-258** (réutilise son encart, ajoute les chefs non outillés), soit il s'en distingue clairement.

## Décision PO requise — périmètre de F-262

| Option | Principe | Valeur vs F-258 | Effort | Avis |
|---|---|---|---|---|
| **A — Catalogue de chefs de demande (par domaine)** | Référentiel `HEAD_OF_CLAIM` par domaine (chefs outillés + non outillés : art. 700, dépens, intérêts, préjudice moral…) + détection d'applicabilité + filet « chefs non plaidés » dans `conclusions-section` | **Forte** (couvre les chefs hors outils, vraie complétude du dispositif) | **Lourd** (catalogue + vagues par domaine) | ✅ si on veut le vrai filet |
| **B — Extension légère de F-258** | Garder le filet outils (F-258) + ajouter seulement les **chefs transverses fixes** (art. 700, dépens, intérêts, exécution provisoire) comme checklist du dispositif | Moyenne (comble les oublis transverses fréquents, sans catalogue métier) | Léger | ⚖️ bon rapport valeur/effort |
| **C — Ne rien faire (F-258 suffit)** | Considérer que F-258 (outils) + la garde dispositif SF-98-55 couvrent l'essentiel | — | Nul | ⚠️ si re-priorisation ailleurs |

## Invariants anti-gadget pour la mini-spec

1. **Pas de doublon F-258** : F-262 ne ré-affiche pas « outils non calculés » ; il traite les chefs **au sens dispositif** (incluant non outillés).
2. **Filet, pas blocage** : encart **non bloquant** (comme F-258), la génération reste possible.
3. **Pas d'invention** : un chef n'est signalé « applicable » que sur une base réelle (flag IA, règle, détection de fait) — jamais un chef théorique non fondé.
4. **3 domaines** : le mécanisme d'affichage est uniforme ; le catalogue (si Option A) se décline par domaine.
5. **Cohérence avec SF-98-55** : les chefs transverses (art. 700, intérêts, capitalisation) sont déjà imposés par la garde du dispositif — le filet les *signale* mais ne contredit pas la garde.

## Décision finale

**GO avec ajustements.** L'« Option A naïve » (dérivation pure des outils) est un doublon de F-258 et NE sera PAS implémentée telle quelle.

### ✅ DÉCISION PO (2026-06-10) : **Option A — catalogue complet des chefs de demande par domaine.**
On construit un vrai **référentiel des chefs de demande** (outillés **et** non outillés : art. 700, dépens, intérêts+capitalisation, exécution provisoire, dommages-intérêts préjudice moral/vexatoire, rappels…) avec détection d'applicabilité, et un **filet « chefs applicables non plaidés »** dans `conclusions-section`. **Framework + vagues par domaine** (travail FR d'abord, puis immigration, famille). **Anti-doublon F-258** : F-262 réutilise/complète le pattern d'encart de F-258 mais traite les chefs au sens **dispositif** (incluant les non outillés) — pas un re-affichage des « outils non calculés ».

**Décomposition** : SF-262-01 (référentiel + catalogue travail FR + détection d'applicabilité + endpoint completeness) → SF-262-02 (encart frontend « chefs non plaidés ») → vagues catalogue immigration / famille. Étape 0 bis requise pour SF-262-02 (encart). F-262 : `Backlog` → `À faire`.

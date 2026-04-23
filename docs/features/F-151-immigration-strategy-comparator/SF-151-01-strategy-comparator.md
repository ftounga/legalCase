# Mini-spec — F-151 / SF-151-01 Comparateur stratégies immigration

## Identifiant · `F-151 / SF-151-01`
## Date · `2026-04-23` · Branche · `feat/SF-151-01-strategy-comparator`

## Objectif
Quand plusieurs options juridiques sont ouvertes sur un dossier immigration (ex : changement de statut immédiat vs attendre l'expiration du titre actuel, ou recours gracieux vs contentieux), l'IA produit un **panneau comparatif** avec 2-3 scenarii stratégiques. Chaque scénario expose : conditions, délai moyen observé, risque d'échec, pièces additionnelles.

## Contexte
Niveau 6 de la hiérarchie — équivalent de F-DT-09 (comparateur jurisprudentiel indemnités) pour l'immigration. Livré dans la foulée de F-150 pour compléter le rattrapage de parité. Scope : backend + frontend intégrés dans une SF (feature de complexité modérée).

## Comportement nominal

### A — Extraction IA
Extension du prompt immigration (`LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION`) : l'IA produit un champ `strategy_scenarios` — tableau de 0 à 3 scénarios. Chaque scénario :

```json
{
  "scenario_label": "Changement de statut immédiat",
  "scenario_description": "Déposer maintenant une demande VPF au titre du mariage.",
  "base_legale": "Art. L.423-1 CESEDA",
  "target_title_code": "CST_VPF",
  "target_title_label": "Carte de séjour « Vie privée et familiale »",
  "delay_days_estimate": "90-180",
  "risk_level": "FAIBLE",
  "risk_justification": "Conditions remplies : mariage + vie commune > 6 mois documentée.",
  "required_additional_pieces": ["Justificatif de vie commune", "Acte de mariage < 3 mois"],
  "advantages": ["Droit au travail plein immédiatement", "Titre stable"],
  "drawbacks": ["Perte de la mention Étudiant — Recherche"]
}
```

### B — Enum risk_level
`FAIBLE` | `MOYEN` | `ELEVE` (aligné sur le vocabulaire F-IA-02 tableau décisionnel).

### C — DTO backend
Nouveau record `ImmigrationStrategyScenario` exposé dans `CaseAnalysisResponse.immigrationStrategyScenarios: List<ImmigrationStrategyScenario>`.

### D — Frontend carte comparateur
Composant `ImmigrationStrategyComparatorComponent` affichant les scenarii **côte à côte** dans une grille responsive (2 colonnes ≥ 1024px, 1 colonne en dessous). Par scénario :
- en-tête : label + badge risque (FAIBLE/MOYEN/ÉLEVÉ coloré)
- description + base légale
- bloc "Titre cible" avec code + libellé
- bloc "Délai observé" (délai estimé en jours)
- "Pièces additionnelles" (liste)
- "Avantages" (icône check vert) / "Inconvénients" (icône × gris)

### E — Affichage conditionnel
Le composant n'apparaît que si `immigrationStrategyScenarios.length >= 2` (un seul scénario n'a pas de valeur comparative).

### F — Rétrocompat
Dossiers pré-F-151 : `immigrationStrategyScenarios` absent → liste vide → rien rendu.

## Critères d'acceptation
- [ ] Record `ImmigrationStrategyScenario` (backend)
- [ ] Extension prompt IA immigration
- [ ] Parseur `extractImmigrationStrategyScenarios` fail-open
- [ ] Constructeur rétrocompat CaseAnalysisResponse
- [ ] Frontend : interface TS + composant standalone + intégration dans synthèse
- [ ] Composant affiche 2-3 scenarii en grille responsive
- [ ] Badges risque colorés (vert/orange/rouge)
- [ ] Tests backend parseur + tests frontend composant
- [ ] Full suites vertes

## Plan de test minimal
**Backend :**
- U-01 : parse 2 scenarii complets
- U-02 : tableau vide/absent → liste vide
- U-03 : risk_level inconnu → scénario skippé (ou `null` sur le champ) — choix V1 : skip pour garantir intégrité
- U-04 : liste required_additional_pieces optionnelle

**Frontend :**
- U-05 : 2 scenarii → 2 cards visibles
- U-06 : 0 ou 1 scénario → composant caché
- U-07 : badge risque affiche la bonne couleur selon niveau
- U-08 : listes avantages / inconvénients rendues correctement

## Tables / endpoints / composants impactés
### Backend
- `ImmigrationStrategyScenario.java` (nouveau record)
- `CaseAnalysisResponse.java` (+champ + parser)
- `LegalDomainPromptBuilder.java` (extension prompt)

### Frontend
- `core/models/case-analysis.model.ts` (+interface)
- `case-files/immigration-strategy-comparator-section/` (nouveau composant)
- `case-files/synthesis/synthesis.component.*` (intégration)

### Pas impacté
- Migration DB : aucune (stockée dans `analysis_result` JSON)
- Autres domaines : inchangés

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Immigration** (FR) | Toutes stratégies CESEDA FR | Principal périmètre V1 |
| **Immigration** (BE) | Permis A/B/C, permis unique | V1 : l'IA produit les scenarii en langage libre, pays BE géré par prompt. Scenarii et délais ajustés par Sonnet selon country. Pas de structure spécifique à créer. |
| **Travail** / **Famille** | Non applicable | Le prompt ne demande ce champ qu'en immigration. F-DT-09 couvre le niveau 6 travail, F-153 couvrira famille. |

## Parité des domaines métier
**Niveau 6 — Comparateur / fourchettes** :
- ✅ Travail : F-DT-09 (comparateur jurisprudentiel indemnités)
- 🚧 Immigration : F-151 (cette SF)
- ❌ Famille : F-153 à livrer

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-150 événements déclencheurs | Complémentaire : F-150 détecte les événements, F-151 compare les chemins possibles pour exploiter ces événements | Intégré |
| F-IM-05 arbre décisionnel titre | F-IM-05 oriente vers UN titre, F-151 compare plusieurs options stratégiques qui peuvent inclure le titre de F-IM-05 ET des alternatives | Cohabitation |
| F-IM-06 générateur recours | Potentiel overlap côté recours (gracieux vs contentieux) mais F-IM-06 génère le DOC, F-151 compare les CHEMINS. Pas de conflit | Non applicable |
| F-IA-02 tableau de bord | Le badge risque réutilise la même échelle FAIBLE/MOYEN/ELEVE que le risque global dossier | Intégré (même vocabulaire) |

## Préoccupations transversales
Aucune.

## Hors scope
- Scoring de chaque scénario au-delà du `risk_level` qualitatif
- Persistance des choix avocat (quel scénario il retient)
- Export PDF des scenarii — à évaluer après feedback terrain

# SF-274-00 — Cadrage cohérence : traitement des pièces adverses (communication / rejet art. 135 CPC)

> Skill : `ai-skills/feature-coherence-challenger.md` — Étape 0 de la séquence CLAUDE.md.
> Feature : **F-274** (Conclusions V4 ④, audit conclusions 2026-06-12, manque métier #4).

## 1. Workflow métier réel de l'avocat cible

Conclusions « en réponse » en procédure civile française. Le principe du **contradictoire** (art. 15 et 16 CPC) impose que chaque partie communique en temps utile les pièces dont elle entend se servir. Réflexes systématiques de l'avocat défendeur (ou de l'avocat en réplique) face aux écritures/pièces adverses :

1. **Demande de communication** des pièces que l'adversaire vise mais n'a pas communiquées (art. 132 CPC : « la partie qui fait état d'une pièce s'oblige à la communiquer ») ;
2. **Rejet des débats** des pièces communiquées **tardivement**, en violation du contradictoire (art. 135 CPC : « le juge peut écarter du débat les pièces qui n'ont pas été communiquées en temps utile »).

Ces deux réflexes se traduisent par des **prétentions de procédure** au dispositif (« ORDONNER la communication de la pièce X », « ÉCARTER des débats les pièces communiquées le … ») et un développement dans la **discussion**.

## 2. Cartographie des features existantes sur ce workflow

| Étape workflow | Feature produit | État |
|---|---|---|
| Ingestion des écritures adverses | **F-261** (SF-261-01) — tag document « écritures adverses » (`adverse_pleadings`) | ✅ livré |
| Extraction des moyens adverses | **F-261** (SF-261-02) — section « MOYENS ADVERSES À RÉFUTER » | ✅ livré (3 domaines FR) |
| Citations adverses douteuses | **SF-98-56 / F-179** — section « JURISPRUDENCE ADVERSE À RÉFUTER » | ✅ livré |
| Ossature procédurale du défendeur (in limine litis) | **F-272** (PROCEDURE_ORDER_GUARD) — exceptions → FNR → fond | ✅ livré (FR) |
| Garde de qualité rédactionnelle transverse | **F-98 / SF-98-55** — REDACTION_QUALITY_GUARD | ✅ livré |
| **Réflexe contradictoire sur les pièces (art. 132/135)** | **F-274 (cette feature)** | ❌ manquant |

**Pré-requis amont — présent ?** OUI. Le déclencheur (« présence de pièces/écritures adverses ») est déjà matérialisé dans le prompt par les sections `MOYENS ADVERSES À RÉFUTER` et `JURISPRUDENCE ADVERSE À RÉFUTER`, alimentées par les documents tagués `adverse_pleadings` (F-261). La liste des **pièces numérotées du dossier** (`PIÈCES NUMÉROTÉES`) est aussi déjà au prompt.

**Sortie exploitable en aval — oui ?** OUI. La sortie est du texte de conclusions (markdown) : prétentions de procédure au dispositif + paragraphe de discussion, consommé identiquement par l'éditeur (F-264), l'export (F-266/F-281) et les versions. Aucun nouvel artefact aval.

## 3. Challenge de cohérence

- **Doublon / gadget ?** NON. Aucune garde existante ne couvre les art. 132/135 CPC. F-272 (PROCEDURE_ORDER_GUARD) traite l'**ordre** des moyens (exceptions/FNR/fond) mais pas la **demande de communication** ni le **rejet des pièces tardives** — ce sont des prétentions de procédure distinctes, non une exception de procédure au sens art. 73 CPC. Le point 8 de REDACTION_QUALITY_GUARD réfute les **moyens** adverses, pas le **régime de communication** des pièces.
- **Invariant « un outil = une situation » respecté ?** OUI — F-274 n'est PAS un outil décisionnel : c'est une **garde de prompt** (comme F-242 / SF-98-55 / F-272 / F-273). Aucun calculateur créé, aucun `decision_tool_visibility_rules`.
- **Risque d'invention ?** Maîtrisé : la garde est **auto-conditionnée** (ne s'applique que si des pièces/écritures adverses sont au dossier) et **anti-invention** (ne demander la communication que de pièces réellement visées-non-communiquées ; ne demander le rejet que si une tardiveté ressort des éléments du dossier ; à défaut, aucune rubrique vide). Symétrie stricte avec le point 5 de PROCEDURE_ORDER_GUARD (« signalement si non applicable »).
- **Portée.** « Uniforme FR » (PRODUCT_SPEC). Les art. 132/135 CPC sont propres à la procédure civile **française** ; le framework F-261 renvoie vide en BE (pas de section adverse) → no-op naturel BE. La garde est gatée FR via `CombinationKey.country()`, comme PROCEDURE_ORDER_GUARD. Transverse aux 3 domaines FR (le régime de communication des pièces ne dépend pas du domaine).

## 4. Verdict

**GO avec ajustements.**

Ajustements retenus :
1. Implémenter en **garde de prompt** (`ADVERSE_PIECES_GUARD`), pas en outil — invariant préservé.
2. **FR uniquement** (gate `country == FRANCE` via `CombinationKey`, miroir de `appliesProcedureOrderGuard`).
3. **Auto-conditionnement dans le texte** sur la présence d'écritures/pièces adverses (sections déjà fournies par F-261) → no-op si aucune pièce adverse, **aucune rubrique vide**, **aucune invention** de pièce ou de tardiveté.
4. **Anti-jargon** (non-régression SF-98-55) : viser les articles (132, 135 CPC), jamais un libellé interne ni un nom de fichier.
5. **Non-régression** : F-272 (ordre in limine litis), point 8 (réfutation des moyens), F-273 (sauf à parfaire) inchangés.

## 5. Invariants anti-gadget pour la mini-spec

- I1 — Garde de prompt transverse construite **une fois**, jamais dupliquée provider par provider.
- I2 — **FR only** ; demandeur **comme** défendeur (les deux peuvent répliquer sur la communication des pièces), mais la garde ne s'applique qu'en présence d'écritures/pièces adverses.
- I3 — **No-op silencieux** sans pièce adverse : pas de section vide, pas de « néant ».
- I4 — **Zéro invention** : ne demander la communication / le rejet que sur la base d'éléments réels du dossier.
- I5 — Aucune table, aucun endpoint, aucun écran, aucune migration. Backend-only.
- I6 — Pas d'impact écran ⇒ **étape 0 bis non requise** (cohérent avec F-272 / F-273).

## 6. Impact PRODUCT_SPEC

Verdict GO ⇒ statut F-274 passe **À faire → en cours de livraison** (mise à jour docs groupée par l'orchestrateur en fin de vague).

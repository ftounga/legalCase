# F-213 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

10 nouveaux outils décisionnels Travail BE-only **priorité P2** s'ajoutent au **panneau outils décisionnels** (onglet Décision du détail dossier) — visibles **uniquement pour les workspaces `country=BELGIQUE` / `legal_domain=DROIT_DU_TRAVAIL`**, masqués pour les workspaces FR. Chacun ouvre une `*-section.component` (formulaire + verdict), pré-remplie par l'IA, suivant le pattern canonique `immigration-title-decision-section`.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — toutes les briques d'infrastructure sont matures (F-207 terminée). Panneau, gate `workspaceCountry`, pré-remplissage `TravailExtractedData`, validation F-IA-03, `critereCode BE_*`, `TOOL_REGISTRY`, pattern `*-analyses` — tous opérationnels. Effort sur la substance juridique P2 et l'isolation BE-only.

---

## Parcours écran réel de l'avocat BE — scénarios P2

Source : `docs/business/parcours-ecran-dossier.md` (enrichi par F-207 passage 6) + audit BE travail §3.

**Scénario licenciement standard (le plus fréquent en P2)**

1. L'avocat BE ouvre un dossier de licenciement → écran **détail du dossier**, 4 onglets.
2. Onglet **Dossier** : contrat de travail uploadé → date de signature détectée par l'IA (pré-2014 ou post-2014 — branchement Claeys vs statut unique).
3. Onglet **Analyse** : pipeline IA — détection `ancienneteAnnees`, `salaireBrut`, `dateRupture`, `motifRupture`, `positionProtegee` (délégué / grossesse), `clauseNonConcurrencePresente`.
4. Onglet **Décision** → panneau outils décisionnels BE : les 10 nouveaux outils F-213 apparaissent en **continuation de la séquence F-207** (8 outils P1 déjà en tête, 10 outils P2 à la suite).
5. L'avocat ouvre `licenciement-be-statut-unique-preavis` OU `licenciement-be-formule-claeys` selon la date du contrat détectée.
6. Si litige motivation : l'avocat ouvre `licenciement-be-cct109-deraisonnable` (score 3/8/12/17 semaines).
7. Si grossesse détectée : `licenciement-be-protection-grossesse`.
8. Si délégué syndical : `licenciement-be-protection-deleguee`.
9. Si modification conditions essentielles (acte équipollent) : `licenciement-be-acte-equivalent`.
10. En parallèle ou séquentiellement : `clause-non-concurrence-be` si clause détectée dans le contrat.
11. Si arriérés salaire : `rappel-salaire-be`.
12. Si transaction proposée : `transaction-be-travail`.
13. Si harcèlement : `harcelement-be-procedure-formelle`.
14. Refresh **dashboard décisionnel** (F-IA-02) → agrégation verdicts.
15. **Génération projet de conclusions** (F-98) — état terminal inchangé.

---

## État terminal du processus

**Inchangé** — « projet de conclusions généré » (tranché par F-98). F-213 enrichit la chaîne décisionnelle **avant** la génération des conclusions, sans déplacer l'état terminal.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone | Statut |
|---|---|---|
| 1-3. Upload, analyse, synthèse | onglets Dossier + Analyse | ✅ inchangés |
| 4. Panneau outils décisionnels (filtre BE) | onglet Décision — `app-decisional-tools-panel` | ✅ existant — séquence F-207 (8 outils P1) en tête |
| 5-13. Outils F-213 (10 sections P2) | onglet Décision — 10 nouvelles `*-section.component` | ❌ **manquant — apport F-213** |
| 14. Dashboard décisionnel | onglet Décision — `app-case-dashboard` | ✅ existant — agrège automatiquement |
| 15. Conclusions | onglet Décision — `app-conclusions-section` (F-98) | ✅ existant |

---

## Position candidate de la feature

Les 10 outils s'insèrent **à l'intérieur du panneau outils décisionnels** (onglet Décision), **après les 8 outils P1 de F-207** — aucun bloc primaire nouveau. Chacun est une entrée `TOOL_REGISTRY` standard (instanciation conditionnelle `workspaceCountry === 'BELGIQUE'`). Le panneau est conçu pour absorber N outils (CONTEXTUAL trigger pattern F-166 / F-IA-04).

---

## Challenge placement

L'écran cible (onglet **Décision** → `app-decisional-tools-panel`) est le placement standard de tous les outils décisionnels existants (Travail FR, Immigration FR/BE, Famille FR/BE, Travail BE P1). Cohérent — l'avocat BE y trouve déjà ses outils F-198 / F-204 / F-207. ✅ **Placement juste.**

---

## Challenge lisibilité de la séquence

⚠️ **Ajustement requis** : les 10 outils P2 doivent s'insérer **après les P1** dans un ordre métier lisible.

**Ordre proposé dans le TOOL_REGISTRY BE (suite F-207)** :

_Outils P1 F-207 (déjà livrés — rappel séquence)_
1. `prescription-be-litige-travail` — ALWAYS_ON (transversal)
2. `c4-onem-checklist` — CONTEXTUAL
3. `contestation-c4-onem` — CONTEXTUAL
4. `at-fedris-declaration` — CONTEXTUAL
5. `refere-tribunal-travail-be` — ALWAYS_ON
6. `rcc-be-conditions` — CONTEXTUAL
7. `rcc-be-indemnite-complementaire` — CONTEXTUAL
8. `outplacement-be-obligatoire-45` — CONTEXTUAL

_Outils P2 F-213 (nouveaux)_
9. `licenciement-be-statut-unique-preavis` — CONTEXTUAL `dateContrat >= 2014-01-01`
10. `licenciement-be-formule-claeys` — CONTEXTUAL `dateContrat < 2014-01-01`
11. `licenciement-be-cct109-deraisonnable` — CONTEXTUAL `type_rupture=LICENCIEMENT_ORDINAIRE`
12. `licenciement-be-protection-grossesse` — CONTEXTUAL `grossesse_ou_maternite_detectee=true`
13. `licenciement-be-protection-deleguee` — CONTEXTUAL `position_protegee=DELEGUE`
14. `licenciement-be-acte-equivalent` — ALWAYS_ON BE (fréquent, branche "modification unilatérale" non détectable automatiquement)
15. `clause-non-concurrence-be` — CONTEXTUAL `clause_non_concurrence_presente=true`
16. `rappel-salaire-be` — ALWAYS_ON BE (très fréquent, transversal)
17. `transaction-be-travail` — CONTEXTUAL `transaction_proposee=true`
18. `harcelement-be-procedure-formelle` — CONTEXTUAL `harcelement_detecte=true`

**Justification des ALWAYS_ON** : `licenciement-be-acte-equivalent` et `rappel-salaire-be` sont ALWAYS_ON car (a) le contrat peut ne pas mentionner explicitement la situation (modification verbale, retenues de salaire discrètes) et (b) leur fréquence est suffisamment haute pour les proposer systématiquement à tout avocat BE Travail.

**Mise en œuvre** : `TOOL_REGISTRY` BE complété avec les 10 nouvelles entrées dans l'ordre ci-dessus. Chaque mini-spec frontend précisera le `trigger_field` et la `visibility` retenus.

---

## Challenge charge écran

Onglet **Décision** porte 3 blocs primaires (`app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`) — seuil ~3 respecté. F-213 enrichit **le contenu interne** du panneau, **pas de nouveau bloc primaire**.

La majorité des outils P2 sont CONTEXTUAL — un dossier de licenciement typique exposera 3-5 outils P1 + 2-4 outils P2 pertinents, pas les 18. Densité raisonnable.

Pour un dossier de licenciement post-2014 sans circonstance spéciale : prescription + C4 + statut unique préavis + éventuellement CCT 109. Soit 4-5 outils. ✅

Pour un dossier de licenciement post-2014 délégué syndical avec grossesse : jusqu'à 7-8 outils — toujours dans les limites acceptables du panneau. ✅

**Aucun dépassement de charge écran.**

---

## Challenge état final / continuité

Après le verdict de chaque outil :
- Refresh dashboard décisionnel (F-IA-02) — `CaseDashboardRefreshService.triggerRefresh()` dans `next:` du POST (pattern SF-IA-02-03). ✅
- Verdicts enrichissent le projet de conclusions (F-98). ✅

Continuité préservée — chaque outil mène vers la suite du parcours.

---

## Ajustements requis

1. **Ordre TOOL_REGISTRY BE** — séquence P1 (F-207) + P2 (F-213) dans l'ordre métier ci-dessus.
2. **`workspaceCountry === 'BELGIQUE'` strict** — test isolation France obligatoire par SF frontend.
3. **CONTEXTUAL vs ALWAYS_ON** arbitré par outil (tableau ci-dessus) et figé dans chaque mini-spec frontend.
4. **Pas d'agrégation visuelle** des outils préavis — `statut-unique-preavis` et `formule-claeys` sont deux entrées TOOL_REGISTRY distinctes (CONTEXTUAL mutuellement exclusifs par `dateContrat`).
5. **Pré-remplissage IA obligatoire** — chaque section frontend implémente `prefillFromAi()` + provenance + `getPrefillCount(input)`.
6. **Champs `TravailExtractedData` nouveaux** (listés par outil dans les mini-specs backend) annotés `// BELGIQUE UNIQUEMENT`.

---

## Invariants anti-surcharge pour les mini-specs

- **Zéro bloc primaire nouveau** — enrichissement du contenu interne du panneau Décision uniquement.
- **`workspaceCountry === 'BELGIQUE'` strict** — pas de fuite FR.
- **Ordre du panneau respecte la séquence métier** (P1 en tête, P2 après, ordonnés par fréquence/urgence).
- **CONTEXTUAL trigger_field cohérent avec les flags IA** — pas de trigger orphelin (test d'intégrité `CritereCodeIntegrityIT` + `DecisionToolVisibilityIntegrityIT` restent verts).
- **Critères F-IA-03 `BE_*` distincts** des codes FR équivalents.
- **`getPrefillCount(input)` obligatoire** — parité stricte avec `prefillFromAi()` runtime.

---

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` devra être enrichi lors du merge F-213 : 7ᵉ passage — ajout du flux outils décisionnels Travail BE P2 (séquence statut unique / Claeys / CCT 109 / grossesse / délégué / acte équipollent / non-concurrence / rappel salaire / transaction / harcèlement), invariant « ordre TOOL_REGISTRY respecte séquence P1 → P2 », nouveaux `trigger_field` CONTEXTUAL.

---

## Décision finale

**GO avec ajustements.** Placement correct (panneau Décision standard BE-only, suite de F-207). Charge écran maîtrisée (outils CONTEXTUAL, densité 3-8 outils max par dossier). Lisibilité séquence requise : `TOOL_REGISTRY` BE ordonné P1 (F-207) → P2 (F-213) dans l'ordre métier. Les 6 ajustements ci-dessus sont à intégrer dans chaque mini-spec.

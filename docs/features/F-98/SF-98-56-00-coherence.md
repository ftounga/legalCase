# SF-98-56 — Cadrage cohérence (étape 0)

> Feature : **Réfutation de la jurisprudence adverse dans les conclusions** (chaînon F-179 → F-98).
> Phase 2bis de l'audit de génération des conclusions (cf. `memory/project_f98_audit_conclusions.md`).
> Skill : `ai-skills/feature-coherence-challenger.md`. Date : 2026-06-09.

## Verdict : **GO avec ajustements** (1 décision PO bloquante — distinction du camp)

---

## Intention métier (1 phrase)

Quand l'adversaire cite, dans ses écritures, une jurisprudence **inexistante** ou dont il **dénature la portée**, LegalCase doit fournir à l'avocat, dans le projet de conclusions, le matériau pour **réfuter** cette citation — au lieu de laisser ce travail de vérification adverse entièrement manuel.

---

## Workflow métier réel de l'utilisateur cible (avocat, contentieux écrit)

> Source : pratique standard du contentieux civil/social français (procédure écrite, échange de conclusions) + signal terrain Renversez 13/05 (demande de génération de conclusions). Étapes marquées ⚠ = hypothèse à valider terrain.

1. L'avocat reçoit le dossier de **son client** (pièces, faits, parfois un premier jeu de conclusions).
2. Il analyse, identifie les moyens de droit, sélectionne sa propre jurisprudence à l'appui.
3. **Posture « en demande »** : il rédige et dépose ses premières conclusions. *À ce stade l'adversaire n'a pas encore conclu → aucune jurisprudence adverse à réfuter.*
4. L'**adversaire** dépose ses conclusions, citant **sa** jurisprudence (parfois inexistante, datée, ou dont il force la portée).
5. L'avocat **lit les conclusions adverses**, repère les arrêts invoqués par l'adversaire.
6. Il **vérifie** chacun : l'arrêt existe-t-il ? dit-il vraiment ce que l'adversaire prétend ? est-il toujours d'actualité ?
7. **Posture « en réponse / réplique »** : il rédige des conclusions en réponse qui **réfutent** point par point les citations adverses fragiles (« l'arrêt n° X n'existe pas », « la portée invoquée est contraire à la solution réelle »).
8. Il dépose ses conclusions en réplique. ⚠ L'intensité de l'étape 7 dépend du dossier.

**Le besoin de la feature se situe aux étapes 5-7, et UNIQUEMENT sur des citations issues des écritures ADVERSES.**

---

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Réception dossier + pièces | F-43 import / upload documents | ✅ Livrée |
| 2. Analyse, moyens, jurisprudence à l'appui | F-3/4/5 analyse, F-242 ajout manuel jurisprudence, F-JU-02 jurisprudence par outil | ✅ Livrées |
| 3. Rédaction conclusions (en demande) | F-98 génération de conclusions | ✅ Livrée |
| 4. Dépôt adverse (hors app) | — (acte de la partie adverse, uploadé comme document) | n/a |
| 5. Repérage des arrêts cités par l'adversaire | F-179 vérification des jurisprudences citées dans les documents uploadés | ✅ Livrée |
| 6. Vérification existence + portée | F-179 (statuts VERIFIED / SUSPECT / NOT_FOUND / UNCERTAIN ; `position_alleguee` ; `explication`) | ✅ Livrée |
| **6 bis. Savoir qu'un document / une citation est du camp ADVERSE** | **— AUCUNE feature** (ni livrée, ni backlog) | ❌ **Manquante** |
| 7. Réfutation dans les conclusions en réponse | **SF-98-56 (feature challengée)** | — |
| 8. Dépôt réplique | F-98 export Word/PDF (SF-98-50/51) | ✅ Livrées |

---

## Position de la nouvelle feature

SF-98-56 s'insère à **l'étape 7** (réfutation). Elle consomme la sortie de F-179 (étapes 5-6) et l'injecte dans le builder de conclusions de F-98 (étape 3/7).

Couture technique repérée (reconnaissance préalable) :
- Source : table `jurisprudence_checks` (`reference`, `statut`, `position_alleguee`, `explication`, `document_name`, `claude_confidence`).
- Récupération : `JurisprudenceCheckRepository.findByCaseFileId(caseFileId)`.
- Injection : nouveau champ dans `ConclusionPromptInput` + loader dans `CaseConclusionService` + méthode `append…` dans `CaseConclusionPromptBuilder` + mise à jour de `JURISPRUDENCE_GUARD` (autoriser une 3ᵉ section, réfutation).

---

## Challenge amont

**Question** : chaque étape AVANT la feature est-elle couverte par une feature existante (livrée ou backlog) ?

- Étapes 1-6 : ✅ toutes couvertes (upload, analyse, F-179 détection + vérification).
- **Étape 6 bis — origine adverse de la citation : ❌ TROU FONCTIONNEL AMONT BLOQUANT.**

F-179 vérifie **toutes** les citations de **tous** les documents uploadés, sans distinguer le camp. La seule donnée d'origine est `document_name` (le nom de fichier). Or :
- Une citation **SUSPECT / NOT_FOUND** peut tout à fait provenir des écritures **du client** (ex. dans notre dossier de test, `04-conclusions-salarie.pdf` = conclusions du salarié = **notre** client).
- Réfuter à l'aveugle une telle citation reviendrait à faire **attaquer, par les conclusions du client, l'argument du client lui-même** — contresens grave, exactement le type de gadget que l'étape 0 doit empêcher.

→ La brique « savoir qu'une citation est du camp adverse » **doit exister** avant que SF-98-56 soit sûre. Elle n'existe pas. C'est l'**ajustement bloquant**.

## Challenge aval

**Question** : la sortie est-elle exploitable par les étapes aval ?

- ✅ Oui : la réfutation s'insère dans le projet de conclusions (texte), directement relue/éditée (SF-98-49), exportée Word/PDF (SF-98-50/51), versionnée (SF-98-52). Aucun trou aval.
- Note de péremption : `jurisprudence_checks.created_at` est rattaché à l'analyse → le calcul de `stale` (SF-98-53) reste cohérent. ✅

---

## STOPs / pré-requis à ajouter au backlog

Le seul verrou est **la fiabilisation de l'origine adverse**. Trois options (décision PO requise — voir « Décision finale ») :

| Option | Principe | Coût | Risque de contresens | Honnêteté produit |
|---|---|---|---|---|
| **A — Sélection avocat (human-in-the-loop)** | F-179 liste déjà les citations détectées par document ; l'avocat **coche** celles issues de l'adversaire à réfuter (ou désigne le(s) document(s) adverse(s)). Seules les cochées alimentent la réfutation. | Faible (un flag UI + filtre), aucune nouvelle table lourde | **Nul** (l'humain tranche) | Élevée (l'avocat reste maître) |
| **B — Tag « camp » du document** | Ajouter à l'upload un rôle de document (`PIECE_CLIENT` / `ECRITURES_ADVERSES` / `NEUTRE`). F-179 hérite du camp ; SF-98-56 ne réfute que `ECRITURES_ADVERSES`. | Moyen (migration + UI upload + propagation) — quasi une feature à part | Faible | Élevée, durable et réutilisable |
| **C — Heuristique sur `document_name`** | Regex sur le nom de fichier (« adverse », « conclusions_adverses », …). | Très faible | **Élevé** (dépend du nommage, faux positifs → réfute le client) | Faible (fragile, silencieux) |

**Recommandation skill** : **Option A** pour le MVP (zéro risque de contresens, coût faible, respecte l'invariant « l'avocat garde le contrôle »), avec **Option B** inscrite au backlog comme évolution durable si le signal terrain confirme l'usage. **Option C rejetée** (gadget fragile, viole l'esprit anti-hallucination du produit).

---

## Invariants anti-gadget pour la mini-spec

1. **Jamais réfuter une citation du propre camp du client.** Une citation n'alimente la réfutation que si son origine adverse est **établie de façon fiable** (option A ou B retenue), jamais déduite à l'aveugle.
2. **Seuls les statuts SUSPECT et NOT_FOUND** alimentent la réfutation (un arrêt VERIFIED adverse est valable — on ne le « réfute » pas ; UNCERTAIN = silence, on n'affirme rien : invariant « silence > erreur » de F-179).
3. **Pas de réfutation inventée.** Le prompt ne doit produire une réfutation que sur la base de `reference` + `explication` + `position_alleguee` fournies ; aucune assertion juridique nouvelle non étayée.
4. **Garde jurisprudence respectée.** La réfutation ne doit pas « citer avec autorité » un arrêt — elle démontre l'absence/la dénaturation de l'arrêt adverse. Mise à jour de `JURISPRUDENCE_GUARD` en ce sens.
5. **Section absente si rien à réfuter.** Aucune citation adverse SUSPECT/NOT_FOUND fiable → aucune section ajoutée (pas de rubrique vide, pas de « néant »).
6. **Pertinence par posture.** La réfutation a surtout du sens en posture « en réponse » ; à défaut de signal de posture explicite, elle reste pilotée par la présence effective de citations adverses fragiles (invariant 5).
7. **Pas de régression SF-98-55.** Le nouveau bloc reste de la matière interne traduite en droit — aucun jargon, aucun `document_name` brut, aucun statut technique (`SUSPECT`) exposé dans l'acte.
8. **Isolation workspace.** Récupération des `jurisprudence_checks` strictement bornée au `caseFileId`/`workspace_id` (déjà garanti côté F-179).

---

## Décision finale

**GO avec ajustements.** La chaîne amont est complète SAUF la **distinction du camp adverse**, qui est un trou fonctionnel bloquant. SF-98-56 ne peut pas démarrer sa mini-spec tant que le moyen de fiabiliser l'origine adverse n'est pas tranché (Option A / B / C).

**Action requise avant mini-spec (étape 1)** : décision PO sur l'option de distinction du camp.

### ✅ DÉCISION PO (2026-06-09) : **Option A — sélection avocat (human-in-the-loop)** retenue pour le MVP.
L'avocat marque, sur la liste des citations détectées par F-179, celles qui sont issues des écritures **adverses** et qu'il veut réfuter. Seules les citations marquées (et de statut SUSPECT/NOT_FOUND) alimentent la réfutation dans les conclusions. **Option B (tag de camp à l'upload)** reste inscrite comme évolution durable possible si le signal terrain le justifie. **Option C rejetée.**

**Conséquences de la décision A** :
- Deux couches dans la feature : (1) **marquage** d'une citation comme « adverse à réfuter » (UI sur la section « Jurisprudences citées » + persistance) ; (2) **injection** des citations marquées dans le builder de conclusions. Le découpage précis (1 ou 2 SF) sera arbitré en mini-spec (étape 1).
- **Impact écran avéré** (case à cocher / action « Marquer comme adverse » sur la liste F-179) → **étape 0 bis (cohérence écran) OBLIGATOIRE avant la mini-spec** → `docs/features/F-98/SF-98-56-00b-ux-coherence.md`.

> SF-98-56 passe de `Backlog` à `À faire` (verdict GO avec ajustements + décision PO prise) — mini-spec conditionnée à l'étape 0 bis.

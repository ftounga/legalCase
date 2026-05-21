# F-JU-01 — Cadrage cohérence (étape 0)

## Verdict : **GO**

Toutes les briques amont et aval nécessaires existent (livrées ou au backlog). F-JU-01 comble une étape manquante du workflow avocat — la **justification jurisprudentielle directe du calcul** — entre les outils décisionnels qui calculent et les features aval qui exploitent les arrêts (F-242, F-243, F-241). Pas de pré-requis bloquant. Quelques invariants anti-gadget à respecter dans la mini-spec.

---

## Intention métier (1 phrase)

Chaque résultat produit par un outil décisionnel (calculator/analyzer/generator) doit être accompagné de la **jurisprudence structurante** qui le fonde — exposée directement dans LegalCase, sans détour par Doctrine ou autre moteur tiers.

---

## Workflow métier réel de l'utilisateur cible (avocat sur un dossier)

⚠ Hypothèse à valider : ce workflow s'appuie sur la pratique standard de l'avocat (pas de doc `docs/business/workflow-*.md` formalisé) et sur la convergence des signaux terrain documentés en mémoire (Mengue 11/05, Gaspard 07/05, Renversez 12/05 et 13/05, 7+ prospects SAF/BE entre 05/05 et 12/05 — cf. entrée backlog F-241 et `memory/project_renversez_post_demo_13_05.md`).

1. **Réception du dossier client** — l'avocat reçoit la situation factuelle + les premières pièces (contrat, courriers, certificats).
2. **Qualification juridique de la situation** — l'avocat identifie le type de procédure applicable (licenciement abusif, OQTF, divorce, naturalisation…) à partir des faits et des pièces.
3. **Application du régime juridique** — l'avocat repère les textes pertinents (Code du travail / Code civil / CESEDA / Code de la sécurité sociale) et la jurisprudence applicable.
4. **Calcul / estimation des conséquences** — montant d'indemnité, délais procéduraux, chances de succès, scoring de risque.
5. **Justification juridique du résultat obtenu** — l'avocat doit pouvoir s'appuyer sur du droit positif pour soutenir sa position auprès du client, de l'adversaire, du juge.
6. **Vérification de la fraîcheur jurisprudentielle** — l'avocat s'assure qu'aucun revirement récent n'invalide la position calculée (étape critique : un arrêt récent de la chambre sociale peut renverser une jurisprudence des 5 années précédentes).
7. **Conseil au client** — l'avocat explique sa position au client en s'appuyant sur les calculs et les arrêts ; il chiffre les chances de succès, recommande une stratégie (transiger / contentieux / abandonner).
8. **Rédaction des conclusions / courriers** — l'avocat rédige les écritures en citant les arrêts qui fondent sa position.
9. **Échange contradictoire** — l'avocat lit les conclusions adverses, vérifie la fidélité des arrêts cités par la partie adverse, contre-argumente.
10. **Plaidoirie / négociation / décision** — phase finale, l'avocat reprend les arrêts pour argumenter à l'audience ou en négociation.

---

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Réception dossier + pièces | F-43 import dossier, F-04 ingestion, F-146 source précise | ✅ Livrée |
| 2. Qualification juridique de la situation | F-04 pipeline IA, F-05 enrichment, F-IA-04 détection type procédure | ✅ Livrée |
| 3. Application régime juridique (textes) | Outils décisionnels F-DT-XX / F-IM-XX / F-FA-XX (~131 dans `TOOL_REGISTRY`) | ✅ La plupart livrées |
| 4. Calcul / estimation des conséquences | Calculators backend dans chaque outil + F-IA-01 pré-remplissage + F-IA-02 dashboard transversal | ✅ Livrée |
| **5. Justification jurisprudentielle du calcul** | **F-JU-01 (la feature challengée)** | 🟡 Backlog (cette étape 0) |
| **6. Vérification fraîcheur juris (revirements)** | **F-JU-01 (cron mensuel intégré)** | 🟡 Backlog (cette étape 0) |
| 7. Conseil au client | F-IA-02 dashboard transversal, F-186 export synthèse PDF, F-176 pistes stratégiques, F-192 propagation pistes | ✅ Livrée |
| 8. Rédaction conclusions / courriers | F-243 conclusions générées (livrée), F-242 citation structurée par avocat (livrée), générateurs de documents par domaine | ✅ Livrée |
| 9. Vérification fidélité arrêts adverses | F-179 vérification jurisprudence citée dans documents uploadés | ✅ Livrée 2026-05-18 |
| 10. Plaidoirie / négociation / décision | Hors périmètre LegalCase (action humaine) | — |

---

## Position de la nouvelle feature

F-JU-01 s'insère **aux étapes 5 et 6** du workflow — la **justification jurisprudentielle du calcul** et la **vérification de la fraîcheur**. C'est aujourd'hui le **seul trou fonctionnel** du workflow entre la qualification IA (étapes 1-3 ✅) / le calcul (étape 4 ✅) et l'exploitation aval (étapes 7-10 ✅).

L'avocat doit aujourd'hui ouvrir Doctrine / Lexis Plus / Lextenso en parallèle de LegalCase pour vérifier la juris qui fonde le calcul. F-JU-01 supprime ce détour.

---

## Challenge amont

**Question** : *« Chaque étape AVANT F-JU-01 dans le workflow métier est-elle couverte par une feature du produit (livrée OU backlog) ? »*

| Pré-requis amont | Statut | Verdict |
|---|---|---|
| Réception et ingestion du dossier (étape 1) | F-43, F-04, F-146 livrées ✅ | OK |
| Qualification IA de la situation (étape 2) | F-04, F-05, F-IA-04 livrées ✅ | OK |
| Outils décisionnels avec branches de calcul identifiables (étape 3-4) | ~131 outils dans `TOOL_REGISTRY`, dont ~80-90 éligibles à des citations ✅ | OK |
| Infrastructure d'extraction d'arrêts jurisprudentiels | F-179 livrée 2026-05-18 (`JurisprudenceVerificationService`, `WebSearchService`, regex + Claude) ✅ | OK — réutilisable |
| Compte OAuth2 PISTE Légifrance / JUDILIBRE | À créer côté ailegalcase | Non bloquant (procédure technique, gratuit, ~1h) |
| Source des chapeaux officiels Cassation | JUDILIBRE API les expose directement ✅ | OK |
| Pattern de citation `ToolJurisprudenceCitable` exposé par outils | Interface à introduire en SF-JU-01-04 (frontend) | Non bloquant (créé dans la feature elle-même) |

**Conclusion amont : aucun trou bloquant**. F-179 fournit l'essentiel de l'infrastructure d'extraction et de vérification réutilisable. Les 131 outils du `TOOL_REGISTRY` existent et leurs branches de calcul sont codées (lisibles par Claude pour formuler les requêtes JUDILIBRE).

---

## Challenge aval

**Question** : *« La sortie de F-JU-01 est-elle exploitable par les étapes AVAL du workflow métier ? »*

| Étape aval | Feature(s) exploitant la sortie | Verdict |
|---|---|---|
| 7. Conseil au client | F-IA-02 dashboard, F-186 export PDF | OK — les citations apparaissent à côté du calcul, l'avocat peut les lire à l'oral |
| 8. Rédaction conclusions | F-243 conclusions générées | **Question à clarifier en mini-spec** : F-243 doit-elle puiser dans les mappings F-JU-01 pour citer les arrêts dans les écritures générées ? Probablement oui. À spécifier dans SF-JU-01-04 ou SF-JU-01-05. |
| 8 bis. Citation structurée manuelle | F-242 livrée 2026-05-18 (citation structurée avocat) | OK — F-JU-01 peut pré-remplir F-242 avec les arrêts du mapping (« suggéré par LegalCase ») |
| 9. Vérification arrêts adverses | F-179 livrée | OK — F-JU-01 et F-179 sont symétriques (F-JU-01 cite proactivement, F-179 vérifie ce qu'on cite à l'avocat) |
| Approfondissement par avocat (lecture de l'arrêt complet) | F-241 deeplinks Doctrine / Lexis Plus | OK — lien Légifrance direct dans F-JU-01 + bouton F-241 « Ouvrir dans Doctrine » à proximité |
| Export PDF synthèse | F-186 | **Question à clarifier en mini-spec** : les citations F-JU-01 doivent-elles apparaître dans l'export PDF de la synthèse ? Probablement oui dans une section dédiée. À spécifier en SF-JU-01-04 ou SF dédiée. |

**Conclusion aval : aucun trou bloquant**. Deux questions à clarifier dans la mini-spec sur l'intégration avec F-243 (conclusions générées) et F-186 (export PDF), mais ce ne sont pas des pré-requis — ce sont des extensions naturelles à ajouter au scope de F-JU-01 ou en V2.

---

## STOPs / pré-requis à ajouter au backlog

**Aucun STOP**. Aucun pré-requis bloquant à ajouter au backlog avant F-JU-01.

Points à confirmer dans la mini-spec (sans bloquer le démarrage) :
1. Intégration F-JU-01 ↔ F-243 (conclusions générées doivent-elles citer les arrêts F-JU-01 ?) — décision à prendre en SF-JU-01-04 ou en SF dédiée
2. Intégration F-JU-01 ↔ F-186 (export PDF synthèse) — section dédiée « Jurisprudence applicable » ?
3. Pré-remplissage F-242 par les arrêts F-JU-01 (« suggéré par LegalCase ») — décision à prendre en mini-spec
4. Création du compte OAuth2 PISTE côté ailegalcase — procédure technique à inclure dans SF-JU-01-02

---

## Invariants anti-gadget pour la mini-spec

Contraintes dures que la mini-spec **ne doit pas relâcher** sous peine de transformer F-JU-01 en gadget :

1. **Top-3 systématique** — jamais 1 seul arrêt cité. Si Claude ne trouve qu'1 arrêt suffisamment confiant, on cite 1 mais on log un signal d'apprentissage. L'objectif est la robustesse aux erreurs marginales (un arrêt limite est dilué dans le top-3).

2. **Chapeau officiel Cassation cité textuellement** — pas de reformulation Claude du résumé. Zéro déformation possible. Si l'arrêt n'a pas de chapeau (rare en hors-Cassation), on cite le résumé Légifrance officiel ou rien.

3. **Date de dernière vérification visible à l'avocat** — mention « Citation indicative — date de dernière vérification : XX/XX/XXXX. L'avocat reste seul juge de l'applicabilité au dossier. » sous chaque bloc citations. Transparence absolue sur la fraîcheur.

4. **Bouton « Signaler un problème »** obligatoire sur chaque citation côté avocat — feedback loop utilisateur qui remonte dans le dashboard admin (crowdsourcing du contrôle qualité). Pas une option V2.

5. **Seuil de confiance minimum 60 %** — si Claude est < 60 % confiant sur la pertinence d'un arrêt pour une branche, le mapping reste **vide** (le composant frontend n'affiche rien). Silence > erreur. Pas d'affichage « par défaut » d'un arrêt douteux.

6. **Compatibilité 131 outils du `TOOL_REGISTRY`** — interface `ToolJurisprudenceCitable` exposée par les composants outils via input `branchActive`. Pas de hardcoding sur 5 outils pilote. Les outils non éligibles (générateurs purs, checklists administratives) restent muets (aucune erreur, juste absence du bloc citations).

7. **Réutilisation infra F-179** — `WebSearchService`, prompt d'extraction, regex de format d'arrêt. Pas de duplication de code d'extraction jurisprudentielle. Si nouvelle source publique (Juridat BE, Cour const. BE), étendre `WebSearchService` plutôt que créer un service parallèle.

8. **Sources publiques uniquement** — JUDILIBRE / ArianeWeb / Légifrance API PISTE (FR), Juridat / Cour const. BE / Cass. BE (BE). Aucun scraping Doctrine / Lexis Plus / Lextenso / Dalloz / Lefebvre. Pas d'accord éditeur. Pas de licence payante.

9. **Mode full auto-pilot par défaut** — Claude évalue et agit sans humain dans la boucle (confirmations + ajouts > 80 % confiance + remplacements > 85 % confiance). Trust mode configurable mais le défaut est auto-pilot. Pas de retour à un design « stagiaire fait tout » ni « validation humaine systématique ».

10. **Alerte massive automatique** — si > 5 % des mappings sont touchés en un seul run du cron mensuel, suspension automatique des actions + email « intervention requise ». Parade aux bugs prompt / API massifs qui pourraient corrompre la base en une nuit.

11. **Audit log complet rejouable** — chaque action du cron mensuel + chaque action manuelle dashboard admin tracée (qui, quand, quelle action, raison Claude + score de confiance). Permet de rejouer la décision en cas de doute ou de problème terrain.

12. **Bootstrap 100 % automatique** — pas de stagiaire / pas d'avocat conseil obligatoire pour le bootstrap initial. La validation post-bootstrap (échantillon ~50 mappings) est facultative et faite par le fondateur en 2-3 h. Si la review identifie > 5 % d'erreur Claude, ajustement prompt et re-run partiel (pas de bootstrap manuel à la main).

---

## Décision finale

**GO**. F-JU-01 passe `Backlog` → `À faire`. Mini-spec SF-JU-01-01 (backend infrastructure) peut démarrer après validation user de ce document.

Le périmètre tel qu'inscrit dans `PRODUCT_SPEC.md` (5 SF, ~8,5 j dev + 2-3 h review humaine + ~12 € LLM bootstrap + ~5-10 €/mois LLM veille) est cohérent avec ce cadrage. Les invariants anti-gadget ci-dessus seront repris en critères d'acceptation des mini-specs.

**Prochaine étape** : étape 0 bis — cadrage cohérence écran (`ai-skills/screen-coherence-challenger.md`) car F-JU-01 a un impact écran (nouveau bloc « Jurisprudence applicable » sous chaque résultat d'outil + dashboard admin `/super-admin/jurisprudence-watch`). À produire en `docs/features/F-JU-01/SF-JU-01-00b-ux-coherence.md` avant la mini-spec SF-JU-01-01.

---

## Sources

- `docs/PRODUCT_SPEC.md` — entrée F-JU-01 (ligne 264, ajoutée 2026-05-21)
- `docs/PRODUCT_SPEC.md` — entrées F-179, F-241, F-242, F-243, F-IA-01 à F-IA-04 (briques amont et aval)
- `memory/project_renversez_post_demo_13_05.md` — signal terrain Renversez 13/05 et 18/05
- `memory/project_mengue_followup.md` — signal terrain Mengue 11/05 (BE)
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` — `TOOL_REGISTRY` (131 entrées)
- ⚠ Hypothèse à valider : workflow avocat reconstruit à partir de la pratique standard, pas de doc `docs/business/workflow-*.md` formalisé. À enrichir lors d'un prochain échange avocat.

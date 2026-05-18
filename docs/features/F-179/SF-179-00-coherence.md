# F-179 — Cadrage cohérence (étape 0)

> Produit via la skill `ai-skills/feature-coherence-challenger.md`. Étape 0 du cycle de gouvernance — placée après l'inscription de F-179 au `PRODUCT_SPEC.md` (statut `Backlog`) et avant la mini-spec.

## Verdict : **GO avec ajustements**

## Intention métier (1 phrase)

Permettre à l'avocat de détecter automatiquement, dans les documents adverses uploadés sur un dossier (typiquement les conclusions de la partie adverse), les références jurisprudentielles citées et de faire vérifier leur **existence réelle** et la **fidélité de la position juridique** qu'on leur attribue — afin de repérer les hallucinations et les citations de mauvaise foi.

## Workflow métier réel de l'utilisateur cible (avocat)

Source : pratique standard de l'avocat plaidant (contentieux du travail / famille / immigration) — ⚠ hypothèse de travail, ancrée sur deux signaux terrain documentés (`MEMORY` : Marjolaine RENVERSEZ 13/05 — friction conclusions adverses ; cas Mata v. Avianca cité par le `PRODUCT_SPEC.md`). Le « signal terrain à confirmer » exigé par la fiche F-179 (« au moins un avocat mentionne une citation suspecte ») reste à valider — voir STOP partiel ci-dessous.

1. L'avocat ouvre un dossier contentieux et reçoit les pièces : pièces de son client + **conclusions de la partie adverse**.
2. Il lit les conclusions adverses pour comprendre la thèse opposée.
3. La partie adverse appuie sa thèse sur des arrêts (`Cass. soc.`, `CE`, `Cour const. BE`, `Cass. BE`…) cités avec un numéro de pourvoi/rôle et une date.
4. L'avocat doit, pour répliquer, **vérifier chaque arrêt cité** : existe-t-il vraiment ? dit-il bien ce que l'adversaire prétend ?
5. Aujourd'hui, il fait cette vérification **à la main** : recherche Légifrance / Juridat / Dalloz / Lexis, lecture de l'arrêt, comparaison avec la position alléguée.
6. Il identifie les citations problématiques : arrêt inexistant (hallucination si conclusions générées par une IA généraliste), arrêt réel mais détourné (position alléguée ≠ contenu réel — mauvaise foi).
7. Il consigne ces anomalies pour les exploiter : un arrêt fantôme ou détourné décrédibilise toute l'argumentation adverse.
8. Il intègre ces points dans **sa réplique / ses propres conclusions**.
9. Il consulte la synthèse de son dossier et les outils décisionnels avant de finaliser sa stratégie.
10. Il génère / rédige le projet de conclusions (F-98) et le finalise.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Réception dossier + pièces (dont conclusions adverses) | F-43 import dossier + upload documents | ✅ Livrée |
| 1bis. Extraction / OCR du texte des documents | F-121 / F-122 OCR, pipeline extraction | ✅ Livrée |
| 1ter. Identification des pièces dans un document | F-145 / F-146 détection pièces + `sourceRef` précis | ✅ Livrée |
| 2-3. Lecture / analyse des conclusions adverses | F-3/F-4/F-5 pipeline IA (chunk → doc → dossier), synthèse | ✅ Livrée |
| **4. Vérification de la jurisprudence citée** | **F-179 (la feature challengée)** | 🟡 Backlog |
| 6. Détection d'incohérence saisie ↔ analyse | F-IA-03 cohérence (saisie avocat ↔ analyse IA) | ✅ Livrée |
| 7. Consignation des anomalies (statut, suivi) | F-92 pièces manquantes (pattern markable), F-194/195/196 alignements | ✅ Livrée |
| 9. Synthèse du dossier, outils décisionnels | F-3/4/5 synthèse, F-IA-04 outils décisionnels | ✅ Livrée |
| 10. Génération de conclusions | F-98 conclusions | 🟡 En cours (SF-98-01 livrée) |

## Position de la nouvelle feature

F-179 s'insère à l'**étape 4** du workflow métier : juste après que les documents adverses ont été uploadés, extraits et analysés (étapes 1-3, toutes ✅), et juste avant que l'avocat exploite les anomalies dans sa réplique (étapes 6-8). C'est une **brique de vérification de sources externes**, intercalée entre l'analyse IA du dossier (amont) et la consignation/exploitation des anomalies (aval).

## Challenge amont

**Question** : chaque étape AVANT F-179 dans le workflow métier est-elle couverte par une feature existante du produit ?

| Pré-requis amont | Couvert ? | Commentaire |
|---|---|---|
| Upload des documents adverses sur le dossier | ✅ F-43 + upload | Brique solide. |
| Extraction du texte (y compris PDF scannés / conclusions volumineuses) | ✅ F-121 / F-122 OCR | Indispensable : sans texte extrait, aucune référence à détecter. Les conclusions adverses sont souvent des PDF — l'OCR couvre le cas scanné. |
| Analyse IA du dossier (post-traitement disponible) | ✅ F-3/4/5 `CaseAnalysisService` | F-179 se branche en post-traitement de `CaseAnalysisService` (pattern miroir F-146 `PiecesPromptContext`, F-IA-03 `SourceExplanationGenerator`). L'infrastructure de post-traitement fail-open existe déjà. |
| Citation précise d'une pièce (`document.pdf · pièce · page N`) | ✅ F-146 Terminée | Prérequis explicite de la fiche F-179. Permet de dire OÙ la référence a été trouvée. |
| Accès à un modèle Claude pour la vérification | ✅ `AnthropicService` (`analyzeWithModel`, prompt caching F-142-04) | Sonnet déjà utilisé pour la synthèse. |

**Conclusion amont** : **aucun trou amont**. Toutes les briques nécessaires sont livrées. F-179 a une base fonctionnelle solide pour s'appuyer.

## Challenge aval

**Question** : la sortie de F-179 (la liste des références jurisprudentielles avec statut `VERIFIED` / `SUSPECT` / `NOT_FOUND` / `UNCERTAIN`) est-elle exploitable par les étapes AVAL du workflow métier ?

| Étape aval | Exploitable ? | Commentaire |
|---|---|---|
| 6. Détection d'incohérence | ✅ via SF-179-04 | Un arrêt `SUSPECT` cité par l'adverse devient une alerte de cohérence dans le popover F-IA-03. Intégration prévue dans le découpage. |
| 7. Consignation des anomalies | ⚠ partiel | F-179 affiche les statuts dans une section dédiée de la synthèse (SF-179-03, pattern visuel F-92/F-93). En V1 il n'y a **pas de statut markable avocat** (vu / traité / écarté) comme pour les pièces manquantes (F-194). Acceptable en V1 : la liste est consultable, le statut IA suffit pour un premier usage. À tracer comme extension possible (cf. invariant 5). |
| 8. Intégration dans la réplique / conclusions (F-98) | ⚠ trou aval mineur | Aujourd'hui le résultat de F-179 n'alimente PAS automatiquement la génération de conclusions F-98. C'est acceptable : l'avocat lit la section « Jurisprudences citées », identifie les arrêts `SUSPECT`/`NOT_FOUND` et les exploite manuellement dans sa réplique. La fiche F-179 exclut explicitement l'export du rapport (V2). **Pas un blocage** — l'avocat reste dans la boucle, c'est lui qui rédige la réplique. |

**Conclusion aval** : **pas de trou aval bloquant**. La sortie est consultable (synthèse) et reliée à F-IA-03 (alerte cohérence). L'absence de statut markable et l'absence d'alimentation automatique de F-98 sont des limitations V1 assumées, conformes au « Hors scope » de la fiche.

## STOPs / pré-requis à ajouter au backlog

**Aucun pré-requis fonctionnel manquant** — toutes les briques amont sont livrées.

**Un point de vigilance non bloquant pour le dev, mais bloquant pour l'activation produit** :

> La fiche F-179 du `PRODUCT_SPEC.md` indique explicitement : *« Signal terrain à confirmer avant démarrage : au moins un avocat mentionne avoir trouvé une citation suspecte dans des conclusions adverses ».*

Ce signal n'est **pas formellement confirmé** à ce jour. Les signaux disponibles sont indirects (friction conclusions adverses chez RENVERSEZ 13/05 ; risque générique cité dans la fiche). Ce cadrage **ne lève pas** cette condition — il relève d'une décision produit. La feature est **fonctionnellement cohérente** (verdict GO), mais l'opérateur doit acter que le dev démarre sur la base d'un signal terrain indirect. Ce n'est pas un STOP de cohérence fonctionnelle : c'est une réserve de priorisation à arbitrer hors de cette skill. **La présente livraison autonome procède sur instruction explicite de l'orchestrateur** ; la réserve est consignée ici pour traçabilité.

## Invariants anti-gadget pour la mini-spec

1. **Vérification uniquement, jamais génération.** F-179 ne produit aucune jurisprudence : il vérifie EXCLUSIVEMENT les références déjà présentes dans les documents uploadés. Toute mini-spec qui ferait suggérer des arrêts par l'IA sort du périmètre → refus.
2. **4 statuts obligatoires et distincts.** `VERIFIED` / `SUSPECT` / `NOT_FOUND` / `UNCERTAIN`. Le statut `UNCERTAIN` est non négociable : il interdit de transformer un knowledge gap Claude en faux `NOT_FOUND`. Une mini-spec qui fusionnerait `NOT_FOUND` et `UNCERTAIN` → refus.
3. **Pas de base documentaire propriétaire.** F-179 s'appuie sur la connaissance de Claude + web search public (Légifrance / Juridat). Aucune table de jurisprudence stockée, aucun corpus importé.
4. **Tolérance d'échec du web search.** Un timeout / une erreur HTTP du fallback web search ne doit JAMAIS faire échouer l'analyse ni produire un `NOT_FOUND` : il bascule en `UNCERTAIN`. Le post-traitement F-179 est **fail-open** comme `SourceExplanationGenerator` / `procedureCheckService` — une exception laisse la `CaseAnalysis` `DONE`.
5. **Traçabilité de la source de vérification.** Chaque résultat indique si le web search a été utilisé (`webSearchUsed`) et, quand disponible, le lien source (`sourceUrl`). L'avocat doit pouvoir refaire la vérification lui-même — l'outil ne se substitue pas à son jugement, il l'oriente.
6. **Isolation workspace stricte.** La table `jurisprudence_checks` porte un `workspace_id` ; tout endpoint de lecture filtre par workspace (404 camouflage hors workspace), conforme au modèle multi-tenant.
7. **FR + BE dès la V1.** La détection regex et le prompt Sonnet couvrent les formats français ET belges (`Cass. soc.`, `CE`, `CA`, `Trib. trav. BE`, `Cour const. BE`, `Cass. BE`). Aucune adaptation par domaine métier (transversal Travail / Famille / Immigration) ; l'adaptation par pays ne porte que sur les sources web search (Légifrance pour FR, Juridat pour BE).
8. **Maîtrise du coût IA.** Un seul appel Sonnet supplémentaire par dossier (extraction + vérification fusionnées dans un prompt unique), avec prompt caching du system prompt (F-142-04). Web search appelé uniquement sur incertitude Claude — rare. Coût cible ≤ 0,10 €/dossier, à documenter dans la mini-spec SF-179-01.

## Décision finale

**GO avec ajustements.** F-179 est fonctionnellement cohérente : toutes les briques amont (upload, extraction/OCR, analyse IA, source précise F-146) sont livrées, et la sortie est exploitable en aval (section synthèse + alerte F-IA-03). Les ajustements sont : (a) acter que le dev démarre sur un signal terrain indirect — réserve de priorisation hors périmètre de cette skill ; (b) assumer en V1 l'absence de statut markable avocat et l'absence d'alimentation automatique de F-98 (conformes au « Hors scope » de la fiche). Les 8 invariants anti-gadget ci-dessus encadrent la mini-spec.

→ Statut `PRODUCT_SPEC.md` : F-179 passe `Backlog` → `À faire` (consolidation par l'orchestrateur — étape 6).
→ Étape suivante : 0 bis cohérence écran (F-179 a un impact écran : section « Jurisprudences citées » dans la synthèse).

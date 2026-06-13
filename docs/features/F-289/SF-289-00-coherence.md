# F-289 — Cadrage cohérence (étape 0)

> Feature : **Vue d'ensemble du dossier — le journal de pilotage** — un poste de pilotage unique qui réconcilie la vague cycle-de-vie (F-282→286) en répondant à : *« qu'a-t-on fait, où en est ce dossier, que dois-je faire, et quand ? »*
> Skill : `ai-skills/feature-coherence-challenger.md`. Date : 2026-06-13. Origine : signal PO — « la vague cycle-de-vie a ajouté beaucoup de richesse mais dispersée sur 4 onglets ; l'avocat doit avoir une vue globale, belle et robuste, avec accès aux pièces et possibilité d'agir ».
> **Statut git : BROUILLON hors repo (`/tmp/F-289/`) — worktree principal verrouillé par une session parallèle (F-288). À basculer dans `docs/features/F-289/` dès libération.**

## Verdict : **GO** — réconciliation d'une dette d'ergonomie créée par la vague elle-même, sur données 100 % existantes.

---

## Intention métier (1 phrase)

Donner à l'avocat **un écran d'accueil unique du dossier** qui agrège — en lecture seule, sans rien dupliquer — l'histoire réalisée (procédure + production), l'état présent, les échéances à venir, les **actions en attente**, et l'**accès direct aux pièces** de chaque étape, avec la possibilité de faire avancer le dossier sans changer d'onglet.

---

## Workflow métier réel de l'avocat cible

1. **Ouvre un dossier** qu'il n'a pas touché depuis 3 semaines → première question : *« où en étais-je ? »*
2. **Reconstitue le contexte** : quelle phase ? qui a conclu en dernier ? ai-je répondu ? quelles pièces sont arrivées ? l'analyse est-elle à jour ?
3. **Identifie ce qui presse** : prochaine échéance, pièces à réclamer au client, questions de l'app restées sans réponse.
4. **Agit** : rédige la réplique, relance l'analyse, marque une échéance traitée, ouvre une pièce adverse pour la lire.
5. **Repart** en ayant fait avancer le dossier d'un cran.

**Aujourd'hui, les étapes 1-3 obligent à naviguer Dossier → Décision → Suivi et à reconstruire mentalement l'état.** C'est la friction d'activation signalée (Renversez, Mengue) que la vague a involontairement aggravée en enrichissant chaque onglet.

---

## Cartographie des features existantes sur ce workflow

| Étape du workflow | Feature qui la sert aujourd'hui | Où | Problème |
|---|---|---|---|
| « où en est la procédure » | phases F-283, rounds F-282 | onglet Suivi | dispersé |
| « qu'ai-je en main » | intake F-285, vague de pièces F-283 | onglet Dossier | dispersé |
| « qu'a produit l'app » | analyse, stratégie F-286, conclusions | onglets Analyse/Décision | jamais réuni |
| « ce qui presse » | échéancier F-284, deadlines F-69 | onglet Suivi | partiel (pas les pièces manquantes ni les questions IA) |
| « accéder aux pièces d'une étape » | documents (preview/download) | onglet Dossier | aucun lien depuis les rounds/vagues |
| « la to-do du dossier » | — | **nulle part** | **angle mort** |

**Constat : aucun écran ne réunit ces fragments. La donnée existe intégralement (≈9 endpoints GET + actions POST/PATCH déjà livrés) — il manque le point de vue.**

---

## Challenge de cohérence

### Amont (les pré-requis fonctionnels existent-ils ?)
**OUI, intégralement.** Toutes les sources sont livrées et exposées : `/contradictoire-rounds`, `/phases`, `/pieces-wave`, `/echeancier`, `/deadlines`, `/intake`, `/strategy`, `/dashboard` (score avocat F-195), `/case-analysis` (statut, faits, risques, pièces manquantes), `/pieces-manquantes-alignment`, `/ai-questions`, `/conclusions/versions`, `/notes`, documents `/preview` `/download`. Aucune capacité à créer en amont.

### Aval (la sortie est-elle exploitable ?)
**OUI.** La vue d'ensemble est un **point d'entrée et de routage** : chaque élément affiché mène à la zone de travail qui le traite (génération de réplique, relance d'analyse, réponse aux questions, ouverture de pièce). Sa sortie = l'avocat dirigé vers la bonne action. Pas de cul-de-sac.

### Anti-doublon (vérifié)
- **vs Synthèse (`/synthesis`)** : la synthèse traite le **fond** (faits du litige, points de droit, risques). La vue d'ensemble traite le **méta-dossier** (procédure + production + échéances + to-do). **La timeline des FAITS du litige reste dans la synthèse** ; le fil ici ne la duplique pas, il y route.
- **vs Stepper de parcours** : le stepper décrit la progression *de l'outil applicatif* (Intake→Analyse→Outils→Synthèse). La vue d'ensemble décrit l'état *du dossier réel*. Rôles distincts ; en V1 on garde les deux et on clarifie les libellés (absorption du stepper = hors scope).
- **vs Dashboard décisionnel** : la vue en montre un **résumé** (N outils calculés, score avocat), pas la grille — qui reste dans Décision.
- **vs onglets Suivi/Dossier** : la vue **agrège et route** ; elle ne réimplémente aucune logique métier (pur read + navigation). Les frises F-282/F-283 restent dans Suivi (le fil unifié est une lecture transverse, pas un remplacement).

---

## Invariants anti-gadget (que la mini-spec DEVRA respecter)

1. **Aucune logique métier dupliquée** : la vue lit les sources existantes et route ; elle ne recalcule ni un délai, ni un score, ni un round.
2. **100 % lecture pour l'affichage** : aucune écriture sur les tables outils / visibilité / analyse. Les **actions** déclenchées (générer réplique, relancer analyse, marquer échéance faite, ajouter round/phase/échéance) passent **exclusivement par les endpoints existants**.
3. **Fail-open par source** : une source en erreur masque sa ligne, jamais l'écran entier.
4. **Honnêteté des états vides** (principe F-258) : dossier neuf → invite à qualifier ; aucune invention d'événement, d'échéance ou de pièce.
5. **Accès aux pièces réel** : un événement « porteur de pièces » ouvre les **vrais** documents (endpoints `/preview` `/download` sécurisés, isolation workspace) — pas un placeholder. Lien affiché seulement si la pièce existe (round `sourceDocumentId` nullable → dégradation gracieuse).
6. **Le bloc « attention » doit avoir un effet** : chaque ligne mène à une action concrète qui fait avancer le dossier (sinon c'est de la décoration).
7. **Anti-surcharge** : voir étape 0 bis (`SF-289-00b`) — impérative, nouvel onglet à fort contenu.

---

## Décision finale

**GO.** La feature comble une dette d'ergonomie **créée par la vague cycle-de-vie elle-même** (richesse dispersée), sur des données et actions **intégralement existantes** (risque backend faible : un agrégateur lecture seule, aucune nouvelle table, aucun LLM). Elle se distingue nettement de la synthèse (fond), du stepper (parcours outil) et du dashboard (verdicts).

**Conditions (portées par les invariants 1-7) :** lecture seule pour l'affichage, fail-open, états vides honnêtes, accès pièces réel, bloc attention actionnable, **étape 0 bis anti-surcharge obligatoire avant la mini-spec.**

→ PRODUCT_SPEC : F-289 `Backlog` → `À faire` (sur GO), puis étape 0 bis, puis mini-spec SF-289-01.

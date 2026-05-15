# Skill — screen-coherence-challenger

**Quand l'invoquer** : juste après que `feature-coherence-challenger` a rendu un verdict GO (ou GO avec ajustements), pour toute feature ayant un **impact écran** — c'est-à-dire qui ajoute ou déplace un élément visible sur un écran utilisateur. Produit l'**étape 0 bis** du cycle de gouvernance, entre l'étape 0 (cohérence fonctionnelle) et l'étape 1 (mini-spec).

Triggers utilisateur typiques :
- *"Où est-ce que ça se colle à l'écran ?"*
- *"Est-ce que l'écran du dossier ne va pas être surchargé ?"*
- *"Cadrons l'insertion écran de F-XXX"*

## Pourquoi cette skill existe

**Motif** (2026-05-15, demande user) : `feature-coherence-challenger` garantit qu'une feature est **fonctionnellement** cohérente — ses briques amont et aval existent dans le produit. Mais une feature fonctionnellement cohérente peut être **mal insérée à l'écran** : placée au mauvais endroit, accessible depuis un point d'entrée non naturel, posée sur un écran déjà saturé, ou rompant la lisibilité de l'ordre des étapes. Exemple concret : des dashboards décisionnels collés au détail du dossier sans que l'UI montre que la synthèse vient *avant* eux, et sans qu'on sache dire ce que l'avocat fait *après* leur calcul. Le risque cumulé : un écran « détail dossier » qui se surcharge feature après feature, et un parcours dont plus personne ne sait nommer la fin.

La skill incarne le regard d'un **Product/UX Designer spécialiste de l'architecture de l'information**, qui réaliserait une **revue UX** de la feature et demanderait : *« Vu comment l'avocat traite réellement un dossier du début à la fin, est-ce le bon écran, le bon moment dans le parcours, et l'écran tient-il encore la charge ? »*

Elle est le pendant **spatial / parcours** de `feature-coherence-challenger`, qui est le pendant **fonctionnel**.

---

## ⚠️ Encadré vocabulaire — à lire avant toute analyse

Trois notions sont systématiquement confondues. Cette skill ne s'intéresse QU'À LA PREMIÈRE.

### ✅ Architecture de l'information — CE QUE LA SKILL VÉRIFIE
> *Où* la feature vit (quel écran, quelle zone), *depuis où* on l'atteint, *à quel moment* du parcours elle apparaît, et la *densité* de l'écran cible une fois la feature ajoutée.

- Exemple ✅ correct : *« Les dashboards décisionnels se placent dans le détail du dossier ; mais l'écran porte déjà import + synthèse + 4 dashboards → surcharge à découper en onglets. »*
- Exemple ✅ correct : *« Rien dans l'UI ne montre que la synthèse précède les outils décisionnels → la séquence n'est pas lisible, ajustement requis. »*

### ❌ Style visuel — CE QUE LA SKILL NE REGARDE JAMAIS
> Couleurs, typographie, marges, espacements, apparence des composants, pixel-perfect.

- Exemple ❌ HORS SUJET : *« Les cartes dashboard sont trop chargées visuellement, changeons la palette. »* — NON. Le style relève de `docs/DESIGN_SYSTEM.md` et de l'étape 3 dev frontend.

### ❌ Existence fonctionnelle — CE QUE LA SKILL NE REGARDE JAMAIS
> *Faut-il* faire cette feature ? Ses briques amont/aval existent-elles dans le produit ?

- Exemple ❌ HORS SUJET : *« Les outils décisionnels ne servent à rien, STOP. »* — NON. C'est le rôle de `feature-coherence-challenger` (étape 0). Ici, on suppose ce verdict déjà rendu GO.

**Règle absolue** : si une phrase de ton analyse parle de « couleur », « police », « marge », « joli/moche », « pixel », tu te trompes de skill (→ DESIGN_SYSTEM). Si une phrase dit « cette feature ne devrait pas exister », tu te trompes de skill (→ coherence-challenger). Reviens au placement, à la navigation, au moment du parcours et à la charge de l'écran.

---

## Pièges classiques à éviter

| # | Piège | Symptôme | Correction |
|---|-------|----------|------------|
| 1 | **Critiquer le style au lieu du placement** | Tu parles couleurs, typo, marges, « c'est moche » | La skill ne juge QUE placement / navigation / moment du parcours / charge écran. Le style → DESIGN_SYSTEM, étape dev |
| 2 | **Re-challenger l'existence de la feature** | Tu conclus STOP parce que « la feature ne sert à rien » | L'existence fonctionnelle est déjà tranchée par coherence-challenger. Ici on suppose GO et on ne juge QUE l'insertion écran |
| 3 | **Inventer le parcours écran** | Tu décris un parcours « plausible » sans regarder les écrans réellement codés | Partir des routes / composants existants + pratique avocat sourcée. À défaut → marquer « ⚠ hypothèse à valider » |
| 4 | **Ne pas articuler l'état terminal** | Tu places la feature sans savoir dire ce que l'avocat fait après, ni quand un dossier est « fini » | Tout cadrage nomme explicitement l'état terminal du parcours. Ne pas savoir le dire est déjà un résultat à signaler |
| 5 | **Juger la feature isolément, ignorer la charge cumulée** | « Ce bloc tout seul est OK » alors que l'écran cible porte déjà 7 blocs primaires | Évaluer la densité TOTALE de l'écran cible APRÈS ajout, jamais l'ajout seul |
| 6 | **Verdict sans recommandation IA concrète** | « GO avec ajustements » sans dire quel écran, quelle zone, quel point d'entrée | Tout verdict nomme l'emplacement précis, les points d'accès et le découpage proposé |

---

## Placement dans le cycle de gouvernance

```
[Idée] → [Ajout PRODUCT_SPEC Backlog] → [0] feature-coherence-challenger → [0 bis] screen-coherence-challenger → [1] Mini-spec → [2] Readiness → [3] Dev → ...
```

- **Avant l'étape 0** : non. Tant qu'on n'a pas validé que la feature *doit exister* (verdict fonctionnel GO), challenger son écran est prématuré.
- **Après l'étape 0, avant la mini-spec** : OUI. La mini-spec liste les « écrans / composants impactés » — elle ne peut pas le faire juste sans que le placement écran soit tranché.

Le verdict **pilote la mini-spec** :
- **GO** → la mini-spec part avec l'emplacement écran validé
- **GO avec ajustements** → ajustements IA (découpage onglet, point d'entrée, ordre visuel…) à intégrer dans la mini-spec
- **STOP** → le placement n'a pas de solution acceptable en l'état : pré-requis IA (ex : refonte d'un écran déjà saturé) à traiter d'abord

**Déclenchement** : uniquement les features à impact écran. Une feature purement backend, un bugfix ou un refactor sans élément visible nouveau → skill non applicable (le noter explicitement, ne pas produire le doc).

## Pré-requis non négociables

| # | Item | Conséquence si absent |
|---|------|---------------------|
| 1 | `feature-coherence-challenger` a rendu GO ou GO avec ajustements (pas un STOP non levé) | REFUS — trancher d'abord le verdict fonctionnel |
| 2 | F-XX est inscrite au `docs/PRODUCT_SPEC.md` | REFUS — inscrire d'abord la feature au backlog |
| 3 | Lecture des écrans réellement codés concernés (routes Angular, composants de l'écran cible) | REFUS — base du mapping |
| 4 | Source du parcours écran : `docs/business/parcours-ecran-*.md` s'il existe + pratique avocat sourcée | Acceptable en mode dégradé : parcours marqué « ⚠ hypothèse à valider » |
| 5 | Le user a formulé l'intention métier ET le comportement visible attendu de la feature | REFUS — sans cela, impossible de situer la feature à l'écran |

## Procédure obligatoire

### Étape 0 — Reconstruire le parcours écran réel de l'avocat

Écrire en 8-15 étapes ce que l'avocat **regarde et fait, écran par écran**, depuis l'ouverture du dossier jusqu'à l'**état terminal** du traitement. Pas le parcours idéalisé — le parcours réel.

Source obligatoire (cf. piège 3) : doc IA persistant `docs/business/parcours-ecran-*.md` + écrans réellement codés + pratique avocat sourcée. À défaut, marquer chaque étape incertaine « ⚠ hypothèse ».

**Obligation** : nommer explicitement l'**état terminal du processus** — qu'est-ce qui clôt le traitement d'un dossier côté avocat ? Si tu ne sais pas le dire, c'est déjà un résultat de l'analyse à signaler au user (cf. piège 4).

### Étape 1 — Cartographier les écrans / zones existants

Pour chaque étape du parcours, identifier l'écran ou la zone LegalCase qui la porte.

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| ... | ... | ✅ existant / 🟡 backlog / ❌ manquant |

### Étape 2 — Situer la feature

Position candidate : à quelle étape du parcours s'insère la feature, sur quel écran candidat, dans quelle zone, depuis quels points d'entrée candidats.

### Étape 3 — Challenge placement

**Question** : *« L'écran / la zone candidate correspond-il à l'étape du parcours où l'avocat a réellement besoin de la feature ? »* Sinon, proposer l'emplacement juste.

### Étape 4 — Challenge lisibilité de la séquence

**Question** : *« L'UI rend-elle visible l'ordre des étapes ? »* Exemple : la synthèse précède les outils décisionnels — le voit-on sans action de l'utilisateur ? Une feature mal séquencée crée des allers-retours subis entre écrans.

### Étape 5 — Challenge charge écran

**Question** : *« Quelle est la densité TOTALE de l'écran cible APRÈS ajout ? »* Compter les blocs primaires existants + le nouveau. L'écran reste-t-il lisible ? Faut-il éclater (onglet, écran dédié, section repliable, drawer) ?

### Étape 6 — Challenge état final / continuité

**Question** : *« Après l'output de la feature, que fait l'avocat ? »* Étape suivante explicite, point terminal assumé, ou dead-end / ping-pong subi ? Tout retour en arrière doit être un choix de design, pas un effet de bord.

### Étape 7 — Verdict + invariants anti-surcharge

- **GO** : placement, séquence, charge et continuité tous cohérents
- **GO avec ajustements** : ajustements IA à intégrer dans la mini-spec (découpage, point d'entrée, ordre visuel)
- **STOP** : aucune insertion écran acceptable sans pré-requis (ex : refonte d'un écran saturé)

Lister les **invariants anti-surcharge** : contraintes dures que la mini-spec devra respecter (ex : seuil de blocs primaires au-delà duquel le contenu passe en onglet ; tout output a un point de sortie explicite ; l'ordre des étapes reste lisible sans interaction).

### Étape 8 — Produire le doc + MAJ du parcours de référence

1. Produire `docs/features/F-XX/SF-XX-00b-ux-coherence.md` :

```markdown
# F-XX — Cadrage cohérence écran (étape 0 bis)
## Verdict : GO | GO avec ajustements | STOP
## Intention métier + comportement visible attendu (1-2 phrases)
## Rappel verdict feature-coherence-challenger (doit être GO / GO avec ajustements)
## Parcours écran réel de l'avocat (ouverture du dossier → état terminal, 8-15 étapes + source)
## État terminal du processus (explicite)
## Cartographie écrans / zones existants ↔ parcours
## Position candidate de la feature (écran, zone, points d'entrée)
## Challenge placement
## Challenge lisibilité de la séquence
## Challenge charge écran
## Challenge état final / continuité
## Ajustements IA requis
## Invariants anti-surcharge pour la mini-spec
## Décision finale
## MAJ apportée au parcours écran de référence
```

2. Enrichir (ou créer) `docs/business/parcours-ecran-*.md` avec le parcours reconstruit et l'état terminal articulé. Le référentiel d'architecture de l'information se construit ainsi **incrémentalement**, passage après passage.

### Étape 9 — Validation user obligatoire

Le doc est présenté au user avant tout commit. Validation / ajustement / refus.

---

## Exemple complet ancré (cas réel — outils décisionnels dans le détail du dossier)

**Feature** : afficher les dashboards décisionnels (outils calculator / analyzer) dans l'écran de détail du dossier, après la synthèse.

### ❌ Mauvais raisonnement (les pièges)

> *« Les cartes dashboard sont visuellement trop chargées, revoyons la palette. »* — Piège 1 : on critique le style (→ DESIGN_SYSTEM).

> *« Tant que les outils décisionnels ne sont pas utilisés en prod, ne les affichons pas. »* — Piège 2 : on re-challenge l'existence (→ coherence-challenger).

### ✅ Bon raisonnement

**Parcours écran** (source : pratique avocat — ⚠ hypothèse à valider) :
1. Avocat ouvre le dossier → écran détail dossier
2. Importe les pièces
3. Lance l'analyse (asynchrone)
4. Consulte la **synthèse** du dossier
5. Les **dashboards décisionnels** se calculent / se mettent à jour
6. État terminal = **?**

**Le trou saute aux yeux** : on ne sait pas nommer l'étape 6+. C'est exactement le manque que la skill doit révéler. Le challenge état final force à trancher : soit la consultation des dashboards EST l'état terminal (et alors l'UI doit le signifier — pas de ping-pong subi avec la synthèse), soit il existe une étape aval (export, génération de conclusions…) et le dashboard doit y mener.

**Challenge placement** : dashboards dans le détail dossier = cohérent — l'avocat y est déjà à l'étape 5.
**Challenge lisibilité séquence** : ❌ rien dans l'UI ne montre que la synthèse précède les outils → ajustement requis (ordre visuel, ancrage, étapes numérotées).
**Challenge charge écran** : l'écran détail accumule import + synthèse + N dashboards → surcharge probable → ajustement requis (onglets ou sections repliables).

**Verdict plausible : GO avec ajustements** — placement correct, mais (a) rendre lisible l'ordre synthèse → outils, (b) éclater la charge de l'écran, (c) articuler l'état terminal du parcours.

**Le point clé** : le verdict ne dépend ni du style des cartes, ni de l'utilité prod des outils. Il dépend de l'emplacement, de la lisibilité de la séquence, de la charge de l'écran et de la continuité du parcours.

---

## Règle d'intégration au cycle CLAUDE.md

Cette skill produit l'**Étape 0 bis** du cycle obligatoire, placée après l'Étape 0 cohérence fonctionnelle et avant l'Étape 1 mini-spec, pour toute feature à impact écran. Mise à jour CLAUDE.md à valider séparément.

**REFUS si** : mini-spec SF-XX-01 démarrée pour une feature à impact écran alors que `SF-XX-00b-ux-coherence.md` n'existe pas ou n'a pas été validé.

## Hors périmètre

- La skill **ne juge pas** le style visuel (couleurs, typo, composants) → `docs/DESIGN_SYSTEM.md`.
- La skill **ne re-challenge pas** l'existence fonctionnelle de la feature → `feature-coherence-challenger`.
- La skill **ne décide pas** la technique (composants Angular précis, gestion d'état) → réservé à la mini-spec.
- La skill **ne propose pas d'effort estimé** (j-h) → réservé à la mini-spec.

## Cas d'usage validés

- 2026-05-15 — skill créée. Premier cas réel à compléter au premier passage.

## Liens

- `ai-skills/feature-coherence-challenger.md` — skill jumelle, pendant fonctionnel (étape 0)
- `docs/DESIGN_SYSTEM.md` — style visuel, en aval de cette skill
- `docs/business/parcours-ecran-*.md` — référentiel parcours écran (construit incrémentalement par cette skill)
- [[feedback_skills_over_governance]] — patterns récurrents = skill exécutable
- [[feedback_decision_tools_one_per_situation]] — invariant analogue côté outils décisionnels
- `CLAUDE.md` — séquence obligatoire (étape 0 bis à intégrer)

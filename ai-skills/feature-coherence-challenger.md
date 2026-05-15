# Skill — feature-coherence-challenger

**Quand l'invoquer** : au moment où l'on décide de développer une feature déjà inscrite au `PRODUCT_SPEC.md` en statut `Backlog`, et qui touche un workflow utilisateur (toutes sauf bugfix / petits refactors). Produit l'**étape 0** du cycle de gouvernance, avant l'étape 1 mini-spec.

Triggers utilisateur typiques :
- *"Cadrons la cohérence de F-XXX avant de coder"*
- *"Est-ce que ça a du sens à ce stade ?"*
- *"Vérifie que ce n'est pas un gadget"*

## Pourquoi cette skill existe

**Motif** (2026-05-14, demande user) : à mesure que LegalCase grandit, le risque de gadgétiser augmente. Une feature peut paraître pertinente isolément mais être incohérente avec le **workflow métier réel de l'utilisateur cible** (un avocat) — par exemple proposer une génération de conclusions alors que la fonctionnalité « analyse de dossier » n'existe pas dans le produit, ou proposer un export Word alors qu'il n'y a pas de système de numérotation des pièces. Ce skill détecte ces incohérences fonctionnelles **avant** le dev.

La skill incarne le regard d'un **avocat senior qui auditerait le produit** et demanderait : *« Est-ce que ça a du sens de construire ça maintenant, vu comment je travaille réellement dans mon cabinet ? »*

---

## ⚠️ Encadré vocabulaire — à lire avant toute analyse

Deux notions sont systématiquement confondues. Cette skill ne s'intéresse QU'À LA PREMIÈRE.

### ✅ Existence fonctionnelle — CE QUE LA SKILL VÉRIFIE
> La fonctionnalité **existe-t-elle dans le produit** (livrée OU inscrite au backlog `PRODUCT_SPEC.md`) ?

- Exemple ✅ correct : *« La feature "analyse de dossier" F-3/F-4/F-5 est inscrite et Terminée au PRODUCT_SPEC → la brique amont des Conclusions existe fonctionnellement. »*
- Exemple ✅ correct : *« Il n'existe aucune feature "numérotation des pièces" ni au backlog ni livrée → trou fonctionnel amont. »*

### ❌ Usage en production — CE QUE LA SKILL NE REGARDE JAMAIS
> Combien de fois la fonctionnalité a-t-elle été utilisée / exécutée en prod ?

- Exemple ❌ HORS SUJET : *« 0 analyses ont tourné en prod ces 7 jours → STOP. »* — NON. L'usage prod est un sujet marketing/adoption distinct. Une feature peu utilisée reste fonctionnellement existante.
- Exemple ❌ HORS SUJET : *« Le pipeline IA n'a jamais été mené à terme par un vrai client → input incertain. »* — NON, ce n'est pas le rôle de cette skill.

**Règle absolue** : si une phrase de ton analyse contient « en prod », « utilisé », « X fois », « 7 derniers jours », « mesure », tu es en train de te tromper de skill. Reviens à l'existence fonctionnelle.

---

## Pièges classiques à éviter

| # | Piège | Symptôme | Correction |
|---|-------|----------|------------|
| 1 | **Confondre existence fonctionnelle et usage prod** | Tu vas mesurer des compteurs en base de données, citer « 0 analyses 7j » | Ne regarde QUE le PRODUCT_SPEC : la feature y est-elle inscrite (livrée ou backlog) ? |
| 2 | **Confondre cohérence métier et stabilité technique** | Tu vas parler de bugs, latence, monitoring, robustesse du pipeline | La skill ne juge pas la qualité technique. Une feature peut être buguée ET fonctionnellement cohérente |
| 3 | **Inventer le workflow métier au lieu de le vérifier** | Tu écris un workflow avocat « plausible » sorti de ton imagination | Le workflow doit venir d'une source : avocat consulté, doc `docs/business/workflow-*.md`, ou signal terrain documenté. Si aucune source → le signaler explicitement comme hypothèse à valider |
| 4 | **Confondre challenge amont et challenge aval** | Tu listes des features « manquantes » sans dire si elles viennent avant ou après dans le workflow | Amont = ce que le métier exige AVANT la feature. Aval = ce qui exploite sa sortie APRÈS. Un trou amont est bloquant ; un trou aval est souvent juste un export à prévoir |
| 5 | **Verdict GO/STOP sans lister les pré-requis concrets** | Tu conclus « STOP » sans dire quelle feature créer pour débloquer | Tout STOP doit nommer la (les) feature(s) pré-requise(s) à ajouter au backlog d'abord |

---

## Placement dans le cycle de gouvernance

Le `PRODUCT_SPEC.md` a 2 rôles : registre d'idées (statut `Backlog`) ET plan de dev actif (`À faire` / `En cours` / `Terminée`).

La skill intervient **au moment de la transition** « idée backlog » → « on développe » :

```
[Idée de feature] → [Ajout PRODUCT_SPEC en statut Backlog] → [0] feature-coherence-challenger → [1] Mini-spec → [2] Readiness → [3] Dev → ...
```

- **Avant l'ajout au PRODUCT_SPEC** : non. On veut pouvoir tout tracer au backlog sans rien oublier — l'inscription en `Backlog` est gratuite et doit rester libre.
- **Après l'ajout, avant la mini-spec** : OUI. C'est là que le coût d'une erreur devient réel (on s'apprête à investir du dev).

Le verdict de la skill **pilote le statut PRODUCT_SPEC** :
- **GO** → la feature passe `Backlog` → `À faire`, on enchaîne la mini-spec
- **GO avec ajustements** → pré-requis ajoutés au backlog d'abord, puis dev
- **STOP** → la feature reste `Backlog` ou passe `Bloqué`, avec les pré-requis listés

## Pré-requis non négociables

| # | Item | Conséquence si absent |
|---|------|---------------------|
| 1 | F-XX est déjà inscrite au `PRODUCT_SPEC.md` (au moins en statut `Backlog`) | REFUS — inscrire d'abord la feature au backlog |
| 2 | Lecture de `docs/PRODUCT_SPEC.md` (features livrées + backlog) | REFUS — base du mapping |
| 3 | Source du workflow métier de l'utilisateur cible (avocat consulté, doc `docs/business/`, signal terrain) | Acceptable en mode dégradé : workflow marqué « ⚠ hypothèse à valider » |
| 4 | Le user a formulé l'intention métier de la feature en français courant | REFUS — sans intention claire, impossible de challenger |

## Procédure obligatoire

### Étape 0 — Reconstruire le workflow métier réel

Écrire en 8-15 étapes ce que fait l'utilisateur cible **dans son métier** autour de la feature. Pas le workflow dans l'app — le workflow dans la **vraie vie du cabinet**.

Source obligatoire (cf. piège 3) : avocat consulté, doc `docs/business/workflow-*.md`, ou signal terrain documenté. À défaut, marquer chaque étape incertaine « ⚠ hypothèse ».

### Étape 1 — Cartographier les features existantes sur ces étapes

Pour chaque étape du workflow métier, identifier la (les) feature(s) LegalCase qui la couvre(nt) :
- ✅ **Livrée** (statut Terminée au PRODUCT_SPEC)
- 🟡 **Backlog** (inscrite au PRODUCT_SPEC, pas encore livrée)
- ❌ **Manquante** (ni livrée, ni au backlog)

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| ... | ... | ✅ / 🟡 / ❌ |

### Étape 2 — Situer la nouvelle feature

Marquer dans le tableau l'étape exacte où s'insère la feature challengée.

### Étape 3 — Challenge amont

**Question** : *« Chaque étape AVANT la feature dans le workflow métier est-elle couverte par une feature du produit (livrée OU backlog) ? »*

Pour chaque trou ❌ amont :
- Soit la brique n'est pas réellement nécessaire (à argumenter — l'avocat le fait peut-être hors outil)
- Soit elle doit être **ajoutée au backlog comme pré-requis** avant la feature challengée

### Étape 4 — Challenge aval

**Question** : *« La sortie de la feature est-elle exploitable par les étapes AVAL du workflow métier ? »*

Pour chaque trou ❌ aval :
- Soit l'étape aval est hors périmètre LegalCase (OK si export prévu)
- Soit elle doit être prévue (au moins au backlog) pour que la feature serve à quelque chose

### Étape 5 — Verdict + invariants anti-gadget

- **GO** : pré-requis amont tous livrés ou au backlog avec séquence acceptable
- **GO avec ajustements** : ajouts backlog requis avant SF-01
- **STOP** : trou fonctionnel majeur amont/aval rendant la feature inopérante

Lister les **invariants anti-gadget** : contraintes dures de la mini-spec qui empêchent l'abandon (ex : si pièces uploadées, citation pièces obligatoire ; si input modifiable, re-génération prévue).

### Étape 6 — Produire `docs/features/F-XX/SF-XX-00-coherence.md`

```markdown
# F-XX — Cadrage cohérence (étape 0)
## Verdict : GO | GO avec ajustements | STOP
## Intention métier (1 phrase)
## Workflow métier réel de l'utilisateur cible (8-15 étapes, + source)
## Cartographie features actuelles ↔ workflow
## Position de la nouvelle feature
## Challenge amont
## Challenge aval
## STOPs / pré-requis à ajouter au backlog
## Invariants anti-gadget pour la mini-spec
## Décision finale
```

### Étape 7 — Validation user obligatoire

Le doc est présenté au user avant tout commit. Validation / ajustement / refus.

---

## Exemple complet ancré (cas fictif — pour cadrer le raisonnement)

**Feature fictive** : « F-999 — Relance automatique du client pour les pièces manquantes » (envoyer un mail au client pour réclamer les documents absents du dossier).

### ❌ Mauvais raisonnement (le piège)

> *« Regardons combien de relances ont été envoyées en prod ces 7 derniers jours. 0 → la feature ne sert à rien, STOP. »*

C'est faux. On a mesuré l'usage prod (piège 1). On n'a rien dit de la cohérence fonctionnelle.

### ✅ Bon raisonnement

**Workflow métier** (source : pratique standard avocat) :
1. Avocat reçoit le dossier + premières pièces du client
2. Avocat identifie les pièces manquantes nécessaires à la défense
3. Avocat relance le client pour obtenir ces pièces
4. Client envoie les pièces → dossier complété
5. Avocat poursuit l'analyse

**Cartographie** :
| Étape métier | Feature LegalCase | Statut |
|---|---|---|
| 1. Réception dossier + pièces | F-43 import dossier | ✅ Livrée |
| 2. **Identification pièces manquantes** | F-XX détection pièces manquantes | ❓ à vérifier au PRODUCT_SPEC |
| 3. Relance client | **F-999 (la feature challengée)** | — |

**Challenge amont** : F-999 (relance) suppose qu'on sache QUELLES pièces manquent. Donc l'étape 2 « identification des pièces manquantes » doit exister dans le produit.
- Si une feature « détection pièces manquantes » est livrée ou au backlog → **GO** : F-999 a une brique amont sur quoi s'appuyer.
- Si AUCUNE feature ne détecte les pièces manquantes → **STOP** : on ne peut pas relancer pour des pièces qu'on n'a pas identifiées. Pré-requis : créer d'abord « F-XX détection pièces manquantes ».

**Le point clé** : le verdict ne dépend PAS de combien de fois la relance a tourné. Il dépend de l'existence fonctionnelle de la brique amont « détection pièces manquantes » dans le produit.

---

## Règle d'intégration au cycle CLAUDE.md

Cette skill produit l'**Étape 0** du cycle obligatoire, placée après l'ajout au PRODUCT_SPEC (statut `Backlog`) et avant l'Étape 1 Mini-spec. Mise à jour CLAUDE.md à valider séparément.

**REFUS si** : mini-spec SF-XX-01 démarrée alors que `SF-XX-00-coherence.md` n'existe pas ou n'a pas été validé.

## Hors périmètre

- La skill **ne mesure pas** l'usage prod (existence fonctionnelle uniquement).
- La skill **ne juge pas** la stabilité technique (bugs, latence).
- La skill **ne décide pas** la technique (stack, schéma DB) — réservé à la mini-spec.
- La skill **ne propose pas d'effort estimé** (j-h) — réservé à la mini-spec.

## Cas d'usage validés

- **2026-05-14/15 — F-243 Conclusions** (premier cas réel) : voir `docs/features/F-243/SF-243-00-coherence.md`.

## Liens

- [[feedback_skills_over_governance]] — patterns récurrents = skill exécutable
- [[feedback_decision_tools_one_per_situation]] — analogue côté outils décisionnels
- `CLAUDE.md` — séquence obligatoire (étape 0 à intégrer)
- `docs/business/workflow-*.md` — référentiels métier par domaine (à constituer si manquant)

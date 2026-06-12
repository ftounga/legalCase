# SF-284-00 — Cadrage cohérence fonctionnelle (étape 0)

> Feature : **F-284 — Échéancier procédural proactif & alertes**
> Skill : `ai-skills/feature-coherence-challenger.md`
> Date : 2026-06-12 · Verdict : **GO avec ajustements**

## 1. Workflow métier réel de l'avocat cible

L'avocat ne « consulte » pas un dossier : il **pilote un calendrier**. Sa hantise n°1 est le
délai forclos (prescription, délai de recours/appel, mise en état, RPVA). Un délai raté =
responsabilité civile professionnelle engagée. Concrètement :

1. Il ouvre LegalCase plusieurs fois par semaine **non pas pour relire** mais pour vérifier
   « qu'est-ce qui tombe bientôt, sur quel dossier ? ».
2. Sur un dossier donné, il veut voir **le prochain couperet** d'un coup d'œil, pas fouiller.
3. Quand un délai approche, il veut une **alerte** (déjà couverte par F-69 SF-69-03 : mail J-15/J-7).

La couche manquante n'est pas la donnée (F-69 stocke déjà les délais) ni l'alerte mail (existe) :
c'est la **mise en avant proactive à l'écran** — transformer une liste passive enfouie en bas de
l'onglet Suivi en un **échéancier en tête**, hiérarchisé par urgence, qui donne une raison
récurrente de revenir.

## 2. Cartographie des features existantes sur ce workflow

| Étape workflow | Feature produit | État |
|---|---|---|
| Stocker un délai (manuel / IA / statutaire) | **F-69** (`case_deadlines`, CRUD, sources MANUAL/AI/STATUTORY) | ✅ existe |
| Détecter les délais à l'analyse | F-69 SF-69-01/05 (`createAiDetectedDeadlines`, référentiels) | ✅ existe |
| Alerter par mail J-15/J-7 | F-69 SF-69-03 (`DeadlineAlertService @Scheduled`) | ✅ existe |
| Échéances de réponse des rounds contradictoires | **F-282** (`contradictoire_rounds.response_due_at`) | ✅ existe (non agrégé aux délais) |
| Signal cabinet « délais urgents » | Dashboard (`buildUrgentDeadlines`, workspace-wide) | ✅ existe |
| **Échéancier proactif à l'écran, hiérarchisé par urgence, en tête de Suivi** | — | ❌ **trou = F-284** |

## 3. Challenge cohérence amont / aval

**Amont (pré-requis fonctionnels)** : tous présents. Les délais existent en base (F-69), les
échéances de réponse existent (F-282), l'auth/workspace est résolue (`CaseDeadlineService`). F-284
n'invente aucune donnée : il **agrège et met en scène** l'existant.

**Aval (sortie exploitable)** : l'échéancier pointe vers les actions déjà offertes — valider un
délai IA (F-69), répondre à un round (F-282), ajouter un délai manuel (F-69). Pas de cul-de-sac.

**Risque doublon** : écarté. Le dashboard agrège **au niveau cabinet** (tous dossiers) ; F-284
agrège **au niveau dossier** et le met **en tête de l'onglet Suivi**, là où l'avocat pilote ce
dossier précis. La liste F-69 reste (édition CRUD détaillée) ; F-284 la **coiffe** d'une vue de
pilotage, comme la frise F-283 coiffe le contradictoire F-282. Pattern de superposition cohérent
avec le reste de l'onglet Suivi.

## 4. Invariants anti-gadget (à respecter par la mini-spec)

- **INV-1** : zéro nouvelle donnée saisissable propre à F-284. L'échéancier est une **vue de
  lecture** sur `case_deadlines` + `contradictoire_rounds.response_due_at`. Les actions renvoient
  aux composants existants.
- **INV-2** : ne pas dupliquer la liste F-69. F-284 = vue de pilotage (hero + top urgences) ;
  l'édition fine reste dans `case-deadlines-section`.
- **INV-3** : hiérarchisation par urgence réelle (jours restants), pas un simple tri date.
  Dépassé > J-7 > J-15 > à venis. Couleurs sémantiques.
- **INV-4** : ne pas réinventer l'alerte mail (F-69 SF-69-03 suffit). F-284 = couche écran.
- **INV-5** : agrégation **read-only** côté backend — aucune mutation, aucune nouvelle table.

## 5. Verdict

**GO avec ajustements.** F-284 comble un trou réel (mise en avant proactive) sans doublonner F-69
(données/alerte) ni le dashboard (niveau cabinet). Ajustement imposé : rester une **couche de
lecture/mise en scène** (INV-1/INV-5), placée **en tête de l'onglet Suivi**, qui agrège aussi les
échéances de réponse F-282 — apport net vs la liste passive actuelle.

→ PRODUCT_SPEC : `Backlog` → `À faire`.

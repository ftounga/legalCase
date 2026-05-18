# F-179 — Cadrage cohérence écran (étape 0 bis)

> Produit via la skill `ai-skills/screen-coherence-challenger.md`. Étape 0 bis du cycle de gouvernance — placée après l'étape 0 (cohérence fonctionnelle, verdict GO avec ajustements) et avant la mini-spec.

## Verdict : **GO avec ajustements**

## Intention métier + comportement visible attendu

L'avocat doit voir, sur la page de synthèse du dossier, une **section « Jurisprudences citées »** listant les références jurisprudentielles détectées dans les documents uploadés (groupées par document), chacune avec un badge de statut (`✅ Vérifiée` / `⚠️ Suspecte` / `❌ Non trouvée` / `❓ Incertaine`), une explication courte et, quand disponible, un lien source cliquable.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO avec ajustements** — toutes les briques amont sont livrées ; ajustements = signal terrain indirect acté, absence V1 de statut markable et d'alimentation F-98 assumées. Cf. `SF-179-00-coherence.md`.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel maintenu) + écrans réellement codés (`case-file-detail.component`, `synthesis.component`). Le détail du dossier est structuré en 4 onglets depuis F-244 ; la **synthèse est un écran dédié** (route `/case-files/:id/synthesis`) atteint depuis l'onglet Analyse.

1. L'avocat ouvre le dossier → écran **détail du dossier**, 4 onglets.
2. Onglet **Dossier** : il importe les pièces (dont les **conclusions adverses**).
3. Onglet **Analyse** : il lance le pipeline IA asynchrone, puis ouvre la **synthèse** du dossier.
4. **Écran synthèse** (`SynthesisComponent`) : il consulte, dans un `mat-accordion` de panneaux, la chronologie, les faits, les points juridiques, les risques, les questions ouvertes, les pièces manquantes, les indemnités estimées, les pistes stratégiques, la checklist procédurale.
5. Pendant le streaming (F-185), les sections s'affichent au fil de l'eau ; un bandeau de progression indique les sections reçues.
6. L'avocat lit la synthèse pour comprendre les forces / faiblesses du dossier — **dont la thèse adverse** telle qu'extraite des conclusions adverses uploadées.
7. Il revient au détail, onglet **Décision** : il remplit les outils décisionnels, consulte le tableau de bord décisionnel.
8. Il génère le **projet de conclusions** (F-98, onglet Décision).
9. Il relit, copie, finalise sa réplique dans son traitement de texte.
10. Onglet **Suivi** : échéances, notes, calendrier procédural.
11. **État terminal** : projet de conclusions généré.

## État terminal du processus (explicite)

✅ Déjà tranché par le référentiel `parcours-ecran-dossier.md` (cadrage F-98, 2026-05-18) : l'état terminal du traitement métier = **« projet de conclusions généré »** (`app-conclusions-section`, onglet Décision). F-179 **n'introduit pas de nouvel état terminal** : c'est une brique de **consultation** intégrée à l'écran de synthèse (étape 4-6 du parcours), en amont de l'état terminal. Après avoir consulté la section « Jurisprudences citées », l'avocat poursuit son parcours normal vers les outils décisionnels puis les conclusions — pas de dead-end.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 1-2. Détail dossier, import pièces | `case-file-detail` onglet Dossier, `docs-card` | ✅ existant |
| 3. Lancement analyse | `app-analysis-pipeline` onglet Analyse | ✅ existant |
| 4. Écran synthèse — `mat-accordion` de panneaux | `SynthesisComponent` | ✅ existant |
| 4. Panneaux : timeline, faits, points juridiques, risques, questions, pièces manquantes, indemnités, pistes, checklist | panneaux `mat-expansion-panel` de `SynthesisComponent` | ✅ existant |
| 5. Streaming sections au fil de l'eau | bandeau `streaming-callout` + `STREAMING_EXPECTED_SECTIONS` | ✅ existant |
| **4bis. Section « Jurisprudences citées »** | **F-179 SF-179-03 (la feature challengée)** | 🟡 backlog |
| 6bis. Alerte cohérence sur arrêt SUSPECT | popover F-IA-03 `app-coherence-popover-trigger` | ✅ existant (intégration SF-179-04) |
| 7-8. Outils décisionnels, conclusions | onglet Décision | ✅ existant |

## Position candidate de la feature

- **Écran** : `SynthesisComponent` (écran synthèse du dossier) — confirmé par la fiche F-179.
- **Zone** : un **nouveau `mat-expansion-panel`** dans le `mat-accordion` principal `synthesis-accordion`, rendu via un composant standalone dédié `app-jurisprudence-citations-section` (pattern miroir `app-divorce-consentement-scoring-section`, `app-immigration-events-section` — sections métier déjà injectées dans le même accordéon).
- **Point d'entrée** : (a) panneau visible directement dans le flux de lecture de la synthèse ; (b) optionnellement un **badge dans la grille `synthesis-grid`** (F-162) si des références ont été détectées, pour la navigation rapide.
- **Ordre dans l'accordéon** : après « Risques » et avant « Questions ouvertes ». Justification : la vérification de jurisprudence adverse est une information à charge/décharge stratégique, naturellement lue dans la continuité des risques du dossier.

## Challenge placement

**Question** : l'écran synthèse correspond-il à l'étape du parcours où l'avocat a besoin de la feature ?

✅ **Oui.** L'avocat lit la thèse adverse à l'étape 4-6, sur l'écran synthèse. La jurisprudence citée par l'adverse est extraite des mêmes documents que ceux analysés pour produire la synthèse. Placer la vérification sur un autre écran (ex. onglet Décision) obligerait l'avocat à un aller-retour : il découvrirait la thèse adverse sur la synthèse mais devrait changer d'écran pour en vérifier les sources. Le placement sur la synthèse est le bon.

## Challenge lisibilité de la séquence

**Question** : l'UI rend-elle visible l'ordre des étapes ?

✅ **Oui, sans ajustement structurel.** La section « Jurisprudences citées » est un panneau parmi d'autres dans l'accordéon de synthèse. L'avocat sait que la synthèse vient APRÈS l'analyse (séquence déjà lisible : il a lancé le pipeline à l'étape 3). La section F-179 ne casse aucune séquence : c'est une information dérivée des documents, au même niveau que les faits ou les risques.

⚠ **Un ajustement de lisibilité interne** : quand le dossier ne contient **aucune référence jurisprudentielle détectée**, le panneau ne doit PAS s'afficher vide (cohérent avec les panneaux existants `@if (synthesis()!.faits.length > 0)`). Quand des références sont détectées mais que **toutes** sont `UNCERTAIN` (web search en échec global), la section doit l'expliciter — pas de page blanche anxiogène. Invariant repris ci-dessous.

## Challenge charge écran

**Question** : densité TOTALE de l'écran synthèse APRÈS ajout ?

L'écran synthèse est un `mat-accordion` qui porte déjà ~9 panneaux conditionnels (timeline, faits, points juridiques, risques, questions, pièces manquantes, indemnités×4, pistes, checklist) + sections métier conditionnelles. L'accordéon est par nature **extensible sans surcharge** : chaque panneau est repliable, n'apparaît que si son contenu existe, et le streaming F-185 affiche progressivement. Ajouter **un panneau de plus** (conditionnel, replié par défaut possible) ne crée pas de surcharge — c'est précisément le pattern d'extension prévu de cet écran.

✅ **Pas de surcharge.** Contrairement à l'écran « détail dossier » (qui a dû être éclaté en onglets par F-244), l'écran synthèse absorbe les nouveaux panneaux par conception. **Ajustement** : le nouveau panneau suit la règle des panneaux conditionnels (`@if length > 0`) et n'est PAS marqué `expanded` par défaut si le dossier porte déjà beaucoup de panneaux — décision de détail laissée à la mini-spec / dev (DESIGN_SYSTEM).

## Challenge état final / continuité

**Question** : après l'output de F-179, que fait l'avocat ?

✅ **Continuité claire.** Après avoir consulté la section « Jurisprudences citées » :
- Il poursuit la lecture de la synthèse (autres panneaux).
- Pour un arrêt `SUSPECT`, SF-179-04 fait remonter une **alerte de cohérence F-IA-03** — l'avocat la voit dans le flux de cohérence existant, pas de point mort.
- Il exploite les anomalies dans sa réplique lors de la génération de conclusions (F-98, onglet Décision) — étape aval naturelle du parcours.
- Le lien `sourceUrl` (quand disponible) lui permet d'ouvrir l'arrêt sur Légifrance / Juridat pour vérifier lui-même.

Aucun ping-pong subi : la section est une **escale de consultation** dans un parcours qui continue vers les conclusions.

## Ajustements IA requis

1. **Emplacement** : nouveau `mat-expansion-panel` dans `synthesis-accordion` de `SynthesisComponent`, après « Risques », avant « Questions ouvertes ». Composant standalone `app-jurisprudence-citations-section` injecté comme `app-divorce-consentement-scoring-section`.
2. **Affichage conditionnel** : le panneau n'apparaît que si au moins une référence a été détectée. Pas de panneau vide.
3. **État « tout incertain »** : si toutes les références sont `UNCERTAIN`, un message d'en-tête explique que la vérification automatique n'a pas pu conclure et invite à la vérification manuelle — pas de page blanche.
4. **Badge grille (optionnel)** : un badge dans `synthesis-grid` (F-162) pointant vers la section, si des références sont détectées. À confirmer en mini-spec SF-179-03 — non bloquant.
5. **Streaming F-185** : la section F-179 est alimentée par un post-traitement (comme les pistes stratégiques / la checklist), pas par le streaming JSON ; elle apparaît une fois l'analyse `DONE`. Cohérent avec les panneaux post-traitement existants — pas besoin de l'ajouter à `STREAMING_EXPECTED_SECTIONS`.

## Invariants anti-surcharge pour la mini-spec

1. Le panneau « Jurisprudences citées » est **conditionnel** (`@if` sur une liste non vide) — jamais affiché vide, comme tous les panneaux de l'accordéon.
2. La section est un **composant standalone dédié** (`app-jurisprudence-citations-section`), pas du HTML inline dans `synthesis.component.html` — cohérent avec les sections métier existantes, limite la charge du template parent.
3. Tout output a un **point de sortie explicite** : badge statut + explication + lien source ; un `SUSPECT` mène à une alerte F-IA-03 (SF-179-04).
4. L'ordre des panneaux de l'accordéon reste **lisible sans interaction** : la section s'insère à une position fixe (après Risques), pas en tête, pas en pied aléatoire.
5. Pas de nouvel écran, pas de nouvelle route : F-179 vit **entièrement dans l'écran synthèse existant**. Aucune nouvelle entrée de navigation.
6. La palette respecte `DESIGN_SYSTEM.md` : navy/or principal, **rouge réservé au statut `SUSPECT`**, gris pour `UNCERTAIN`. (Style = étape dev, rappelé ici uniquement comme garde-fou.)

## Décision finale

**GO avec ajustements.** Le placement (panneau de l'accordéon de synthèse) est le bon — l'avocat lit la thèse adverse sur cet écran, au bon moment du parcours. La séquence reste lisible, l'écran synthèse absorbe le nouveau panneau sans surcharge (accordéon extensible par conception), et la continuité vers les conclusions (F-98) + l'alerte F-IA-03 est assurée. Les 6 invariants anti-surcharge encadrent la mini-spec. Les ajustements IA (affichage conditionnel, état « tout incertain », composant standalone) sont intégrés au découpage.

→ Étape suivante : découpage (`feature-splitter`) + mini-specs (`story-writer`).

## MAJ apportée au parcours écran de référence

L'écran **synthèse** (`SynthesisComponent`) n'était pas encore décrit dans `docs/business/parcours-ecran-dossier.md` (qui couvre le détail du dossier). Ce cadrage documente le parcours de l'écran synthèse et y insère la zone F-179. Le référentiel `parcours-ecran-dossier.md` est enrichi d'une note renvoyant à ce parcours synthèse (la synthèse est un sous-écran de l'onglet Analyse). Aucune modification du parcours du détail du dossier — F-179 ne touche pas cet écran.

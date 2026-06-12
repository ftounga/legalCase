# F-282 — Cadrage cohérence écran (étape 0 bis)

> Skill `screen-coherence-challenger`. Date : 2026-06-12. Suite de l'étape 0 GO (`SF-282-00-coherence.md`). Feature à fort enjeu **design** (exigence PO « beauté écran premier ordre »).

## Verdict : **GO avec ajustements**

## Intention métier + comportement visible attendu
Donner à l'avocat une **vue claire du cycle contradictoire** de son dossier (les échanges successifs de conclusions : qui a déposé quoi, quand, à qui le tour, sous quel délai) et un point d'action **« générer ma réplique au dernier jeu adverse »**. Le dossier cesse d'être un instantané : il devient une **frise d'échanges vivante**.

## Rappel verdict étape 0
**GO** (`SF-282-00-coherence.md`) — F-282 **orchestre** des briques livrées (F-261 réfutation, F-271 récap, F-98 génération/versions, F-69 délais) en un cycle de rounds ; il ne les duplique pas.

## Parcours écran réel (ouverture dossier → état terminal) — détail dossier, 4 onglets
1. Ouvre le dossier → 4 onglets (Dossier / Analyse / Décision / Suivi) + en-tête `app-case-dashboard-stepper`.
2. Onglet **Dossier** : pièces ; **[existant F-261]** tag « Écritures adverses » sur le document du jeu adverse reçu.
3. Onglet **Analyse** → synthèse.
4. Onglet **Décision** : outils → tableau de bord → **`app-conclusions-section`** (génération/versions/lifecycle).
5. Onglet **Suivi** : `app-case-deadlines-section` (échéances), `app-case-notes-section` (notes), **calendrier procédural**.
6. **[NOUVEAU F-282]** L'avocat voit, dans l'onglet **Suivi**, la **frise des échanges contradictoires** (rounds) — et au round « à lui », l'invite à **générer sa réplique**.
7. Génération de la réplique = `app-conclusions-section` (Décision), **consciente du round** (répond au jeu adverse du round courant via F-261, récapitule via F-271).
8. **État terminal (réajusté)** : non plus « projet de conclusions généré » figé, mais **« réplique du round courant générée »** — l'état terminal **se réarme à chaque nouveau jeu adverse** (c'est le moteur de rétention).

## État terminal du processus (explicite)
F-98 avait tranché l'état terminal = « projet de conclusions généré ». **F-282 le rend cyclique** : chaque round rouvre un état terminal « réplique générée ». Ce n'est pas une contradiction — c'est la même production (conclusions), **réinstanciée par échange**. À documenter dans le référentiel.

## Cartographie écrans / zones ↔ parcours
| Étape | Zone LegalCase | Statut |
|---|---|---|
| 2. Upload + tag jeu adverse | Onglet Dossier, `#section-documents` (F-261 booléen `adverse_pleadings`) | ✅ existant |
| 4/7. Génération réplique | `app-conclusions-section` (Décision) | ✅ existant (à rendre **round-aware**) |
| 5. Calendrier procédural / échéances | Onglet Suivi (`app-case-deadlines-section` + calendrier) | ✅ existant |
| **6. Frise des échanges (rounds)** | **Onglet Suivi — enrichit le calendrier procédural** | 🆕 |
| Indicateur de round | `app-case-dashboard-stepper` (en-tête) | ✅ existant (à enrichir) |

## Position candidate de la feature
- **Frise des rounds** : **dans l'onglet Suivi**, en **élevant le « calendrier procédural » existant** en **frise du cycle d'échanges** (round 1 = nous, round 2 = adverse, round 3 = nous à échéance J…). **PAS un 4ᵉ bloc primaire** (Suivi est au seuil de 3) — c'est une **refonte du bloc calendrier** en vue contradictoire. Synergie directe avec **F-284** (échéancier proactif).
- **CTA « générer ma réplique au round N »** : **dans `app-conclusions-section`** (Décision), round-aware — pas de nouveau bloc.
- **Indicateur compact** : un badge « Round N · à vous / en attente adverse » dans le **stepper d'en-tête**.

## Challenge placement
✅ **Cohérent.** Le cycle est par nature **temporel/procédural** → sa maison naturelle est l'onglet **Suivi** (qui porte déjà délais + calendrier). L'action (générer) reste là où vivent les conclusions (Décision). L'avocat ne mémorise rien hors contexte.

## Challenge lisibilité de la séquence
⚠️ **Ajustement requis (point principal — inter-onglets).** Le cycle vit sur **3 onglets** : tag adverse (Dossier) → frise (Suivi) → génération (Décision). Risque d'éparpillement. Ajustements (légers, sans surcharge) :
- **a.** L'indicateur de round dans le **stepper d'en-tête** (visible quel que soit l'onglet) = fil rouge permanent (« Round 3 · à vous, échéance 14/07 »).
- **b.** Depuis la frise (Suivi), au round « à vous », un **bouton « Générer ma réplique »** qui **route vers `app-conclusions-section`** (Décision) pré-ciblée sur le round — le signal porte son action (invariant anti-orphelin F-206).
- **c.** Dans `app-conclusions-section`, un **bandeau de contexte** « Réplique au jeu adverse du round 2 (déposé le …) » quand la génération est round-aware.

## Challenge charge écran
✅ **Aucune surcharge — invariant 3 blocs respecté.** F-282 **n'ajoute aucun bloc primaire** : il **transforme** le calendrier procédural (Suivi) en frise d'échanges, enrichit le stepper (en-tête, hors décompte), et ajoute un bandeau + CTA **dans** la section conclusions existante. Suivi reste à 3 blocs ; Décision reste à 3 blocs.

## Challenge état final / continuité
✅ **Continuité = le cœur de la feature.** L'état terminal devient **cyclique** (réplique par round) au lieu de figé. Pas de dead-end : chaque jeu adverse reçu **réarme** une invite. Le seul maillon faible (séquence inter-onglets) est traité par a/b/c.

## Exigence design impérative (la frise des rounds est l'objet vedette)
- **Frise des échanges = pièce maîtresse visuelle.** Production-grade, charte `DESIGN_SYSTEM.md` : navy `#1A3A5C` / or `#C9973A`, Merriweather pour les libellés d'étape, Inter pour le corps, JetBrains Mono pour dates/numéros ; espacements multiples de 4px. Distinction visuelle nette **nous vs adverse** (couleur/alignement), état du round (déposé / reçu / **à vous, échéance J** / en attente). Zéro « AI-generic », zéro tableau brut.
- Cohérence avec le `app-case-dashboard-stepper` existant (même langage visuel d'étapes).
- Responsive (frise verticale en étroit).

## Invariants anti-surcharge pour la mini-spec
1. **Aucun bloc primaire nouveau** (Suivi et Décision restent à 3 blocs) — F-282 enrichit `calendrier procédural`, `stepper`, `conclusions-section`.
2. **Indicateur de round permanent** dans l'en-tête (fil rouge inter-onglets), jamais orphelin de son action.
3. **CTA round-aware** route vers la génération existante ; pas de duplication de la génération.
4. **Pas d'écran vide** : si 0 round (dossier neuf sans échange), la frise montre l'état initial « Round 1 — votre saisine » sans rubrique vide.
5. **Beauté = critère d'acceptation**, pas un nice-to-have (revue visuelle PO obligatoire).

## Décision finale
**GO avec ajustements.** Placement naturel (Suivi pour la frise, Décision pour l'action, en-tête pour le fil rouge), **zéro nouveau bloc primaire**, continuité cyclique assurée. La mini-spec intègre les 3 ajustements (a/b/c) + les 5 invariants + l'**exigence design** comme critère d'acceptation. Enrichit le référentiel `parcours-ecran-dossier.md` (calendrier procédural → frise d'échanges ; état terminal cyclique).

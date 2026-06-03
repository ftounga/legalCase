# F-158 V4 — Cadrage cohérence écran (étape 0 bis)

> Refonte du messaging de la landing page V4. Skill : `ai-skills/screen-coherence-challenger.md`. Date : 2026-06-03.
> Pré-requis : verdict étape 0 = **GO avec ajustements** (`SF-158v4-00-coherence.md`). ✅

## Verdict : **GO avec ajustements**

L'insertion des nouveaux arguments (conclusions, jurisprudence) est légitime, **mais l'écran est déjà saturé (16 sections)**. L'ajustement n'est pas « ajouter 2 sections de plus » — c'est **restructurer le récit autour de la chaîne de valeur** pour que l'écran ne grossisse pas indéfiniment et que la promesse terminale (conclusions) remonte en tête.

## Intention métier + comportement visible attendu

Le prospect avocat doit comprendre, dès le hero et au fil du scroll, que LegalCase couvre **toute la chaîne — des pièces jusqu'aux conclusions déposables** — et pas seulement l'analyse. Comportement visible : un hero qui nomme l'état terminal (conclusions), une section dédiée « rédaction de conclusions », une accroche « jurisprudence vérifiable », un chiffre d'outils à jour.

## Rappel verdict feature-coherence-challenger

GO avec ajustements (cf. `SF-158v4-00-coherence.md`). Aucune brique inexistante mise en avant.

## Parcours écran réel du prospect (arrivée → état terminal)

Source : `landing.component.html` (relevé 2026-06-03, 930 lignes).

1. **Hero** (l.16-62) — H1 « 92 outils décisionnels… » + CTA + 4 stats.
2. Carrousel **Démos** vidéo (l.65-140).
3. **Problème / Solution** (l.142-219).
4. **Fonctionnalités** — 8 cartes (l.221-297).
5. **Domaines couverts** — 3 domaines (l.299-353).
6. **OCR + Vision** (l.355-399).
7. **Outils décisionnels** — grille « 92 » + catalogue (l.401-479).
8. **Pipeline IA** — 5 étapes (l.481-529).
9. **Témoignages** (l.531-573).
10. **Différenciation** — 4 points (l.575-608).
11. **Confiance / RGPD** (l.610-642).
12. **Pricing** — 4 plans (l.644-821).
13. **FAQ** (l.823-894).
14. **CTA final** (l.896-913).
15. **Footer** (l.915-929).

## État terminal du processus (explicite)

État terminal du parcours landing = **clic de conversion** (essai gratuit / réserver une démo). Le récit qui y mène doit désormais culminer sur la promesse à plus forte valeur : *« jusqu'aux conclusions, dans votre style »*.

## Cartographie écrans / zones ↔ parcours

| Zone landing | Étape métier représentée | Écart à corriger |
|---|---|---|
| Hero H1 « 92 outils » | étape 4 (outils) | S'arrête à mi-chaîne ; chiffre périmé |
| Section Outils (grille 92) | étape 4 | Chiffre périmé, catalogue figé 04/05 |
| Section OCR/Vision | étape 2 | OK |
| Pipeline IA (5 étapes) | étapes 2-4 | Ne va pas jusqu'aux conclusions |
| — (aucune zone) | **étape 5 jurisprudence** | ❌ Absent |
| — (aucune zone) | **étapes 7-8 conclusions + export** | ❌ Absent |

## Position candidate des nouveaux éléments

- **Hero** : réécrire la H1 pour nommer la chaîne complète (pièces → décisions → conclusions). Remplacer la stat « 92 » par le chiffre réel.
- **Section « Rédaction de conclusions »** : nouveau bloc, placé **juste après la section Outils décisionnels** (l.479) — c'est la suite logique du pipeline (l'outil calcule → la conclusion se rédige). Point d'entrée naturel dans le scroll.
- **Jurisprudence vérifiable** : intégrée **dans** la section Conclusions (citations auto F-JU-02) + accroche dans Différenciation (l.575) — pas une 17ᵉ section autonome.
- **Pipeline IA** (l.481) : étendre la séquence à une 6ᵉ étape « conclusions générées » pour matérialiser l'état terminal.

## Challenge placement

✅ Légitime : la section Conclusions se place sur le fil narratif existant (après Outils, avant Pipeline étendu). La jurisprudence se rattache à Conclusions + Différenciation. Pas de point d'entrée artificiel.

## Challenge lisibilité de la séquence

La landing raconte aujourd'hui « analyse → outils » et **s'arrête**. Ajouter Conclusions en bout de chaîne **améliore** la lisibilité du récit (on voit enfin la fin). Étendre le Pipeline IA à l'étape conclusions rend la séquence complète lisible sans interaction.

## Challenge charge écran

⚠️ **Point dur.** L'écran porte déjà **15-16 sections**. Ajouter naïvement « Conclusions » + « Jurisprudence » → 17-18 sections = surcharge, scroll interminable, dilution. **Ajustement obligatoire** : ne pas empiler. Options à trancher en mini-spec — (a) **fusionner** des sections redondantes (Fonctionnalités 8-cartes l.221 recoupe partiellement Différenciation l.575) pour faire de la place ; (b) intégrer la jurisprudence DANS Conclusions et Différenciation plutôt qu'en section neuve. **Plafond** : pas plus de +1 section primaire nette.

## Challenge état final / continuité

Le hero et la dernière section narrative avant le pricing doivent porter la promesse terminale (conclusions). Continuité OK : la nouvelle section débouche naturellement sur Pricing → CTA.

## Ajustements IA requis

1. Hero : H1 + stats reflètent la chaîne complète et le chiffre d'outils réel.
2. +1 section primaire nette maximum (« Rédaction de conclusions ») ; jurisprudence intégrée, pas autonome.
3. Avant d'ajouter, identifier 1 section à fusionner/alléger (candidat : Fonctionnalités ↔ Différenciation) pour tenir le plafond.
4. Pipeline IA étendu à l'étape « conclusions » (continuité de la séquence).

## Invariants anti-surcharge pour la mini-spec

- **Plafond +1 section primaire nette** : toute nouvelle section s'accompagne d'une fusion/allègement si le total dépasse l'existant +1.
- **Jurisprudence non autonome** : rattachée à Conclusions + Différenciation.
- **Tout argument a sa place dans le fil narratif pièces → décisions → conclusions → preuve (témoignages/juris) → prix** ; pas de bloc orphelin.
- **Le hero nomme l'état terminal** (conclusions) sans attendre le scroll.

## Décision finale

**GO avec ajustements.** Insertion légitime sur le fil narratif, sous réserve du plafond anti-surcharge (+1 section nette, jurisprudence intégrée, une section allégée en contrepartie). Passage à l'étape 1 (mini-spec) autorisé.

## MAJ apportée au parcours écran de référence

Référentiel à créer/enrichir : `docs/business/parcours-ecran-landing.md` (n'existe pas encore — les 3 référentiels actuels couvrent cabinet/dossier/super-admin, pas la landing publique). Sera produit avec la mini-spec, capturant la séquence narrative cible : `hero(promesse terminale) → démos → problème/solution → pièces(OCR/Vision) → analyse → outils décisionnels → rédaction conclusions(+juris) → pipeline complet → preuve(témoignages/différenciation/juris) → confiance → pricing → CTA`.

# Mini-spec — F-158 / SF-158-07 (V5) — Refresh landing « poste de pilotage du dossier vivant »

> Feature parente : **F-158** (landing). Branche : `feat/F-158-v5-poste-pilotage`. Date : 2026-06-14. Statut : `ready`.
>
> **Origine** : audit demandé par le PO (« la landing est-elle toujours d'actualité et optimale ? »). Constat : la landing (gelée depuis F-158 V4, 03/06) raconte un **outil ponctuel** (pièces → conclusions one-shot), alors que le produit est devenu un **poste de pilotage du dossier vivant** (vague F-282→F-292, ~10 derniers jours). Le PO a validé le **repositionnement « poste de pilotage, pas juste un outil ponctuel »**.
>
> **Étape 0 / 0 bis** : non applicables au sens strict — il s'agit d'un **refresh de contenu marketing** de la landing (pas un nouvel écran applicatif du référentiel `parcours-ecran-*`, pas un nouveau workflow avocat dans l'app). Le repositionnement est une **décision PO explicite**, pas un sujet de cohérence à challenger. Cadre = lignée F-158 (déjà au PRODUCT_SPEC).

## Objectif
Mettre la landing en cohérence avec l'état actuel du produit : (1) **corriger la crédibilité** (retirer les témoignages fabriqués et le claim de temps non mesuré), (2) **assumer le repositionnement « poste de pilotage du dossier vivant »** (Vue d'ensemble, cycle contradictoire, phases, échéancier, qualification, composition), (3) **rafraîchir le chiffre d'outils** (250+ → 280+, réel 288).

## Comportement attendu (changements de contenu)
1. **Correctif crédibilité**
   - Section « Témoignages » (3 citations attribuées « Me L./B./D. ») **retirée** — 0 client réel ⇒ pas de social proof fabriqué (risque juridique/réputation). Remplacée par un commentaire HTML (à réintroduire avec de vrais retours consentants).
   - Le claim chiffré non mesuré « 15 min vs 3 h » disparaît avec ces témoignages (cohérent avec la décision PO V4 : pas de gain de temps non mesuré).
2. **Repositionnement « poste de pilotage »**
   - **Hero** : sous-titre élevé — on garde la colonne vertébrale « De vos pièces jusqu'aux conclusions » (H1 inchangé) et on ajoute la dimension pilotage (« devient votre poste de pilotage : phases, échéances et échanges contradictoires suivis de la saisine à l'audience »).
   - **Nouvelle section `#pilotage`** « Un poste de pilotage, pas un outil ponctuel » (réutilise les classes existantes `.why-us`/`.why-grid`/`.why-card`, **aucun SCSS nouveau**), 6 cartes : Vue d'ensemble (F-289), Cycle contradictoire (F-282), Phases procédurales (F-283), Échéancier proactif (F-284), Qualification d'entrée (F-285), Conclusions sous contrôle (F-287 streaming + F-288 composition).
   - **Lien de navigation** « Pilotage » ajouté au menu (après « Conclusions »).
   - **SEO/JSON-LD** (`landing.component.ts`) : description + featureList enrichies de la dimension pilotage.
3. **Chiffre d'outils**
   - Catalogue régénéré (`node scripts/build-landing-catalog.mjs`) → **288 outils** (FR=188, BE=100).
   - Toutes les occurrences visibles « 250+ » → « 280+ » (conservateur, réel 288) : hero (stat `data-target`, sous-titre, CTA), carte solution, titre section outils, title/description/shortDescription + JSON-LD.

## Hors scope (explicite)
- **Aucune refonte conversion lourde** : le diagnostic traction (08/06) est une **absence de PMF**, pas un problème de copy → on corrige l'exactitude et le positionnement, on ne reconstruit pas le tunnel.
- Pas de modification de la page DRH `/employeur` (pivot test séparé).
- Pas de nouveaux composants/SCSS (réutilisation des patterns existants).
- Pas de modification du pipeline (6 étapes) ni des vidéos démo.

## Critères d'acceptation
- [ ] Plus aucune citation/témoignage attribué fabriqué dans la landing (`grep -i "Me L\.\|Me B\.\|Me D\.\|testimonial-quote"` → vide).
- [ ] Section `#pilotage` présente avec 6 cartes couvrant Vue d'ensemble / contradictoire / phases / échéancier / qualification / composition.
- [ ] Hero porte la dimension « poste de pilotage » sans supprimer « pièces → conclusions ».
- [ ] Lien menu « Pilotage » → `#pilotage`.
- [ ] Aucune occurrence « 250 » résiduelle (html + ts) ; chiffre = 280+ partout ; `data-target="280"`.
- [ ] Catalogue régénéré (288), build OK (`ng build`), spec landing vert.
- [ ] SEO : title ≤ 70 car, description ≤ ~165 car, featureList enrichie.

## Technique
- **Fichiers** : `frontend/src/app/landing/landing.component.html` (nav, hero, nouvelle section, suppression témoignages, chiffres), `landing.component.ts` (SEO/JSON-LD, chiffres), `landing-tools-catalog.ts` (régénéré, **ne pas éditer à la main**).
- **Migration / backend** : aucun. **Aucun nouveau composant, aucun SCSS** (réutilise `.why-*`).
- ⚠️ Rappel `feedback_no_thirdparty_brands_landing` : pas de marque tierce dans le copy (OK — « Legal OCR/Vision » conservés, Anthropic seulement dans la FAQ confidentialité existante).
- ⚠️ Rappel `feedback_landing_viewencapsulation_none_css_collision` : la landing est en `ViewEncapsulation.None` → ne pas introduire de classes génériques nouvelles (on réutilise des classes déjà scoping-safe `.why-*`, pas de `.hero`-like générique ajouté).

## Plan de test
- `npm test -- --testPathPattern=landing` (spec landing vert ; il ne référence pas les témoignages → suppression sûre).
- `ng build` (validation templates).
- Vérif manuelle staging post-deploy : section Pilotage visible, lien menu, plus de témoignages, chiffre 280+.

## Analyse d'impact
- **Navigation/routing** : ancre interne `#pilotage` (pas de route/guard). 
- **Préoccupations transversales** : aucune (contenu landing public, pas d'auth/workspace/plan).
- **Smoke E2E** : `smoke.yml` se déclenche post-CI ; les smoke sont en faux-négatif connu (host déprécié) — non bloquant, non lié.

## Notes
- Le repositionnement **garde** « pièces → conclusions » (prouvé V4) et l'**élève** : produire *et* piloter.
- Réintroduire de vrais témoignages dès qu'il y a des clients consentants (ré-ouvrir une SF).

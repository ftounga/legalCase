# Mini-spec — F-158 / SF-158-04 — Refonte hero « chaîne de valeur » + chiffre d'outils réel + régénération catalogue

## Identifiant

`F-158 / SF-158-04`

## Feature parente

`F-158` — Refonte landing page (V3 Terminée → **réactivée en vague V4**).

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-158-04-refonte-hero-chaine`

---

## Objectif

Réécrire le hero de la landing pour qu'il nomme la **chaîne de valeur complète** (« de vos pièces jusqu'aux conclusions déposables »), corriger le **chiffre d'outils** périmé (« 92 » → réel, 254 au registre) et **régénérer** le catalogue `landing-tools-catalog.ts` figé depuis le 04/05.

---

## Comportement attendu

### Cas nominal

La landing publique (`/`) affiche :
1. **H1** réécrit nommant l'état terminal du parcours : pièces → analyse → outils décisionnels → **conclusions**. Formulation cible (à affiner au dev, sens figé) : *« De vos pièces jusqu'aux conclusions. LegalCase analyse votre dossier, pré-remplit 250+ outils décisionnels et rédige le projet de conclusions — dans votre style. »*
2. **Sous-titre** ajusté : mentionne OCR+Vision, outils pré-remplis et **rédaction de conclusions** (3 domaines × 2 pays). Ne crée pas la section Conclusions (→ SF-158-05), annonce seulement.
3. **Stats hero** : la carte « 92 Outils décisionnels intégrés » devient « **250+** Outils décisionnels intégrés » (chiffre arrondi prudent ; source = nombre d'entrées `TOOL_REGISTRY`, 254 au 2026-06-03). Les 3 autres stats inchangées (le claim « 10× » est traité en SF-158-06, hors périmètre ici).
4. **Section Outils** (l.401-479) : titre « Quelques-uns des 92 outils » → « **250+** outils » (alignement avec le hero). Grille et catalogue inchangés fonctionnellement, mais **alimentés par le catalogue régénéré**.
5. **`landing-tools-catalog.ts` régénéré** depuis `TOOL_REGISTRY` (254 entrées) — le compteur dynamique du showcase reflète le nombre réel par domaine/pays.

### Cas d'erreur

Page statique (pas d'appel réseau dans le hero). Cas couverts par les tests de rendu :

| Situation | Comportement attendu |
|-----------|---------------------|
| Catalogue régénéré incohérent (id orphelin sans label) | Build/test échoue (le test d'intégrité du showcase doit casser, pas afficher un id brut) |
| `prefers-reduced-motion` actif | Aucune animation hero ajoutée (pas de régression sur l'existant) |
| Chiffre codé en dur divergent du catalogue | Test vérifiant la cohérence hero ↔ longueur catalogue échoue |

---

## Analyse de cohérence transversale

### Périmètres scannés
- **Source de vérité du chiffre** : `TOOL_REGISTRY` (`decisional-tools-panel.component.ts`, 254 entrées au 2026-06-03) — **source unique**, le catalogue landing en dérive. Le chiffre affiché ne doit jamais être un nombre magique déconnecté.
- **Autres pays/domaines** : répartition réelle 228 FR / 26 BE → le copy ne doit pas suggérer de parité FR/BE (invariant cadrage étape 0). Le showcase garde ses filtres FR/BE/domaine.
- **Régénération catalogue** : `build-catalog.py` référencé dans l'en-tête du catalogue **n'existe plus dans le repo**. Décision technique (documentée en PR) : recréer un script de génération reproductible (ou extraction scriptée depuis `TOOL_REGISTRY`) plutôt qu'une saisie manuelle, pour que le refresh reste automatisable.

### Préoccupation transversale cochée
- **Navigation / routing** : aucune nouvelle route, aucun guard. Hero = contenu statique. → pas d'impact transversal auth/workspace.
- **Outil décisionnel métier** : aucune création/modification d'outil. Le catalogue est un **reflet en lecture** du registre — aucune règle `decision_tool_visibility_rules` touchée.

---

## Critères d'acceptation vérifiables

1. ✅ Le H1 mentionne explicitement la rédaction de **conclusions** (état terminal) et n'affiche plus « 92 ».
2. ✅ La stat hero affiche « 250+ » (ou le chiffre exact figé) et plus « 92 ».
3. ✅ Le titre de la section Outils est aligné sur le même chiffre que le hero.
4. ✅ `landing-tools-catalog.ts` contient le nombre réel d'entrées du registre (254 ± variations documentées), et chaque entrée a un `label` non vide (pas d'id brut).
5. ✅ Le compteur dynamique du showcase (`landing-tools-showcase`) affiche un total cohérent avec le catalogue régénéré.
6. ✅ Aucun claim nouveau non sourcé introduit (le « 10× » et le « fax 200 dpi » restent inchangés, traités en SF-158-06).
7. ✅ Le copy ne suggère pas de parité FR/BE (formulation honnête de la couverture).
8. ✅ Pricing, RGPD, FAQ inchangés.

---

## Plan de test minimal

- **Jest `landing.component.spec.ts`** : (a) le H1 contient « conclusions » ; (b) le H1/stat ne contient plus « 92 » ; (c) la stat outils et le titre section Outils affichent le même chiffre.
- **Jest `landing-tools-showcase.component.spec.ts`** : total du catalogue régénéré = nombre attendu ; aucun `label` vide ; filtres FR/BE/domaine cohérents avec la nouvelle répartition (228/26).
- **Test d'intégrité catalogue** : tout `id` du catalogue existe dans `TOOL_REGISTRY` (pas d'orphelin), et le compte du catalogue == compte du registre (garde-fou anti-dérive future).
- **Smoke E2E Playwright** (`e2e/smoke/landing.spec.ts`) : mise à jour de l'assertion « H1 92 outils » → nouveau H1 + chiffre. ⚠️ Lancer `cd e2e && npm test` (préoccupation transversale navigation cochée). **Note dette connue** : le host smoke staging déprécié (`staging.legalcase.ng-itconsulting.com`) provoque un faux négatif OIDC — vérifier health+routes d'abord, viser `staging.legalcase.fr`.
- **Isolation workspace** : N/A (page publique non authentifiée).

---

## Tables / endpoints / composants impactés

- `frontend/src/app/landing/landing.component.html` — hero (l.16-62) + titre section Outils (l.401).
- `frontend/src/app/landing/landing.component.ts` — éventuel signal/constante du chiffre (source unique).
- `frontend/src/app/landing/landing-tools-catalog.ts` — **régénéré** (92 → 254 entrées).
- `frontend/src/app/landing/landing-tools-showcase/` — vérif compteur/filtres.
- `frontend/src/app/landing/landing.component.spec.ts` + `landing-tools-showcase.component.spec.ts` — tests.
- `e2e/smoke/landing.spec.ts` — assertion H1/chiffre.
- **Aucune** table, **aucun** endpoint backend.

---

## Hors périmètre

- **Section « Rédaction de conclusions »** + jurisprudence intégrée + pipeline étendu → **SF-158-05**.
- **Nettoyage des claims** « 10× plus rapide » / « fax 200 dpi » + SEO/OG/JSON-LD + allègement d'une section en contrepartie (plafond charge écran) → **SF-158-06**.
- Refonte visuelle / charte (DESIGN_SYSTEM) — exclu.
- Modification d'outils décisionnels ou de leur visibilité — exclu.

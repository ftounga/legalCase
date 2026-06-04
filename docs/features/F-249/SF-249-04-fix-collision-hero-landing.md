# Mini-spec — F-249 / SF-249-04 — Bugfix : collision de classe `.hero` (landing → dashboard)

## Identifiant

`F-249 / SF-249-04` — **bugfix**

## Type

Bugfix CSS — exempté étapes 0/0 bis.

## Constat (retour PO 2026-06-04)

Le dashboard d'accueil affiche un **gros bloc bleu plein écran** (greeting navy illisible dessus), persistant malgré SF-249-02 et SF-249-03. Diagnostic par capture du DOM authentifié (Playwright sur staging) :
- `<header class="hero">` du dashboard est rendu en **944×720 px, fond `rgb(15,36,56)`**.
- Or `dashboard.component.scss` ne donne au hero que `margin-bottom` (vérifié dans le bundle servi).

## Cause racine

`landing.component.ts` est en **`ViewEncapsulation.None`**. Sa règle `landing.component.scss` :
```scss
.hero { min-height: 100vh; display:flex; align-items:center; background: var(--navy-deep); }
```
devient donc **globale** et s'applique au `<header class="hero">` du **dashboard** (introduit par F-249) — d'où le bloc plein écran navy + greeting centré. **Collision de nom de classe**. Les 3 itérations précédentes ne corrigeaient que le `.hero` *scopé* du dashboard, sans effet sur le `.hero` *global* de la landing.

Une seule classe en collision (vérifié : `hero-greeting`, `kpi`, `dash-overview`, etc. ne sont pas définies par la landing).

## Correction

Renommer la classe du conteneur d'en-tête du dashboard `.hero` → **`.dash-hero`** (unique), pour qu'elle n'hérite plus du style global de la landing. Une fois le conteneur renommé, les règles globales `.hero h1`, `.hero p` etc. de la landing ne ciblent plus le dashboard.

- `dashboard.component.html` : `<header class="hero">` → `<header class="dash-hero">`.
- `dashboard.component.scss` : `.hero {` → `.dash-hero {` (+ commentaire expliquant la collision).
- `dashboard.component.spec.ts` : assertion `.hero .hero-spark` → `.dash-hero .hero-spark`.

## Critères d'acceptation

1. ✅ Le `<header>` d'accueil porte `class="dash-hero"`, plus `class="hero"`.
2. ✅ Le hero du dashboard n'a plus de fond navy ni de `min-height:100vh` (le style global landing ne le cible plus).
3. ✅ Le greeting navy est lisible sur fond clair.
4. ✅ Tests dashboard verts.
5. ✅ **Vérifié sur le rendu authentifié réel** (capture Playwright staging post-déploiement).

## Plan de test

- Jest `dashboard.component.spec.ts` : 22/22 (exécuté).
- **Vérification visuelle obligatoire** : capture Playwright authentifiée sur staging après déploiement — le bloc bleu doit avoir disparu.

## Dette identifiée (hors périmètre, à tracer)

`landing.component` en `ViewEncapsulation.None` avec des classes génériques (`.hero`, `.hero h1`…) **pollue le CSS global** et peut entrer en collision avec n'importe quel autre écran. Correctif de fond (scoper la landing ou préfixer toutes ses classes) à planifier séparément. SF-249-04 ne traite que la collision `.hero` du dashboard.

## Hors périmètre

Refonte de la landing, scoping global, autres écrans.

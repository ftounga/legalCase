# F-258 / SF-258-02 — Cadrage cohérence (étape 0) + cohérence écran (étape 0 bis)

> Feature : **v2 de l'alerte « outils pré-remplis non calculés »** — la porter du bandeau passif vers le **checkpoint de génération** (la modale de composition F-288), en avertissement **fort**. Origine : signal PO 2026-06-13 (« j'avais demandé une validation forte, pas juste un bandeau »). Skills : `feature-coherence-challenger` + `screen-coherence-challenger`.

## Verdict : **GO avec ajustements** (frontend-only)

## Intention
Au clic **« Générer »**, si des outils pertinents ne sont pas calculés, le rappeler **fort, au moment de l'action**, avec un accès direct pour aller les compléter — au lieu d'un bandeau passif sur la page que l'avocat ne « voit » pas.

## Décision majeure prise en conversation (anti-gadget)
❌ **PAS de bouton « Calculer tous les outils pré-remplis »** : un pré-remplissage IA est **très souvent incomplet** (champs à saisir à la main, ex. convention collective) → un auto-calc en masse **planterait** ou produirait des **résultats faux**. C'est la raison exacte pour laquelle le PO avait écarté l'auto-calc en juin. **On garde le geste manuel** (l'avocat complète + calcule outil par outil).

## Étape 0 — cohérence fonctionnelle
- **Détection des non-calculés** : existe déjà (`conclusions-section.refreshMissingTools` : `missingToolsCount = (alwaysOn + contextual) − tiles calculées`, F-258/SF-258-01). ✅
- **Modale de génération** : existe (F-288 `conclusion-composition-dialog`, ouverte au clic Générer). ✅
- **Navigation vers les outils** : existe (`viewToolsRequested` → parent défile vers `#section-outils-decisionnels`, réutilisé de F-258). ✅
- **Aucun trou amont/aval.** La feature ne fait que **recombiner des briques livrées**. → GO.

## Étape 0 bis — cohérence écran
- **Placement** : un **bloc d'avertissement en tête de la modale** F-288 (au-dessus des listes de composition), affiché **seulement si `missingToolsCount > 0`** : « ⚠ N outils pertinents ne sont pas calculés. Ils ne nourriront pas l'acte tant qu'ils ne sont pas complétés et calculés. » + bouton **[ Aller compléter ces outils ]**.
- **Déclenchement (changement clé)** : la modale s'ouvre désormais au clic Générer dès qu'il y a **soit des éléments curables (calculés/moyens), soit des non-calculés (`missingToolsCount > 0`)**. Aujourd'hui elle se **saute** quand 0 curable → c'est le trou qu'on bouche (le checkpoint ne doit plus être contournable quand il reste des outils à calculer).
- **Action [ Aller compléter ces outils ]** : ferme la modale **sans générer** et émet `viewToolsRequested` (défilement vers les outils, comportement F-258). L'avocat complète à la main puis recalcule.
- **Non bloquant** : « Confirmer & générer » **reste disponible** même avec des non-calculés. La modale **EST** la validation forte (on ne peut plus générer sans la traverser et voir l'avertissement) ; le clic « Confirmer & générer » est l'acquittement délibéré. Pas de blocage dur (cas légitimes : brouillon, outils non pertinents pour la ligne).
- **Sort du bandeau F-258** : **conservé** (hint ambiant sur la page, sans coût) — il vit *avant* le clic ; le bloc modale vit *au* clic. Pas de conflit, pas de doublon fonctionnel (même donnée, deux moments). *Décision réversible : si jugé redondant après livraison, retirer le bandeau.*
- **Charge écran** : la modale gagne un bloc en tête ; reste lisible (un encart + listes). Pas de 4ᵉ zone, pas de nouvelle route.

## Invariants anti-gadget / anti-surcharge
1. **Pas d'auto-calc** (géré ci-dessus).
2. Le bloc d'avertissement n'apparaît **que si `missingToolsCount > 0`** ; sinon la modale est exactement celle d'aujourd'hui.
3. **Non bloquant** : « Confirmer & générer » jamais désactivé.
4. **Réutilise** `missingToolsCount` + `viewToolsRequested` existants — **aucun backend**, aucune nouvelle donnée.
5. La modale s'ouvre si `missingToolsCount > 0` **même** sans aucun outil calculé (checkpoint non contournable) ; si **ni** non-calculés **ni** curables → pas de modale (génération directe, inchangé).

## Décision finale
**GO avec ajustements**, **frontend-only**. v2 = avertissement fort dans le checkpoint + ouverture de la modale quand il reste des non-calculés + navigation « aller compléter ». Pas d'auto-calc, pas de blocage. Mini-spec : `SF-258-02`.

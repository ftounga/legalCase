# Mini-spec — F-179 / SF-179-05 Confirmation d'existence des citations adverses via JUDILIBRE

## Identifiant
`F-179 / SF-179-05`

## Feature parente
`F-179` (Vérification des références jurisprudentielles citées par la partie adverse). SF de durcissement de fiabilité — évolution interne d'un mécanisme existant (le badge « Vérifiée » existe déjà, aucun nouvel écran ni workflow) → exemptée des étapes 0 / 0 bis.

## Statut
`ready`

## Branche Git
`feat/SF-179-05-confirmation-judilibre`

## Objectif (une phrase)
Confirmer l'**existence** des citations adverses **françaises de la Cour de cassation** via l'API officielle **JUDILIBRE** (recherche par n° de pourvoi) avant le scraping Légifrance, pour qu'un arrêt réel mais peu connu passe de « Incertaine » à « Vérifiée » au lieu de rester bloqué.

## Problème (diagnostic staging 2026-06-08, dossier de test F-179)
Le système F-179 fonctionne pour l'essentiel : faux arrêts → `NOT_FOUND`, citation détournée → `SUSPECT`, arrêt très connu (Macron 21-14.490) → `VERIFIED`. **Mais 2 arrêts réels moins célèbres** (`soc. 98-41.609` de 2000, `Rogié 88-44.308` de 1990) sont restés `UNCERTAIN` : Claude n'en a pas une connaissance certaine (LOW/MEDIUM) et le **web search a échoué à les confirmer** (`web_search_used=t`, `source_url=null`). Cause : `WebSearchService.searchJurisprudence` fait un GET HTTP sur `legifrance.gouv.fr/search` (SPA JS) dont `interpret()` ne sait pas extraire le résultat → `UNCERTAIN`. Or on dispose déjà de `JudilibreApiClient` — l'API **officielle** de la Cour de cassation (PISTE), fiable et structurée.

## Comportement nominal
Dans `WebSearchService.searchJurisprudence(reference, country)`, **avant** le scraping :
1. Si `country` ≠ Belgique **et** un **n° de pourvoi** est extractible de `reference` (format `NN-NN.NNN`, ex. `98-41.609`) :
   - appeler `judilibre.fetchArretsByKeyword(numero, périodeLarge, 5)` ;
   - si **un** arrêt retourné a un `numeroPourvoi` qui **matche** (comparaison sur chiffres seuls) le numéro extrait → `WebSearchResult.found(arret.lienLegifrance())` ; **fin** (pas de scraping).
2. Sinon (BE, pas de numéro, JUDILIBRE no-op / vide / aucun arrêt matchant) → **scraping Légifrance/juportal existant** (comportement actuel inchangé).

La promotion `UNCERTAIN → VERIFIED` reste pilotée par le code appelant (`JurisprudenceVerificationService`, `FOUND → VERIFIED`) — **sémantique SF-179-02 inchangée**.

## Cas d'erreur / bords
- JUDILIBRE sans credentials → `fetchArretsByKeyword` no-op (liste vide) → fallback scraping. Aucune régression.
- Exception JUDILIBRE → déjà captée en interne (retourne liste vide) → fallback scraping. Aucune exception propagée.
- Arrêt JUDILIBRE au **mauvais** numéro → **non** considéré comme FOUND (matching strict) → fallback. Pas de faux positif.
- Numéro non extractible → JUDILIBRE **non** appelé (évite une `query` inutile) → scraping.

## Solution technique (backend uniquement, **pas de migration**)
1. **`WebSearchService`** : injecter `JudilibreApiClient` (constructeur principal + constructeur de test). Ajouter `extractPourvoiNumber(reference)` (regex `\\b(\\d{2}[.\\-\\s]\\d{2}[.\\-\\s]\\d{3})\\b`) + `normalizeNumber` (chiffres seuls) + `tryJudilibreConfirmation(reference)` retournant `Optional<WebSearchResult>`.
2. Brancher `tryJudilibreConfirmation` en tête de `searchJurisprudence` pour la branche FR ; si présent → retour immédiat ; sinon → flux scraping actuel.
3. Période de recherche : large explicite (`LocalDate.of(1960,1,1)` → `now()+1j`) — une recherche par n° de pourvoi est très discriminante.

## Critères d'acceptation (vérifiables)
1. FR + numéro extractible + JUDILIBRE renvoie un arrêt **au numéro matchant** → `FOUND(lienLegifrance)`, **sans** appel au scraping. (test)
2. FR + JUDILIBRE vide / no-op → fallback scraping (résultat actuel). (test)
3. FR + arrêt JUDILIBRE **non** matchant → fallback scraping, **pas** de FOUND. (test)
4. BE → JUDILIBRE **jamais** appelé. (test)
5. Référence sans numéro de pourvoi → JUDILIBRE **jamais** appelé → scraping. (test)
6. Suite backend verte, aucune régression sur les tests `WebSearchServiceTest` existants.

## Plan de test minimal
- **`WebSearchServiceTest`** (Mockito : `JudilibreApiClient` mocké + `RestClient` mocké) : cas 1-5 ci-dessus. Vérifier `verify(judilibre, never())` pour BE et numéro absent ; vérifier l'absence d'appel `restClient` quand JUDILIBRE confirme.
- **Isolation workspace** : N/A — vérification interne, pas d'accès multi-tenant nouveau (les `JurisprudenceCheck` restent scoppés comme avant).

## Tables / endpoints / composants impactés
- **Backend** : `WebSearchService` (injection + logique JUDILIBRE). Réutilise `JudilibreApiClient`, `JudilibreArret`, `WebSearchResult` (inchangés).
- **Frontend** : aucun (le badge « Vérifiée » et son affichage existent — seule la **fréquence** d'atteinte augmente).
- **Migration** : **aucune**.

### Préoccupations transversales
- **Appel externe** (JUDILIBRE) ajouté dans le flux de vérification **asynchrone** (post-analyse, jamais synchrone bloquant). Borné par `maxWebSearchesPerCase` (la confirmation s'inscrit dans la même boucle déjà plafonnée). Pas d'appel **Anthropic** (API Cour de cassation) → pas de gate Anthropic. Pas d'auth/workspace/navigation/plan impacté.

## Hors périmètre
- Conseil d'État / cours d'appel / arrêts BE → restent sur le web search existant (JUDILIBRE = Cour de cassation).
- Vérification de la **fidélité** de la position via JUDILIBRE (on confirme l'existence ; la fidélité reste jugée par Claude en amont, comme SF-179-02).
- Remplacement total du scraping Légifrance (conservé en fallback).

# Mini-spec — F-98 / SF-98-61 — Finitions rédactionnelles des conclusions (audit)

> Phase 3 de l'audit F-98 — 3 défauts de finition relevés à l'audit LEMAIRE. Backend-only (prompt/garde + 1 section). Étapes 0/0 bis non applicables (enrichit le contenu généré, famille SF-98-55).

## Identifiant
`F-98 / SF-98-61`

## Statut
`ready`

## Branche
`feat/SF-98-61-finitions-redactionnelles`

## Objectif
> Corriger 3 défauts de finition de l'acte généré : (3a) adresse des parties en placeholder « [adresse] » ; (3b) nom d'avocat halluciné ; (3c) jurisprudence d'outil « plaquée » non topique.

## Correctifs

### 3a — Identité / adresse des parties
**Cause** : `TravailExtractedData` extrait bien `nom_salarie`/`prenom_salarie`/`adresse_salarie`/`nom_employeur`/`adresse_employeur` (sérialisés dans `travail_extracted_data` du JSON d'analyse), mais `CaseConclusionPromptBuilder` ne les injecte jamais (la `appendSynthesis` ne lit que faits/points_juridiques/risques) → le LLM met « [adresse] ».
**Fix** : nouvelle section `=== IDENTITÉ DES PARTIES ===` dans `buildUserMessage`, parsée depuis `travail_extracted_data` du `analysisResultJson` (clés ci-dessus). Absente si les clés ne sont pas présentes (autres domaines → suivi). + garde : « Reprends les identités et adresses fournies dans IDENTITÉ DES PARTIES ; à défaut mets « [à compléter] », n'invente jamais. »

### 3b — Nom d'avocat halluciné
**Cause** : aucun nom d'avocat/cabinet n'est extrait ni passé au prompt → le LLM invente un nom (ex. « Maître Sophie BERNARD »).
**Fix (anti-hallucination, sans plomberie de données)** : garde — « Ne signe JAMAIS avec un nom d'avocat inventé. Termine par un emplacement de signature neutre « [Nom et qualité de l'avocat] » que l'avocat complétera. » (pas de nom propre dans l'acte généré).

### 3c — Jurisprudence d'outil plaquée
**Cause** : `ConclusionsJurisprudenceContext.collectForCaseFile` injecte TOUS les arrêts mappés aux outils utilisés, sans filtre de pertinence ; la garde dit « cite si la section le contient » sans exigence de topicalité.
**Fix (prompt, minimal)** : renforcer `JURISPRUDENCE_GUARD` — « Même si une référence figure dans « JURISPRUDENCE APPLICABLE PAR OUTIL », ne l'utilise QUE si elle éclaire réellement le moyen ou le fait débattu. Ne plaque pas un arrêt non topique. »

## Critères d'acceptation
- [ ] La garde système contient les 3 consignes (adresses fournies/pas de placeholder ; pas de nom d'avocat inventé ; pertinence jurisprudence).
- [ ] Section `IDENTITÉ DES PARTIES` injectée quand `travail_extracted_data` porte les identités ; absente sinon.
- [ ] Non-régression : assertions SF-98-55 (anti-jargon, dispositif, syllogisme, subsidiaires) + SF-98-56 (réfutation) + JURISPRUDENCE_GUARD existant conservées.
- [ ] Validation staging : acte généré avec adresses réelles (plus de « [adresse] »), signature en placeholder neutre (plus de nom inventé), pas d'arrêt d'outil hors-sujet.

## Hors scope
- Identité des parties pour Immigration/Famille (clés `immigration_extracted_data`/`famille_extracted_data`) — suivi si signal (le fix travail couvre le cas audité).
- Filtrage backend de la jurisprudence (option B) — la consigne de pertinence prompt suffit en MVP.

## Technique
- `CaseConclusionPromptBuilder` : section IDENTITÉ DES PARTIES (parse `travail_extracted_data`) + `REDACTION_QUALITY_GUARD` (3a, 3b) + `JURISPRUDENCE_GUARD` (3c). Aucune migration, aucun endpoint, aucun frontend.

## Plan de test
- [ ] UT `CaseConclusionPromptBuilderTest` : section IDENTITÉ présente avec identités / absente sans ; garde contient les 3 consignes ; non-régression assertions existantes.

## Préoccupations transversales
- [x] Aucune (prompt système ; pas d'auth/workspace/plan/navigation/endpoint/schéma).

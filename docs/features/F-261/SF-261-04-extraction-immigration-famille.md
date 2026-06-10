# Mini-spec — F-261 / SF-261-04 — Extraction des moyens adverses : vagues Immigration FR + Famille FR

> Programme Conclusions V2 / F-261. Vagues domaine de SF-261-02 (le framework existe, seuls les prompts manquent). Backend-only. Pas de nouvelle étape 0/0 bis (couvert par `SF-261-00-coherence.md` : « framework + vagues par domaine »).

## Identifiant
`F-261 / SF-261-04`

## Statut
`ready`

## Branche
`feat/SF-261-04-extraction-im-fa`

## Objectif
> Étendre l'extraction des moyens adverses (SF-261-02, aujourd'hui **travail FR** seul) aux domaines **immigration FR** et **famille FR**, pour que « conclusions en réponse » fonctionne dans les 3 domaines V1.

## Comportement attendu
1. À la génération, `AdverseMoyensExtractor.extract` sélectionne le **prompt d'extraction selon le domaine** : `DROIT_DU_TRAVAIL` (existant), `DROIT_IMMIGRATION`, `DROIT_FAMILLE` — pays `FRANCE`.
2. Pour immigration/famille FR + document adverse + texte → extraction LLM des moyens `{thèse, fondements, piècesInvoquées}` (anti-invention, fail-open, comme travail).
3. Domaines/pays non couverts (BE pour l'instant) → `List.of()` (no-op).
4. Le reste (section « MOYENS ADVERSES À RÉFUTER », point 8 de la garde, injection) est **inchangé** (mécanisme uniforme déjà livré).

## Critères d'acceptation
- [ ] `extract` immigration FR + texte → appelle le LLM avec le prompt immigration → parse les moyens.
- [ ] `extract` famille FR + texte → appelle le LLM avec le prompt famille → parse les moyens.
- [ ] Domaine FR non couvert / pays BE → `List.of()` (pas d'appel).
- [ ] Fail-open conservé (échec/texte vide → vide).
- [ ] Non-régression travail FR (SF-261-02) et SF-98-55/56/60/61.

## Périmètre
### Hors scope
- Belgique (BE) — vague ultérieure si signal.
- Persistance/affichage des moyens.

## Technique (backend-only, aucune migration)
- `AdverseMoyensExtractor` :
  - + `SYSTEM_PROMPT_IMMIGRATION_FR` (moyens de la partie adverse en contentieux des étrangers — ex. la préfecture/l'administration défendant l'OQTF / le refus de titre : thèse + fondements CESEDA/CJA + pièces).
  - + `SYSTEM_PROMPT_FAMILLE_FR` (moyens de la partie adverse en contentieux familial — ex. l'autre époux/parent : thèse + fondements Code civil + pièces).
  - Généraliser la sélection : `systemPromptFor(domain, country)` → prompt travail/immigration/famille si `FRANCE`, sinon `null` ; `extract` appelle le LLM si prompt non null, sinon `List.of()`. Conserver `temperature=0`, gate `AiCallContext` SYSTEM_CASE_CONCLUSION, anti-invention, fail-open.

## Plan de test
- [ ] `AdverseMoyensExtractorTest` : immigration FR → appelle LLM (mocké) → parse ; famille FR → idem ; BE / domaine inconnu → vide ; non-régression travail FR.

## Préoccupations transversales
- [x] Aucune (prompt système ; pas d'endpoint/schéma/UI).

## Dépendances
- SF-261-02 (framework + travail FR) — `done` (PR #1620).

## Notes
- Vagues domaine pures (prompts) — même structure que les vagues de F-98. Le mécanisme d'injection/réfutation est déjà uniforme.

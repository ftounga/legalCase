# Mini-spec — F-98 / SF-98-60 — Demandes subsidiaires dans les conclusions

> Phase 3 de l'audit F-98. Enrichissement de la garde rédactionnelle commune (famille SF-98-55).
> Backend-only (garde de prompt) — étapes 0 / 0 bis non applicables (aucun élément d'UI nouveau, aucun nouvel endpoint, aucun nouveau workflow ; enrichit le contenu généré, comme SF-98-55).

## Identifiant
`F-98 / SF-98-60`

## Statut
`ready`

## Branche
`feat/SF-98-60-demandes-subsidiaires`

## Objectif
> Structurer le dispositif des conclusions en « À titre principal » / « À titre subsidiaire » lorsque la logique juridique le justifie, en plaidant subsidiairement les chefs qui restent dus même si la demande principale échoue.

## Problème (audit LEMAIRE)
Le dispositif généré exposait les demandes principales mais n'articulait pas de **demandes subsidiaires** (manque de complétude vs la pratique : ex. indemnités légales de rupture dues même si l'absence de cause réelle et sérieuse n'est pas retenue). La garde `REDACTION_QUALITY_GUARD` (SF-98-55) couvre les postes systématiques (art. 700, intérêts, capitalisation, astreinte) mais pas le niveau principal/subsidiaire.

## Correctif
Ajout d'un **point 5 « Demandes subsidiaires »** à `REDACTION_QUALITY_GUARD` (prompt système commun → couvre les 45 cellules sans toucher aux trames) :
- structurer le dispositif « À titre principal » / « À titre subsidiaire » quand la logique le justifie ;
- plaider subsidiairement les chefs dus indépendamment (indemnités légales de rupture : licenciement, préavis, congés payés afférents ; rappels de salaire incontestables) ;
- **garde anti-invention** : aucun chef non étayé par les faits / pièces / verdicts ; à défaut d'élément fondant, ne pas en ajouter (non-régression anti-hallucination).

## Critères d'acceptation
- [ ] La garde système contient le point « Demandes subsidiaires » (`À titre subsidiaire`, garde anti-invention) sur toute cellule.
- [ ] Validation staging : un acte généré sur un dossier de licenciement présente, le cas échéant, un dispositif « À titre principal » / « À titre subsidiaire » avec des chefs subsidiaires plausibles.
- [ ] Non-régression : aucun chef subsidiaire inventé sans fondement (anti-hallucination) ; non-régression des assertions SF-98-55 (anti-jargon, dispositif, syllogisme).

## Hors scope
- Forfait jours (déjà exploité — gain marginal, traité séparément si besoin).
- Tout calcul/chiffrage backend de montants subsidiaires (le LLM s'appuie sur les verdicts d'outils + pièces fournis).

## Technique
- `CaseConclusionPromptBuilder.REDACTION_QUALITY_GUARD` : ajout point 5. Aucun changement d'API, de schéma, ni de frontend.

## Plan de test
- [ ] UT `CaseConclusionPromptBuilderTest.buildSystemPrompt_includesRedactionQualityGuard_onEveryCell` : assertions sur le point 5.
- [ ] Validation staging (régénérer un dossier licenciement → observer le bloc subsidiaire).

## Préoccupations transversales
- [x] Aucune (prompt système ; pas d'auth/workspace/plan/navigation/endpoint/schéma).

## Notes
- Même mécanisme que SF-98-55 : un seul changement de garde couvre les 45 cellules.

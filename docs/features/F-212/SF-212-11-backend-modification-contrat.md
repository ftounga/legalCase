# SF-212-11 — Backend : outil décisionnel « modification du contrat — refus du salarié »

> Feature F-212. Outil : `F-DT-70-modification-contrat-refus`. Fondement : jurisprudence Cass. soc. sur la distinction modification du contrat / changement des conditions de travail ; L. 1222-6 CT (modification pour motif économique).

## Objectif

Fournir le moteur backend qui analyse si une mesure de l'employeur constitue une **modification du contrat** (nécessitant l'accord du salarié) ou un simple **changement des conditions de travail** (relevant du pouvoir de direction), et évalue les conséquences du refus du salarié.

## Comportement nominal

`POST /api/v1/case-files/{caseFileId}/modification-contrat-refus` + `GET`.

L'analyseur distingue :
- **Éléments essentiels du contrat** (modification = accord nécessaire) : rémunération, qualification professionnelle, durée du travail contractuellement stipulée, lieu de travail (si clause explicite).
- **Conditions de travail** (pouvoir de direction, pas d'accord nécessaire) : horaires dans la plage contractuelle, lieu dans le même secteur géographique, tâches dans la qualification.

En cas de **modification pour motif économique** (L. 1222-6) : notification écrite + délai de réflexion 1 mois (2 mois si PSE).

**Conséquences du refus** :
- Modification sans motif éco : l'employeur doit soit renoncer, soit licencier → licenciement sans cause réelle et sérieuse si modification illégitime.
- Modification pour motif éco (L. 1222-6) : refus dans le délai → licenciement économique.

Verdict `AnalyseModificationContrat` : `MODIFICATION_CONTRAT` / `CHANGEMENT_CONDITIONS_TRAVAIL` / `INCERTAIN` + conséquences du refus.

## Cas d'erreur

- `caseFileId` hors workspace → 404.
- Domaine ≠ `DROIT_DU_TRAVAIL` → 422.
- Corps invalide → 400.

## Contrat API (figé — référence pour SF-212-12)

```
POST /api/v1/case-files/{caseFileId}/modification-contrat-refus
Request {
  elementModifie: enum REMUNERATION|QUALIFICATION|DUREE_TRAVAIL|LIEU_TRAVAIL|HORAIRES|TACHES|AUTRE,
  elementExplicitementContractualisé: boolean,
  motifEconomique: boolean,
  notificationEcriteL1222_6: boolean|null,
  delaiReflexionRespecteMois: int|null,
  reponsesSalarie: enum REFUS|ACCEPTATION|EN_ATTENTE
}
Response 200 {
  ...inputs (snapshot),
  analyseModification: MODIFICATION_CONTRAT|CHANGEMENT_CONDITIONS_TRAVAIL|INCERTAIN,
  scoreModificationContrat: int,
  consequences: [{type, description, fondement}],
  alerteDelaiReflexion: boolean,
  basesJuridiques: [String],
  messages: [String],
  calculatedAt: Instant
}
GET …/modification-contrat-refus → 200 | 204
```

`critereCode` F-IA-03 : `DT70_ELEMENT_CONTRACTUALISÉ`, `DT70_MOTIF_ECONOMIQUE`, `DT70_NOTIFICATION_L1222_6`, `DT70_DELAI_REFLEXION`.

## Pré-remplissage IA (invariant F-246)

Extension `TravailExtractedData` — objet `modification_contrat_detail` :
`modifContratElementModifie`, `modifContratContractualise`, `modifContratMotifEco`, `modifContratNotifEcrite`.
Extension `LegalDomainPromptBuilder`.

## Critères d'acceptation

1. Rémunération modifiée + contractualisée → `MODIFICATION_CONTRAT`.
2. Horaires modifiés + non contractualisés → `CHANGEMENT_CONDITIONS_TRAVAIL`.
3. Motif éco + notification L. 1222-6 non envoyée → `alerteDelaiReflexion = true`.
4. Conséquences du refus différentes selon motif éco / non éco.
5. 422 hors `DROIT_DU_TRAVAIL`.
6. Isolation workspace → 404.
7. `tool_id=F-DT-70-modification-contrat-refus` dans `KNOWN_FRONTEND_TOOL_IDS`.

## Plan de test

- **UT `ModificationContratRefusCalculatorTest`** : chaque `elementModifie` ; avec/sans contractualisation ; avec/sans motif éco.
- **IT `ModificationContratRefusControllerIT`**.

## Tables / endpoints / composants impactés

- **Nouvelle table** `modification_contrat_refus_analyses`.
- **Seed** : `tool_id=F-DT-70-modification-contrat-refus`, `trigger_field=modification_contrat_refusee`, `trigger_value=true`.
- Nouveaux fichiers + modifications standard.

## Hors périmètre

- Frontend (→ SF-212-12).
- Génération du courrier de refus (F-98).

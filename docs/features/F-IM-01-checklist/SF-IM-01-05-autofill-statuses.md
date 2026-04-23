# Mini-spec — SF-IM-01-05 Pré-remplissage automatique statuts checklist

## Objectif
Au premier chargement de la checklist d'un dossier, passer automatiquement à **PRESENT** les pièces déjà détectées dans les documents uploadés (via F-145 `DocumentPieceType`). Réduit le travail manuel de cochage pour l'avocat.

## Comportement
- À l'ouverture d'une checklist (GET `/checklist?titreType=X&country=Y`), si **aucun** `ImmigrationPieceCheck` n'existe encore pour ce triplet `(caseFileId, titreType, country)`, déclencher un pré-remplissage automatique.
- Pour chaque item du référentiel, matcher contre les `DocumentPieceType` détectés dans le dossier via un mapping libellé ↔ type de pièce.
- Items matchés → status `PRESENT` + persisté en DB.
- Items non matchés → status `INCONNU` (comportement actuel).
- Si l'avocat a déjà modifié (au moins 1 check existe) : **ne pas écraser**.

## Mapping libellé checklist ↔ DocumentPieceType
Statique dans un helper Java (simple, stable, testable) :
- "Passeport…" → `PASSEPORT`
- "Acte de mariage…" → `ACTE_MARIAGE`
- "Acte de naissance…" → `ACTE_NAISSANCE` ou `ACTE_NAISSANCE_ENFANT`
- "Justificatif de domicile…" / "…bail…" → `BAIL_LOCATION` ou `QUITTANCE_LOYER` ou `ATTESTATION_HEBERGEMENT`
- "Contrat de travail…" → `CONTRAT`
- "Bulletins…" → `BULLETIN_PAIE`
- "Titre de séjour…" → `TITRE_DE_SEJOUR`
- "Diplôme…" → pas de mapping (type MANQUANT dans enum, rester INCONNU)
- "Avis d'imposition…" → `AVIS_IMPOSITION`
- "Visa…" → `VISA`
- "Récépissé…" → `RECEPISSE_PREFECTURE`
- "Décision OQTF…" → `DECISION_OQTF`
- "Attestation…" → `ATTESTATION`
- "Photo d'identité…" → `PHOTO` ou `PIECE_IDENTITE`

## Critères d'acceptation
- [ ] Service `ImmigrationPieceAutoFillService.matchPieceTypes(label)` retourne le Set de DocumentPieceType candidats pour un libellé
- [ ] `ImmigrationChecklistService.get()` appelle l'autofill si aucun check existant
- [ ] Tests : 6 items mappés / 1 non mappé / idempotence (pas d'écrasement si checks existants)
- [ ] Full suite backend verte

## Hors scope
- Matching pièces spécifiques avec contenu visuel (Vision) — V2
- Configuration du mapping via DB — V2

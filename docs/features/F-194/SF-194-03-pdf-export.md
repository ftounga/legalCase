# Mini-spec — F-194 / SF-194-03 Section "Pièces à demander au client" en début d'export PDF synthèse

## Identifiant

`F-194 / SF-194-03`

## Feature parente

`F-194` — Pièces manquantes markables + matérialisation au run enrichi

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-194-03-pdf-export-pieces`

## Pattern de référence

**SF-192-03 + SF-193-03**. La section F-194 est insérée APRÈS « Conformité procédurale » F-193 (qui suit elle-même « Stratégies retenues » F-192).

## Ordre des sections en début de PDF synthèse

1. Page de garde (existant)
2. **🎯 Stratégies retenues** (F-192 SF-192-03 mergée)
3. **🔍 Conformité procédurale validée par votre avocat** (F-193 SF-193-03 en cours)
4. **📎 Pièces à demander au client** (F-194 SF-194-03 — nouvelle, **valeur produit forte** : l'avocat envoie le PDF au client comme todo-list)
5. Timeline / Faits / Risques / etc. (existant)

---

## Objectif

Ajouter une section dédiée « 📎 Pièces à demander au client » en début de PDF synthèse, **présentée pour usage opérationnel** (l'avocat envoie le PDF au client comme todo-list de pièces à fournir). Visuellement plus saillante que les autres sections F-192/F-193 — c'est le **livrable principal** du PDF côté client.

---

## Comportement attendu

### Cas nominal

1. L'avocat clique « Exporter en PDF ». `PdfExportService.export(caseFile, synthesis, retainedPistes?, procedureChecksAlignment?, piecesAlignment?)` est appelé avec un nouveau 5ᵉ paramètre `piecesAlignment?: PieceManquanteAlignment[]`.
2. `SynthesisComponent.exportPdf()` charge avant export (en parallèle des 2 services F-192/F-193 déjà en place) :
   - **Nouveau** : `PieceManquanteAlignmentService.getForCaseFile(id)` (introduit par SF-194-02)
   - Timeout 5 s sur chacun (fail-open silencieux → `[]` si erreur)
3. Si ≥ 1 pièce statut À_DEMANDER, le PDF inclut une nouvelle section « 📎 Pièces à demander au client » insérée APRÈS « Conformité procédurale » F-193, avec le contenu suivant :
   - **Titre de section** : encadré navy/or **proéminent** (taille 18 bold, fond or léger en bandeau, avec bordure or — visuel commercial)
   - **Sous-titre** : « Pour avancer sur votre dossier, merci de transmettre les pièces suivantes : » (Inter regular 11)
   - **Liste à cases à cocher** (☐ pour rendu papier — visuel tableau avec colonnes "À fournir" + "Pièce" + "Destinataire" + "Date butoir") :
     - Pièce (Inter regular 12 + libellé)
     - Destinataire (Inter italique 9 gris si renseigné, sinon "Client" par défaut)
     - Date butoir = today + 14j formaté `JJ/MM/AAAA` JetBrains Mono 9
4. **Sous-section optionnelle** : si ≥ 1 pièce statut OBTENUE, un sous-bloc petit `✅ Pièces déjà reçues : N` (Inter regular 9 gris, sans liste détaillée — juste compteur — sinon le PDF devient trop long)
5. **Sous-section optionnelle** : si ≥ 1 pièce statut NON_APPLICABLE, sous-bloc petit `🚫 Pièces non applicables au dossier : N` (idem compteur)
6. Page break après la section (la todo-list peut être imprimée seule).

Si **aucune pièce statut À_DEMANDER** → section omise (cas nominal — pas de demande à formuler).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Endpoint timeout / 404 / 500 | Section omise, log warn console, fail-open |
| Aucune pièce À_DEMANDER | Section omise (cas nominal) |
| Toutes les pièces OBTENUE/NON_APPLICABLE | Section omise (cas nominal — rien à demander) |

---

## Critères d'acceptation

- [ ] **CA-01** : sur un dossier avec ≥ 1 pièce À_DEMANDER, l'export PDF inclut la section « 📎 Pièces à demander au client » en page 3-4 (après « Conformité procédurale »)
- [ ] **CA-02** : sur un dossier sans pièce À_DEMANDER, le PDF est identique au comportement actuel
- [ ] **CA-03** : layout en tableau case à cocher (☐) + Pièce + Destinataire + Date butoir
- [ ] **CA-04** : titre **proéminent** (encadré navy/or, taille 18 bold, fond or léger) — saillant pour usage opérationnel
- [ ] **CA-05 destinataire** : si pièce a `destinataire = "Préfecture"`, affichage "Préfecture" ; sinon par défaut "Client"
- [ ] **CA-06 date butoir** : today + 14j formaté `JJ/MM/AAAA` JetBrains Mono
- [ ] **CA-07 sous-blocs compteurs** : compteurs OBTENUE et NON_APPLICABLE affichés en sous-bloc petit (sans liste détaillée)
- [ ] **CA-08 ordre des sections** : Stratégies retenues (F-192) → Conformité procédurale (F-193) → Pièces à demander (F-194) → Timeline / Faits / etc.
- [ ] **CA-09 fail-open** : endpoint timeout → section omise, le reste du PDF se génère
- [ ] **CA-10 fail-open indépendant** : si F-192 ou F-193 échoue, F-194 continue (et inverse)
- [ ] **CA-11 visuel charte** : palette navy/or DESIGN_SYSTEM.md, fond or léger, bordure or, JetBrains Mono pour dates, Inter pour le reste
- [ ] **CA-12 page break** : la section finit par un page break (la todo-list peut être imprimée seule pour envoi au client)

---

## Périmètre

### Hors scope V1

- (a) Génération d'un PDF dédié « Liste des pièces à demander » (sans le reste de la synthèse) — V1 = section dans le PDF synthèse standard
- (b) Email automatique au client avec la todo-list en pièce jointe
- (c) Signature électronique de la liste reçue
- (d) Personnalisation du visuel (couleurs, logo cabinet) — V1 = charte DESIGN_SYSTEM.md
- (e) Multilingue — V1 = FR uniquement
- (f) Export Word / DOCX — V1 = PDF uniquement

---

## Technique

### Composants Angular impactés

- `PdfExportService.export(caseFile, synthesis, retainedPistes?, procedureChecksAlignment?, piecesAlignment?)` (signature étendue, 5ᵉ paramètre optionnel)
- `PdfExportService.buildPiecesADemanderSection(piecesAlignment): Content[]` (nouvelle méthode privée)
- `SynthesisComponent.exportPdf()` (étendu) — appel `PieceManquanteAlignmentService.getForCaseFile(id)` en parallèle
- `PieceManquanteAlignmentService` (réutilisé de SF-194-02)

### Migration

- [x] Aucune

---

## Plan de test

### Tests Jest (~8-10 tests)

- `PdfExportServiceTest` :
  - `export(caseFile, synthesis, [], [], [])` → PDF sans section pièces
  - `export(caseFile, synthesis, [], [], [pieceADemander])` → section + ligne tableau case à cocher
  - `export(caseFile, synthesis, [], [], [pieceObtenue])` → section absente (OBTENUE non listé en À_DEMANDER) MAIS compteur "✅ Pièces déjà reçues : 1" affiché si autres À_DEMANDER présentes
  - `export(caseFile, synthesis, [], [], [aDemander, obtenue, nonApp])` → section avec liste À_DEMANDER + 2 sous-compteurs
  - section insérée APRÈS « Conformité procédurale » et AVANT Timeline (vérifier index dans `content[]`)
  - encadré titre or + bordure or visible (vérifier styles pdfmake)
  - date butoir today + 14j formatée JetBrains Mono
- `SynthesisComponentTest` :
  - clic export → `PieceManquanteAlignmentService.getForCaseFile` appelé en parallèle des 2 autres services
  - timeout `piecesAlignment` → export quand même appelé avec `[]` en 5ᵉ argument

### Isolation workspace

- [x] Non applicable côté frontend pur

---

## Dépendances

- F-92 ✅
- F-192 SF-192-03 ✅
- F-193 SF-193-03 (en cours)
- **SF-194-01 backend** — endpoint `/pieces-manquantes-alignment`
- **SF-194-02 frontend** — `PieceManquanteAlignmentService` + modèle

---

## Notes et décisions

- **Décision 2026-05-06** : visuel **proéminent** (encadré or) pour cette section — c'est le **livrable principal** côté client (l'avocat envoie le PDF au client comme todo-list). Saillance volontaire vs sections F-192/F-193 plus discrètes (qui sont pour l'avocat).
- **Décision 2026-05-06** : compteurs OBTENUE/NON_APPLICABLE plutôt que listes détaillées — éviter d'allonger le PDF. L'info principale est ce qui reste à fournir (À_DEMANDER).
- **Décision 2026-05-06** : layout tableau case à cocher (☐) — facilite l'usage papier (le client coche au fur et à mesure).
- **Décision 2026-05-06** : page break après la section — la todo-list peut être détachée et imprimée seule.
- **Décision 2026-05-06** : ordre des sections dans le PDF reflète la priorité de lecture pour le client (stratégie → conformité procédurale → pièces à fournir → faits du dossier).

# SF-211-04 — Pacte successoral global BE 2018 — backend

## Objectif (1 phrase)
Vérifier les conditions formelles et de fond d'un pacte successoral global belge (Loi 31/07/2017 modifiant CC art. 1100/1+ — entrée en vigueur 01/09/2018) et qualifier le risque d'annulation.

## Comportement nominal
- POST `/api/v1/case-files/{caseFileId}/pacte-successoral-be-2018-analysis`
- Body : `dateSignaturePacte` (LocalDate, requis), `acteAuthentique` (boolean — notaire), `accordTousHeritiersReservataires` (boolean), `equilibreDonationsRapportables` (boolean), `presenceTousHeritiersReservataires` (boolean)
- Calculator `PacteSuccessoralBe2018Calculator` calcule :
  - Vérifie que la signature ≥ 01/09/2018 (entrée en vigueur loi)
  - Vérifie acte authentique notarié (obligatoire — sinon NUL)
  - Vérifie présence + accord de tous héritiers réservataires (sinon CONTESTABLE/NUL)
  - Vérifie équilibre des donations rapportables (sinon CONTESTABLE)
  - `verdict` ∈ {VALIDE, CONTESTABLE, NUL}
  - Liste des `motifsAnnulation` / `motifsContestabilite`
- Persistance 1:1 `pacte_successoral_be_2018_analyses`
- GET → 200 ou 404

## Cas d'erreur
- 400 si dateSignaturePacte futur ou < 2018-09-01 (irrecevabilité absolue — concept inexistant avant)
- 400 si paramètres null
- 400 si workspace.country ≠ BELGIQUE
- 400 si caseFile.legalDomain ≠ DROIT_FAMILLE
- 404 isolation workspace

## Critères d'acceptation vérifiables
- [x] Pacte authentique + tous présents + tous d'accord + équilibre → VALIDE
- [x] Acte sous seing privé → NUL
- [x] Manque accord héritier → CONTESTABLE (ou NUL si absent du pacte)
- [x] Déséquilibre donations → CONTESTABLE
- [x] Date pré-2018 → 400
- [x] POST FR → 400
- [x] GET sans POST → 404

## Plan de test minimal
- **UT** `PacteSuccessoralBe2018CalculatorTest` : 10+ tests (verdicts VALIDE/CONTESTABLE/NUL, chaque condition échouée, date pré-2018, dates futures, paramètres invalides)

## Tables / endpoints / composants impactés
- **Nouvelle table** `pacte_successoral_be_2018_analyses` (id, case_file_id UNIQUE, date_signature_pacte DATE NOT NULL, acte_authentique BOOLEAN NOT NULL, accord_tous_heritiers_reservataires BOOLEAN NOT NULL, equilibre_donations_rapportables BOOLEAN NOT NULL, presence_tous_heritiers_reservataires BOOLEAN NOT NULL, country VARCHAR(20) NOT NULL, result_data TEXT NOT NULL, timestamps)
- **Migration** `227-create-pacte-successoral-be-2018-analyses.xml` (table — pas de seed visibility ici)
- **Endpoint** `PacteSuccessoralBe2018Controller` (POST, GET)

## Hors périmètre
- Composant Angular (SF F-211 frontend ultérieure)
- Seed `decision_tool_visibility_rules` (différé pour CI verte)
- Pactes successoraux ponctuels (CC art. 1389+) — couvert par autre outil futur

## Impact par domaine métier
**Sensible Famille BE uniquement.** Concept FR équivalent : pacte successoral renforcé FR (art. 1078-4+ CC FR — donation-partage transgénérationnelle, RAAR — renonciation anticipée à l'action en réduction) — couvert par F-FA distinct. Aucun impact Travail / Immigration.

## Parité des domaines métier
Niveau 5 (analyse validité VALIDE/CONTESTABLE/NUL). FR équivalent à traiter en feature jumelle si nécessaire (pacte successoral renforcé FR — RAAR — non couvert à ce jour, ouvre opportunité backlog hors F-211).

## Analyse de cohérence transversale
- **Référence pattern** : F-208 (verdict + motifs liste).
- **Pas de chevauchement** avec partage-successoral / indivision / rapport-succession (F-FA-20/21/22) — concepts orthogonaux (pacte = anticipation vivante / partage = répartition après décès).

## Audit "Impact F-166 cross-C×D"
- **BE×Famille** : nouvel outil contextuel (trigger `pacte_successoral_envisage=true` flag F-202). Seed différé.
- **FR×Famille** : équivalent non couvert (RAAR / donation-partage transgénérationnelle) — opportunité backlog hors F-211.
- Autres : non concernés.

## Audit "exhaustivité droit national BE"
- Source juridique : Loi du 31/07/2017 modifiant CC art. 1100/1+ et s. — entrée en vigueur 01/09/2018.
- Conditions formelles : acte authentique notarié OBLIGATOIRE (art. 1100/5).
- Conditions de fond : présence et accord de tous les héritiers réservataires (art. 1100/2).
- Équilibre des donations rapportables (art. 1100/7).
- FR équivalent : pacte successoral renforcé (art. 1078-4+ CC FR — donation-partage transgénérationnelle, RAAR art. 929 CC FR) — non couvert à ce jour, opportunité backlog.

## Contrat API
**POST** `/api/v1/case-files/{caseFileId}/pacte-successoral-be-2018-analysis`
```json
{
  "dateSignaturePacte": "2025-04-15",
  "acteAuthentique": true,
  "accordTousHeritiersReservataires": true,
  "equilibreDonationsRapportables": true,
  "presenceTousHeritiersReservataires": true
}
```
Réponse :
```json
{
  "caseFileId": "...",
  "country": "BELGIQUE",
  "dateSignaturePacte": "2025-04-15",
  "acteAuthentique": true,
  "accordTousHeritiersReservataires": true,
  "equilibreDonationsRapportables": true,
  "presenceTousHeritiersReservataires": true,
  "verdict": "VALIDE",
  "motifsAnnulation": [],
  "motifsContestabilite": [],
  "formule": "Pacte successoral global signé le ...",
  "baseJuridique": "Loi 31/07/2017, CC art. 1100/1 à 1100/7",
  "messages": ["..."]
}
```

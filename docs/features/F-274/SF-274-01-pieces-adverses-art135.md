# SF-274-01 — Garde « pièces adverses » : communication & rejet des pièces tardives (art. 132/135 CPC)

> Feature **F-274** (Conclusions V4 ④). Étape 0 : `SF-274-00-coherence.md` — verdict **GO avec ajustements**.
> Étape 0 bis : **non requise** (backend-only, aucun impact écran — cf. invariant I6).

## Objectif (une phrase)

Doter le générateur de conclusions du réflexe du contradictoire sur les pièces adverses — **demande de communication** (art. 132 CPC) et **rejet des pièces communiquées tardivement** (art. 135 CPC) — via une garde de prompt transverse, FR uniquement, auto-conditionnée à la présence d'écritures/pièces adverses au dossier.

## Comportement nominal

1. À l'assemblage du prompt système (`buildSystemPrompt`), pour une cellule **FR** (tous domaines, demandeur comme défendeur), injecter une garde `ADVERSE_PIECES_GUARD` qui demande au modèle, **lorsque** la partie adverse a communiqué des écritures/pièces (sections `MOYENS ADVERSES À RÉFUTER` / `JURISPRUDENCE ADVERSE À RÉFUTER` présentes dans le message utilisateur) :
   - de **solliciter la communication** des pièces que l'adversaire vise sans les avoir communiquées (art. 132 CPC), sous forme de prétention au dispositif et de développement en discussion ;
   - le cas échéant, de **demander le rejet des débats** des pièces communiquées en violation du contradictoire / tardivement (art. 135 CPC), uniquement si une tardiveté/violation ressort des éléments du dossier ;
   - de viser les **articles applicables** (132, 135, et le cas échéant 15-16 CPC) sans exposer de jargon interne ni de nom de fichier.
2. Pour une cellule **BE**, ou en l'absence de toute écriture/pièce adverse, la garde est **inopérante** (no-op) : aucune rubrique vide, aucune invention.

## Cas d'erreur / garde-fous

- **No-op silencieux** : la garde n'oblige PAS à produire ces prétentions ; elle ne s'active qu'en présence réelle d'éléments adverses. À défaut → aucune section ajoutée (symétrie du point 5 de PROCEDURE_ORDER_GUARD).
- **Anti-invention** : interdiction de demander la communication ou le rejet d'une pièce sans base factuelle dans le dossier ; interdiction d'inventer une date de communication tardive.
- **Anti-jargon (non-régression SF-98-55)** : viser les articles du CPC, jamais un libellé d'outil ni un nom de document.
- **Cellule inconnue** : `buildSystemPrompt` lève déjà `IllegalStateException` (comportement inchangé).

## Critères d'acceptation vérifiables

- CA1 — Le prompt système d'une cellule **FR demandeur** contient la garde (mots-clés : « article 132 », « article 135 », « communiqu », « écarter des débats »).
- CA2 — Le prompt système d'une cellule **FR défendeur** contient la garde.
- CA3 — Le prompt système d'une cellule **BE** ne contient PAS la garde.
- CA4 — La garde porte explicitement l'auto-conditionnement (« lorsque la partie adverse a communiqué ») et l'anti-invention (« n'invente aucune pièce ni aucune date »).
- CA5 — La garde ne réintroduit aucun jargon interne (pas de « F-DT », pas de nom de fichier).
- CA6 — Non-régression : REDACTION_QUALITY_GUARD (point 8 réfutation), PROCEDURE_ORDER_GUARD (in limine litis), JURISPRUDENCE_GUARD restent présents et inchangés sur une cellule FR défendeur.
- CA7 — `appliesAdversePiecesGuard(null)` → `false` ; cellule FR → `true` ; cellule BE → `false`.

## Plan de test minimal (unitaire)

`CaseConclusionPromptBuilderTest` (mêmes fixtures que F-272/F-273) :
- garde présente sur cellule FR demandeur (CA1) ;
- garde présente sur cellule FR défendeur (CA2) ;
- garde absente sur cellule BE (CA3) — via une cellule CPH/FOND/DEFENDEUR BELGIQUE (provider existant ? sinon vérifier l'absence par `appliesAdversePiecesGuard` directement) ;
- garde porte auto-conditionnement + anti-invention (CA4) ;
- garde sans jargon (CA5) ;
- coexistence avec PROCEDURE_ORDER_GUARD + REDACTION_QUALITY_GUARD sur FR défendeur (CA6) ;
- `appliesAdversePiecesGuard` : null → false, FR → true, BE → false (CA7).

**Isolation workspace** : sans objet (garde statique dérivée de la seule `CombinationKey`, aucune donnée cross-workspace).

## Tables / endpoints / composants impactés

- **Tables** : aucune. **Endpoints** : aucun. **Migration Liquibase** : aucune. **Frontend** : aucun.
- **Composant** : `CaseConclusionPromptBuilder` (constante `ADVERSE_PIECES_GUARD` + méthode `appliesAdversePiecesGuard` + injection conditionnelle dans `buildSystemPrompt`).
- **Tests** : `CaseConclusionPromptBuilderTest`.

## Préoccupations transversales

- Auth / Principal : non. Workspace context : non. Plans / limites : non. Navigation / routing : non.
- **Outil décisionnel** : **non** — c'est une garde de prompt, pas un outil (invariant « un outil = une situation » préservé ; aucune `decision_tool_visibility_rules`).
- Smoke E2E : non requis (aucun déclencheur transversal coché).

## Hors périmètre

- BE (Code judiciaire — régime de communication propre).
- Détection automatique/déterministe de la tardiveté (dates de communication structurées) — non disponible ; la garde reste qualitative et fondée sur les seuls éléments fournis.
- Tout écran, endpoint, persistance.

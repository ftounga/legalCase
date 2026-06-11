# SF-275-01 — Auto-remplissage de l'en-tête POUR / CONTRE des conclusions depuis la position du dossier

> Feature : **F-275** (Conclusions V4 ⑤). Étape 0 : `SF-275-00-coherence.md` (verdict **GO avec ajustements**, recentré sur l'orientation POUR/CONTRE).
> Étape 0 bis : **non requise** (backend-only, aucun impact écran).

## Objectif (une phrase)

Orienter l'en-tête des conclusions générées en « POUR [le client] / CONTRE [l'adversaire] » de façon déterministe à partir de la position procédurale du dossier, en réutilisant les identités déjà extraites (SF-98-61), sans rien inventer (`[à compléter]` reste la valeur honnête pour tout champ absent).

## Comportement nominal

1. À la génération de conclusions, la section « IDENTITÉ DES PARTIES » du message utilisateur (SF-98-61) est **annotée** avec la position procédurale : indication explicite de quelle partie est le **client (POUR)** et laquelle est l'**adversaire (CONTRE)**, à partir de la position de la combinaison procédurale.
2. La correspondance position↔rôle pour le travail réutilise celle déjà tranchée par F-269 : SALARIÉ = demandeur (POUR si position demanderesse), EMPLOYEUR = défendeur. Pour les positions de défense, l'orientation s'inverse.
3. Le **point 6 de REDACTION_QUALITY_GUARD** est renforcé : exiger un en-tête structuré « POUR … » / « CONTRE … » repris des identités fournies, en plaçant le client du bon côté selon l'annotation de position, et `[à compléter]` uniquement pour les champs réellement absents.

## Cas d'erreur / limites

- **Position inconnue / non mappable** (domaine sans correspondance position↔rôle explicite, ou position absente) → l'annotation d'orientation est **omise** (no-op) : la section IDENTITÉ reste telle quelle, le point 6 retombe sur le comportement actuel (reprise des identités, `[à compléter]` à défaut). Aucune inversion devinée à tort.
- **Aucune identité extraite** (immigration / famille, ou travail sans identité détectée) → section IDENTITÉ absente (no-op SF-98-61 inchangé) ; l'en-tête sera `[à compléter]` côté modèle. Aucune rubrique vide.
- **JSON d'analyse illisible / absent** → fail-open existant (SF-98-61) : aucune section, aucune exception propagée.

## Critères d'acceptation vérifiables

- CA1 — Pour un dossier travail FR en position **demanderesse** avec identités salarié+employeur, la section IDENTITÉ DES PARTIES indique que le SALARIÉ est le client (POUR) et l'EMPLOYEUR l'adversaire (CONTRE).
- CA2 — Pour un dossier travail FR en position de **défense** (employeur défendeur), l'annotation indique l'EMPLOYEUR comme client (POUR) et le SALARIÉ comme adversaire (CONTRE).
- CA3 — Pour une **position non mappable** (ou null), aucune annotation POUR/CONTRE n'est ajoutée (la section reste celle de SF-98-61 ; pas d'inversion devinée).
- CA4 — Le point 6 de REDACTION_QUALITY_GUARD exige l'en-tête « POUR / CONTRE » et conserve `[à compléter]` + l'interdiction d'inventer une adresse/identité (non-régression SF-98-55).
- CA5 — Sans identité extraite (immigration/famille), aucune section IDENTITÉ et aucune annotation (no-op).
- CA6 — Les gardes existantes (JURISPRUDENCE_GUARD, PROCEDURE_ORDER_GUARD, ADVERSE_PIECES_GUARD, points 1-5 et 7-10 de REDACTION_QUALITY_GUARD) restent inchangées.

## Plan de test minimal

Tests unitaires `CaseConclusionPromptBuilderTest` (package conclusion) :
- T1 (CA1) — position demanderesse travail FR → annotation « client (POUR) = … SALARIÉ » + « adverse (CONTRE) = … EMPLOYEUR ».
- T2 (CA2) — position défense travail FR → annotation inversée (employeur POUR).
- T3 (CA3) — position null / non mappable → section IDENTITÉ sans ligne d'orientation POUR/CONTRE.
- T4 (CA4) — `buildSystemPrompt` contient l'exigence « POUR » / « CONTRE » au point 6, et conserve « [à compléter] » + « n'invente jamais d'adresse ».
- T5 (CA5) — sans identité → pas de section IDENTITÉ, pas d'annotation.
- T6 (CA6) — non-régression : les 4 gardes coexistent ; les autres points (5, 7, 8, 10) du REDACTION_QUALITY_GUARD restent présents.
- **Isolation workspace** : N/A — assemblage de prompt pur, sans accès données cross-workspace (l'input est déjà résolu par `CaseConclusionService` sur le dossier courant). Le test de coverage IT existant du package reste vert.

## Tables / endpoints / composants impactés

- **Composant** : `CaseConclusionPromptBuilder` (méthode `appendPartiesIdentity` enrichie d'un paramètre d'orientation + `REDACTION_QUALITY_GUARD` point 6). Le helper d'orientation est dérivé de `positionLabel`/`positionCode` déjà dans `ConclusionPromptInput`.
- **Aucune** nouvelle table, **aucun** endpoint, **aucun** écran, **aucune** migration Liquibase.
- **Préoccupations transversales** : aucune (pas d'auth, pas de workspace context nouveau, pas de plan/limite, pas de navigation, pas d'outil décisionnel). Pas de smoke E2E requis.

## Hors périmètre

- Ré-extraction ou nouvelle clé d'identités (SF-98-61 reste la source — pas de nouvelle extraction IA).
- Identités pour immigration / famille (aucune extraction d'identité existante ; `[à compléter]` honnête).
- Champ éditable d'identité côté écran / formulaire de saisie manuelle (aucun impact UI).
- BE : pas d'élargissement spécifique ; l'orientation est transverse, la donnée reste fournie où elle existe.

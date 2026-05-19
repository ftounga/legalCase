# F-121 / SF-121-06 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Quand une pièce d'un dossier ne peut pas être lue automatiquement, l'avocat doit voir clairement quoi faire et pouvoir le faire sur place — réessayer/corriger ou retirer la pièce — au lieu de rester face à une barre de progression rouge sans issue.

## Workflow métier réel de l'utilisateur cible

Source : pratique standard avocat + **signal terrain documenté** — incident production RENVERSEZ du 2026-05-19 (dossier `stanojevic`) : l'avocate a uploadé son dossier, vu une barre rouge, supprimé des doublons, ré-uploadé deux fois le même bordereau de pièces, sans jamais comprendre comment sortir de l'état d'échec.

1. L'avocat constitue son dossier : il rassemble les pièces (contrat, bulletins, courriers, attestations, scans, photos).
2. Il verse les pièces dans LegalCase (upload).
3. LegalCase extrait le texte de chaque pièce puis l'analyse.
4. Certaines pièces — scans de mauvaise qualité, photos, PDF trop lourds — échouent à l'extraction.
5. L'avocat doit **s'apercevoir** qu'une pièce n'a pas été exploitée.
6. Il **juge** si cette pièce est importante pour l'analyse.
7. Si oui : il **corrige** — re-scanne mieux, découpe le PDF trop lourd, ré-uploade (« réessayer »).
8. Si non, ou s'il n'a pas le temps : il **retire** la pièce du périmètre pour ne pas rester bloqué (« marche arrière »).
9. Il poursuit : l'analyse du dossier doit aboutir sur les pièces exploitables.
10. Il consulte la synthèse.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Constitution du dossier | hors outil | — |
| 2. Upload des pièces | F-43 import / upload de documents | ✅ Livrée |
| 3. Extraction + analyse | F-3/4/5 pipeline IA + F-122 OCR | ✅ Livrée |
| 4. Échec d'extraction sur certaines pièces | F-121-01 détection des motifs d'échec | ✅ Livrée |
| 5. L'avocat s'aperçoit de l'échec | F-121-02 badge « Non analysable » + notif in-app/email · F-121-04 step 2 « N non analysables / M » | ✅ Livrée |
| 6. Juger l'importance de la pièce | jugement avocat | hors outil |
| 7. Réessayer / corriger | F-122-05 retry OCR (motifs `EMPTY_TEXT`/`OCR_FAILED` seulement) · re-upload manuel | 🟡 Partiel |
| 8. Marche arrière : retirer la pièce | suppression de document (`DocumentDeleteService` + bouton liste docs) · SF-121-05 rend le bouton de nouveau utilisable | ✅ Livrée |
| 9. L'analyse aboutit sur le reste | SF-121-05 (job `DOCUMENT_ANALYSIS` terminal sur échec partiel) | ✅ Livrée |
| 10. Synthèse | F-3/4/5 | ✅ Livrée |

## Position de la nouvelle feature

SF-121-06 s'insère aux **étapes 5, 7 et 8** : la **lisibilité** de l'échec et la **mise à portée** des actions de récupération. Elle ne crée aucune brique métier nouvelle — toutes existent — elle **relie** ce que le produit détecte (étape 5) aux actions qu'il sait déjà faire (étapes 7-8).

## Challenge amont

Chaque étape avant SF-121-06 est-elle couverte ?
- Détection de l'échec (étape 4) : ✅ F-121-01.
- Signalement de l'échec (étape 5) : ✅ F-121-02 / F-121-04.
- Action « retirer » (étape 8) : ✅ suppression de document, débloquée par SF-121-05.
- Action « réessayer » OCR (étape 7) : ✅ F-122-05 pour `EMPTY_TEXT`/`OCR_FAILED`.

**Aucun trou fonctionnel amont.** Toutes les briques sont livrées.

## Challenge aval

Après que l'avocat a retiré ou corrigé la pièce :
- L'analyse du dossier aboutit sur les pièces exploitables : ✅ SF-121-05.
- La synthèse est produite : ✅ F-3/4/5.

**Aucun trou fonctionnel aval.**

## STOPs / pré-requis à ajouter au backlog

Aucun. Le seul « trou » est un trou de **parcours / lisibilité** — le produit détecte l'échec et possède les actions, mais ne les relie pas : l'avocat voit un état rouge sans chemin de sortie. C'est exactement le périmètre de SF-121-06, pas un pré-requis manquant.

## Invariants anti-gadget pour la mini-spec

1. **Frontend pur.** Le déblocage backend est déjà livré (SF-121-05). SF-121-06 ne touche aucun endpoint, aucune table, aucun job.
2. **Aucune action mensongère.** Le chemin proposé doit pointer vers une action qui aboutit réellement : « retirer le document » (existe). Ne PAS afficher un bouton « réessayer » générique pour `OCR_UNSUPPORTED_SIZE` — l'OCR refuserait le fichier à l'identique. Pour ce motif, le message dit la vérité : « fichier trop volumineux, divisez-le ».
3. **Message spécifique au motif d'échec** (`failureReason`) : `OCR_UNSUPPORTED_SIZE` → « trop volumineux, découpez le fichier » ; `EMPTY_TEXT`/`OCR_FAILED` → renvoyer vers le retry OCR existant (F-122-05) ; `CORRUPTED`/`UNSUPPORTED_FORMAT` → « fichier illisible, remplacez-le ». Pas de message générique unique.
4. **Ne pas masquer le signal.** F-121-04 a délibérément rendu la step 2 visible (rouge) pour avertir que la synthèse peut être incomplète. SF-121-06 **garde l'avertissement** et le rend actionnable — il ne le supprime pas. Toute évolution de la couleur/sémantique de la step 2 est une modification explicite d'une décision F-121-04, à acter dans l'étape 0 bis / la mini-spec, jamais en silence.
5. **Récupération sans dépendre du support.** À l'issue de SF-121-06, l'avocat sort de l'état d'échec seul, sans intervention de notre part — c'est le critère de réussite.

## Décision finale

**GO.** Toutes les briques amont et aval sont livrées (dont SF-121-05 qui vient de débloquer la suppression). SF-121-06 est une feature de **lisibilité de parcours** à l'intérieur du périmètre F-121 : elle relie la détection d'échec aux actions de récupération existantes. Feature à impact écran → étape 0 bis (cohérence écran) requise avant la mini-spec.

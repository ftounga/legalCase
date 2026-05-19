# F-121 / SF-121-06 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

Quand une pièce échoue à l'extraction, l'avocat doit voir **quoi faire** et atteindre l'action de récupération sans la chercher. Comportement visible : un message d'échec **spécifique au motif** porté par la pièce concernée, et un signal de pipeline qui **oriente** vers l'action au lieu d'être un cul-de-sac rouge.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — toutes les briques amont/aval sont livrées (détection F-121-01, signalement F-121-02/04, suppression de document débloquée par SF-121-05). SF-121-06 est une feature de lisibilité de parcours, pas une brique fonctionnelle nouvelle.

## Parcours écran réel de l'avocat

Source : `docs/business/parcours-ecran-dossier.md` (référentiel) + écran réellement codé (`case-file-detail`, 4 onglets depuis F-244) + signal terrain (incident RENVERSEZ 2026-05-19).

1. L'avocat ouvre le dossier → écran **détail du dossier**, 4 onglets (Dossier / Analyse / Décision / Suivi).
2. Onglet **Dossier** : il importe les pièces et voit la **liste des documents**.
3. Une pièce échoue à l'extraction → dans la liste, elle porte le badge **« Non analysable »** + tooltip, et une icône **corbeille** (suppression).
4. Onglet **Analyse** : `app-analysis-pipeline` affiche la **step 2 « Analyse des documents »** en rouge avec le compteur « N non analysables / M ».
5. L'avocat doit comprendre que le dossier n'est pas cassé — seules N pièces n'ont pas pu être lues.
6. Il juge si ces pièces sont importantes.
7. S'il veut les garder : il corrige hors outil (re-scan, découpe du PDF trop lourd) puis ré-uploade (onglet Dossier).
8. S'il les abandonne : il **retire** la pièce via la corbeille de la liste (onglet Dossier) → le signal d'échec disparaît.
9. L'analyse aboutit sur les pièces exploitables (SF-121-05) ; il consulte la **synthèse** (onglet Analyse).
10. Onglet **Décision** : outils décisionnels, puis génération du **projet de conclusions**.
11. **État terminal** : projet de conclusions généré.

## État terminal du processus

Inchangé — « projet de conclusions généré » (tranché au cadrage F-98, cf. référentiel). SF-121-06 ne déplace pas l'état terminal ; elle garantit qu'un échec d'extraction **ne bloque pas le parcours avant** de l'atteindre.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone | Statut |
|---|---|---|
| 2-3. Liste des pièces, badge « Non analysable » | Onglet **Dossier** — `section#section-documents` / `docs-card` | ✅ existant |
| 3. Action de récupération (corbeille) | Onglet **Dossier** — bouton supprimer par ligne de document | ✅ existant |
| 4. Signal d'échec global | Onglet **Analyse** — `app-analysis-pipeline`, step 2 | ✅ existant |
| 7. Retry OCR (motifs `EMPTY_TEXT`/`OCR_FAILED`) | Onglet **Dossier** — bandeau F-122-05 | ✅ existant |
| Message actionnable spécifique au motif | — | ❌ **manquant — apport SF-121-06** |

## Position candidate de la feature

SF-121-06 **n'ajoute aucun bloc primaire**. Elle enrichit deux zones existantes :
- **Onglet Dossier — ligne du document en échec** : le badge « Non analysable » gagne un message spécifique au `failureReason` et l'action de récupération est explicite à côté.
- **Onglet Analyse — pipeline step 2** : le compteur « N non analysables » gagne une **orientation** vers l'onglet Dossier (là où se fait l'action).

## Challenge placement

L'action de récupération (supprimer / ré-uploader) vit dans la **liste des documents (onglet Dossier)** — c'est le bon endroit : l'avocat y est déjà à l'étape 2-3, et c'est là que la pièce existe. ✅ Le message actionnable doit donc être porté **prioritairement par la ligne du document** dans l'onglet Dossier. ✅ Placement cohérent.

## Challenge lisibilité de la séquence

⚠️ **Point dur** : le **signal** d'échec le plus visible (step 2 rouge) est sur l'onglet **Analyse**, alors que l'**action** de récupération est sur l'onglet **Dossier**. Un avocat qui regarde la barre rouge de l'onglet Analyse n'a aucune indication qu'il doit changer d'onglet pour agir → c'est précisément le cul-de-sac vécu le 2026-05-19. **Ajustement requis** : la step 2 en échec doit explicitement renvoyer vers l'onglet Dossier / la liste des pièces.

## Challenge charge écran

- Onglet **Dossier** : 3 blocs primaires (Métadonnées, Stats, Documents). SF-121-06 enrichit le contenu **interne** du bloc Documents (ligne de document) — **aucun bloc primaire nouveau**. Seuil ~3 respecté. ✅
- Onglet **Analyse** : 1 bloc (Pipeline). SF-121-06 enrichit le texte de la step 2 — **aucun bloc nouveau**. ✅

Aucune surcharge.

## Challenge état final / continuité

Aujourd'hui l'échec d'extraction est un **dead-end subi** : barre rouge, pas de chemin. Après SF-121-06, l'avocat dispose toujours d'un pas suivant explicite (retirer ou corriger la pièce) → il rejoint le parcours normal jusqu'à l'état terminal. ✅ Le retour en arrière (« retirer la pièce ») devient un **choix de design assumé**, plus un effet de bord.

## Ajustements IA requis

1. **Message porté par la ligne du document en échec** (onglet Dossier), **spécifique au `failureReason`** :
   - `OCR_UNSUPPORTED_SIZE` → « Fichier trop volumineux pour l'analyse automatique — divisez-le en fichiers plus légers et ré-uploadez. »
   - `EMPTY_TEXT` / `OCR_FAILED` → renvoyer vers le bandeau retry OCR existant (F-122-05) — ne pas dupliquer l'action.
   - `CORRUPTED` / `UNSUPPORTED_FORMAT` → « Fichier illisible — remplacez-le par une version valide. »
2. **La step 2 en échec (onglet Analyse) oriente** : le compteur « N non analysables / M » est accompagné d'un renvoi explicite vers l'onglet Dossier / la liste des pièces (lien ou mention). Le signal F-121-04 est **conservé** — on ne supprime pas l'avertissement, on le rend directionnel.
3. **Aucune action mensongère** : pas de bouton « réessayer » générique pour `OCR_UNSUPPORTED_SIZE` (l'OCR re-refuserait). L'action proposée pour ce motif est « retirer » ou « ré-uploader découpé ».

## Invariants anti-surcharge pour la mini-spec

- **Zéro bloc primaire nouveau** — SF-121-06 enrichit le contenu interne des zones Documents (onglet Dossier) et Pipeline (onglet Analyse). Seuil ~3 blocs/onglet préservé.
- **Le signal et l'action peuvent être sur deux onglets, mais le signal doit toujours nommer l'onglet de l'action** — pas de signal orphelin.
- **Tout message d'échec est spécifique au `failureReason`** — pas de texte générique unique.
- **F-121-04 préservé** : la step 2 reste un signal visible d'échec (la synthèse peut être incomplète) ; SF-121-06 le rend directionnel, ne le masque pas.

## Décision finale

**GO avec ajustements.** Placement correct (l'action vit dans la liste des documents, onglet Dossier), charge écran nulle (aucun bloc nouveau), continuité rétablie (l'échec cesse d'être un dead-end). Un ajustement structurant : **bridger le décalage inter-onglets** — le signal d'échec de l'onglet Analyse doit renvoyer vers l'onglet Dossier où se fait l'action. Les 3 ajustements IA ci-dessus sont à intégrer dans la mini-spec.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` enrichi : 5ᵉ passage — ajout de la gestion de l'échec d'extraction comme branche du parcours (étapes 3-8), et explicitation du décalage inter-onglets signal (Analyse) ↔ action (Dossier).

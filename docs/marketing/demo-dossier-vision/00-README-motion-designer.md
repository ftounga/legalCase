# Brief motion designer — VIDÉO 6 — Legal OCR + Legal Vision

> Vidéo finale de la série de 6. Met en scène la fonctionnalité phare **Legal OCR + Legal Vision** : LegalCase ingère et comprend des pièces de toute nature (PDF natifs, scans dégradés, photos téléphone, captures d'écran, vidéos), et bascule intelligemment entre deux moteurs de lecture selon le support.
>
> Durée cible : **1 minute 20 secondes**. Voir `script-video-6.md` pour le découpage détaillé.

---

## Le dossier de simulation : Sophie MARTIN — Ordonnance de protection

**Cliente** : Sophie MARTIN, 34 ans, infirmière
**Adresse** : 12 rue des Lilas, 75011 Paris
**Conjoint** : Julien VASSEUR, 36 ans
**Mariage** : 14 juillet 2018, mairie du 11<sup>e</sup> arrondissement
**Faits** : nuit du 3 au 4 mai 2026 — escalade de violences conjugales, conjoint tente de forcer la porte du domicile après que Sophie l'a verrouillée
**Avocate** : Maître Camille DUBOIS, barreau de Paris
**Demande** : ordonnance de protection (art. 515-9 et s. C. civ.) — délai 6 jours

> Tous les noms, adresses, dates et numéros sont fictifs. Aucune correspondance avec une affaire réelle.

---

## Vue d'ensemble — les 7 pièces du dossier

| # | Fichier | Format final | Qualité | Routage attendu | Pourquoi cette pièce |
|---|---|---|---|---|---|
| 01 | `01-acte-mariage.pdf` | PDF natif | Propre | Pipeline standard (ni OCR ni Vision) | Établit la baseline : LegalCase ne sur-traite pas ce qui est déjà propre |
| 02 | `02-main-courante-commissariat.pdf` | PDF scanné 10 pages contenant **4 sous-pièces** | Scan administratif lisible | **Legal OCR + détection sous-pièces** | Démontre la séparation automatique d'un PDF contenant plusieurs documents |
| 03 | `03-certificat-medical.jpg` | Photo téléphone JPG | Photo prise par la cliente, mal cadrée, doigt visible, tampon écrasé | **Legal OCR mode dégradé** + édition avocat | Démontre l'extraction sur fichier dégradé + correction manuelle de l'avocat |
| 04 | `04-sms-conjoint.png` | Capture d'écran iPhone | Native bonne qualité mais format conversationnel | **Legal Vision** | Aucun OCR classique ne sait reconstruire un fil de conversation |
| 05 | `05-porte-forcee.jpg` | Photo téléphone JPG | Scène sans texte | **Legal Vision** | Image pure — décrire une scène, c'est du Vision uniquement |
| 06 | `06-camera-voisin.mp4` | Vidéo MP4 | 12s, basse qualité, sans audio | **Legal Vision** (extraction frames clés) | Le cas extrême — peu d'outils traitent la vidéo en pièce de dossier |
| 07 | `07-facture-serrurier.jpg` | Photo téléphone JPG | Papier froissé, pris dans des conditions de luminosité difficiles | **Legal OCR mode dégradé** | Établit la temporalité des faits |

**Synthèse pour le moteur de routage** :
- 1 pièce → pipeline standard (PDF natif propre)
- 3 pièces → Legal OCR (dont 2 en mode dégradé)
- 3 pièces → Legal Vision

---

## Code couleur des badges (cohérent avec les 5 vidéos précédentes)

| Badge | Couleur | Cas d'emploi |
|---|---|---|
| Pré-rempli par l'IA | Doré (`#C9A227`) | Champ extrait automatiquement |
| Vérifié par l'avocat | Vert (`#2D7A3E`) | Champ corrigé manuellement après extraction |
| Source : pièce N°XX | Bleu (`#1F4FA0`) | Lien cliquable vers la pièce d'origine |
| Legal OCR | Indigo nuit (`#1A2B5F`) | Routage moteur OCR |
| Legal Vision | Violet électrique (`#5B2BC9`) | Routage moteur Vision |
| Délai conforme | Vert (`#2D7A3E`) | Délai légal respecté |
| Risque détecté | Orange (`#D97706`) | Alerte non bloquante |

---

## Conventions visuelles à respecter

1. **Header LegalCase** : conforme `docs/DESIGN_SYSTEM.md` — pas de variante créative.
2. **Police** : Inter pour le corps, JetBrains Mono pour les références légales et codes (CESEDA, art. 515-9 C. civ., etc.).
3. **Pas de marques tierces visibles** : ni Anthropic, ni OpenAI, ni Claude, ni AWS Textract. Seuls les noms produit "Legal OCR" et "Legal Vision" sont cités à l'écran.
4. **Pas de logo concurrent** : si un visuel "outil bloqué / message d'erreur" est nécessaire (accroche), créer une UI générique sans branding.
5. **Réalisme** : les pièces 03, 05, 07 doivent vraiment ressembler à ce qu'un client envoie en panique — flou de bougé, doigt dans le cadre, lumière dure, pli du papier visible. Ne pas embellir.

---

## Liste des fichiers livrés dans ce dossier

| Fichier | Contenu |
|---|---|
| `00-README-motion-designer.md` | Ce brief général |
| `01-acte-mariage.md` | Contenu textuel intégral à intégrer dans le PDF natif |
| `02-main-courante-commissariat.md` | Contenu textuel des 4 sous-pièces + caractéristiques scan |
| `03-certificat-medical.md` | Contenu + zone ambiguë à dégrader pour la démo édition |
| `04-sms-conjoint.md` | Fil de conversation complet à reproduire en capture iPhone |
| `05-porte-forcee.md` | Description de scène pour la photo |
| `06-camera-voisin.md` | Storyboard vidéo 12s |
| `07-facture-serrurier.md` | Contenu facture + état de dégradation |
| `script-video-6.md` | Script vidéo final, 80s, 9 blocs |

---

## Points clés à montrer dans la vidéo (rappel)

Ces 4 mécaniques produit doivent être **visibles à l'écran** dans la vidéo finale. Le script y revient bloc par bloc :

1. **Routage intelligent** OCR vs Vision selon le support
2. **Détection automatique de sous-pièces** dans un PDF qui en contient plusieurs (pièce 02)
3. **Édition de l'extrait à côté de l'aperçu** original — l'avocat garde la main (pièce 03)
4. **Source cliquable depuis la synthèse / chronologie** — chaque fait reste relié à sa pièce d'origine

---

## Stratégie de production des pièces — comment les générer

Chaque brief de pièce (`01-…` à `07-…`) contient une section **"Génération automatique"** en bas, avec soit un **script exécutable**, soit un **prompt prêt à coller** dans un outil grand public.

### Récapitulatif

| Pièce | Méthode | Outil recommandé | Sortie attendue |
|---|---|---|---|
| 01 — Acte de mariage | Script HTML → PDF | Chrome headless ou weasyprint | `01-acte-mariage.pdf` (PDF natif) |
| 02 — Main courante (4 sous-pièces) | 4 scripts HTML → PDF + fusion | Chrome headless + `pdfunite` | `02-main-courante-commissariat.pdf` (10 pages) — le motion designer applique ensuite l'effet "scan" en post-prod |
| 03 — Certificat médical | Prompt image generation | ChatGPT (DALL-E 3) ou MidJourney | `03-certificat-medical.jpg` |
| 04 — Capture SMS iPhone | Script HTML/CSS + capture | Chrome DevTools (mode mobile) ou Puppeteer | `04-sms-conjoint.png` |
| 05 — Photo porte forcée | Prompt image generation | ChatGPT (DALL-E 3) ou MidJourney | `05-porte-forcee.jpg` |
| 06 — Vidéo caméra voisin | Prompt vidéo + prompts frames | Sora / Runway / Veo / Pika (vidéo) ou DALL-E 3 (3 frames de fallback) | `06-camera-voisin.mp4` |
| 07 — Facture serrurier | Prompt image generation | ChatGPT (DALL-E 3) ou MidJourney | `07-facture-serrurier.jpg` |

### Outils d'image generation — conseils

- **DALL-E 3 dans ChatGPT (Plus / Team / Enterprise)** : meilleur respect des prompts longs et détaillés en français. Recommandé en premier choix pour les pièces 03, 05, 07.
- **MidJourney v6+** : meilleur photoréalisme texturé (papier froissé, tampons écrasés), mais prompts plus condensés (max ~60 mots utiles).
- **Gemini Image (Imagen 3)** : alternative équivalente à DALL-E 3.
- **Sora / Runway Gen-3 / Google Veo / Pika** : pour la pièce 06 vidéo. Si aucun n'est accessible, fallback sur 3 images statiques générées en DALL-E 3 + assemblage en motion design.

### Workflow conseillé

1. **Étape 1 — pièces scriptées (01, 02, 04)** : exécuter les scripts fournis pour produire les fichiers de base (PDF natifs, capture iMessage).
2. **Étape 2 — pièces dégradées (02 finale, 03, 05, 07)** : pour la pièce 02, le motion designer applique l'effet "scan" sur le PDF généré (légère inclinaison 1°, bruit de scanner, baisse de contraste). Pour les pièces 03, 05, 07, copier-coller le prompt fourni dans ChatGPT (DALL-E 3), itérer 2-3 fois pour affiner.
3. **Étape 3 — pièce vidéo (06)** : si accès Sora/Runway/Veo/Pika, utiliser le prompt vidéo. Sinon, générer les 3 frames clés via DALL-E 3 et assembler en motion design (12s avec horodatage incrusté qui défile).
4. **Étape 4 — métadonnées EXIF** : injecter dans les JPG les métadonnées EXIF cohérentes (date, GPS, modèle d'appareil) via `exiftool` — la pièce 05 décrit ces métadonnées en détail.

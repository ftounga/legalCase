# VIDÉO 6 — 1 minute 20 secondes — "LegalCase comprend ce que vos clients vous envoient"

(Angle : fonctionnalité phare **Legal OCR + Legal Vision** — LegalCase ingère et qualifie tout ce qu'un client envoie en panique, photos floues, captures d'écran, vidéos comprises. Différenciation forte vs concurrents qui exigent du PDF natif propre. Dossier de simulation : Sophie MARTIN — ordonnance de protection, droit de la famille FR.)

---

## [0s–10s] — ACCROCHE

▎ Voix off :
▎ "Sept pièces. Quatre formats. Trois qualités."
▎ "La plupart des outils refusent ça."
▎ "LegalCase ne refuse rien."

Visuel : 4 vignettes hétérogènes apparaissent en cascade sur fond sombre — photo froissée du certificat médical, capture iPhone du fil SMS, miniature vidéo basse définition, scan administratif. Sur "refusent ça" → l'écran d'un outil concurrent générique (UI neutre, sans logo) affiche un message "Format non pris en charge — Veuillez fournir un PDF natif". Sur "ne refuse rien" → bascule franche sur l'interface LegalCase, les 7 vignettes du dossier MARTIN aspirées dans la zone "Déposer toutes les pièces du dossier".

---

## [10s–18s] — LE MOTEUR DE ROUTAGE

▎ Voix off :
▎ "LegalCase regarde chaque pièce, décide comment la lire. Texte propre — Legal OCR. Photo, capture, vidéo — Legal Vision."

Visuel : convoyeur central animé. Chaque vignette glisse, le moteur (animation circulaire stylisée) la regarde, attribue un badge :
- Acte de mariage → "Texte natif détecté" (badge gris discret)
- Main courante (10 pages) → **Legal OCR** (badge indigo nuit)
- Certificat médical photographié → **Legal OCR — mode dégradé**
- SMS du conjoint → **Legal Vision** (badge violet électrique)
- Photo porte forcée → **Legal Vision**
- Vidéo caméra voisin → **Legal Vision**
- Facture serrurier → **Legal OCR — mode dégradé**

Compteur en haut à droite : "7 pièces · 1 PDF natif · 3 OCR · 3 Vision".

---

## [18s–28s] — DÉTECTION DE SOUS-PIÈCES

▎ Voix off :
▎ "Et quand un document en contient plusieurs, LegalCase les sépare automatiquement."

Visuel : zoom sur la pièce 02 — `main-courante-commissariat.pdf`, 10 pages. Le PDF s'ouvre, un curseur d'analyse passe rapidement page par page. Puis le PDF se "déplie" comme un éventail en **4 cartes filles** étalées à l'écran :

▎ A · Procès-verbal de dépôt de plainte (p. 1-3)
▎ B · Déposition complémentaire manuscrite (p. 4-5)
▎ C · Photocopie carte nationale d'identité (p. 6)
▎ D · Formulaire de plainte signé (p. 7-10)

Chaque carte porte son propre badge **Legal OCR**. Bandeau bas : **"1 PDF déposé → 4 pièces identifiées et indexées séparément"**.

---

## [28s–38s] — LEGAL OCR SUR LE DÉGUEU

▎ Voix off :
▎ "Le client photographie en urgence. Le tampon est écrasé, le doigt dans le cadre. LegalCase l'extrait quand même."

Visuel : plein écran sur la pièce 03 — `certificat-medical.jpg`. La photo est manifestement dégradée : doigt en flou dans le coin, reflet de néon dans l'angle haut, marqueurs jaune fluo de Sophie sur "ITT" et "violences au domicile conjugal", tampon professionnel rond qui chevauche la conclusion. Animation : un cadre de scan progresse de haut en bas. À droite, les champs structurés apparaissent un par un :

- Médecin : Dr Anaïs DELAUNAY
- N° Ordre : 75/123/456
- Date examen : 04/05/2026 — 11h40
- Patiente : Sophie MARTIN
- Lésions : hématome bras droit, excoriation main droite, état de stress aigu
- ITT : **B JOURS** ⚠ (badge orange "Confiance faible")

---

## [38s–48s] — ÉDITION + APERÇU SYNCHRONISÉ

▎ Voix off :
▎ "Vous gardez la main. Chaque extrait s'édite à côté de la pièce d'origine."

Visuel : split-screen 50/50.
- Gauche : photo originale du certificat, **zoom automatique** sur la zone du tampon qui chevauche le chiffre.
- Droite : champs extraits, le champ "ITT" surligné en orange.

L'avocat (curseur visible) clique sur "B JOURS" → champ devient éditable → tape "8 JOURS" → valide. Le badge change en cascade : doré "Pré-rempli par l'IA" → vert "Vérifié par l'avocat". Compteur de fiabilité du document remonte de 87 % à 99 %.

---

## [48s–62s] — LEGAL VISION : 3 SUPPORTS IMPOSSIBLES

▎ Voix off :
▎ "Là où d'autres outils s'arrêtent, Legal Vision lit la conversation, décrit la scène, identifie l'événement filmé."
▎ "Chaque support nourrit le dossier."

Visuel : trois vignettes côte à côte qui se révèlent en cascade, chacune reçoit sa carte de synthèse Legal Vision en surimpression.

**Vignette 1 — SMS du conjoint** (`04-sms-conjoint.png`)

▎ 💬 23 messages — 27/04 → 04/05 — escalade détectée
▎ Menaces explicites identifiées (3) :
▎ • 03/05 18:51 — "Tu vas voir si tu m'ouvres pas"
▎ • 03/05 23:08 — "Tu vas le regretter"
▎ • 03/05 23:11 — "Si je rentre je te casse la gueule"

**Vignette 2 — Photo porte palière** (`05-porte-forcee.jpg`)

▎ 🚪 EXIF 04/05 09:14 — 11ᵉ arr. Paris
▎ Description : porte palière en bois, chambranle fendu (15 cm) au niveau de la serrure, trace de coups à hauteur d'épaule, marque de semelle en bas, serrure partiellement déboîtée. Compatible avec une tentative d'effraction.

**Vignette 3 — Vidéo caméra voisin** (`06-camera-voisin.mp4`)

▎ 🎥 12 s — horodatage incrusté 03/05 23:11
▎ Événement filmé : individu masculin se présente devant la porte palière, frappe à coups de poing, donne un coup d'épaule, puis un coup de pied bas de porte, avant de quitter le cadre.
▎ Frames clés extraites : 3

Chaque vignette porte le badge **Legal Vision** violet en bas à droite.

---

## [62s–70s] — CHRONOLOGIE + SOURCE CLIQUABLE

▎ Voix off :
▎ "Tout converge. Et chaque fait reste relié à sa source."

Visuel : la **timeline du dossier MARTIN** se reconstruit ligne par ligne sous les yeux du spectateur :

▎ 27/04 19:42 → 03/05 18:51 — Escalade SMS du conjoint (15 msg) — Source : SMS
▎ 03/05 23:11–23:12 — Tentative d'effraction filmée — Source : caméra voisin
▎ 04/05 09:14 — Constat photo des dégâts — Source : photo porte
▎ 04/05 09:25 — Dépôt de plainte commissariat 11ᵉ — Source : main courante (sous-pièce A)
▎ 04/05 10:45–12:20 — Intervention serrurier urgence — Source : facture
▎ 04/05 11:40 — Examen médical SAU — ITT 8 jours — Source : certificat médical

L'avocate (curseur) clique sur la ligne "Intervention serrurier" → l'écran zoome instantanément sur la facture originale (pièce 07, papier froissé, tampon "PAYÉ" visible). Retour automatique à la timeline en 1 seconde.

---

## [70s–76s] — OUTIL DÉCISIONNEL : ORDONNANCE DE PROTECTION

▎ Voix off :
▎ "La requête en ordonnance de protection est pré-remplie. Faits qualifiés, pièces probantes attribuées, base légale citée."

Visuel : bloc générateur de requête OP — pré-rempli avec badge doré "Pré-rempli par l'IA".

▎ Type : Requête en ordonnance de protection
▎ Fondement : art. 515-9 et s. C. civ.
▎ Juridiction : Juge aux affaires familiales — TJ Paris
▎ Faits invoqués : violences (art. 222-13 CP) — menaces (art. 222-17 CP) — tentative d'effraction
▎ Pièces probantes attribuées par fait : 7 pièces — 11 sous-éléments
▎ **Délai conforme — déposable demain matin** (badge vert)

---

## [76s–80s] — SIGNATURE

▎ Voix off :
▎ "Vos clients vous envoient le réel. LegalCase le comprend."

Visuel : fade vers fond noir. La phrase apparaît en deux temps : "**Vos clients vous envoient le réel.**" puis "**LegalCase le comprend.**" Logo LegalCase en bas. Tagline produit secondaire en petits caractères : "Legal OCR · Legal Vision".

---

## Notes de réalisation

- **Pacing** : la vidéo est dense. Le motion designer doit veiller à ce que chaque bloc respire — au moins 0,5 s de pause visuelle entre deux blocs, sinon la lecture devient agressive.
- **Pas de musique pathétique** sur les blocs 6 et 7 (faits de violence). Bande-son orchestrale sobre, mêmes nappes que les vidéos 1 à 5 pour cohérence de série.
- **Cohérence visuelle série** : header LegalCase, code couleur des badges, polices Inter / JetBrains Mono — strictement identiques aux 5 vidéos précédentes. La vidéo 6 doit être reconnaissable comme appartenant à la même série, mais avec une **densité de mécaniques produit visibles** plus forte (4 mécaniques mises en avant : routage, sous-pièces, édition+aperçu, source cliquable).
- **Ce qui doit rester comme moment fort** :
  1. La bascule "refusent ça / ne refuse rien" (0–10s)
  2. Le PDF qui se déplie en 4 sous-pièces (18–28s)
  3. Le split-screen édition avec le badge qui passe au vert (38–48s)
  4. Le clic depuis la timeline qui ramène à la facture froissée (62–70s)
  5. Le verdict "déposable demain matin" (70–76s)
- **Différenciation concurrentielle** : citée 1 seule fois ("Là où d'autres outils s'arrêtent", 48s). Jamais nominative. Suffit.
- **Conformité copy** : aucune marque tierce citée (pas d'Anthropic, pas d'OpenAI, pas de Claude, pas de Textract). Les seuls noms produit affichés à l'écran sont **Legal OCR** et **Legal Vision**. ✓

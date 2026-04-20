/**
 * SF-140-01 : documentation métier par type de référentiel.
 *
 * Chaque entrée est rédigée pour l'avocat utilisateur — en langage métier,
 * sans jargon technique ni codes de feature internes (pas de "F-DT-07" etc.).
 * Les outils sont désignés par leur nom fonctionnel tel qu'il apparaît dans
 * l'interface du dossier.
 */
export interface SectionDoc {
  /** Nom humain du type (plus détaillé que SECTION_LABELS). */
  title: string;
  /** Description métier : à quoi ça sert. */
  description: string;
  /** Liste des outils de l'application qui consomment ce référentiel. */
  usedIn: string[];
  /** Instructions de modification pour l'avocat. */
  howToModify: string;
  /** Description du format attendu en langage clair (pas du JSON brut). */
  format: string;
}

export const SECTION_DOCS: Record<string, SectionDoc> = {
  CONVENTION_BAREMES: {
    title: 'Conventions collectives et commissions paritaires',
    description:
      'Barèmes d\'ancienneté conventionnels — congés légaux, congés supplémentaires d\'ancienneté et '
      + 'primes d\'ancienneté — par convention collective (France) et commission paritaire (Belgique). '
      + 'Sert de référence pour comparer ce qui est prévu dans le contrat de travail à ce qui est dû '
      + 'au titre de la convention applicable.',
    usedIn: [
      'Outil Ancienneté : pré-remplissage des droits conventionnels dès que la CCN/CP est identifiée, calcul des écarts contrat vs convention.',
      'Comparateur d\'indemnités : détermination du plafond applicable (Macron FR / CCT 109 BE) à partir de l\'ancienneté reconstituée.',
      'Fiche prud\'homale / requête tribunal du travail : les droits conventionnels sont intégrés automatiquement aux pièces chiffrées.',
    ],
    howToModify:
      'Cliquez sur l\'icône crayon à droite d\'une entrée pour modifier ses valeurs. Vos ajustements '
      + 'sont enregistrés au niveau de votre cabinet (workspace) et ne remplacent pas la valeur d\'origine — '
      + 'une étiquette "Perso" s\'affiche sur les entrées modifiées. Pour revenir à la valeur d\'origine, '
      + 'supprimez votre version.',
    format:
      'Congés légaux en jours ouvrables, suivis de tranches d\'ancienneté qui ajoutent des jours de '
      + 'congés supplémentaires (ex. +1 jour après 5 ans, +2 jours après 10 ans), et de tranches d\'ancienneté '
      + 'qui fixent un pourcentage de prime (ex. 3 % après 3 ans, 6 % après 6 ans).',
  },

  LICENCIEMENT_CRITERES: {
    title: 'Critères de validité du licenciement',
    description:
      'Grille d\'évaluation du risque de requalification d\'un licenciement — procédure (convocation, '
      + 'entretien, délais), motivation (cause réelle et sérieuse) et conditions de forme. Chaque critère '
      + 'porte un poids, la somme étant de 100. Un critère bloquant non conforme suffit à invalider.',
    usedIn: [
      'Outil Validité du licenciement : l\'avocat répond "Oui / Non / Inconnu" à chaque critère, l\'outil calcule le score et le verdict (Valide, Risque modéré, Risque élevé, Invalide).',
      'Fiche prud\'homale : les critères non conformes alimentent les moyens de droit.',
    ],
    howToModify:
      'Cliquez sur le crayon pour ajuster le poids, marquer un critère comme bloquant ou non, ou reformuler '
      + 'la question posée à l\'avocat. Ajustement workspace-level.',
    format:
      'Pour chaque critère : un poids en points (total 100 à travers les critères FR ou BE), un drapeau '
      + 'bloquant (si "Non" → verdict Invalide peu importe le score), et la question précise qui sera posée '
      + 'à l\'avocat dans le formulaire de l\'outil.',
  },

  RUPTURE_CONV_CRITERES: {
    title: 'Critères de validité de la rupture conventionnelle',
    description:
      'Grille d\'évaluation du risque de nullité d\'une rupture conventionnelle homologuée (France) : '
      + 'consentement libre, délai de rétractation de 15 jours, homologation DREETS, assistance documentée, '
      + 'indemnité spécifique au moins égale à l\'indemnité légale, entretiens préalables tenus.',
    usedIn: [
      'Outil Validité de la rupture conventionnelle : l\'avocat répond critère par critère, l\'outil calcule le score de risque de nullité.',
    ],
    howToModify:
      'Cliquez sur le crayon pour ajuster le poids d\'un critère, son caractère bloquant, ou la formulation '
      + 'de la question. Scope workspace.',
    format:
      'Même structure que les critères de licenciement : poids (total 100), drapeau bloquant, description '
      + 'qui sert de question dans le formulaire.',
  },

  LITIGATION_TYPE: {
    title: 'Types de litige et délais de prescription',
    description:
      'Pour chaque type de litige prud\'homal (licenciement sans cause réelle, discrimination, harcèlement, '
      + 'non-paiement de salaire…), durée de prescription légale et référence de l\'article. Utilisé pour '
      + 'calculer automatiquement la date limite au-delà de laquelle une action contentieuse serait irrecevable.',
    usedIn: [
      'Bloc "Délais légaux" sur chaque dossier droit du travail : dès que l\'IA identifie le type de litige dans les documents, l\'outil affiche la date limite de saisine.',
      'Alertes délai : si la date limite est proche ou dépassée, une alerte apparaît sur le dossier et sur le tableau de bord d\'accueil.',
    ],
    howToModify:
      'Cliquez sur le crayon pour changer la durée de prescription ou l\'article de référence. À utiliser '
      + 'avec prudence : il s\'agit de délais légaux.',
    format:
      'Durée en années et référence à l\'article (ex. "Art. L1471-1 Code du travail" pour le licenciement sans cause).',
  },

  BAREME_MACRON: {
    title: 'Types de rupture éligibles au barème Macron',
    description:
      'Indique si un type de rupture ouvre droit aux dommages-intérêts prud\'homaux encadrés par le barème '
      + 'Macron (art. L1235-3). Techniquement un marqueur oui/non — les valeurs chiffrées du barème sont '
      + 'dans "Barèmes d\'indemnités".',
    usedIn: [
      'Comparateur d\'indemnités : aiguille vers le mode Macron (licenciement) vs indemnité spécifique (rupture conventionnelle) selon le type détecté.',
    ],
    howToModify:
      'Ce tableau ne se modifie normalement pas (ancrage textuel). L\'édition existe pour les cas exotiques. '
      + 'Si vous constatez un manque, c\'est plutôt un type de rupture à ajouter à l\'application.',
    format: 'Drapeau "supporté" oui/non.',
  },

  INDEMNITE_BAREMES: {
    title: 'Barèmes d\'indemnités prud\'homales',
    description:
      'Fourchettes plancher/plafond des dommages-intérêts. Deux entrées : le barème Macron année par année '
      + 'de 0 à 29 ans d\'ancienneté (France, art. L1235-3), et la fourchette CCT 109 en semaines (Belgique, '
      + 'licenciement manifestement déraisonnable).',
    usedIn: [
      'Comparateur d\'indemnités : affiche la fourchette chiffrée correspondant à l\'ancienneté et au pays, '
      + 'puis applique un ajustement jurisprudentiel (âge, senior, etc.) pour proposer une estimation basse / médiane / haute.',
      'Synthèse du dossier : encart "Indemnités estimées" du tableau de bord décisionnel.',
    ],
    howToModify:
      'L\'ajustement de ces valeurs doit rester exceptionnel (textes légaux). Cliquez sur le crayon uniquement '
      + 'pour prendre en compte une évolution réglementaire pas encore intégrée.',
    format:
      'Macron : liste des 30 années avec pour chacune le plancher et le plafond en mois de salaire. '
      + 'CCT 109 : plancher et plafond en semaines de salaire.',
  },

  IMMIGRATION_TITLES: {
    title: 'Titres de séjour et conditions',
    description:
      'Catalogue des titres de séjour disponibles (VLS-TS, carte de séjour, passeport talent, carte A/B/C, '
      + 'permis unique…) avec pour chacun le motif associé (travail, étudiant, famille, asile), les conditions '
      + 'à remplir, les pièces standards, et le délai moyen d\'instruction constaté.',
    usedIn: [
      'Outil Arbre décisionnel titre de séjour : l\'avocat répond à un questionnaire (nationalité, motif, durée…), '
      + 'l\'outil propose les titres compatibles en s\'appuyant sur cette base.',
      'Outil Checklist pièces : les pièces requises sont prises depuis ce référentiel au moment où l\'avocat ouvre la checklist.',
    ],
    howToModify:
      'Cliquez sur le crayon pour adapter les conditions ou la liste des pièces selon votre pratique locale '
      + '(préfectures variables). Scope workspace.',
    format:
      'Motif (travail / famille / études / asile / autre), texte des conditions, liste de pièces standards, '
      + 'délai moyen d\'instruction en jours.',
  },

  IMMIGRATION_RECOURS: {
    title: 'Types de recours en droit des étrangers',
    description:
      'Recours possibles contre une décision défavorable : recours gracieux préfet, contentieux TA, CNDA pour '
      + 'l\'asile (France) ; CGRA, CCE, Conseil d\'État (Belgique). Chaque entrée précise la juridiction '
      + 'compétente, le délai de recours, les textes applicables et les pièces standards.',
    usedIn: [
      'Outil Générateur de recours : produit un projet de mémoire structuré (en-tête, objet, visa, moyens, conclusions) '
      + 'à partir du type choisi. Délai limite et alerte de délai calculés automatiquement depuis la date de notification.',
    ],
    howToModify:
      'Cliquez sur le crayon pour affiner les textes de référence, les pièces à joindre, ou le délai (dans la '
      + 'limite des textes). Les recours dont la procédure évolue (ex. réforme CESEDA) peuvent nécessiter une mise à jour.',
    format:
      'Juridiction compétente, délai en jours, liste d\'articles / textes applicables, liste de pièces standards.',
  },

  IMMIGRATION_WORK_RIGHTS: {
    title: 'Droits au travail par titre de séjour',
    description:
      'Pour chaque titre de séjour, indique si le détenteur a le droit de travailler (Oui / Non / Conditionnel), '
      + 'les conditions éventuelles (APT, durée, secteur) et les obligations de l\'employeur (vérification préfecture, '
      + 'déclaration Dimona, DPAE…).',
    usedIn: [
      'Outil Droit au travail du demandeur : l\'avocat sélectionne le titre et le pays, l\'outil affiche le statut et '
      + 'les obligations à rappeler à l\'employeur.',
    ],
    howToModify:
      'Cliquez sur le crayon pour adapter les conditions ou obligations si votre pratique détecte une nuance '
      + 'locale ou un changement réglementaire.',
    format:
      'Statut du droit au travail (OUI / NON / CONDITIONNEL), conditions en texte libre, liste d\'obligations employeur.',
  },

  IMMIGRATION_JALONS: {
    title: 'Jalons procéduraux en immigration',
    description:
      'Séquence temporelle des étapes standards d\'une procédure administrative (instruction préfecture, '
      + 'silence valant rejet, recours gracieux, délais de notification…). Exprimé en décalage de jours par '
      + 'rapport à une date d\'ancrage (dépôt de la demande typiquement).',
    usedIn: [
      'Calendrier procédural du dossier immigration : génère des dates prévisionnelles pour chaque jalon, '
      + 'visibles dans la timeline du dossier.',
    ],
    howToModify:
      'Cliquez sur le crayon pour ajuster les décalages (variations préfectures). Scope workspace.',
    format:
      'Pour chaque jalon : libellé et nombre de jours par rapport à la date d\'origine (positif = après).',
  },

  IMMIGRATION_PIECES: {
    title: 'Pièces standards par type de titre',
    description:
      'Liste type de documents à fournir pour une demande de titre de séjour donnée. Sert de base à la '
      + 'checklist — l\'avocat peut cocher au fur et à mesure.',
    usedIn: [
      'Outil Checklist pièces : les éléments sont copiés depuis cette base à l\'initialisation, puis l\'avocat '
      + 'coche, ajoute ou supprime selon le cas.',
    ],
    howToModify:
      'Cliquez sur le crayon pour adapter la liste (ajouter une pièce spécifique à votre préfecture par exemple).',
    format: 'Liste simple d\'intitulés de pièces.',
  },

  PENSION_TAUX: {
    title: 'Barème de la pension alimentaire',
    description:
      'Table de référence utilisée par le juge aux affaires familiales pour évaluer la pension alimentaire : '
      + 'pourcentage du revenu de l\'obligé en fonction du nombre d\'enfants et du mode de garde (exclusive '
      + 'vs alternée). Une entrée par pays (UNAF pour la France, CGKR/Jodl pour la Belgique).',
    usedIn: [
      'Outil Grille pension alimentaire : produit une fourchette indicative à partir des revenus, du nombre '
      + 'd\'enfants et du mode de garde retenu.',
      'Synthèse droit de la famille : encart "Pension estimée" avec la fourchette.',
    ],
    howToModify:
      'Cliquez sur le crayon pour adapter les pourcentages si la jurisprudence locale de votre juridiction s\'en écarte.',
    format:
      'Matrice : pour chaque nombre d\'enfants (1 à 5+), deux pourcentages — un pour la garde exclusive, un pour la garde alternée.',
  },

  PRESTATION_COEFF: {
    title: 'Coefficient de prestation compensatoire',
    description:
      'Paramètres du calcul indicatif de la prestation compensatoire (art. 271 Code civil) : coefficient appliqué '
      + 'à l\'écart de revenus entre les époux, et durée de mariage de référence pour la proratisation.',
    usedIn: [
      'Outil Calcul prestation compensatoire : produit une fourchette indicative à partir de la durée du mariage, '
      + 'des revenus et du patrimoine des époux.',
    ],
    howToModify:
      'À ajuster si vous voulez refléter une jurisprudence plus généreuse ou plus restrictive que la moyenne.',
    format:
      'Coefficient (multiplicateur de l\'écart de revenus, typiquement 0.3 en France) et durée de référence '
      + 'en années pour le prorata.',
  },

  GARDE_MODES: {
    title: 'Modes de garde d\'enfants',
    description:
      'Modes de répartition de la garde : résidence alternée une semaine sur deux, droit de visite et d\'hébergement '
      + '(DVH) classique, DVH élargi. Pour chaque mode, la répartition annuelle en jours et les périodes types '
      + '(week-ends, vacances).',
    usedIn: [
      'Outil Calendrier de garde : génère un calendrier annuel conforme aux usages JAF (France) ou tribunal de la '
      + 'famille (Belgique) à partir du mode choisi.',
    ],
    howToModify:
      'Cliquez sur le crayon pour définir un mode spécifique à votre pratique (ex. 9 jours / 5 jours au lieu du classique).',
    format:
      'Type de répartition, nombre de jours par an pour chaque parent, description des périodes types, '
      + 'règle de partage des vacances scolaires.',
  },

  DIVORCE_ETAPES: {
    title: 'Étapes de la procédure de divorce par consentement mutuel',
    description:
      'Séquence ordonnée des étapes obligatoires : entretien préalable, rédaction de la convention, délai de '
      + 'réflexion de 15 jours, signature, dépôt chez le notaire (France) ou requête conjointe au tribunal de '
      + 'la famille (Belgique). Chaque étape a un ordre, une description, un délai associé, et un drapeau "obligatoire".',
    usedIn: [
      'Outil Checklist divorce amiable : affiche les étapes dans l\'ordre avec les cases à cocher et les '
      + 'alertes de délai.',
    ],
    howToModify:
      'Cliquez sur le crayon pour reformuler une étape ou ajuster un délai. L\'ordre est important — il '
      + 'conditionne l\'affichage.',
    format:
      'Numéro d\'ordre, description de l\'étape, délai typique (texte libre), drapeau obligatoire oui/non.',
  },

  DIVORCE_PIECES: {
    title: 'Pièces requises pour un divorce amiable',
    description:
      'Liste type des documents à réunir pour une procédure de divorce par consentement mutuel : état civil, '
      + 'contrat de mariage, justificatifs de revenus et de patrimoine, convention, attestations diverses.',
    usedIn: [
      'Outil Checklist divorce amiable : l\'avocat coche les pièces au fur et à mesure de leur réception, '
      + 'l\'outil alerte quand il manque des éléments obligatoires.',
    ],
    howToModify:
      'Cliquez sur le crayon pour ajuster la description ou la durée de validité d\'une pièce. Scope workspace.',
    format:
      'Description de la pièce, validité (par exemple "moins de 3 mois") et drapeau obligatoire oui/non.',
  },
};

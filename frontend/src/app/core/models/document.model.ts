export type ExtractionStatus = 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';

export type ExtractionFailureReason =
  | 'EMPTY_TEXT'
  | 'UNSUPPORTED_FORMAT'
  | 'CORRUPTED'
  | 'EXTRACTION_EXCEPTION'
  | 'OCR_FAILED'
  | 'OCR_UNSUPPORTED_SIZE'
  | 'OCR_QUOTA_EXCEEDED';

export type DocumentPieceType =
  // Communs aux 3 domaines
  | 'PHOTO' | 'LETTRE' | 'EMAIL' | 'SMS' | 'ATTESTATION'
  | 'PIECE_IDENTITE' | 'CERTIFICAT_MEDICAL' | 'AUTRE'
  // Droit du travail
  | 'CONTRAT' | 'BULLETIN_PAIE'
  // Immigration (SF-145-09)
  | 'TITRE_DE_SEJOUR' | 'PASSEPORT' | 'VISA' | 'ACTE_NAISSANCE'
  | 'AVIS_IMPOSITION' | 'QUITTANCE_LOYER' | 'PROMESSE_EMBAUCHE'
  | 'RECEPISSE_PREFECTURE' | 'DECISION_OQTF' | 'RECOURS_CONTENTIEUX'
  | 'ATTESTATION_HEBERGEMENT'
  // Famille (SF-145-09)
  | 'ACTE_MARIAGE' | 'ACTE_NAISSANCE_ENFANT' | 'JUGEMENT_DIVORCE'
  | 'LIVRET_FAMILLE' | 'JUSTIFICATIF_REVENUS'
  // Commun 3 domaines (SF-145-10)
  | 'BAIL_LOCATION';

/** SF-145-01 : résumé d'une pièce identifiée dans un document composite. */
export interface DocumentPieceSummary {
  id: string;
  type: DocumentPieceType;
  label: string | null;
  pageStart: number;
  pageEnd: number;
  orderIndex: number;
  /**
   * F-260 SF-260-01 : numéro de pièce persistant et stable (1..N par dossier),
   * lu par les conclusions (F-98) ET la fiche prud'homale (source unique).
   * `null` uniquement pour de très vieilles pièces non backfillées → fallback
   * d'index côté UI.
   */
  pieceNumber?: number | null;
  /** SF-148-01 : description visuelle produite par LegalCase Vision (null si non enrichi). */
  visualDescription?: string | null;
  /** SF-148-03 : statut de l'enrichissement visuel (NOT_APPLICABLE / PENDING / DONE / FAILED). */
  visionStatus?: VisionStatus;
}

export type VisionStatus = 'NOT_APPLICABLE' | 'PENDING' | 'DONE' | 'FAILED';

export interface Document {
  id: string;
  caseFileId: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
  /** SF-121-01 : statut de l'extraction si elle existe (null sinon). */
  extractionStatus?: ExtractionStatus | null;
  /** SF-121-01 : motif d'échec si extractionStatus === 'FAILED'. */
  failureReason?: ExtractionFailureReason | null;
  /** SF-144-01 : true pendant l'appel synchrone à Textract (feedback polling). */
  ocrRunning?: boolean;
  /** SF-144-01 : true si l'extraction a été produite via Textract (affiche chip `OCR`). */
  ocrExtracted?: boolean;
  /** SF-145-01 : pièces identifiées dans le document composite. */
  pieces?: DocumentPieceSummary[];
  /**
   * F-261 SF-261-01 : document marqué « écritures adverses » (conclusions de la
   * partie adverse). Désigne la source des moyens à extraire (SF-261-02) puis
   * réfuter (SF-261-03). Distinct du marquage de citation adverse (SF-98-56).
   */
  adversePleadings?: boolean;
}

/**
 * SF-145-11 : retourne les types de pièces applicables à un domaine juridique
 * (miroir de DocumentPieceType.applicableFor côté backend). Si legalDomain
 * null/inconnu, retourne tous les types (fallback permissif).
 */
export function applicableForDomain(legalDomain: string | null | undefined): DocumentPieceType[] {
  const common: DocumentPieceType[] = [
    'PHOTO', 'LETTRE', 'EMAIL', 'SMS', 'ATTESTATION',
    'PIECE_IDENTITE', 'CERTIFICAT_MEDICAL', 'AUTRE',
    'BAIL_LOCATION',
  ];
  const travail: DocumentPieceType[] = ['CONTRAT', 'BULLETIN_PAIE', 'JUSTIFICATIF_REVENUS'];
  const immigration: DocumentPieceType[] = [
    'TITRE_DE_SEJOUR', 'PASSEPORT', 'VISA', 'ACTE_NAISSANCE',
    'AVIS_IMPOSITION', 'QUITTANCE_LOYER', 'PROMESSE_EMBAUCHE',
    'RECEPISSE_PREFECTURE', 'DECISION_OQTF', 'RECOURS_CONTENTIEUX',
    'ATTESTATION_HEBERGEMENT', 'ACTE_MARIAGE',
  ];
  const famille: DocumentPieceType[] = [
    'ACTE_MARIAGE', 'ACTE_NAISSANCE_ENFANT', 'JUGEMENT_DIVORCE',
    'LIVRET_FAMILLE', 'JUSTIFICATIF_REVENUS', 'ACTE_NAISSANCE',
  ];
  switch (legalDomain) {
    case 'DROIT_DU_TRAVAIL':  return [...common, ...travail];
    case 'DROIT_IMMIGRATION': return [...common, ...immigration];
    case 'DROIT_FAMILLE':     return [...common, ...famille];
    default: return Array.from(new Set([...common, ...travail, ...immigration, ...famille]));
  }
}

/** SF-145-02 + SF-145-09 : libellé court par type pour les chips + sidebar. */
export function documentPieceTypeLabel(type: DocumentPieceType): string {
  switch (type) {
    // Communs
    case 'PHOTO':          return 'Photo';
    case 'LETTRE':         return 'Lettre';
    case 'EMAIL':          return 'Email';
    case 'SMS':            return 'SMS';
    case 'ATTESTATION':    return 'Attestation';
    case 'PIECE_IDENTITE': return 'Identité';
    case 'CERTIFICAT_MEDICAL': return 'Certificat médical';
    case 'AUTRE':          return 'Pièce';
    // Droit du travail
    case 'CONTRAT':        return 'Contrat';
    case 'BULLETIN_PAIE':  return 'Bulletin de paie';
    // Immigration
    case 'TITRE_DE_SEJOUR': return 'Titre de séjour';
    case 'PASSEPORT':      return 'Passeport';
    case 'VISA':           return 'Visa';
    case 'ACTE_NAISSANCE': return 'Acte de naissance';
    case 'AVIS_IMPOSITION': return 'Avis d\'imposition';
    case 'QUITTANCE_LOYER': return 'Quittance de loyer';
    case 'PROMESSE_EMBAUCHE': return 'Promesse d\'embauche';
    case 'RECEPISSE_PREFECTURE': return 'Récépissé préfecture';
    case 'DECISION_OQTF':  return 'Décision OQTF';
    case 'RECOURS_CONTENTIEUX': return 'Recours contentieux';
    case 'ATTESTATION_HEBERGEMENT': return 'Attestation d\'hébergement';
    // Famille
    case 'ACTE_MARIAGE':   return 'Acte de mariage';
    case 'ACTE_NAISSANCE_ENFANT': return 'Acte de naissance enfant';
    case 'JUGEMENT_DIVORCE': return 'Jugement de divorce';
    case 'LIVRET_FAMILLE': return 'Livret de famille';
    case 'JUSTIFICATIF_REVENUS': return 'Justificatif de revenus';
    // Commun 3 domaines (SF-145-10)
    case 'BAIL_LOCATION':  return 'Bail de location';
    default:               return 'Pièce';
  }
}

/** SF-145-02 + SF-145-09 : icône Material par type. */
export function documentPieceTypeIcon(type: DocumentPieceType): string {
  switch (type) {
    // Communs
    case 'PHOTO':          return 'image';
    case 'LETTRE':         return 'mail';
    case 'EMAIL':          return 'email';
    case 'SMS':            return 'chat';
    case 'ATTESTATION':    return 'edit_note';
    case 'PIECE_IDENTITE': return 'badge';
    case 'CERTIFICAT_MEDICAL': return 'medical_information';
    case 'AUTRE':          return 'description';
    // Droit du travail
    case 'CONTRAT':        return 'description';
    case 'BULLETIN_PAIE':  return 'receipt_long';
    // Immigration
    case 'TITRE_DE_SEJOUR': return 'card_travel';
    case 'PASSEPORT':      return 'flight_takeoff';
    case 'VISA':           return 'public';
    case 'ACTE_NAISSANCE': return 'child_care';
    case 'AVIS_IMPOSITION': return 'account_balance';
    case 'QUITTANCE_LOYER': return 'home';
    case 'PROMESSE_EMBAUCHE': return 'work_outline';
    case 'RECEPISSE_PREFECTURE': return 'receipt';
    case 'DECISION_OQTF':  return 'gavel';
    case 'RECOURS_CONTENTIEUX': return 'how_to_vote';
    case 'ATTESTATION_HEBERGEMENT': return 'holiday_village';
    // Famille
    case 'ACTE_MARIAGE':   return 'favorite';
    case 'ACTE_NAISSANCE_ENFANT': return 'child_friendly';
    case 'JUGEMENT_DIVORCE': return 'gavel';
    case 'LIVRET_FAMILLE': return 'menu_book';
    case 'JUSTIFICATIF_REVENUS': return 'euro';
    // Commun 3 domaines (SF-145-10)
    case 'BAIL_LOCATION':  return 'apartment';
    default:               return 'description';
  }
}

/** SF-121-02 + SF-122-01 : libellé humain court pour le badge UI. */
export function extractionFailureLabel(reason: ExtractionFailureReason | null | undefined): string {
  switch (reason) {
    case 'EMPTY_TEXT':
      return 'Document illisible (scan sans texte ou image non-textuelle)';
    case 'UNSUPPORTED_FORMAT':
      return 'Format non pris en charge';
    case 'CORRUPTED':
      return 'Document corrompu';
    case 'EXTRACTION_EXCEPTION':
      return 'Erreur technique';
    case 'OCR_FAILED':
      return 'Reconnaissance OCR impossible — réessayer ou envoyer un autre document';
    case 'OCR_UNSUPPORTED_SIZE':
      return 'Document trop volumineux pour l\'OCR (max 5 Mo / 11 pages)';
    case 'OCR_QUOTA_EXCEEDED':
      return 'Quota OCR atteint — voir votre espace Abonnement pour acheter des pages supplémentaires';
    default:
      return 'Extraction impossible';
  }
}

/**
 * SF-121-06 : message de récupération actionnable, spécifique au motif d'échec.
 * Affiché en clair sous le nom du document non analysable — il dit à l'avocat
 * quoi faire (pas seulement ce qui a échoué, contrairement à extractionFailureLabel).
 */
export function extractionRecoveryHint(reason: ExtractionFailureReason | null | undefined): string {
  switch (reason) {
    case 'OCR_UNSUPPORTED_SIZE':
      return 'Fichier trop volumineux pour l\'analyse automatique (max 5 Mo / 11 pages). '
        + 'Divisez-le en fichiers plus légers et ré-uploadez-le.';
    case 'EMPTY_TEXT':
    case 'OCR_FAILED':
      return 'Document scanné non reconnu. Utilisez « Relancer avec OCR » ci-dessus, '
        + 'ou remplacez le document.';
    case 'CORRUPTED':
    case 'UNSUPPORTED_FORMAT':
      return 'Fichier illisible. Remplacez-le par une version valide puis ré-uploadez.';
    case 'EXTRACTION_EXCEPTION':
      return 'L\'extraction a échoué. Ré-uploadez le document ; si l\'erreur persiste, '
        + 'contactez le support.';
    case 'OCR_QUOTA_EXCEEDED':
      return 'Quota OCR atteint. Achetez des pages supplémentaires depuis Abonnement, '
        + 'puis relancez l\'OCR.';
    default:
      return 'Extraction impossible. Ré-uploadez le document ou remplacez-le.';
  }
}

import { extractionRecoveryHint } from './document.model';

// SF-121-06 : message de récupération actionnable, spécifique au motif d'échec.
describe('extractionRecoveryHint', () => {
  it('OCR_UNSUPPORTED_SIZE → message "divisez le fichier"', () => {
    expect(extractionRecoveryHint('OCR_UNSUPPORTED_SIZE')).toBe(
      'Fichier trop volumineux pour l\'analyse automatique (max 5 Mo / 11 pages). '
      + 'Divisez-le en fichiers plus légers et ré-uploadez-le.',
    );
  });

  it('EMPTY_TEXT → message renvoyant vers « Relancer avec OCR »', () => {
    expect(extractionRecoveryHint('EMPTY_TEXT')).toBe(
      'Document scanné non reconnu. Utilisez « Relancer avec OCR » ci-dessus, '
      + 'ou remplacez le document.',
    );
  });

  it('OCR_FAILED → même message OCR que EMPTY_TEXT', () => {
    expect(extractionRecoveryHint('OCR_FAILED')).toBe(extractionRecoveryHint('EMPTY_TEXT'));
  });

  it('CORRUPTED → message "fichier illisible"', () => {
    expect(extractionRecoveryHint('CORRUPTED')).toBe(
      'Fichier illisible. Remplacez-le par une version valide puis ré-uploadez.',
    );
  });

  it('UNSUPPORTED_FORMAT → même message que CORRUPTED', () => {
    expect(extractionRecoveryHint('UNSUPPORTED_FORMAT')).toBe(extractionRecoveryHint('CORRUPTED'));
  });

  it('EXTRACTION_EXCEPTION → message support', () => {
    expect(extractionRecoveryHint('EXTRACTION_EXCEPTION')).toBe(
      'L\'extraction a échoué. Ré-uploadez le document ; si l\'erreur persiste, '
      + 'contactez le support.',
    );
  });

  it('OCR_QUOTA_EXCEEDED → message achat de pages', () => {
    expect(extractionRecoveryHint('OCR_QUOTA_EXCEEDED')).toBe(
      'Quota OCR atteint. Achetez des pages supplémentaires depuis Abonnement, '
      + 'puis relancez l\'OCR.',
    );
  });

  it('null → message générique de repli', () => {
    expect(extractionRecoveryHint(null)).toBe(
      'Extraction impossible. Ré-uploadez le document ou remplacez-le.',
    );
  });

  it('undefined → message générique de repli', () => {
    expect(extractionRecoveryHint(undefined)).toBe(
      'Extraction impossible. Ré-uploadez le document ou remplacez-le.',
    );
  });

  it('motif inconnu → message générique de repli', () => {
    expect(extractionRecoveryHint('SOMETHING_ELSE' as never)).toBe(
      'Extraction impossible. Ré-uploadez le document ou remplacez-le.',
    );
  });
});

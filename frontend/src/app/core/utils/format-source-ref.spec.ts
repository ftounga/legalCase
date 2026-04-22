import { formatSourceRef } from './format-source-ref';

describe('formatSourceRef', () => {
  it('U-01 — sourceRef complet → doc · type « label » · p. X-Y', () => {
    const out = formatSourceRef({
      source: 'dossier.pdf',
      extrait: null,
      sourceRef: {
        documentName: 'dossier.pdf',
        pieceType: 'CONTRAT',
        pieceLabel: 'Contrat Dupont',
        pageStart: 1,
        pageEnd: 2,
      },
    });
    expect(out).toBe('dossier.pdf · Contrat « Contrat Dupont » · p. 1-2');
  });

  it('U-02 — pieceLabel vide → utilise pieceTypeLabel uniquement', () => {
    const out = formatSourceRef({
      source: null,
      extrait: null,
      sourceRef: {
        documentName: 'dossier.pdf',
        pieceType: 'SMS',
        pieceLabel: null,
        pageStart: 3,
        pageEnd: 3,
      },
    });
    expect(out).toBe('dossier.pdf · SMS · p. 3');
  });

  it('U-03 — pageStart === pageEnd → "p. X"', () => {
    const out = formatSourceRef({
      source: null,
      extrait: null,
      sourceRef: {
        documentName: 'doc.pdf',
        pieceType: 'ATTESTATION',
        pieceLabel: 'A',
        pageStart: 5,
        pageEnd: 5,
      },
    });
    expect(out).toBe('doc.pdf · Attestation « A » · p. 5');
  });

  it('U-04 — sourceRef null + source renseignée → fallback legacy', () => {
    const out = formatSourceRef({
      source: 'contrat.pdf',
      extrait: null,
      sourceRef: null,
    });
    expect(out).toBe('contrat.pdf');
  });

  it('U-05 — rien du tout → null', () => {
    const out = formatSourceRef({ source: null, extrait: null, sourceRef: null });
    expect(out).toBeNull();
  });

  it('U-06 — extrait présent → appendu avec guillemets', () => {
    const out = formatSourceRef({
      source: 'contrat.pdf',
      extrait: 'Il est mis fin au contrat',
      sourceRef: null,
    });
    expect(out).toBe('contrat.pdf — « Il est mis fin au contrat »');
  });

  it('U-07 — sourceRef + extrait combinés', () => {
    const out = formatSourceRef({
      source: null,
      extrait: 'clause abusive',
      sourceRef: {
        documentName: 'dossier.pdf',
        pieceType: 'CONTRAT',
        pieceLabel: 'Contrat Dupont',
        pageStart: 2,
        pageEnd: 2,
      },
    });
    expect(out).toBe('dossier.pdf · Contrat « Contrat Dupont » · p. 2 — « clause abusive »');
  });

  it('U-08 — sourceRef.pageStart null → pas de section pages', () => {
    const out = formatSourceRef({
      source: null,
      extrait: null,
      sourceRef: {
        documentName: 'dossier.pdf',
        pieceType: 'CONTRAT',
        pieceLabel: null,
        pageStart: null,
        pageEnd: null,
      },
    });
    expect(out).toBe('dossier.pdf · Contrat');
  });

  it('U-09 — sourceRef.documentName null → fallback sur source string', () => {
    const out = formatSourceRef({
      source: 'ancien-doc.pdf',
      extrait: null,
      sourceRef: { documentName: null, pieceType: null, pieceLabel: null, pageStart: null, pageEnd: null },
    });
    expect(out).toBe('ancien-doc.pdf');
  });
});

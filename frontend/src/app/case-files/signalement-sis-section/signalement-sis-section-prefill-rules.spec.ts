import { SignalementSisPrefillRules as Rules } from './signalement-sis-section-prefill-rules';

describe('SignalementSisPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: {
        signalementSisDetecte: true,
        signalementSisEtatSignalant: 'FRANCE' as const,
        signalementSisMotifSignalement: 'IRTF' as const,
        signalementSisTitreSejourValide: true,
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('signalementConnu derived from detection flag (true only)', () => {
    expect(Rules.computeSignalementConnu({ aiData: { signalementSisDetecte: true } })).toBe(true);
    expect(Rules.computeSignalementConnu({ aiData: { signalementSisDetecte: false } })).toBeNull();
    expect(Rules.computeSignalementConnu({ aiData: {} })).toBeNull();
  });

  it('returns etatSignalant when in whitelist, rejects others', () => {
    expect(Rules.computeEtatSignalant({ aiData: { signalementSisEtatSignalant: 'FRANCE' } })).toBe('FRANCE');
    expect(Rules.computeEtatSignalant({ aiData: { signalementSisEtatSignalant: 'AUTRE_ETAT_MEMBRE' } }))
      .toBe('AUTRE_ETAT_MEMBRE');
    expect(Rules.computeEtatSignalant({
      aiData: { signalementSisEtatSignalant: 'ESPAGNE' as unknown as 'INCONNU' },
    })).toBeNull();
  });

  it('returns motifSignalement when in whitelist, rejects others', () => {
    expect(Rules.computeMotifSignalement({ aiData: { signalementSisMotifSignalement: 'IRTF' } })).toBe('IRTF');
    expect(Rules.computeMotifSignalement({ aiData: { signalementSisMotifSignalement: 'MENACE_ORDRE_PUBLIC' } }))
      .toBe('MENACE_ORDRE_PUBLIC');
    expect(Rules.computeMotifSignalement({
      aiData: { signalementSisMotifSignalement: 'ESPIONNAGE' as unknown as 'AUTRE' },
    })).toBeNull();
  });

  it('returns titreSejourValide when boolean, rejects non-boolean', () => {
    expect(Rules.computeTitreSejourValide({ aiData: { signalementSisTitreSejourValide: true } })).toBe(true);
    expect(Rules.computeTitreSejourValide({ aiData: { signalementSisTitreSejourValide: false } })).toBe(false);
    expect(Rules.computeTitreSejourValide({
      aiData: { signalementSisTitreSejourValide: 'true' as unknown as boolean },
    })).toBeNull();
  });

  it('returns 1 when only etatSignalant is present (partiel)', () => {
    expect(Rules.computePrefillCount({ aiData: { signalementSisEtatSignalant: 'FRANCE' } })).toBe(1);
  });

  it('returns 4 when all 4 prefill fields are present (complet)', () => {
    const input = {
      aiData: {
        signalementSisDetecte: true,
        signalementSisEtatSignalant: 'AUTRE_ETAT_MEMBRE' as const,
        signalementSisMotifSignalement: 'MESURE_ELOIGNEMENT_ETRANGERE' as const,
        signalementSisTitreSejourValide: true,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(4);
  });
});

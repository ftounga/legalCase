import { SinglePermitBePrefillRules as Rules } from './single-permit-be-section-prefill-rules';

describe('SinglePermitBePrefillRules', () => {
  it('returns 0 when no aiData (gate BE manquant)', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when workspaceCountry is FRANCE (mono-pays BE)', () => {
    const input = {
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitDateFin: '2026-12-31',
        singlePermitRegion: 'WALLONIE',
        singlePermitTypeActivite: 'SALARIE',
        singlePermitMotif: 'NOUVEAU',
      },
      workspaceCountry: 'FRANCE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 2 when only 2 partial fields present', () => {
    const input = {
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitRegion: 'BRUXELLES',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeDateDebutPermit(input)).toBe('2026-01-01');
    expect(Rules.computeRegionInstruction(input)).toBe('BRUXELLES');
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('returns 5 when all 5 prefill fields are present', () => {
    const input = {
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitDateFin: '2026-12-31',
        singlePermitRegion: 'WALLONIE',
        singlePermitTypeActivite: 'SALARIE',
        singlePermitMotif: 'NOUVEAU',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(5);
  });

  it('rejects malformed dates', () => {
    expect(Rules.computeDateDebutPermit({
      aiData: { singlePermitDateDebut: '01/01/2026' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeDateFinPermit({
      aiData: { singlePermitDateFin: 'not-a-date' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('normalizes region case (wallonie -> WALLONIE)', () => {
    expect(Rules.computeRegionInstruction({
      aiData: { singlePermitRegion: 'wallonie' as unknown as 'WALLONIE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('WALLONIE');
    expect(Rules.computeRegionInstruction({
      aiData: { singlePermitRegion: ' Flandre ' as unknown as 'FLANDRE' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('FLANDRE');
  });

  it('rejects region outside whitelist', () => {
    expect(Rules.computeRegionInstruction({
      aiData: { singlePermitRegion: 'LIEGE' as unknown as 'WALLONIE' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
    expect(Rules.computeRegionInstruction({
      aiData: { singlePermitRegion: null },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('accepts the 5 type activité values', () => {
    for (const t of ['SALARIE', 'STAGIAIRE', 'DETACHE', 'CHERCHEUR', 'ETUDIANT']) {
      expect(Rules.computeTypeActivite({
        aiData: { singlePermitTypeActivite: t as 'SALARIE' },
        workspaceCountry: 'BELGIQUE',
      })).toBe(t);
    }
  });

  it('rejects unknown type activité', () => {
    expect(Rules.computeTypeActivite({
      aiData: { singlePermitTypeActivite: 'BENEVOLE' as unknown as 'SALARIE' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('accepts NOUVEAU and RENOUVELLEMENT motif only', () => {
    expect(Rules.computeMotifDemande({
      aiData: { singlePermitMotif: 'NOUVEAU' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('NOUVEAU');
    expect(Rules.computeMotifDemande({
      aiData: { singlePermitMotif: 'RENOUVELLEMENT' },
      workspaceCountry: 'BELGIQUE',
    })).toBe('RENOUVELLEMENT');
    expect(Rules.computeMotifDemande({
      aiData: { singlePermitMotif: 'TRANSFERT' as unknown as 'NOUVEAU' },
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('does NOT count etrangerMalade fields (other Immigration tool)', () => {
    const input = {
      aiData: {
        etrangerMaladePathologie: 'VIH',
        etrangerMaladeAvisOFII: 'FAVORABLE',
      },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });
});

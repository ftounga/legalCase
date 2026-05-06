import { computeBadge, getProcedureChecksBadgeFor } from './procedure-check-badge.helper';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';

const TOOL_ID = 'F-DT-08-licenciement-validity';

function check(matchStatus: ProcedureCheckAlignment['matchStatus'], toolIdCible: string | null = TOOL_ID): ProcedureCheckAlignment {
  return {
    checkId: matchStatus + '-' + Math.random().toString(36).slice(2, 6),
    libelle: 'Notification du licenciement',
    critereCode: 'LICENCIEMENT_NOTIFICATION',
    statut: matchStatus === 'ALIGNED' || matchStatus === 'DIVERGENT' ? 'VERIFIED'
      : matchStatus === 'NON_COMPLIANT_FLAG' ? 'NON_COMPLIANT'
      : 'TO_CHECK',
    expectedValue: 'LRAR',
    raison: matchStatus === 'NON_COMPLIANT_FLAG' ? 'Pas de LRAR transmis' : null,
    toolIdCible,
    matchStatus,
  };
}

describe('procedure-check-badge.helper — F-193 SF-193-02', () => {
  describe('computeBadge', () => {
    it('liste vide → kind=none, counts à zéro', () => {
      expect(computeBadge([])).toEqual({
        kind: 'none',
        counts: { verified: 0, nonCompliant: 0, toVerify: 0 },
      });
    });

    it('uniquement ALIGNED → kind=verified', () => {
      expect(computeBadge([check('ALIGNED'), check('ALIGNED')])).toEqual({
        kind: 'verified',
        counts: { verified: 2, nonCompliant: 0, toVerify: 0 },
      });
    });

    it('uniquement NON_COMPLIANT_FLAG → kind=non_compliant', () => {
      expect(computeBadge([check('NON_COMPLIANT_FLAG')])).toEqual({
        kind: 'non_compliant',
        counts: { verified: 0, nonCompliant: 1, toVerify: 0 },
      });
    });

    it('uniquement TO_VERIFY_FLAG → kind=to_verify', () => {
      expect(computeBadge([check('TO_VERIFY_FLAG')])).toEqual({
        kind: 'to_verify',
        counts: { verified: 0, nonCompliant: 0, toVerify: 1 },
      });
    });

    it('ALIGNED + NON_COMPLIANT_FLAG → kind=mixed', () => {
      expect(computeBadge([check('ALIGNED'), check('NON_COMPLIANT_FLAG')])).toEqual({
        kind: 'mixed',
        counts: { verified: 1, nonCompliant: 1, toVerify: 0 },
      });
    });

    it('ignore DIVERGENT et NOT_ANALYZED dans le compteur (counts à 0)', () => {
      expect(computeBadge([check('DIVERGENT'), check('NOT_ANALYZED')])).toEqual({
        kind: 'none',
        counts: { verified: 0, nonCompliant: 0, toVerify: 0 },
      });
    });
  });

  describe('getProcedureChecksBadgeFor — filtre par toolId', () => {
    it('filtre les checks d\'autres outils', () => {
      const list = [
        check('ALIGNED', TOOL_ID),
        check('NON_COMPLIANT_FLAG', 'F-DT-09-comparateur-indemnites'),
      ];
      expect(getProcedureChecksBadgeFor({ proceduresChecksAlignment: list }, TOOL_ID)).toEqual({
        kind: 'verified',
        counts: { verified: 1, nonCompliant: 0, toVerify: 0 },
      });
    });

    it('input null/undefined → kind=none', () => {
      expect(getProcedureChecksBadgeFor({}, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { verified: 0, nonCompliant: 0, toVerify: 0 },
      });
      expect(getProcedureChecksBadgeFor({ proceduresChecksAlignment: null }, TOOL_ID)).toEqual({
        kind: 'none',
        counts: { verified: 0, nonCompliant: 0, toVerify: 0 },
      });
    });
  });
});

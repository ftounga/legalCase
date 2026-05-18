import {
  ProcedureNulliteLicenciementSectionPrefillRules,
  ProcedureNulliteLicenciementPrefillInput,
  computeConvocationEnvoyee,
  computeDateConvocation,
  computeDateEntretien,
  computeEntretienTenu,
  computeDateNotification,
  computeLettreEcrite,
  computeLettreMotivee,
  computeMotivationSuffisante,
  computePrefillCount,
} from './procedure-nullite-licenciement-section-prefill-rules';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

/** Fixture IA complète — les 8 champs pré-remplissables renseignés. */
const FULL_AI: TravailExtractedData = {
  convocationEntretienDetectee: true,
  dateConvocationEntretienDetectee: '2026-02-10',
  dateEntretienPrealableDetectee: '2026-02-18',
  entretienPrealableTenuDetected: { reponse: 'OUI', justification: 'PV produit' },
  dateLicenciement: '2026-02-25',
  lettreLicenciementEcriteDetectee: true,
  lettreLicenciementMotiveeDetected: { reponse: 'NON', justification: 'Motif vague' },
  motivationLettreSuffisanteDetected: { reponse: 'NON', justification: 'Pas de fait précis' },
};

function input(
  aiData: TravailExtractedData | null,
  workspaceCountry = 'FRANCE',
): ProcedureNulliteLicenciementPrefillInput {
  return { aiData, workspaceCountry };
}

describe('ProcedureNulliteLicenciementSectionPrefillRules', () => {
  describe('computePrefillCount', () => {
    it('cas (a) — input vide retourne 0', () => {
      expect(computePrefillCount({})).toBe(0);
    });

    it('cas (a) — aiData null retourne 0', () => {
      expect(computePrefillCount(input(null))).toBe(0);
    });

    it('cas (b) — 3 champs partiels renseignés retourne 3', () => {
      const partial: TravailExtractedData = {
        convocationEntretienDetectee: true,
        dateConvocationEntretienDetectee: '2026-02-10',
        dateLicenciement: '2026-02-25',
      };
      expect(computePrefillCount(input(partial))).toBe(3);
    });

    it('cas (c) — 8 champs cas nominal retourne 8', () => {
      expect(computePrefillCount(input(FULL_AI))).toBe(8);
    });

    it('workspaceCountry = BELGIQUE retourne 0 même avec aiData complet', () => {
      expect(computePrefillCount(input(FULL_AI, 'BELGIQUE'))).toBe(0);
    });

    it('workspaceCountry absent retourne 0 (gate FR strict)', () => {
      expect(computePrefillCount({ aiData: FULL_AI })).toBe(0);
    });
  });

  describe('computeConvocationEnvoyee', () => {
    it('booléen true renvoyé tel quel', () => {
      expect(computeConvocationEnvoyee(input({ convocationEntretienDetectee: true }))).toBe(true);
    });

    it('booléen false renvoyé tel quel', () => {
      expect(computeConvocationEnvoyee(input({ convocationEntretienDetectee: false }))).toBe(false);
    });

    it('null si champ absent', () => {
      expect(computeConvocationEnvoyee(input({}))).toBeNull();
    });

    it('null si BE', () => {
      expect(
        computeConvocationEnvoyee(input({ convocationEntretienDetectee: true }, 'BELGIQUE')),
      ).toBeNull();
    });
  });

  describe('computeDateConvocation / computeDateEntretien / computeDateNotification', () => {
    it('date ISO valide acceptée', () => {
      expect(computeDateConvocation(input({ dateConvocationEntretienDetectee: '2026-02-10' })))
        .toBe('2026-02-10');
      expect(computeDateEntretien(input({ dateEntretienPrealableDetectee: '2026-02-18' })))
        .toBe('2026-02-18');
      expect(computeDateNotification(input({ dateLicenciement: '2026-02-25' })))
        .toBe('2026-02-25');
    });

    it('date non ISO rejetée → null', () => {
      expect(computeDateConvocation(input({ dateConvocationEntretienDetectee: '10/02/2026' })))
        .toBeNull();
      expect(computeDateEntretien(input({ dateEntretienPrealableDetectee: 'le 18 fevrier' })))
        .toBeNull();
    });

    it('null si BE', () => {
      expect(
        computeDateNotification(input({ dateLicenciement: '2026-02-25' }, 'BELGIQUE')),
      ).toBeNull();
    });
  });

  describe('computeEntretienTenu / computeLettreMotivee / computeMotivationSuffisante', () => {
    it('DetectedAnswer OUI → true', () => {
      expect(computeEntretienTenu(input({ entretienPrealableTenuDetected: { reponse: 'OUI' } })))
        .toBe(true);
    });

    it('DetectedAnswer NON → false', () => {
      expect(computeLettreMotivee(input({ lettreLicenciementMotiveeDetected: { reponse: 'NON' } })))
        .toBe(false);
    });

    it('DetectedAnswer INCONNU → null (non pré-rempli)', () => {
      expect(
        computeMotivationSuffisante(input({ motivationLettreSuffisanteDetected: { reponse: 'INCONNU' } })),
      ).toBeNull();
    });

    it('DetectedAnswer absent → null', () => {
      expect(computeEntretienTenu(input({}))).toBeNull();
    });

    it('null si BE', () => {
      expect(
        computeEntretienTenu(input({ entretienPrealableTenuDetected: { reponse: 'OUI' } }, 'BELGIQUE')),
      ).toBeNull();
    });
  });

  describe('computeLettreEcrite', () => {
    it('booléen false renvoyé tel quel', () => {
      expect(computeLettreEcrite(input({ lettreLicenciementEcriteDetectee: false }))).toBe(false);
    });

    it('null si champ absent', () => {
      expect(computeLettreEcrite(input({}))).toBeNull();
    });
  });

  it('expose le barrel ProcedureNulliteLicenciementSectionPrefillRules', () => {
    expect(ProcedureNulliteLicenciementSectionPrefillRules.computePrefillCount).toBe(
      computePrefillCount,
    );
    expect(ProcedureNulliteLicenciementSectionPrefillRules.computeEntretienTenu).toBe(
      computeEntretienTenu,
    );
  });

  it('ne réexpose plus la constante PREFILL_COUNT_ALWAYS_ZERO', () => {
    expect(
      (ProcedureNulliteLicenciementSectionPrefillRules as Record<string, unknown>)[
        'PREFILL_COUNT_ALWAYS_ZERO'
      ],
    ).toBeUndefined();
  });
});

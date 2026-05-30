import { CadreDirigeantStatutPrefillRules as Rules } from './cadre-dirigeant-statut-section-prefill-rules';

describe('CadreDirigeantStatutPrefillRules', () => {
  it('returns 0 when no aiData', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
  });

  it('returns 0 when workspaceCountry is BELGIQUE (mono-pays FR)', () => {
    const input = {
      aiData: { cadreParticipationDirection: true, salaireBrutMensuel: 9000 },
      workspaceCountry: 'BELGIQUE',
    };
    expect(Rules.computeParticipationDirection(input)).toBeNull();
    expect(Rules.computeNiveauRemuneration(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('returns 1 (partiel) when only participationDirection present', () => {
    const input = { aiData: { cadreParticipationDirection: true } };
    expect(Rules.computeParticipationDirection(input)).toBe(true);
    expect(Rules.computeNiveauRemuneration(input)).toBeNull();
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps participationDirection=false (booléen explicite, compte comme pré-rempli)', () => {
    const input = { aiData: { cadreParticipationDirection: false } };
    expect(Rules.computeParticipationDirection(input)).toBe(false);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('maps niveauRemuneration from aiData.salaireBrutMensuel (> 0)', () => {
    const input = { aiData: { salaireBrutMensuel: 9000 } };
    expect(Rules.computeNiveauRemuneration(input)).toBe(9000);
    expect(Rules.computePrefillCount(input)).toBe(1);
  });

  it('returns 2 (nominal) when both fields present', () => {
    const input = {
      aiData: { cadreParticipationDirection: true, salaireBrutMensuel: 12000 },
    };
    expect(Rules.computeParticipationDirection(input)).toBe(true);
    expect(Rules.computeNiveauRemuneration(input)).toBe(12000);
    expect(Rules.computePrefillCount(input)).toBe(2);
  });

  it('rejects a non-boolean participationDirection', () => {
    expect(Rules.computeParticipationDirection({ aiData: { cadreParticipationDirection: 'oui' as unknown as boolean } })).toBeNull();
    expect(Rules.computeParticipationDirection({ aiData: { cadreParticipationDirection: 1 as unknown as boolean } })).toBeNull();
    expect(Rules.computeParticipationDirection({ aiData: { cadreParticipationDirection: null } })).toBeNull();
  });

  it('rejects a non-positive or non-numeric niveauRemuneration', () => {
    expect(Rules.computeNiveauRemuneration({ aiData: { salaireBrutMensuel: 0 } })).toBeNull();
    expect(Rules.computeNiveauRemuneration({ aiData: { salaireBrutMensuel: -100 } })).toBeNull();
    expect(Rules.computeNiveauRemuneration({ aiData: { salaireBrutMensuel: '9000' as unknown as number } })).toBeNull();
    expect(Rules.computeNiveauRemuneration({ aiData: { salaireBrutMensuel: null } })).toBeNull();
  });

  it('does NOT count statutCadreDirigeantDetecte flag alone (visibility trigger, not a form field)', () => {
    const input = { aiData: { statutCadreDirigeantDetecte: true } };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  it('treats absent workspaceCountry as FRANCE (default)', () => {
    const input = {
      aiData: { cadreParticipationDirection: true, salaireBrutMensuel: 9000 },
    };
    expect(Rules.computePrefillCount(input)).toBe(2);
  });
});

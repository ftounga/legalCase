import { TempsTrajetDeplacementPrefillRules } from './temps-trajet-deplacement-section-prefill-rules';

describe('TempsTrajetDeplacementPrefillRules', () => {

  it('computeTypeTrajet lit un type connu depuis Sf218dDetail', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({
      aiData: { type_trajet: 'ITINERANT_SANS_LIEU_FIXE' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('ITINERANT_SANS_LIEU_FIXE');
  });

  it('computeTypeTrajet normalise la casse', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({
      aiData: { type_trajet: 'domicile_travail_habituel' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe('DOMICILE_TRAVAIL_HABITUEL');
  });

  it('computeTypeTrajet ignore une valeur inconnue / absente', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({
      aiData: { type_trajet: 'INCONNU' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({
      aiData: {} as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({})).toBeNull();
  });

  it('computeTypeTrajet retourne null hors France', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTypeTrajet({
      aiData: { type_trajet: 'ITINERANT_SANS_LIEU_FIXE' } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computeTempsTrajetQuotidienMinutes lit des minutes ≥ 0 (nombre ou chaîne)', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes({
      aiData: { temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(90);
    expect(TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes({
      aiData: { temps_trajet_quotidien_minutes: '45' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(45);
  });

  it('computeTempsTrajetQuotidienMinutes ignore les valeurs négatives / non numériques', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes({
      aiData: { temps_trajet_quotidien_minutes: -5 } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
    expect(TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes({
      aiData: { temps_trajet_quotidien_minutes: 'abc' } as any,
      workspaceCountry: 'FRANCE',
    })).toBeNull();
  });

  it('computeTempsTrajetQuotidienMinutes retourne null hors France', () => {
    expect(TempsTrajetDeplacementPrefillRules.computeTempsTrajetQuotidienMinutes({
      aiData: { temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBeNull();
  });

  it('computePrefillCount additionne les deux champs pré-remplis', () => {
    expect(TempsTrajetDeplacementPrefillRules.computePrefillCount({
      aiData: { type_trajet: 'DOMICILE_TRAVAIL_HABITUEL', temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(2);
    expect(TempsTrajetDeplacementPrefillRules.computePrefillCount({
      aiData: { type_trajet: 'DOMICILE_TRAVAIL_HABITUEL' } as any,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(TempsTrajetDeplacementPrefillRules.computePrefillCount({})).toBe(0);
    expect(TempsTrajetDeplacementPrefillRules.computePrefillCount({
      aiData: { type_trajet: 'DOMICILE_TRAVAIL_HABITUEL', temps_trajet_quotidien_minutes: 90 } as any,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });
});

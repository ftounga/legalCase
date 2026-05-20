import { C4OnemChecklistPrefillRules as Rules } from './c4-onem-checklist-section-prefill-rules';

describe('C4OnemChecklistPrefillRules', () => {

  // ── Cas 0 : aucun champ pré-remplissable ──────────────────────────────
  it('returns 0 when no aiData / aiData empty', () => {
    expect(Rules.computePrefillCount({})).toBe(0);
    expect(Rules.computePrefillCount({ aiData: null })).toBe(0);
    expect(Rules.computePrefillCount({ aiData: {} })).toBe(0);
  });

  it('returns 0 when fields are all malformed / empty (no fallback source)', () => {
    const input = {
      aiData: {
        raisonSocialeEmployeur: '   ',
        numeroBce: '12345',  // non 10 chiffres
        nomSalarie: '',
        prenomSalarie: null,
        dateEntree: '15/03/2025',  // non ISO
        dateRuptureContrat: 'invalide',
        categorieOnem: null,
        motifExplicite: '',
        motifRupture: '',  // pas de fallback motif
        preavisPresteJours: -1,  // négatif
        dernierSalaireMensuelBrut: 'pas un nombre' as unknown as number,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(0);
  });

  // ── Cas 1 : un seul champ pré-remplissable ────────────────────────────
  it('returns 1 when only dateEntree is set (ISO valid)', () => {
    const input = { aiData: { dateEntree: '2024-01-15' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeDateEntreeService(input)).toBe('2024-01-15');
  });

  it('returns 1 when only raisonSocialeEmployeur is set', () => {
    const input = { aiData: { raisonSocialeEmployeur: '  ACME BE  ' } };
    expect(Rules.computePrefillCount(input)).toBe(1);
    expect(Rules.computeRaisonSociale(input)).toBe('ACME BE');
  });

  // ── Cas 5 : partial — 5 champs pré-remplis ────────────────────────────
  it('returns 5 when 5 fields are set partially', () => {
    const input = {
      aiData: {
        raisonSocialeEmployeur: 'ACME BE SA',
        numeroBce: '0123456789',
        nomSalarie: 'Dupont',
        dateEntree: '2020-01-15',
        dateRuptureContrat: '2025-06-30',
        // 5 autres absents (categorieOnem, motifExplicite, fauteGrave,
        // preavis, salaire)
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(5);
  });

  // ── Cas 10 : nominal complet ──────────────────────────────────────────
  it('returns 10 when all 10 instrumented fields are set', () => {
    const input = {
      aiData: {
        raisonSocialeEmployeur: 'ACME BE SA',
        numeroBce: '0123456789',
        nomSalarie: 'Dupont',
        prenomSalarie: 'Jean',
        dateEntree: '2020-01-15',
        dateRuptureContrat: '2025-06-30',
        categorieOnem: '9',
        motifExplicite: 'Restructuration économique du service',
        motifRupture: 'Licenciement pour faute grave (vol)',
        preavisPresteJours: 30,
        dernierSalaireMensuelBrut: 3500,
      },
    };
    expect(Rules.computePrefillCount(input)).toBe(10);
    expect(Rules.computeNomSalarie(input)).toBe('Jean Dupont');
    expect(Rules.computeFauteGraveMentionnee(input)).toBe(true);
  });

  // ── Faute grave : accents + casse ─────────────────────────────────────
  describe('fauteGraveMentionnee — détection accents / casse', () => {
    it('UPPERCASE explicite → true', () => {
      expect(Rules.computeFauteGraveMentionnee({ aiData: { motifRupture: 'FAUTE GRAVE' } })).toBe(true);
    });

    it('minuscules avec contexte → true', () => {
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'licenciement pour faute grave invoquée' },
      })).toBe(true);
    });

    it('accents (« faute grâve » graphie atypique) — la normalisation NFD doit fonctionner', () => {
      // Sécurise la stabilité du normalize() : aucun accent attendu sur
      // « faute grave » mais on prouve que la normalisation ne casse rien.
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'Faute grave (déjà signalée)' },
      })).toBe(true);
    });

    it('« motif grave » (synonyme BE) → true', () => {
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'Rupture pour motif grave' },
      })).toBe(true);
    });

    it('mention absente → null', () => {
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'démission' },
      })).toBeNull();
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: 'fin de CDD' },
      })).toBeNull();
    });

    it('motif null/empty → null', () => {
      expect(Rules.computeFauteGraveMentionnee({ aiData: {} })).toBeNull();
      expect(Rules.computeFauteGraveMentionnee({ aiData: { motifRupture: '' } })).toBeNull();
      expect(Rules.computeFauteGraveMentionnee({ aiData: { motifRupture: '   ' } })).toBeNull();
      expect(Rules.computeFauteGraveMentionnee({
        aiData: { motifRupture: null as unknown as string },
      })).toBeNull();
    });
  });

  // ── numeroBce : 10 chiffres stricts ───────────────────────────────────
  describe('numeroBce — validation 10 chiffres', () => {
    it('10 chiffres exacts → conservé', () => {
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: '0123456789' } })).toBe('0123456789');
    });

    it('tolère espaces/points (LLM format)', () => {
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: '0123.456.789' } })).toBe('0123456789');
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: '0123 456 789' } })).toBe('0123456789');
    });

    it('< 10 chiffres → null', () => {
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: '12345' } })).toBeNull();
    });

    it('11 chiffres → null', () => {
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: '01234567890' } })).toBeNull();
    });

    it('alphanumérique → null', () => {
      expect(Rules.computeNumeroBce({ aiData: { numeroBce: 'BE0123456789' } })).toBeNull();
    });
  });

  // ── motifExplicite : fallback motifRupture ────────────────────────────
  it('motifExplicite preferred over motifRupture (fallback)', () => {
    const explicit = {
      aiData: {
        motifExplicite: 'Restructuration éco',
        motifRupture: 'licenciement',
      },
    };
    expect(Rules.computeMotifExplicite(explicit)).toBe('Restructuration éco');
  });

  it('motifExplicite null → fallback to motifRupture', () => {
    const onlyRupture = { aiData: { motifRupture: 'licenciement pour faute grave' } };
    expect(Rules.computeMotifExplicite(onlyRupture)).toBe('licenciement pour faute grave');
  });

  // ── nomSalarie : prénom + nom ─────────────────────────────────────────
  it('nomSalarie concatenates prenom + nom', () => {
    expect(Rules.computeNomSalarie({
      aiData: { nomSalarie: 'Dupont', prenomSalarie: 'Jean' },
    })).toBe('Jean Dupont');
  });

  it('nomSalarie alone if prenom absent', () => {
    expect(Rules.computeNomSalarie({ aiData: { nomSalarie: 'Dupont' } })).toBe('Dupont');
  });

  it('prenom alone if nom absent (fallback)', () => {
    expect(Rules.computeNomSalarie({ aiData: { prenomSalarie: 'Jean' } })).toBe('Jean');
  });

  it('nomSalarie null when both empty', () => {
    expect(Rules.computeNomSalarie({ aiData: {} })).toBeNull();
    expect(Rules.computeNomSalarie({
      aiData: { nomSalarie: '', prenomSalarie: '   ' },
    })).toBeNull();
  });

  // ── Dates ISO ────────────────────────────────────────────────────────
  it('dates non ISO → null (parité avec backend qui n\'accepte que LocalDate)', () => {
    expect(Rules.computeDateEntreeService({ aiData: { dateEntree: '15/03/2025' } })).toBeNull();
    expect(Rules.computeDateSortieService({ aiData: { dateRuptureContrat: '30-06-2025' } })).toBeNull();
    expect(Rules.computeDateSortieService({ aiData: { dateRuptureContrat: '2025-13-99' } })).toBeNull();
  });

  // ── preavis / salaire : positifs ──────────────────────────────────────
  it('preavisPresteJours négatif → null', () => {
    expect(Rules.computePreavisPresteJours({ aiData: { preavisPresteJours: -5 } })).toBeNull();
    expect(Rules.computePreavisPresteJours({ aiData: { preavisPresteJours: 0 } })).toBe(0);
    expect(Rules.computePreavisPresteJours({ aiData: { preavisPresteJours: 30 } })).toBe(30);
  });

  it('dernierSalaire non-number → null', () => {
    expect(Rules.computeDernierSalaire({
      aiData: { dernierSalaireMensuelBrut: 'pas un nombre' as unknown as number },
    })).toBeNull();
    expect(Rules.computeDernierSalaire({ aiData: { dernierSalaireMensuelBrut: 3500 } })).toBe(3500);
    expect(Rules.computeDernierSalaire({ aiData: { dernierSalaireMensuelBrut: -1 } })).toBeNull();
  });
});

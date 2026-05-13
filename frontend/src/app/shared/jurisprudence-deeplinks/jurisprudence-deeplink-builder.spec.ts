import {
  buildDoctrineUrl,
  buildLexisPlusUrl,
  buildLextensoUrl,
  extractKeywords,
} from './jurisprudence-deeplink-builder';

describe('jurisprudence-deeplink-builder', () => {
  describe('extractKeywords', () => {
    it('retourne [] pour texte vide, null ou undefined', () => {
      expect(extractKeywords('')).toEqual([]);
      expect(extractKeywords('   ')).toEqual([]);
      expect(extractKeywords(null)).toEqual([]);
      expect(extractKeywords(undefined)).toEqual([]);
    });

    it('extrait les mots significatifs et filtre les stop-words', () => {
      const result = extractKeywords(
        "Le licenciement pour faute grave doit être motivé par l'employeur"
      );
      expect(result).toContain('licenciement');
      expect(result).toContain('faute');
      expect(result).toContain('grave');
      expect(result).toContain('motivé');
      expect(result).toContain('employeur');
      expect(result).not.toContain('le');
      expect(result).not.toContain('pour');
      expect(result).not.toContain('par');
    });

    it('filtre les tokens trop courts (< 4 caractères)', () => {
      const result = extractKeywords('Le bon CDI a une fin claire');
      expect(result).not.toContain('bon');
      expect(result).not.toContain('cdi');
      expect(result).not.toContain('fin');
      expect(result).toContain('claire');
    });

    it('dédoublonne en conservant l\'ordre d\'apparition', () => {
      const result = extractKeywords(
        'licenciement abusif licenciement injustifié licenciement'
      );
      expect(result.filter(k => k === 'licenciement')).toHaveLength(1);
      expect(result[0]).toBe('licenciement');
      expect(result).toContain('abusif');
      expect(result).toContain('injustifié');
    });

    it('tronque à 8 mots-clés maximum', () => {
      const result = extractKeywords(
        'licenciement faute grave employeur motivation préavis indemnité chômage transaction conventionnelle rupture conventionnelle'
      );
      expect(result.length).toBeLessThanOrEqual(8);
    });

    it('préserve les accents Unicode dans la tokenisation', () => {
      const result = extractKeywords('Préavis légalement obligé');
      expect(result).toContain('préavis');
      expect(result).toContain('légalement');
      expect(result).toContain('obligé');
    });

    it('lowercase tous les tokens', () => {
      const result = extractKeywords('LICENCIEMENT Faute GRAVE');
      expect(result).toEqual(['licenciement', 'faute', 'grave']);
    });
  });

  describe('buildDoctrineUrl', () => {
    it('construit une URL Doctrine FR par défaut', () => {
      const url = buildDoctrineUrl(['licenciement', 'faute', 'grave']);
      expect(url).toContain('https://www.doctrine.fr/search');
      expect(url).toContain('type=jurisprudence');
      expect(url).toContain('licenciement');
    });

    it('construit une URL Doctrine BE quand country=BE', () => {
      const url = buildDoctrineUrl(['licenciement', 'préavis'], 'BE');
      expect(url).toContain('https://www.doctrine.be/search');
      expect(url).toContain('type=jurisprudence');
    });

    it('encode correctement les accents et caractères spéciaux', () => {
      const url = buildDoctrineUrl(['préavis', 'légalement']);
      expect(url).toContain('pr%C3%A9avis');
      expect(url).toContain('l%C3%A9galement');
    });

    it('encode l\'espace entre mots-clés', () => {
      const url = buildDoctrineUrl(['licenciement', 'faute']);
      expect(url).toContain('licenciement%20faute');
    });
  });

  describe('buildLexisPlusUrl', () => {
    it('construit une URL Lexis 360 Intelligence valide', () => {
      const url = buildLexisPlusUrl(['licenciement', 'faute', 'grave']);
      expect(url).toContain('https://www.lexis360intelligence.fr/recherche');
      expect(url).toContain('q=');
      expect(url).toContain('licenciement');
    });

    it('encode correctement les accents', () => {
      const url = buildLexisPlusUrl(['préavis']);
      expect(url).toContain('pr%C3%A9avis');
    });
  });

  describe('buildLextensoUrl', () => {
    it('construit une URL Lextenso valide', () => {
      const url = buildLextensoUrl(['rupture', 'conventionnelle']);
      expect(url).toContain('https://www.lextenso.fr/recherche');
      expect(url).toContain('searchText=');
      expect(url).toContain('rupture');
    });

    it('encode correctement les accents', () => {
      const url = buildLextensoUrl(['préavis']);
      expect(url).toContain('pr%C3%A9avis');
    });
  });
});

import {
  isDispositifSection,
  parseConclusion,
  toDispositifItems,
  toParagraphs,
} from './conclusion-parser';

describe('parseConclusion', () => {
  it('content vide / null / undefined → aucune section', () => {
    expect(parseConclusion(null)).toEqual([]);
    expect(parseConclusion(undefined)).toEqual([]);
    expect(parseConclusion('')).toEqual([]);
    expect(parseConclusion('   \n  ')).toEqual([]);
  });

  it('découpe plusieurs sections selon l\'heuristique MAJUSCULES', () => {
    const content = [
      'FAITS ET PROCÉDURE',
      'Le salarié a été licencié.',
      '',
      'DISCUSSION',
      'Le licenciement est sans cause.',
    ].join('\n');

    const sections = parseConclusion(content);
    expect(sections.length).toBe(2);
    expect(sections[0].title).toBe('FAITS ET PROCÉDURE');
    expect(sections[0].lines).toContain('Le salarié a été licencié.');
    expect(sections[1].title).toBe('DISCUSSION');
    expect(sections[1].lines).toContain('Le licenciement est sans cause.');
  });

  it('texte avant le 1er titre → section title:null (préambule)', () => {
    const content = [
      'Préambule introductif.',
      '',
      'DISCUSSION',
      'Argument.',
    ].join('\n');

    const sections = parseConclusion(content);
    expect(sections[0].title).toBeNull();
    expect(sections[0].lines).toContain('Préambule introductif.');
    expect(sections[1].title).toBe('DISCUSSION');
  });

  it('aucun titre détecté → une seule section fallback title:null', () => {
    const content = 'Première ligne.\nDeuxième ligne.';
    const sections = parseConclusion(content);
    expect(sections.length).toBe(1);
    expect(sections[0].title).toBeNull();
    expect(sections[0].lines).toEqual(['Première ligne.', 'Deuxième ligne.']);
  });

  it('détecte POUR / CONTRE comme titres de section', () => {
    const content = ['POUR', 'M. Dupont', 'CONTRE', 'La société Y'].join('\n');
    const sections = parseConclusion(content);
    expect(sections[0].title).toBe('POUR');
    expect(sections[0].lines).toEqual(['M. Dupont']);
    expect(sections[1].title).toBe('CONTRE');
    expect(sections[1].lines).toEqual(['La société Y']);
  });

  it('PAR CES MOTIFS → section dispositif avec items', () => {
    const content = [
      'PAR CES MOTIFS',
      'DIRE le licenciement sans cause.',
      'CONDAMNER la société à 10 000 €.',
    ].join('\n');

    const sections = parseConclusion(content);
    const dispositif = sections.find((s) => isDispositifSection(s));
    expect(dispositif).toBeDefined();
    const items = toDispositifItems(dispositif!.lines);
    expect(items.length).toBe(2);
    expect(items[0]).toBe('DIRE le licenciement sans cause.');
  });
});

describe('isDispositifSection', () => {
  it('reconnaît « PAR CES MOTIFS » insensible à la casse/accents/ponctuation', () => {
    expect(isDispositifSection({ title: 'PAR CES MOTIFS', lines: [] })).toBe(true);
    expect(isDispositifSection({ title: '  PAR CES MOTIFS  ', lines: [] })).toBe(true);
  });

  it('rejette les autres titres et le préambule', () => {
    expect(isDispositifSection({ title: 'DISCUSSION', lines: [] })).toBe(false);
    expect(isDispositifSection({ title: null, lines: [] })).toBe(false);
  });
});

describe('toParagraphs', () => {
  it('sépare les paragraphes sur ligne vide, joint les lignes d\'un bloc', () => {
    const lines = ['Ligne A', 'suite A', '', 'Ligne B'];
    expect(toParagraphs(lines)).toEqual(['Ligne A suite A', 'Ligne B']);
  });

  it('ignore les lignes vides en tête/queue, pas de paragraphe vide', () => {
    expect(toParagraphs(['', 'X', '', ''])).toEqual(['X']);
  });
});

describe('toDispositifItems', () => {
  it('retire la numérotation / puce source de tête', () => {
    const lines = ['1. Premier', '- Deuxième', '• Troisième', '2) Quatrième'];
    expect(toDispositifItems(lines)).toEqual([
      'Premier',
      'Deuxième',
      'Troisième',
      'Quatrième',
    ]);
  });

  it('ignore les lignes vides', () => {
    expect(toDispositifItems(['', 'Item', '  '])).toEqual(['Item']);
  });
});

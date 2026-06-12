import { diffLines, summarizeDiff } from './conclusion-diff.util';

describe('conclusion-diff.util', () => {
  describe('diffLines', () => {
    it('marque toutes les lignes inchangées quand les contenus sont identiques', () => {
      const diff = diffLines('a\nb\nc', 'a\nb\nc');
      expect(diff.every((l) => l.type === 'unchanged')).toBe(true);
      expect(diff.map((l) => l.text)).toEqual(['a', 'b', 'c']);
    });

    it('détecte une ligne ajoutée', () => {
      const diff = diffLines('a\nb', 'a\nb\nc');
      expect(diff).toEqual([
        { type: 'unchanged', text: 'a' },
        { type: 'unchanged', text: 'b' },
        { type: 'added', text: 'c' },
      ]);
    });

    it('détecte une ligne supprimée', () => {
      const diff = diffLines('a\nb\nc', 'a\nc');
      expect(diff).toEqual([
        { type: 'unchanged', text: 'a' },
        { type: 'removed', text: 'b' },
        { type: 'unchanged', text: 'c' },
      ]);
    });

    it('rend une modification de ligne comme suppression + ajout', () => {
      const diff = diffLines('a\nb', 'a\nB');
      expect(diff).toEqual([
        { type: 'unchanged', text: 'a' },
        { type: 'removed', text: 'b' },
        { type: 'added', text: 'B' },
      ]);
    });

    it('traite un base vide comme un ajout intégral', () => {
      const diff = diffLines('', 'x\ny');
      expect(diff).toEqual([
        { type: 'added', text: 'x' },
        { type: 'added', text: 'y' },
      ]);
    });

    it('traite un target vide comme une suppression intégrale', () => {
      const diff = diffLines('x\ny', '');
      expect(diff).toEqual([
        { type: 'removed', text: 'x' },
        { type: 'removed', text: 'y' },
      ]);
    });

    it('gère null/undefined comme un document vide', () => {
      expect(diffLines(null, null)).toEqual([]);
      expect(diffLines(undefined, 'a')).toEqual([{ type: 'added', text: 'a' }]);
    });

    it('normalise CRLF pour ne pas générer de faux écarts', () => {
      const diff = diffLines('a\r\nb', 'a\nb');
      expect(diff.every((l) => l.type === 'unchanged')).toBe(true);
    });
  });

  describe('summarizeDiff', () => {
    it('compte les ajouts et suppressions', () => {
      const diff = diffLines('a\nb\nc', 'a\nB\nc\nd');
      // b -> B (removed b, added B) ; + d (added)
      expect(summarizeDiff(diff)).toEqual({ added: 2, removed: 1 });
    });

    it('renvoie 0/0 pour des contenus identiques', () => {
      expect(summarizeDiff(diffLines('a\nb', 'a\nb'))).toEqual({
        added: 0,
        removed: 0,
      });
    });
  });
});

import { TestBed } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { LegalCitationsPipe } from './legal-citations.pipe';

describe('LegalCitationsPipe', () => {
  let pipe: LegalCitationsPipe;
  let sanitizer: DomSanitizer;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    sanitizer = TestBed.inject(DomSanitizer);
    pipe = new LegalCitationsPipe(sanitizer);
  });

  // Helper — transform and turn SafeHtml back to raw string for assertions.
  const render = (input: string): string => {
    const result = pipe.transform(input) as any;
    return sanitizer.sanitize(1 /* SecurityContext.HTML */, result) || '';
  };

  it('wraps "Art. L.1235-3-1 Code du travail" in <code>', () => {
    const out = render('Voir Art. L.1235-3-1 Code du travail pour le détail.');
    expect(out).toContain('<code>Art. L.1235-3-1 Code du travail</code>');
  });

  it('wraps bare "L.3245-1" reference', () => {
    const out = render('Prescription 3 ans (L.3245-1)');
    expect(out).toContain('<code>L.3245-1</code>');
  });

  it('wraps "art. L.614-5 al. 2 CESEDA"', () => {
    const out = render('Recours devant le TA suspensif (art. L.614-5 al. 2 CESEDA).');
    expect(out).toContain('<code>art. L.614-5 al. 2 CESEDA</code>');
  });

  it('wraps "Loi 15/12/1980"', () => {
    const out = render('Conforme à la Loi 15/12/1980');
    expect(out).toContain('<code>Loi 15/12/1980</code>');
  });

  it('wraps "art. 39/82 §4 Loi 15/12/1980"', () => {
    const out = render('URGENT : recours en extrême urgence (art. 39/82 §4 Loi 15/12/1980).');
    expect(out).toContain('<code>');
  });

  it('returns empty string for null / undefined / empty', () => {
    expect(pipe.transform(null) as any).toBe('');
    expect(pipe.transform(undefined) as any).toBe('');
    expect(pipe.transform('') as any).toBe('');
  });

  it('escapes HTML in the input to prevent XSS', () => {
    const out = render('<script>alert(1)</script> Art. L.1 Code du travail');
    expect(out).not.toContain('<script>');
    expect(out).toContain('&lt;script&gt;');
  });

  it('leaves plain text without citations untouched', () => {
    const out = render('Recours suspensif possible.');
    expect(out).toBe('Recours suspensif possible.');
  });
});

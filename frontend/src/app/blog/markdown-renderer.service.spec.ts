import { TestBed } from '@angular/core/testing';
import { MarkdownRendererService } from './markdown-renderer.service';

describe('MarkdownRendererService', () => {
  let service: MarkdownRendererService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MarkdownRendererService);
  });

  it('renders empty string for null/empty input', () => {
    expect(service.render('')).toBe('');
    expect(service.render(null as unknown as string)).toBe('');
  });

  it('renders H2 from "## title"', () => {
    expect(service.render('## Mon titre')).toBe('<h2>Mon titre</h2>');
  });

  it('renders H3 from "### title"', () => {
    expect(service.render('### Sous-titre')).toBe('<h3>Sous-titre</h3>');
  });

  it('aggregates consecutive lines into single paragraph', () => {
    const md = 'Première ligne.\nDeuxième ligne.';
    expect(service.render(md)).toBe('<p>Première ligne. Deuxième ligne.</p>');
  });

  it('separates paragraphs by empty line', () => {
    const md = 'Para 1.\n\nPara 2.';
    expect(service.render(md)).toBe('<p>Para 1.</p>\n<p>Para 2.</p>');
  });

  it('escapes HTML in input', () => {
    const md = '<script>alert(1)</script>';
    const out = service.render(md);
    expect(out).not.toContain('<script>');
    expect(out).toContain('&lt;script&gt;');
  });

  it('renders bold and italic', () => {
    expect(service.render('Voici **fort** et *italique*.')).toContain(
      '<strong>fort</strong>');
    expect(service.render('Voici **fort** et *italique*.')).toContain(
      '<em>italique</em>');
  });

  it('renders unordered list', () => {
    const md = '- Item 1\n- Item 2\n- Item 3';
    expect(service.render(md)).toBe(
      '<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>');
  });

  it('renders ordered list', () => {
    const md = '1. Premier\n2. Deuxième';
    expect(service.render(md)).toBe('<ol><li>Premier</li><li>Deuxième</li></ol>');
  });

  it('renders link with safe URL', () => {
    expect(service.render('Voir [LegalCase](https://legalcase.com).')).toContain(
      '<a href="https://legalcase.com" rel="noopener">LegalCase</a>');
  });

  it('blocks unsafe URL schemes', () => {
    const out = service.render('[click](javascript:alert(1))');
    expect(out).toContain('href="#"');
    expect(out).not.toContain('javascript:');
  });

  it('allows relative URLs starting with /', () => {
    expect(service.render('[Contact](/contact)')).toContain('href="/contact"');
  });

  it('renders complete article-like markdown', () => {
    const md = `Introduction au sujet.

## Première section

Contenu de la première section avec **emphase**.

## Deuxième section

- Premier point
- Second point

Conclusion.`;
    const out = service.render(md);
    expect(out).toContain('<h2>Première section</h2>');
    expect(out).toContain('<h2>Deuxième section</h2>');
    expect(out).toContain('<strong>emphase</strong>');
    expect(out).toContain('<ul><li>Premier point</li>');
  });
});

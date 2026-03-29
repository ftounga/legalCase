import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { LegalPageComponent } from './legal-page.component';
import { MENTIONS_LEGALES, POLITIQUE_CONFIDENTIALITE, CGU } from './legal-content';

function makeRoute(title: string, sections: unknown[]) {
  return { snapshot: { data: { title, sections } } };
}

describe('LegalPageComponent', () => {
  async function setup(title: string, sections: unknown[]) {
    await TestBed.configureTestingModule({
      imports: [LegalPageComponent, RouterTestingModule],
      providers: [{ provide: ActivatedRoute, useValue: makeRoute(title, sections) }]
    }).compileComponents();
    const fixture: ComponentFixture<LegalPageComponent> = TestBed.createComponent(LegalPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('T1 — /mentions-legales : affiche le titre et le contenu', async () => {
    const fixture = await setup('Mentions légales', MENTIONS_LEGALES);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Mentions légales');
    expect(el.querySelector('.legal-body')?.textContent).toContain('NG-CONSULTING');
  });

  it('T2 — /privacy : affiche le titre et les sous-traitants', async () => {
    const fixture = await setup('Politique de confidentialité', POLITIQUE_CONFIDENTIALITE);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Politique de confidentialité');
    expect(el.querySelector('.legal-body')?.textContent).toContain('Anthropic');
  });

  it('T3 — /cgu : affiche le titre et l\'avertissement IA', async () => {
    const fixture = await setup('Conditions Générales d\'Utilisation', CGU);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Conditions Générales');
    expect(el.querySelector('.legal-body')?.textContent).toContain('conseil juridique');
  });

  it('T4 — footer contient les 3 liens légaux', async () => {
    const fixture = await setup('Mentions légales', MENTIONS_LEGALES);
    const links = fixture.nativeElement.querySelectorAll('.legal-footer a');
    const hrefs = Array.from(links).map((a: any) => a.getAttribute('href') ?? a.getAttribute('ng-reflect-router-link'));
    expect(hrefs.some(h => h?.includes('mentions-legales'))).toBeTrue();
    expect(hrefs.some(h => h?.includes('privacy'))).toBeTrue();
    expect(hrefs.some(h => h?.includes('cgu'))).toBeTrue();
  });
});

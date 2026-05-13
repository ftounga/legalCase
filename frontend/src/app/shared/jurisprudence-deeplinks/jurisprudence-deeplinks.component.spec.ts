import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { JurisprudenceDeeplinksComponent } from './jurisprudence-deeplinks.component';

describe('JurisprudenceDeeplinksComponent', () => {
  let component: JurisprudenceDeeplinksComponent;
  let fixture: ComponentFixture<JurisprudenceDeeplinksComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JurisprudenceDeeplinksComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(JurisprudenceDeeplinksComponent);
    component = fixture.componentInstance;
  });

  it('crée le composant', () => {
    expect(component).toBeTruthy();
  });

  it('ne rend rien si pointText est vide', () => {
    component.pointText = '';
    fixture.detectChanges();
    const container = fixture.debugElement.query(By.css('.jurispru-deeplinks'));
    expect(container).toBeNull();
  });

  it('ne rend rien si pointText ne contient que des stop-words ou tokens trop courts', () => {
    component.pointText = 'le la les un une';
    fixture.detectChanges();
    const container = fixture.debugElement.query(By.css('.jurispru-deeplinks'));
    expect(container).toBeNull();
  });

  it('rend les 3 boutons (Doctrine + Lexis+ + Lextenso) en mode FR par défaut', () => {
    component.pointText = 'Licenciement pour faute grave non motivé';
    fixture.detectChanges();
    const doctrine = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-doctrine"]')
    );
    const lexis = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-lexisplus"]')
    );
    const lextenso = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-lextenso"]')
    );
    expect(doctrine).toBeTruthy();
    expect(lexis).toBeTruthy();
    expect(lextenso).toBeTruthy();
  });

  it('rend uniquement le bouton Doctrine en mode BE', () => {
    component.pointText = 'Licenciement pour faute grave non motivé';
    component.workspaceCountry = 'BE';
    fixture.detectChanges();
    const doctrine = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-doctrine"]')
    );
    const lexis = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-lexisplus"]')
    );
    const lextenso = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-lextenso"]')
    );
    expect(doctrine).toBeTruthy();
    expect(lexis).toBeNull();
    expect(lextenso).toBeNull();
  });

  it('utilise www.doctrine.be quand workspaceCountry=BE', () => {
    component.pointText = 'Licenciement pour faute grave';
    component.workspaceCountry = 'BE';
    fixture.detectChanges();
    const doctrineLink = fixture.debugElement.query(
      By.css('[data-testid="jurispru-deeplink-doctrine"]')
    );
    const href = doctrineLink.nativeElement.getAttribute('href');
    expect(href).toContain('doctrine.be');
  });

  it('applique target="_blank" et rel="noopener noreferrer" sur tous les liens', () => {
    component.pointText = 'Licenciement pour faute grave non motivé';
    fixture.detectChanges();
    const links = fixture.debugElement.queryAll(By.css('a.jurispru-deeplinks__btn'));
    expect(links.length).toBeGreaterThan(0);
    for (const link of links) {
      const el = link.nativeElement as HTMLAnchorElement;
      expect(el.getAttribute('target')).toBe('_blank');
      expect(el.getAttribute('rel')).toBe('noopener noreferrer');
    }
  });
});

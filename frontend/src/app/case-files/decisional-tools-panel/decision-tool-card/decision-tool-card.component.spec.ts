import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { DecisionToolCardComponent } from './decision-tool-card.component';
import { DecisionToolSummary, formatSummary } from '../decision-tool-summary.model';

describe('DecisionToolCardComponent', () => {
  let component: DecisionToolCardComponent;
  let fixture: ComponentFixture<DecisionToolCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoopAnimationsModule, DecisionToolCardComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DecisionToolCardComponent);
    component = fixture.componentInstance;

    component.toolId = 'F-IM-05-titre-sejour';
    component.theme = 'DIAGNOSTIC';
    component.icon = 'badge';
    component.title = 'TITRE DE SÉJOUR RECOMMANDÉ';
  });

  function rootEl(): HTMLElement {
    return fixture.debugElement.query(By.css('.tool-card')).nativeElement;
  }

  function badge(selector: string): HTMLElement | null {
    const el = fixture.debugElement.query(By.css(selector));
    return el ? (el.nativeElement as HTMLElement) : null;
  }

  describe('rendu nominal', () => {
    it('rend titre + verdict formaté avec WARNING (bordure orange)', () => {
      component.summary = {
        label: 'Verdict',
        primaryValue: '65 %',
        secondaryValue: 'MOYEN',
        alertLevel: 'WARNING',
      };
      fixture.detectChanges();

      const root = rootEl();
      expect(root.className).toContain('card--warning');

      const verdict = fixture.debugElement.query(By.css('.tool-card__verdict'));
      expect(verdict.nativeElement.textContent.trim()).toBe('Verdict : 65 % MOYEN');

      const titleEl = fixture.debugElement.query(By.css('.tool-card__title'));
      expect(titleEl.nativeElement.textContent.trim()).toBe('TITRE DE SÉJOUR RECOMMANDÉ');
    });

    it('rend bordure verte pour alertLevel OK', () => {
      component.summary = { label: 'Verdict', primaryValue: 'Conforme', alertLevel: 'OK' };
      fixture.detectChanges();
      expect(rootEl().className).toContain('card--ok');
    });

    it('rend bordure rouge pour alertLevel ALERT', () => {
      component.summary = { label: 'Délai', primaryValue: 'J-2', alertLevel: 'ALERT' };
      fixture.detectChanges();
      expect(rootEl().className).toContain('card--alert');
    });

    it('aucune bordure colorée si alertLevel undefined', () => {
      component.summary = { label: 'Indemnité', primaryValue: '3 200 €' };
      fixture.detectChanges();
      const cls = rootEl().className;
      expect(cls).not.toContain('card--ok');
      expect(cls).not.toContain('card--warning');
      expect(cls).not.toContain('card--alert');
    });
  });

  describe('fallback summary null ou primaryValue vide', () => {
    it('affiche "Cliquer pour utiliser" quand summary est null', () => {
      component.summary = null;
      fixture.detectChanges();
      const placeholder = fixture.debugElement.query(By.css('.tool-card__placeholder'));
      expect(placeholder.nativeElement.textContent.trim()).toBe('Cliquer pour utiliser');
      expect(fixture.debugElement.query(By.css('.tool-card__verdict'))).toBeNull();
    });

    it('affiche "Cliquer pour utiliser" quand primaryValue est vide', () => {
      component.summary = { label: 'Verdict', primaryValue: '' };
      fixture.detectChanges();
      const placeholder = fixture.debugElement.query(By.css('.tool-card__placeholder'));
      expect(placeholder).not.toBeNull();
      expect(fixture.debugElement.query(By.css('.tool-card__verdict'))).toBeNull();
    });
  });

  describe('badges conditionnels', () => {
    it('badge ✨ visible si prefillCount > 0', () => {
      component.prefillCount = 3;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--prefill')).not.toBeNull();
    });

    it('badge ✨ absent si prefillCount === 0', () => {
      component.prefillCount = 0;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--prefill')).toBeNull();
    });

    it('badge ✨ absent si prefillCount === null', () => {
      component.prefillCount = null;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--prefill')).toBeNull();
    });

    it('badge 🔴 visible si coherenceAlertCount > 0', () => {
      component.coherenceAlertCount = 2;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--coherence')).not.toBeNull();
    });

    it('badge 🔴 absent si coherenceAlertCount === 0', () => {
      component.coherenceAlertCount = 0;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--coherence')).toBeNull();
    });

    it('badge ⚠️ visible si metierAlertLevel === WARNING', () => {
      component.metierAlertLevel = 'WARNING';
      fixture.detectChanges();
      expect(badge('.tool-card__badge--metier')).not.toBeNull();
    });

    it('badge ⚠️ visible si metierAlertLevel === ALERT', () => {
      component.metierAlertLevel = 'ALERT';
      fixture.detectChanges();
      expect(badge('.tool-card__badge--metier')).not.toBeNull();
    });

    it('badge ⚠️ absent si metierAlertLevel === OK', () => {
      component.metierAlertLevel = 'OK';
      fixture.detectChanges();
      expect(badge('.tool-card__badge--metier')).toBeNull();
    });

    it('badge ⚠️ absent si metierAlertLevel === null', () => {
      component.metierAlertLevel = null;
      fixture.detectChanges();
      expect(badge('.tool-card__badge--metier')).toBeNull();
    });
  });

  describe('SF-177-13 — visibilité pré-remplissage IA', () => {
    it('T-01: pill contient icône auto_awesome + compteur visible quand prefillCount > 0', () => {
      component.prefillCount = 5;
      fixture.detectChanges();
      const pill = badge('.tool-card__badge--prefill');
      expect(pill).not.toBeNull();
      const icon = pill!.querySelector('mat-icon');
      expect(icon).not.toBeNull();
      expect(icon!.textContent?.trim()).toBe('auto_awesome');
      const count = pill!.querySelector('.tool-card__badge-count');
      expect(count).not.toBeNull();
      expect(count!.textContent?.trim()).toBe('5');
    });

    it('T-02: card a la classe tool-card--prefilled quand prefillCount > 0', () => {
      component.prefillCount = 3;
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.tool-card');
      expect(card.classList.contains('tool-card--prefilled')).toBe(true);
    });

    it('T-03: card sans classe tool-card--prefilled quand prefillCount === 0', () => {
      component.prefillCount = 0;
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.tool-card');
      expect(card.classList.contains('tool-card--prefilled')).toBe(false);
    });

    it('T-04: card sans classe tool-card--prefilled quand prefillCount === null', () => {
      component.prefillCount = null;
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.tool-card');
      expect(card.classList.contains('tool-card--prefilled')).toBe(false);
    });

    it('T-05: aria-label inclut le compteur (pluriel correct)', () => {
      component.prefillCount = 4;
      fixture.detectChanges();
      const pill = badge('.tool-card__badge--prefill');
      expect(pill!.getAttribute('aria-label')).toBe("Pré-rempli par l'IA, 4 champs");
    });

    it('T-06: aria-label singulier quand 1 seul champ', () => {
      component.prefillCount = 1;
      fixture.detectChanges();
      const pill = badge('.tool-card__badge--prefill');
      expect(pill!.getAttribute('aria-label')).toBe("Pré-rempli par l'IA, 1 champ");
    });
  });

  describe('SF-159-02 — flashing input', () => {
    it('ajoute la classe tool-card--flashing quand flashing=true', () => {
      component.flashing = true;
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.tool-card');
      expect(card.classList.contains('tool-card--flashing')).toBe(true);
    });

    it('pas de classe tool-card--flashing quand flashing=false (default)', () => {
      component.flashing = false;
      fixture.detectChanges();
      const card = fixture.nativeElement.querySelector('.tool-card');
      expect(card.classList.contains('tool-card--flashing')).toBe(false);
    });
  });

  describe('interactions', () => {
    it('émet open au click', () => {
      let count = 0;
      component.open.subscribe(() => count++);
      fixture.detectChanges();
      rootEl().click();
      expect(count).toBe(1);
    });

    it('émet open au keydown Enter', () => {
      let count = 0;
      component.open.subscribe(() => count++);
      fixture.detectChanges();
      const event = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
      rootEl().dispatchEvent(event);
      expect(count).toBe(1);
    });

    it('émet open au keydown Space', () => {
      let count = 0;
      component.open.subscribe(() => count++);
      fixture.detectChanges();
      const event = new KeyboardEvent('keydown', { key: ' ', cancelable: true });
      rootEl().dispatchEvent(event);
      expect(count).toBe(1);
    });

    it("n'émet pas open au keydown autre touche", () => {
      let count = 0;
      component.open.subscribe(() => count++);
      fixture.detectChanges();
      rootEl().dispatchEvent(new KeyboardEvent('keydown', { key: 'a' }));
      expect(count).toBe(0);
    });

    it("n'émet pas open quand disabled au click", () => {
      let count = 0;
      component.open.subscribe(() => count++);
      component.disabled = true;
      fixture.detectChanges();
      rootEl().click();
      expect(count).toBe(0);
    });

    it("n'émet pas open quand disabled au keydown Enter", () => {
      let count = 0;
      component.open.subscribe(() => count++);
      component.disabled = true;
      fixture.detectChanges();
      rootEl().dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
      expect(count).toBe(0);
    });
  });

  describe('accessibilité', () => {
    it('expose role=button, tabindex=0, aria-label sur la card non-disabled', () => {
      fixture.detectChanges();
      const root = rootEl();
      expect(root.getAttribute('role')).toBe('button');
      expect(root.getAttribute('tabindex')).toBe('0');
      expect(root.getAttribute('aria-label')).toBe('TITRE DE SÉJOUR RECOMMANDÉ');
    });

    it('expose tabindex=-1 et aria-disabled=true quand disabled', () => {
      component.disabled = true;
      fixture.detectChanges();
      const root = rootEl();
      expect(root.getAttribute('tabindex')).toBe('-1');
      expect(root.getAttribute('aria-disabled')).toBe('true');
    });
  });

  describe('formatSummary util', () => {
    it('retourne null quand summary est null', () => {
      expect(formatSummary(null)).toBeNull();
    });

    it('retourne null quand primaryValue est vide', () => {
      expect(formatSummary({ label: 'X', primaryValue: '' })).toBeNull();
    });

    it('formate label + primaryValue sans secondaryValue', () => {
      expect(
        formatSummary({ label: 'Verdict', primaryValue: 'Conforme' } as DecisionToolSummary),
      ).toBe('Verdict : Conforme');
    });

    it('formate label + primaryValue + secondaryValue', () => {
      expect(
        formatSummary({
          label: 'Indemnité',
          primaryValue: '3 200 €',
          secondaryValue: '(brut)',
        } as DecisionToolSummary),
      ).toBe('Indemnité : 3 200 € (brut)');
    });
  });
});

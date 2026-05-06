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

  describe('F-192 SF-192-02 — pill or pistes retenues', () => {
    it('CA-07 aligned : pill or visible avec count quand kind=aligned', () => {
      component.retainedPistesBadge = { kind: 'aligned', count: 3 };
      fixture.detectChanges();
      const pill = badge('[data-testid="retained-pistes-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--retained-aligned')).toBe(true);
      const count = pill!.querySelector('.tool-card__badge-count');
      expect(count!.textContent?.trim()).toBe('3');
      const icon = pill!.querySelector('mat-icon');
      expect(icon!.textContent?.trim()).toBe('push_pin');
    });

    it('CA-07 divergent : pill or contour visible quand kind=divergent', () => {
      component.retainedPistesBadge = { kind: 'divergent', count: 2 };
      fixture.detectChanges();
      const pill = badge('[data-testid="retained-pistes-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--retained-divergent')).toBe(true);
    });

    it('CA-07 none : pill absent quand kind=none', () => {
      component.retainedPistesBadge = { kind: 'none', count: 0 };
      fixture.detectChanges();
      expect(badge('[data-testid="retained-pistes-badge"]')).toBeNull();
    });

    it('CA-07 null : pill absent quand input null', () => {
      component.retainedPistesBadge = null;
      fixture.detectChanges();
      expect(badge('[data-testid="retained-pistes-badge"]')).toBeNull();
    });

    it('CA-07 aria-label aligned reflète le count', () => {
      component.retainedPistesBadge = { kind: 'aligned', count: 4 };
      fixture.detectChanges();
      const pill = badge('[data-testid="retained-pistes-badge"]');
      expect(pill!.getAttribute('aria-label')).toBe('Aligné stratégie retenue (4)');
    });

    it('CA-07 aria-label divergent reflète le count', () => {
      component.retainedPistesBadge = { kind: 'divergent', count: 1 };
      fixture.detectChanges();
      const pill = badge('[data-testid="retained-pistes-badge"]');
      expect(pill!.getAttribute('aria-label')).toBe('Divergence stratégie retenue (1)');
    });
  });

  describe('F-193 SF-193-02 — pill « 🔍 Procédure (V/N/T) »', () => {
    it('CA-07 verified : pill or visible quand kind=verified avec compteurs V/N/T', () => {
      component.proceduresChecksBadge = {
        kind: 'verified',
        counts: { verified: 3, nonCompliant: 0, toVerify: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="procedures-checks-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--procedures-verified')).toBe(true);
      const count = pill!.querySelector('.tool-card__badge-count');
      expect(count!.textContent?.trim()).toBe('3/0/0');
      const icon = pill!.querySelector('mat-icon');
      expect(icon!.textContent?.trim()).toBe('verified');
    });

    it('CA-08 non_compliant : pill rouge subtil visible (priorité visuelle)', () => {
      component.proceduresChecksBadge = {
        kind: 'non_compliant',
        counts: { verified: 0, nonCompliant: 2, toVerify: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="procedures-checks-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--procedures-non-compliant')).toBe(true);
      expect(pill!.querySelector('.tool-card__badge-count')!.textContent?.trim()).toBe('0/2/0');
    });

    it('CA-07 mixed (1 ALIGNED + 1 NON_COMPLIANT) → pill rouge subtil (non_compliant > verified)', () => {
      component.proceduresChecksBadge = {
        kind: 'mixed',
        counts: { verified: 1, nonCompliant: 1, toVerify: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="procedures-checks-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--procedures-non-compliant')).toBe(true);
      expect(pill!.querySelector('.tool-card__badge-count')!.textContent?.trim()).toBe('1/1/0');
    });

    it('CA-07 none → pill absent quand kind=none', () => {
      component.proceduresChecksBadge = {
        kind: 'none',
        counts: { verified: 0, nonCompliant: 0, toVerify: 0 },
      };
      fixture.detectChanges();
      expect(badge('[data-testid="procedures-checks-badge"]')).toBeNull();
    });

    it('CA-07 null → pill absent (composant non instrumenté SF-193-02)', () => {
      component.proceduresChecksBadge = null;
      fixture.detectChanges();
      expect(badge('[data-testid="procedures-checks-badge"]')).toBeNull();
    });
  });

  describe('F-194 SF-194-02 — pill « 📎 Pièces (D/O/N) »', () => {
    it('missing : pill navy clair visible (priorité visuelle — il manque encore des pièces)', () => {
      component.piecesBadge = {
        kind: 'missing',
        counts: { aDemander: 2, obtenues: 1, nonApplicable: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="pieces-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--pieces-missing')).toBe(true);
      const count = pill!.querySelector('.tool-card__badge-count');
      expect(count!.textContent?.trim()).toBe('2/1/0');
      const icon = pill!.querySelector('mat-icon');
      expect(icon!.textContent?.trim()).toBe('attach_file');
    });

    it('obtained : pill or plein visible quand toutes pièces sont obtenues', () => {
      component.piecesBadge = {
        kind: 'obtained',
        counts: { aDemander: 0, obtenues: 3, nonApplicable: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="pieces-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--pieces-obtained')).toBe(true);
      expect(pill!.querySelector('.tool-card__badge-count')!.textContent?.trim()).toBe('0/3/0');
    });

    it('partial : pill or contour quand obtenues + non applicable cohabitent', () => {
      component.piecesBadge = {
        kind: 'partial',
        counts: { aDemander: 0, obtenues: 2, nonApplicable: 1 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="pieces-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--pieces-partial')).toBe(true);
    });

    it('not_applicable : pill gris quand toutes pièces sont écartées', () => {
      component.piecesBadge = {
        kind: 'not_applicable',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 2 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="pieces-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--pieces-not-applicable')).toBe(true);
    });

    it('none → pill absent', () => {
      component.piecesBadge = {
        kind: 'none',
        counts: { aDemander: 0, obtenues: 0, nonApplicable: 0 },
      };
      fixture.detectChanges();
      expect(badge('[data-testid="pieces-badge"]')).toBeNull();
    });

    it('null → pill absent (composant non instrumenté SF-194-02)', () => {
      component.piecesBadge = null;
      fixture.detectChanges();
      expect(badge('[data-testid="pieces-badge"]')).toBeNull();
    });

    it('aria-label reflète les compteurs', () => {
      component.piecesBadge = {
        kind: 'missing',
        counts: { aDemander: 3, obtenues: 1, nonApplicable: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="pieces-badge"]');
      expect(pill!.getAttribute('aria-label')).toBe(
        'Pièces : 3 à demander, 1 obtenue(s), 0 non applicable(s)',
      );
    });
  });

  describe('F-195 SF-195-02 — pill « ⚠️ Risques (V/E) »', () => {
    it('validated_critical : pill rouge subtil visible (priorité visuelle absolue)', () => {
      component.risquesBadge = {
        kind: 'validated_critical',
        counts: { aCreuser: 0, valides: 1, ecartes: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--risques-validated-critical')).toBe(true);
      const count = pill!.querySelector('.tool-card__badge-count');
      expect(count!.textContent?.trim()).toBe('1/0');
      const icon = pill!.querySelector('mat-icon');
      expect(icon!.textContent?.trim()).toBe('warning');
    });

    it('validated : pill or plein quand risque non critique validé', () => {
      component.risquesBadge = {
        kind: 'validated',
        counts: { aCreuser: 0, valides: 2, ecartes: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--risques-validated')).toBe(true);
      expect(pill!.querySelector('.tool-card__badge-count')!.textContent?.trim()).toBe('2/0');
    });

    it('mixed : pill or contour quand VALIDE + ECARTE non critique', () => {
      component.risquesBadge = {
        kind: 'mixed',
        counts: { aCreuser: 0, valides: 1, ecartes: 1 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--risques-mixed')).toBe(true);
      expect(pill!.querySelector('.tool-card__badge-count')!.textContent?.trim()).toBe('1/1');
    });

    it('discarded : pill gris quand uniquement ECARTE', () => {
      component.risquesBadge = {
        kind: 'discarded',
        counts: { aCreuser: 0, valides: 0, ecartes: 1 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--risques-discarded')).toBe(true);
    });

    it('to_explore : pill navy contour quand uniquement A_CREUSER', () => {
      component.risquesBadge = {
        kind: 'to_explore',
        counts: { aCreuser: 2, valides: 0, ecartes: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill).not.toBeNull();
      expect(pill!.classList.contains('tool-card__badge--risques-to-explore')).toBe(true);
    });

    it('none → pill absent', () => {
      component.risquesBadge = {
        kind: 'none',
        counts: { aCreuser: 0, valides: 0, ecartes: 0 },
      };
      fixture.detectChanges();
      expect(badge('[data-testid="risques-badge"]')).toBeNull();
    });

    it('null → pill absent (composant non instrumenté SF-195-02)', () => {
      component.risquesBadge = null;
      fixture.detectChanges();
      expect(badge('[data-testid="risques-badge"]')).toBeNull();
    });

    it('aria-label reflète les compteurs', () => {
      component.risquesBadge = {
        kind: 'validated',
        counts: { aCreuser: 1, valides: 2, ecartes: 0 },
      };
      fixture.detectChanges();
      const pill = badge('[data-testid="risques-badge"]');
      expect(pill!.getAttribute('aria-label')).toBe(
        'Risques : 2 validé(s), 0 écarté(s), 1 à creuser',
      );
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

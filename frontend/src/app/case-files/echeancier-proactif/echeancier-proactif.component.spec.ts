import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { EcheancierProactifComponent } from './echeancier-proactif.component';
import { EcheancierService } from '../../core/services/echeancier.service';
import { EcheancierItem, EcheancierResponse } from '../../core/models/echeancier.model';

let seq = 0;
function item(over: Partial<EcheancierItem>): EcheancierItem {
  return {
    id: over.id ?? `item-${++seq}`,
    label: over.label ?? 'Délai',
    dueDate: over.dueDate ?? '2026-12-01',
    daysUntil: over.daysUntil ?? 10,
    urgency: over.urgency ?? 'SOON',
    kind: over.kind ?? 'DEADLINE',
    source: over.source ?? 'MANUAL',
  };
}

function resp(items: EcheancierItem[]): EcheancierResponse {
  const count = (u: string) => items.filter((i) => i.urgency === u).length;
  return {
    items,
    nextItem: items[0] ?? null,
    counts: {
      overdue: count('OVERDUE'),
      critical: count('CRITICAL'),
      soon: count('SOON'),
      upcoming: count('UPCOMING'),
      total: items.length,
    },
  };
}

describe('EcheancierProactifComponent', () => {
  let fixture: ComponentFixture<EcheancierProactifComponent>;
  let component: EcheancierProactifComponent;
  let service: jasmine.SpyObj<EcheancierService>;

  function setup(data: EcheancierResponse | 'error'): void {
    service = jasmine.createSpyObj<EcheancierService>('EcheancierService', ['get']);
    service.get.and.returnValue(data === 'error' ? throwError(() => new Error('x')) : of(data));

    TestBed.configureTestingModule({
      imports: [EcheancierProactifComponent],
      providers: [{ provide: EcheancierService, useValue: service }],
    });
    fixture = TestBed.createComponent(EcheancierProactifComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
  }

  function el(testid: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-testid="${testid}"]`);
  }

  // AC1 : hero OVERDUE
  it('rend le hero avec la classe overdue et le libellé J+X (dépassé)', () => {
    setup(resp([item({ urgency: 'OVERDUE', daysUntil: -2, label: 'Prescription' })]));
    const hero = el('eche-hero');
    expect(hero).toBeTruthy();
    expect(hero!.classList).toContain('eche--overdue');
    expect(el('eche-hero-count')!.textContent).toContain('J+2 (dépassé)');
  });

  // AC2 : 5 items → hero + exactement 3 secondaires + lien
  it('limite à 3 échéances secondaires et propose le lien vers le détail', () => {
    const items = [
      item({ urgency: 'CRITICAL', daysUntil: 1 }),
      item({ urgency: 'SOON', daysUntil: 9 }),
      item({ urgency: 'SOON', daysUntil: 12 }),
      item({ urgency: 'UPCOMING', daysUntil: 20 }),
      item({ urgency: 'UPCOMING', daysUntil: 30 }),
    ];
    setup(resp(items));
    expect(component.secondary().length).toBe(3);
    expect(component.hiddenCount()).toBe(1);
    expect(el('eche-view-all')!.textContent).toContain('+1');
  });

  // AC3 : état vide
  it('rend l’état vide quand aucune échéance', () => {
    setup(resp([]));
    expect(el('eche-empty')).toBeTruthy();
    expect(el('eche-hero')).toBeFalsy();
    expect(component.isEmpty()).toBe(true);
  });

  // AC4 : sous contrôle (pas d'alarme)
  it('affiche la pilule « sous contrôle » et pas d’alarme quand rien d’urgent', () => {
    setup(resp([item({ urgency: 'UPCOMING', daysUntil: 40 })]));
    expect(component.underControl()).toBe(true);
    expect(el('eche-calm-pill')).toBeTruthy();
    expect(el('eche-alert-pill')).toBeFalsy();
  });

  it('affiche la pilule d’alerte quand un délai est en retard', () => {
    setup(resp([item({ urgency: 'OVERDUE', daysUntil: -1 })]));
    expect(el('eche-alert-pill')).toBeTruthy();
    expect(el('eche-calm-pill')).toBeFalsy();
  });

  it('masque la carte en cas d’échec de chargement', () => {
    setup('error');
    expect(component.failed()).toBe(true);
    expect(el('echeancier-proactif')).toBeFalsy();
  });

  it('émet viewAllRequested au clic sur le lien', () => {
    setup(resp([item({}), item({}), item({}), item({}), item({})]));
    const spy = jasmine.createSpy('viewAll');
    component.viewAllRequested.subscribe(spy);
    el('eche-view-all')!.click();
    expect(spy).toHaveBeenCalled();
  });

  it('mappe correctement les libellés de jours', () => {
    setup(resp([item({})]));
    expect(component.daysLabel(0)).toBe("Aujourd'hui");
    expect(component.daysLabel(5)).toBe('J-5');
    expect(component.daysLabel(-3)).toBe('J+3 (dépassé)');
  });
});

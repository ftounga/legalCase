import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {
  ConclusionCompositionDialogComponent,
  ConclusionCompositionDialogData,
  ConclusionCompositionDialogResult,
} from './conclusion-composition-dialog.component';
import { Composition } from '../../../core/services/conclusion-composition.service';

describe('ConclusionCompositionDialogComponent', () => {
  let fixture: ComponentFixture<ConclusionCompositionDialogComponent>;
  let component: ConclusionCompositionDialogComponent;
  let dialogRefSpy: jasmine.SpyObj<
    MatDialogRef<
      ConclusionCompositionDialogComponent,
      ConclusionCompositionDialogResult | undefined
    >
  >;

  function composition(): Composition {
    return {
      dimensions: [
        {
          key: 'DECISION_TOOL',
          label: 'Outils décisionnels',
          items: [
            { key: 'tool-a', label: 'Indemnité de licenciement', included: true },
            { key: 'tool-b', label: 'Préavis', included: false },
            { key: 'tool-c', label: 'Heures supplémentaires', included: true },
          ],
        },
      ],
    };
  }

  async function setup(data: ConclusionCompositionDialogData): Promise<void> {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [ConclusionCompositionDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ConclusionCompositionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('pré-coche les items selon `included` (décoché si exclu)', async () => {
    await setup({ composition: composition() });
    const items = component.dimensions()[0].items;
    expect(items.find((i) => i.key === 'tool-a')?.checked).toBe(true);
    expect(items.find((i) => i.key === 'tool-b')?.checked).toBe(false);
    expect(items.find((i) => i.key === 'tool-c')?.checked).toBe(true);
  });

  it('confirm() renvoie les itemKey décochés (exclusions)', async () => {
    await setup({ composition: composition() });
    // tool-b est déjà décoché ; on décoche aussi tool-c.
    component.toggle('DECISION_TOOL', 'tool-c', false);
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      exclusions: [
        { dimension: 'DECISION_TOOL', itemKey: 'tool-b' },
        { dimension: 'DECISION_TOOL', itemKey: 'tool-c' },
      ],
    });
  });

  it('cancel() ferme sans exclusions (undefined)', async () => {
    await setup({ composition: composition() });
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(undefined);
  });

  it('checkAll / uncheckAll basculent toute la dimension', async () => {
    await setup({ composition: composition() });
    component.uncheckAll('DECISION_TOOL');
    expect(component.dimensions()[0].items.every((i) => !i.checked)).toBe(true);
    expect(component.allUnchecked()).toBe(true);
    component.checkAll('DECISION_TOOL');
    expect(component.dimensions()[0].items.every((i) => i.checked)).toBe(true);
    expect(component.allUnchecked()).toBe(false);
  });

  it('allUnchecked → avertissement affiché dans le DOM', async () => {
    await setup({ composition: composition() });
    component.uncheckAll('DECISION_TOOL');
    fixture.detectChanges();
    const warning = fixture.nativeElement.querySelector(
      '[data-testid="composition-all-unchecked-warning"]',
    );
    expect(warning).not.toBeNull();
    // Le bouton « Confirmer & générer » reste actif (jamais désactivé).
    const confirmBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      '[data-testid="composition-confirm-btn"]',
    );
    expect(confirmBtn.disabled).toBe(false);
  });
});

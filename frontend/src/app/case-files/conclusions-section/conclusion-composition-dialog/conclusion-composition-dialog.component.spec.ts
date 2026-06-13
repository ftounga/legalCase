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

  /**
   * F-288 / SF-288-03 — Composition à 2 dimensions : outils décisionnels
   * (vague 1) + moyens adverses (`ADVERSE_MOYEN`, items = moyens persistés).
   */
  function compositionWithMoyens(): Composition {
    return {
      dimensions: [
        {
          key: 'DECISION_TOOL',
          label: 'Outils décisionnels',
          items: [
            { key: 'tool-a', label: 'Indemnité de licenciement', included: true },
            { key: 'tool-b', label: 'Préavis', included: false },
          ],
        },
        {
          key: 'ADVERSE_MOYEN',
          label: 'Moyens adverses',
          items: [
            { key: 'moyen-1', label: 'Faute grave caractérisée…', included: true },
            { key: 'moyen-2', label: 'Procédure régulière…', included: true },
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

  // ---------------------------------------------------------------------------
  // F-288 / SF-288-03 — dimension « Moyens adverses » (ADVERSE_MOYEN)
  // ---------------------------------------------------------------------------

  it('SF-288-03 — rend 2 sections quand la composition a 2 dimensions (outils + moyens) (C7)', async () => {
    await setup({ composition: compositionWithMoyens() });
    fixture.detectChanges();

    // Le composant porte bien les 2 dimensions.
    const keys = component.dimensions().map((d) => d.key);
    expect(keys).toEqual(['DECISION_TOOL', 'ADVERSE_MOYEN']);

    // Les 2 sections sont rendues dans le DOM.
    const toolsSection = fixture.nativeElement.querySelector(
      '[data-testid="composition-dimension-DECISION_TOOL"]',
    );
    const moyensSection = fixture.nativeElement.querySelector(
      '[data-testid="composition-dimension-ADVERSE_MOYEN"]',
    );
    expect(toolsSection).not.toBeNull();
    expect(moyensSection).not.toBeNull();
    expect(moyensSection.textContent).toContain('Moyens adverses');

    // Les items de chaque dimension sont rendus.
    expect(
      fixture.nativeElement.querySelector('[data-testid="composition-item-moyen-1"]'),
    ).not.toBeNull();
    expect(
      fixture.nativeElement.querySelector('[data-testid="composition-item-moyen-2"]'),
    ).not.toBeNull();
  });

  it('SF-288-03 — décocher un moyen → exclusion ADVERSE_MOYEN dans le résultat', async () => {
    await setup({ composition: compositionWithMoyens() });
    // L'avocat ne veut pas réfuter moyen-2.
    component.toggle('ADVERSE_MOYEN', 'moyen-2', false);
    component.confirm();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      exclusions: [
        { dimension: 'DECISION_TOOL', itemKey: 'tool-b' },
        { dimension: 'ADVERSE_MOYEN', itemKey: 'moyen-2' },
      ],
    });
  });

  it('SF-288-03 — la vague 1 (outils) n’est pas régressée avec 2 dimensions', async () => {
    await setup({ composition: compositionWithMoyens() });
    // On décoche un outil ; les moyens restent tous cochés.
    component.toggle('DECISION_TOOL', 'tool-a', false);
    component.confirm();

    expect(dialogRefSpy.close).toHaveBeenCalledWith({
      exclusions: [
        { dimension: 'DECISION_TOOL', itemKey: 'tool-a' },
        { dimension: 'DECISION_TOOL', itemKey: 'tool-b' },
      ],
    });
  });

  it('SF-288-03 — checkAll/uncheckAll sont isolés par dimension', async () => {
    await setup({ composition: compositionWithMoyens() });
    // Tout décocher la dimension des moyens ne touche pas les outils.
    component.uncheckAll('ADVERSE_MOYEN');
    const tools = component.dimensions().find((d) => d.key === 'DECISION_TOOL');
    const moyens = component.dimensions().find((d) => d.key === 'ADVERSE_MOYEN');
    expect(moyens?.items.every((i) => !i.checked)).toBe(true);
    // Les outils gardent leur état initial (tool-a coché, tool-b décoché).
    expect(tools?.items.find((i) => i.key === 'tool-a')?.checked).toBe(true);
    expect(tools?.items.find((i) => i.key === 'tool-b')?.checked).toBe(false);
    // allUnchecked reste faux tant qu'un outil est coché.
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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { ToolJurisprudenceCitationsComponent } from './tool-jurisprudence-citations.component';
import { ToolJurisprudenceClientService } from '../../core/services/tool-jurisprudence-client.service';
import { ToolJurisprudenceCitation } from '../../core/models/tool-jurisprudence-citation.model';

describe('ToolJurisprudenceCitationsComponent', () => {
  let component: ToolJurisprudenceCitationsComponent;
  let fixture: ComponentFixture<ToolJurisprudenceCitationsComponent>;
  let clientService: jest.Mocked<ToolJurisprudenceClientService>;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    clientService = {
      findByToolAndBranch: jest.fn().mockReturnValue(of([])),
      signalProblem: jest.fn().mockReturnValue(of(undefined)),
    } as any;
    snackBar = { open: jest.fn() } as any;

    await TestBed.configureTestingModule({
      imports: [ToolJurisprudenceCitationsComponent, NoopAnimationsModule],
      providers: [
        { provide: ToolJurisprudenceClientService, useValue: clientService },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ToolJurisprudenceCitationsComponent);
    component = fixture.componentInstance;
  });

  function buildCitation(overrides: Partial<ToolJurisprudenceCitation> = {}): ToolJurisprudenceCitation {
    return {
      id: '00000000-0000-0000-0000-000000000001',
      arretRef: 'Cass. soc. 8 janv. 2025, n° 23-12.345',
      juridiction: 'Cour de cassation, chambre sociale',
      dateArret: '2025-01-08',
      numeroPourvoi: '23-12.345',
      lienLegifrance: 'https://www.legifrance.gouv.fr/juri/id/X',
      chapeauOfficiel: 'Chapeau test.',
      lastVerifiedAt: '2026-05-01T03:00:00Z',
      confidenceScore: 0.92,
      ...overrides,
    };
  }

  it('T-01 — empty list → block absent from DOM', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([]));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    const block = fixture.nativeElement.querySelector('[data-test="juris-citations"]');
    expect(block).toBeFalsy();
  });

  it('T-02 — 3 arrets → 3 cards rendered', () => {
    const list = [
      buildCitation({ id: '1' }),
      buildCitation({ id: '2' }),
      buildCitation({ id: '3' }),
    ];
    clientService.findByToolAndBranch.mockReturnValue(of(list));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    const cards = fixture.nativeElement.querySelectorAll('.juris-citation');
    expect(cards.length).toBe(3);
  });

  it('T-03 — disclaimer affiche la date la plus récente', () => {
    const list = [
      buildCitation({ id: '1', lastVerifiedAt: '2026-04-01T00:00:00Z' }),
      buildCitation({ id: '2', lastVerifiedAt: '2026-05-15T00:00:00Z' }),
      buildCitation({ id: '3', lastVerifiedAt: '2025-12-01T00:00:00Z' }),
    ];
    clientService.findByToolAndBranch.mockReturnValue(of(list));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    const disclaimer = fixture.nativeElement.querySelector('[data-test="juris-disclaimer"]');
    expect(disclaimer.textContent).toContain('15/05/2026');
  });

  it('T-04 — lien Légifrance ouvre nouvel onglet', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([buildCitation()]));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('[data-test="juris-legifrance"]') as HTMLAnchorElement;
    expect(link.target).toBe('_blank');
    expect(link.rel).toContain('noopener');
    expect(link.rel).toContain('noreferrer');
  });

  it('T-05 — clic Signaler ouvre prompt inline', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([buildCitation({ id: 'abc' })]));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    const btn = fixture.nativeElement.querySelector('[data-test="juris-signal-btn"]') as HTMLButtonElement;
    btn.click();
    fixture.detectChanges();

    const form = fixture.nativeElement.querySelector('[data-test="juris-signal-form"]');
    expect(form).toBeTruthy();
  });

  it('T-06 — submitSignal appelle le service et affiche snackbar de confirmation', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([buildCitation({ id: 'abc' })]));
    clientService.signalProblem.mockReturnValue(of(undefined));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    component['openSignal']('abc');
    component['signalComment'] = 'arrêt obsolète';
    component['submitSignal']();

    expect(clientService.signalProblem).toHaveBeenCalledWith('f-dt-30', 'abc', 'arrêt obsolète');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Signalement transmis. Merci.', 'OK', expect.anything());
  });

  it('T-07 — submitSignal échec → snackbar erreur', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([buildCitation({ id: 'abc' })]));
    clientService.signalProblem.mockReturnValue(throwError(() => new Error('fail')));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-x';
    fixture.detectChanges();

    component['openSignal']('abc');
    component['submitSignal']();

    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Échec'), 'OK', expect.anything());
  });

  it('T-08 — ngOnChanges sur branchActive → re-fetch', () => {
    clientService.findByToolAndBranch.mockReturnValue(of([]));
    component.toolId = 'f-dt-30';
    component.branchActive = 'branche-1';
    fixture.detectChanges();
    expect(clientService.findByToolAndBranch).toHaveBeenCalledWith('f-dt-30', 'branche-1');

    component.branchActive = 'branche-2';
    component.ngOnChanges({ branchActive: { previousValue: 'branche-1', currentValue: 'branche-2', firstChange: false, isFirstChange: () => false } });

    expect(clientService.findByToolAndBranch).toHaveBeenLastCalledWith('f-dt-30', 'branche-2');
  });
});

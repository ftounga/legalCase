import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { NaturalisationRecoursTaSectionComponent } from './naturalisation-recours-ta-section.component';
import { NaturalisationRecoursTaResponse } from '../../core/models/naturalisation-recours-ta.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('NaturalisationRecoursTaSectionComponent', () => {
  let component: NaturalisationRecoursTaSectionComponent;
  let fixture: ComponentFixture<NaturalisationRecoursTaSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-recours-ta-analysis';

  function frResponse(overrides: Partial<NaturalisationRecoursTaResponse> = {}): NaturalisationRecoursTaResponse {
    return {
      caseFileId: 'case-1',
      dateRefusDecret: '2026-01-15',
      motivationRefus: 'Défaut d\'assimilation',
      recoursPrerequis: true,
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      dateEcheanceRecoursTa: '2026-03-15',
      joursRestants: 45,
      tribunalCompetent: 'Tribunal administratif de Nantes',
      basesJuridiques: ['CJA art. R. 421-1', 'Décret naturalisation'],
      motifsRecoursDisponibles: ["Erreur manifeste d'appréciation", 'Vice de procédure'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateRefusDecret.set('2026-01-15');
    component.motivationRefus.set('Défaut d\'assimilation');
    component.recoursPrerequis.set(true);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Recours TA Nantes naturalisation', dueDate: '2026-03-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [NaturalisationRecoursTaSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NaturalisationRecoursTaSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(NaturalisationRecoursTaSectionComponent.TOOL_LABEL).toContain('NANTES');
    expect(NaturalisationRecoursTaSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(NaturalisationRecoursTaSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when date present (FRANCE)', () => {
    expect(NaturalisationRecoursTaSectionComponent.getPrefillCount({
      aiData: { naturalisationDateRefus: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(NaturalisationRecoursTaSectionComponent.getPrefillCount({
      aiData: { naturalisationDateRefus: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'URGENT', recoursPrerequis: false }));
    expect(component.result()!.statut).toBe('URGENT');
    expect(component.showForm()).toBe(false);
    expect(component.dateRefusDecret()).toBe('2026-01-15');
    expect(component.recoursPrerequis()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires dateRefusDecret only', () => {
    expect(component.formValid()).toBe(false);
    component.dateRefusDecret.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateRefusDecret: '2026-01-15',
      motivationRefus: 'Défaut d\'assimilation',
      recoursPrerequis: true,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  // --- chip PRESCRIT (rouge) + TA Nantes en gras ---

  it('PRESCRIT -> danger chip rendered + negative jours danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRESCRIT', joursRestants: -30 }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.nrt-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('prescrit');
    const joursBig = fixture.nativeElement.querySelector('.nrt-jours-big--danger');
    expect(joursBig).not.toBeNull();
    expect(joursBig.textContent.trim()).toBe('-30');
  });

  it('renders tribunalCompetent (TA Nantes) in the bold/tribunal value', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const tribunal = fixture.nativeElement.querySelector('.nrt-bd-value--tribunal');
    expect(tribunal).not.toBeNull();
    expect(tribunal.textContent).toContain('Nantes');
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: RECOURS_POSSIBLE -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE', dateEcheanceRecoursTa: '2026-03-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TA Nantes naturalisation', '2026-03-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'URGENT', dateEcheanceRecoursTa: '2026-02-10', joursRestants: 12 }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TA Nantes naturalisation', '2026-02-10');
  });

  it('bridge F-69: PRESCRIT -> no deadline created', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRESCRIT', joursRestants: -10 }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('RECOURS_POSSIBLE');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with date -> pre-fills dateRefusDecret + provenance IA', () => {
    component.aiData = { naturalisationDateRefus: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateRefusDecret()).toBe('2026-02-20');
    expect(component.provenanceDateRefus()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { naturalisationDateRefus: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateRefusDecret: '2026-01-15' }));
    expect(component.dateRefusDecret()).toBe('2026-01-15');
    expect(component.provenanceDateRefus()).toBeNull();
  });

  it('onDateRefusChange clears provenance', () => {
    component.aiData = { naturalisationDateRefus: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateRefus()).toBe('IA');
    component.onDateRefusChange('2026-03-01');
    expect(component.provenanceDateRefus()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts (rouge URGENT + PRESCRIT)', () => {
    expect(component.bannerClass('RECOURS_POSSIBLE')).toContain('nrt-banner--success');
    expect(component.bannerClass('URGENT')).toContain('nrt-banner--warning');
    expect(component.bannerClass('PRESCRIT')).toContain('nrt-banner--danger');
    expect(component.bannerIcon('RECOURS_POSSIBLE')).toBe('check_circle');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('PRESCRIT')).toBe('error');
    expect(component.statutLabel('PRESCRIT')).toContain('prescrit');
    expect(component.statutLabel('RECOURS_POSSIBLE')).toContain('possible');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.dateRefusDecret()).toBeNull();
    component.aiData = { naturalisationDateRefus: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateRefusDecret()).toBe('2026-05-01');
    expect(component.provenanceDateRefus()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.nrt-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });

  it('renders motifsRecoursDisponibles list in result', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.nrt-motifs-list li');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain("Erreur manifeste d'appréciation");
  });
});

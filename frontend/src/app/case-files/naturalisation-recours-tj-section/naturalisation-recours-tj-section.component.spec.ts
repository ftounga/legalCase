import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { NaturalisationRecoursTjSectionComponent } from './naturalisation-recours-tj-section.component';
import { NaturalisationRecoursTjResponse } from '../../core/models/naturalisation-recours-tj.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('NaturalisationRecoursTjSectionComponent', () => {
  let component: NaturalisationRecoursTjSectionComponent;
  let fixture: ComponentFixture<NaturalisationRecoursTjSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/naturalisation-recours-tj-analysis';

  function frResponse(overrides: Partial<NaturalisationRecoursTjResponse> = {}): NaturalisationRecoursTjResponse {
    return {
      caseFileId: 'case-1',
      voieNaturalisation: 'MARIAGE',
      dateRefusDeclaration: '2026-01-15',
      typeRefus: 'REFUS_ENREGISTREMENT',
      country: 'FRANCE',
      statut: 'RECOURS_POSSIBLE',
      dateEcheanceRecoursJudicaire: '2026-07-15',
      joursRestants: 120,
      tribunalCompetent: 'Tribunal judiciaire de Paris',
      basesJuridiques: ['C. civ. art. 26-3', 'C. civ. art. 26-4'],
      motifsRecoursDisponibles: ["Erreur d'appréciation", 'Vice de procédure'],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.voieNaturalisation.set('MARIAGE');
    component.dateRefusDeclaration.set('2026-01-15');
    component.typeRefus.set('REFUS_ENREGISTREMENT');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Recours TJ naturalisation', dueDate: '2026-07-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [NaturalisationRecoursTjSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NaturalisationRecoursTjSectionComponent);
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
    expect(NaturalisationRecoursTjSectionComponent.TOOL_LABEL).toContain('NATURALISATION');
    expect(NaturalisationRecoursTjSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(NaturalisationRecoursTjSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 when voie + date present (FRANCE)', () => {
    expect(NaturalisationRecoursTjSectionComponent.getPrefillCount({
      aiData: { naturalisationVoie: 'MARIAGE', naturalisationDateRefus: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(NaturalisationRecoursTjSectionComponent.getPrefillCount({
      aiData: { naturalisationVoie: 'MARIAGE', naturalisationDateRefus: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'URGENT', voieNaturalisation: 'ASCENDANT', typeRefus: 'CONTESTATION_NATIONALITE' }));
    expect(component.result()!.statut).toBe('URGENT');
    expect(component.showForm()).toBe(false);
    expect(component.voieNaturalisation()).toBe('ASCENDANT');
    expect(component.dateRefusDeclaration()).toBe('2026-01-15');
    expect(component.typeRefus()).toBe('CONTESTATION_NATIONALITE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires voie + date + typeRefus', () => {
    expect(component.formValid()).toBe(false);
    component.voieNaturalisation.set('MARIAGE');
    expect(component.formValid()).toBe(false);
    component.dateRefusDeclaration.set('2026-01-15');
    expect(component.formValid()).toBe(false);
    component.typeRefus.set('REFUS_ENREGISTREMENT');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      voieNaturalisation: 'MARIAGE',
      dateRefusDeclaration: '2026-01-15',
      typeRefus: 'REFUS_ENREGISTREMENT',
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

  // --- chip PRESCRIT (rouge) ---

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

  // --- Bridge échéance F-69 ---

  it('bridge F-69: RECOURS_POSSIBLE -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_POSSIBLE', dateEcheanceRecoursJudicaire: '2026-07-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TJ naturalisation', '2026-07-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'URGENT', dateEcheanceRecoursJudicaire: '2026-06-01', joursRestants: 15 }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours TJ naturalisation', '2026-06-01');
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

  it('aiData with voie + date -> pre-fills + provenance IA', () => {
    component.aiData = { naturalisationVoie: 'ASCENDANT', naturalisationDateRefus: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.voieNaturalisation()).toBe('ASCENDANT');
    expect(component.dateRefusDeclaration()).toBe('2026-02-20');
    expect(component.provenanceVoie()).toBe('IA');
    expect(component.provenanceDateRefus()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { naturalisationVoie: 'ASCENDANT', naturalisationDateRefus: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ voieNaturalisation: 'MARIAGE', dateRefusDeclaration: '2026-01-15' }));
    expect(component.voieNaturalisation()).toBe('MARIAGE');
    expect(component.dateRefusDeclaration()).toBe('2026-01-15');
    expect(component.provenanceVoie()).toBeNull();
  });

  it('onDateRefusChange / onVoieChange clear provenance', () => {
    component.aiData = { naturalisationVoie: 'MARIAGE', naturalisationDateRefus: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceVoie()).toBe('IA');
    component.onVoieChange('ASCENDANT');
    expect(component.provenanceVoie()).toBeNull();
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

  it('voieLabel / typeRefusLabel map codes to FR labels', () => {
    expect(component.voieLabel('MARIAGE')).toContain('mariage');
    expect(component.voieLabel('ASCENDANT')).toContain('ascendant');
    expect(component.voieLabel('MINEUR_22_1')).toContain('mineur');
    expect(component.voieLabel(null)).toBe('');
    expect(component.typeRefusLabel('REFUS_ENREGISTREMENT')).toContain("enregistrement");
    expect(component.typeRefusLabel('CONTESTATION_NATIONALITE')).toContain('Contestation');
    expect(component.typeRefusLabel(null)).toBe('');
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
    expect(component.voieNaturalisation()).toBeNull();
    component.aiData = { naturalisationVoie: 'MINEUR_22_1', naturalisationDateRefus: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.voieNaturalisation()).toBe('MINEUR_22_1');
    expect(component.dateRefusDeclaration()).toBe('2026-05-01');
    expect(component.provenanceVoie()).toBe('IA');
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
    expect(items[0].textContent).toContain("Erreur d'appréciation");
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { ItfJudiciaireSectionComponent } from './itf-judiciaire-section.component';
import { ItfJudiciaireResponse } from '../../core/models/itf-judiciaire.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('ItfJudiciaireSectionComponent', () => {
  let component: ItfJudiciaireSectionComponent;
  let fixture: ComponentFixture<ItfJudiciaireSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/itf-judiciaire-analysis';

  function frResponse(overrides: Partial<ItfJudiciaireResponse> = {}): ItfJudiciaireResponse {
    return {
      caseFileId: 'case-1',
      dateCondamnation: '2026-01-15',
      dureeITFAnnees: 5,
      infractionPrincipale: 'Trafic de stupéfiants',
      condamnationDefinitive: false,
      dateEcheanceRecoursPenal: '2026-01-25',
      country: 'FRANCE',
      statut: 'APPEL_POSSIBLE',
      dateEcheanceReleve: '2027-01-15',
      voiesRecours: ['Appel : 10 jours à compter du prononcé', 'Pourvoi en cassation : 5 jours'],
      requisReleve: ['Résider hors de France', 'Justifier de garanties de réinsertion'],
      distinctionItfVsIrtf: "L'ITF est une peine pénale (juge) ; l'IRTF est une mesure administrative (préfet).",
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateCondamnation.set('2026-01-15');
    component.dureeITFAnnees.set(5);
    component.infractionPrincipale.set('Trafic de stupéfiants');
    component.condamnationDefinitive.set(false);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Recours pénal ITF', dueDate: '2026-01-25' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [ItfJudiciaireSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ItfJudiciaireSectionComponent);
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
    expect(ItfJudiciaireSectionComponent.TOOL_LABEL).toContain('ITF');
    expect(ItfJudiciaireSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ItfJudiciaireSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 when date + duree present (FRANCE)', () => {
    expect(ItfJudiciaireSectionComponent.getPrefillCount({
      aiData: { itfJudiciaireDateCondamnation: '2026-01-15', itfJudiciaireDureeAnnees: 5 },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ItfJudiciaireSectionComponent.getPrefillCount({
      aiData: { itfJudiciaireDateCondamnation: '2026-01-15', itfJudiciaireDureeAnnees: 5 },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'RELEVE_POSSIBLE', dureeITFAnnees: 10, condamnationDefinitive: true }));
    expect(component.result()!.statut).toBe('RELEVE_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dateCondamnation()).toBe('2026-01-15');
    expect(component.dureeITFAnnees()).toBe(10);
    expect(component.condamnationDefinitive()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires date + positive duree + non-empty infraction', () => {
    component.dateCondamnation.set(null);
    component.dureeITFAnnees.set(5);
    component.infractionPrincipale.set('X');
    expect(component.formValid()).toBe(false);
    component.dateCondamnation.set('2026-01-15');
    component.dureeITFAnnees.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeITFAnnees.set(5);
    component.infractionPrincipale.set('   ');
    expect(component.formValid()).toBe(false);
    component.infractionPrincipale.set('Trafic');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.dateEcheanceRecoursPenal.set('2026-01-25');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateCondamnation: '2026-01-15',
      dureeITFAnnees: 5,
      infractionPrincipale: 'Trafic de stupéfiants',
      condamnationDefinitive: false,
      dateEcheanceRecoursPenal: '2026-01-25',
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('APPEL_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateCondamnation.set(null);
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

  // --- chips + result rendering ---

  it('APPEL_POSSIBLE -> success chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--success');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('Appel possible');
  });

  it('RECOURS_PRESCRIT -> danger chip + echeance danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RECOURS_PRESCRIT' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('prescrit');
    const big = fixture.nativeElement.querySelector('.acc-jours-big--danger');
    expect(big).not.toBeNull();
  });

  it('renders voiesRecours list and requisReleve list', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const voies = fixture.nativeElement.querySelectorAll('[data-testid="voies-recours-block"] li');
    expect(voies.length).toBe(2);
    expect(voies[0].textContent).toContain('Appel');
    const requis = fixture.nativeElement.querySelectorAll('[data-testid="requis-releve-block"] li');
    expect(requis.length).toBe(2);
  });

  it('renders the distinctionItfVsIrtf info box', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const box = fixture.nativeElement.querySelector('[data-testid="distinction-itf-irtf"]');
    expect(box).not.toBeNull();
    expect(box.textContent).toContain('IRTF');
    expect(box.textContent).toContain('administrative');
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: APPEL_POSSIBLE -> creates deadline with label + recours date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.dateEcheanceRecoursPenal.set('2026-01-25');
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'APPEL_POSSIBLE' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours pénal ITF', '2026-01-25');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: APPEL_POSSIBLE without recours date -> falls back to dateEcheanceReleve', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.dateEcheanceRecoursPenal.set(null);
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'APPEL_POSSIBLE', dateEcheanceReleve: '2027-01-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Recours pénal ITF', '2027-01-15');
  });

  it('bridge F-69: RELEVE_POSSIBLE -> no deadline (APPEL_POSSIBLE only)', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'RELEVE_POSSIBLE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'APPEL_POSSIBLE' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.statut).toBe('APPEL_POSSIBLE');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'APPEL_POSSIBLE' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill / labels ---

  it('aiData with date + duree -> pre-fills both + provenance IA', () => {
    component.aiData = { itfJudiciaireDateCondamnation: '2026-02-20', itfJudiciaireDureeAnnees: 8 } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateCondamnation()).toBe('2026-02-20');
    expect(component.dureeITFAnnees()).toBe(8);
    expect(component.provenanceDateCondamnation()).toBe('IA');
    expect(component.provenanceDuree()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { itfJudiciaireDateCondamnation: '2099-01-01', itfJudiciaireDureeAnnees: 99 } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateCondamnation: '2026-01-15', dureeITFAnnees: 5 }));
    expect(component.dateCondamnation()).toBe('2026-01-15');
    expect(component.dureeITFAnnees()).toBe(5);
    expect(component.provenanceDateCondamnation()).toBeNull();
  });

  it('onDateCondamnationChange clears provenance', () => {
    component.aiData = { itfJudiciaireDateCondamnation: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateCondamnation()).toBe('IA');
    component.onDateCondamnationChange('2026-03-01');
    expect(component.provenanceDateCondamnation()).toBeNull();
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('APPEL_POSSIBLE')).toContain('acc-banner--success');
    expect(component.bannerClass('RELEVE_POSSIBLE')).toContain('acc-banner--info');
    expect(component.bannerClass('EN_COURS_PURGE')).toContain('acc-banner--warning');
    expect(component.bannerClass('RECOURS_PRESCRIT')).toContain('acc-banner--danger');
    expect(component.bannerIcon('APPEL_POSSIBLE')).toBe('check_circle');
    expect(component.bannerIcon('RECOURS_PRESCRIT')).toBe('error');
    expect(component.statutLabel('APPEL_POSSIBLE')).toContain('Appel');
    expect(component.statutLabel('RELEVE_POSSIBLE')).toContain('Relevé');
    expect(component.statutLabel('EN_COURS_PURGE')).toContain('exécution');
    expect(component.statutLabel('RECOURS_PRESCRIT')).toContain('prescrit');
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
    expect(component.dateCondamnation()).toBeNull();
    component.aiData = { itfJudiciaireDateCondamnation: '2026-05-01', itfJudiciaireDureeAnnees: 3 } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateCondamnation()).toBe('2026-05-01');
    expect(component.dureeITFAnnees()).toBe(3);
    expect(component.provenanceDateCondamnation()).toBe('IA');
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.acc-banner--info');
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
});

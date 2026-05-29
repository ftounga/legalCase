import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AppelCaaCassationSectionComponent } from './appel-caa-cassation-section.component';
import { AppelCaaCassationResponse } from '../../core/models/appel-caa-cassation.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('AppelCaaCassationSectionComponent', () => {
  let component: AppelCaaCassationSectionComponent;
  let fixture: ComponentFixture<AppelCaaCassationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/appel-caa-cassation-analysis';

  function frResponse(overrides: Partial<AppelCaaCassationResponse> = {}): AppelCaaCassationResponse {
    return {
      caseFileId: 'case-1',
      dateJugementTA: '2026-01-15',
      typeDecisionTA: 'REJET',
      typeContentieux: 'OQTF',
      delaiSpecialOQTF: true,
      country: 'FRANCE',
      statut: 'APPEL_POSSIBLE',
      dateEcheanceAppelCaa: '2026-01-30',
      joursRestants: 12,
      courAppelCompetente: "Cour administrative d'appel de Paris",
      motifsAppelPossibles: ["Erreur de droit", "Erreur d'appréciation"],
      filtrePourvoisCassation: true,
      delaiCassationCeMois: 2,
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateJugementTA.set('2026-01-15');
    component.typeDecisionTA.set('REJET');
    component.typeContentieux.set('OQTF');
    component.delaiSpecialOQTF.set(true);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: 'Appel CAA contentieux étrangers', dueDate: '2026-01-30' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [AppelCaaCassationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppelCaaCassationSectionComponent);
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
    expect(AppelCaaCassationSectionComponent.TOOL_LABEL).toContain('APPEL CAA');
    expect(AppelCaaCassationSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AppelCaaCassationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when date present (FRANCE)', () => {
    expect(AppelCaaCassationSectionComponent.getPrefillCount({
      aiData: { recoursDateJugementTA: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AppelCaaCassationSectionComponent.getPrefillCount({
      aiData: { recoursDateJugementTA: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({ statut: 'URGENT', delaiSpecialOQTF: false, typeContentieux: 'REFUS_TITRE' }));
    expect(component.result()!.statut).toBe('URGENT');
    expect(component.showForm()).toBe(false);
    expect(component.dateJugementTA()).toBe('2026-01-15');
    expect(component.delaiSpecialOQTF()).toBe(false);
    expect(component.typeContentieux()).toBe('REFUS_TITRE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid requires dateJugementTA only', () => {
    component.dateJugementTA.set(null);
    expect(component.formValid()).toBe(false);
    component.dateJugementTA.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateJugementTA: '2026-01-15',
      typeDecisionTA: 'REJET',
      typeContentieux: 'OQTF',
      delaiSpecialOQTF: true,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('APPEL_POSSIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateJugementTA.set(null);
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

  // --- délai spécial OQTF 15 j ---

  it('delaiSpecialOQTF 15j: URGENT echeance rendered in red (urgent class)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    // délai spécial OQTF 15 j → échéance proche, statut URGENT, jours <= 15
    httpMock.expectOne((r) => r.method === 'POST').flush(
      frResponse({ statut: 'URGENT', dateEcheanceAppelCaa: '2026-01-30', joursRestants: 8 }),
    );
    fixture.detectChanges();
    const echeance = fixture.nativeElement.querySelector('.acc-bd-value--echeance-urgent');
    expect(echeance).not.toBeNull();
    expect(echeance.textContent).toContain('2026-01-30');
  });

  it('PRESCRIT -> danger chip rendered + negative jours danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'PRESCRIT', joursRestants: -30 }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acc-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('prescrit');
    const joursBig = fixture.nativeElement.querySelector('.acc-jours-big--danger');
    expect(joursBig).not.toBeNull();
    expect(joursBig.textContent.trim()).toBe('-30');
  });

  it('renders courAppelCompetente in the cour value', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const cour = fixture.nativeElement.querySelector('.acc-bd-value--cour');
    expect(cour).not.toBeNull();
    expect(cour.textContent).toContain("Cour administrative d'appel de Paris");
  });

  // --- bannière filtre pourvoi cassation ---

  it('filtrePourvoisCassation=true -> renders L.821-2 info banner', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ filtrePourvoisCassation: true }));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="filtre-pourvoi-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('L. 821-2');
  });

  it('filtrePourvoisCassation=false -> no filter banner', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ filtrePourvoisCassation: false }));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="filtre-pourvoi-banner"]');
    expect(banner).toBeNull();
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: APPEL_POSSIBLE -> creates deadline with label + echeance date', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'APPEL_POSSIBLE', dateEcheanceAppelCaa: '2026-01-30' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Appel CAA contentieux étrangers', '2026-01-30');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ statut: 'URGENT', dateEcheanceAppelCaa: '2026-01-25', joursRestants: 5 }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', 'Appel CAA contentieux étrangers', '2026-01-25');
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

  it('aiData with date -> pre-fills dateJugementTA + provenance IA', () => {
    component.aiData = { recoursDateJugementTA: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateJugementTA()).toBe('2026-02-20');
    expect(component.provenanceDateJugement()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { recoursDateJugementTA: '2099-01-01' } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateJugementTA: '2026-01-15' }));
    expect(component.dateJugementTA()).toBe('2026-01-15');
    expect(component.provenanceDateJugement()).toBeNull();
  });

  it('onDateJugementChange clears provenance', () => {
    component.aiData = { recoursDateJugementTA: '2026-02-20' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateJugement()).toBe('IA');
    component.onDateJugementChange('2026-03-01');
    expect(component.provenanceDateJugement()).toBeNull();
  });

  it('onTypeContentieuxChange OQTF -> delaiSpecialOQTF true ; AUTRE -> false', () => {
    component.onTypeContentieuxChange('AUTRE');
    expect(component.typeContentieux()).toBe('AUTRE');
    expect(component.delaiSpecialOQTF()).toBe(false);
    component.onTypeContentieuxChange('OQTF');
    expect(component.typeContentieux()).toBe('OQTF');
    expect(component.delaiSpecialOQTF()).toBe(true);
  });

  it('bannerClass / bannerIcon / statutLabel cover all statuts', () => {
    expect(component.bannerClass('APPEL_POSSIBLE')).toContain('acc-banner--success');
    expect(component.bannerClass('URGENT')).toContain('acc-banner--warning');
    expect(component.bannerClass('PRESCRIT')).toContain('acc-banner--danger');
    expect(component.bannerIcon('APPEL_POSSIBLE')).toBe('check_circle');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('PRESCRIT')).toBe('error');
    expect(component.statutLabel('PRESCRIT')).toContain('prescrit');
    expect(component.statutLabel('APPEL_POSSIBLE')).toContain('possible');
  });

  it('typeDecisionLabel / typeContentieuxLabel cover values', () => {
    expect(component.typeDecisionLabel('REJET')).toContain('Rejet');
    expect(component.typeDecisionLabel('ANNULATION')).toContain('Annulation');
    expect(component.typeContentieuxLabel('OQTF')).toBe('OQTF');
    expect(component.typeContentieuxLabel('REFUS_TITRE')).toContain('Refus');
    expect(component.typeContentieuxLabel('EXPULSION')).toContain('Expulsion');
    expect(component.typeContentieuxLabel('AUTRE')).toContain('Autre');
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
    expect(component.dateJugementTA()).toBeNull();
    component.aiData = { recoursDateJugementTA: '2026-05-01' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateJugementTA()).toBe('2026-05-01');
    expect(component.provenanceDateJugement()).toBe('IA');
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

  it('renders motifsAppelPossibles list in result', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.acc-motifs-list li');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Erreur de droit');
  });
});

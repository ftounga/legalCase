import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';
import { of, throwError } from 'rxjs';

import { AppelCphSectionComponent } from './appel-cph-section.component';
import { AppelCphResponse } from '../../core/models/appel-cph.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';

describe('AppelCphSectionComponent', () => {
  let component: AppelCphSectionComponent;
  let fixture: ComponentFixture<AppelCphSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let deadlineSpy: jasmine.SpyObj<CaseDeadlineService>;

  const BASE_URL = '/api/v1/case-files/case-1/appel-cph-analysis';

  function frResponse(overrides: Partial<AppelCphResponse> = {}): AppelCphResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationJugement: '2026-01-15',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
      dateLimiteAppel: '2026-02-15',
      joursRestants: 20,
      verdict: 'DELAI_OUVERT',
      checklist: [
        { libelle: "Déclaration d'appel RPVA", obligatoire: true, bloquant: false, baseJuridique: 'art. 901 CPC' },
        { libelle: 'Représentation constituée', obligatoire: true, bloquant: false, baseJuridique: 'R. 1461-2 CPC' },
      ],
      renvoiPourvoiCassation: null,
      country: 'FRANCE',
      baseJuridique: 'art. 538 CPC ; R. 1461-1 CPC',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateNotificationJugement.set('2026-01-15');
    component.partieAppelante.set('SALARIE');
    component.modeNotification.set('SIGNIFICATION');
    component.representationConstituee.set('AVOCAT');
    component.jugementEnDernierRessort.set(false);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    deadlineSpy = jasmine.createSpyObj('CaseDeadlineService', ['create']);
    deadlineSpy.create.and.returnValue(of({ id: 'd-1', label: "Appel CPH cour d'appel", dueDate: '2026-02-15' } as unknown as CaseDeadline));
    await TestBed.configureTestingModule({
      imports: [AppelCphSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDeadlineService, useValue: deadlineSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppelCphSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(AppelCphSectionComponent.TOOL_LABEL).toContain('APPEL CPH');
    expect(AppelCphSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AppelCphSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when dateNotificationJugement present (FRANCE)', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: { dateNotificationJugement: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AppelCphSectionComponent.getPrefillCount({
      aiData: { dateNotificationJugement: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

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

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ verdict: 'DELAI_URGENT', representationConstituee: 'DEFENSEUR_SYNDICAL', partieAppelante: 'EMPLOYEUR' }));
    expect(component.result()!.verdict).toBe('DELAI_URGENT');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationJugement()).toBe('2026-01-15');
    expect(component.representationConstituee()).toBe('DEFENSEUR_SYNDICAL');
    expect(component.partieAppelante()).toBe('EMPLOYEUR');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form + analyze ---

  it('formValid requires dateNotificationJugement only', () => {
    component.dateNotificationJugement.set(null);
    expect(component.formValid()).toBe(false);
    component.dateNotificationJugement.set('2026-01-15');
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + exact body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationJugement: '2026-01-15',
      partieAppelante: 'SALARIE',
      modeNotification: 'SIGNIFICATION',
      representationConstituee: 'AVOCAT',
      jugementEnDernierRessort: false,
    });
    req.flush(frResponse());
    expect(component.result()!.verdict).toBe('DELAI_OUVERT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationJugement.set(null);
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

  // --- chip 4 verdicts ---

  it('verdict DELAI_OUVERT -> success chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_OUVERT' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acph-chip--success');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('ouvert');
  });

  it('verdict DELAI_URGENT -> warning chip + urgent echeance class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(
      frResponse({ verdict: 'DELAI_URGENT', dateLimiteAppel: '2026-02-15', joursRestants: 5 }),
    );
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acph-chip--warning');
    expect(chip).not.toBeNull();
    const echeance = fixture.nativeElement.querySelector('.acph-bd-value--echeance-urgent');
    expect(echeance).not.toBeNull();
    expect(echeance.textContent).toContain('2026-02-15');
  });

  it('verdict DELAI_EXPIRE -> danger chip + negative jours danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_EXPIRE', joursRestants: -12 }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acph-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('expiré');
    const joursBig = fixture.nativeElement.querySelector('.acph-jours-big--danger');
    expect(joursBig).not.toBeNull();
    expect(joursBig.textContent.trim()).toBe('-12');
  });

  it('verdict VOIE_FERMEE -> neutral chip + no jours-restants + pourvoi banner', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(
      frResponse({ verdict: 'VOIE_FERMEE', jugementEnDernierRessort: true, joursRestants: 0 }),
    );
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.acph-chip--neutral');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('fermée');
    // jours restants masqués quand la voie est fermée
    const jours = fixture.nativeElement.querySelector('[data-testid="jours-restants"]');
    expect(jours).toBeNull();
    const pourvoi = fixture.nativeElement.querySelector('[data-testid="pourvoi-link-banner"]');
    expect(pourvoi).not.toBeNull();
    expect(pourvoi.textContent).toContain('F-DT-87');
  });

  // --- checklist ---

  it('renders the checklist items from the response', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="checklist-block"]');
    expect(block).not.toBeNull();
    const items = fixture.nativeElement.querySelectorAll('.acph-checklist-item');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain("Déclaration d'appel RPVA");
    expect(items[0].textContent).toContain('art. 901 CPC');
  });

  it('renders a bloquant checklist item with the bloquant class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      representationConstituee: 'AUCUNE',
      checklist: [
        { libelle: 'Représentation obligatoire non constituée', obligatoire: true, bloquant: true, baseJuridique: 'R. 1461-2 CPC' },
      ],
    }));
    fixture.detectChanges();
    const bloquant = fixture.nativeElement.querySelector('.acph-checklist-item--bloquant');
    expect(bloquant).not.toBeNull();
    expect(bloquant.textContent).toContain('Représentation obligatoire non constituée');
  });

  // --- lien croisé pourvoi F-DT-87 ---

  it('renvoiPourvoiCassation present -> pourvoi link banner shows the renvoi text', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      verdict: 'DELAI_OUVERT',
      renvoiPourvoiCassation: 'Pourvoi possible dans un délai de 2 mois (F-DT-87)',
    }));
    fixture.detectChanges();
    const pourvoi = fixture.nativeElement.querySelector('[data-testid="pourvoi-link-banner"]');
    expect(pourvoi).not.toBeNull();
    expect(pourvoi.textContent).toContain('F-DT-87');
  });

  it('no pourvoi banner when voie ouverte and no renvoi', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_OUVERT', jugementEnDernierRessort: false, renvoiPourvoiCassation: null }));
    fixture.detectChanges();
    const pourvoi = fixture.nativeElement.querySelector('[data-testid="pourvoi-link-banner"]');
    expect(pourvoi).toBeNull();
  });

  it('showPourvoiLink true when jugementEnDernierRessort even if verdict not VOIE_FERMEE', () => {
    expect(component.showPourvoiLink(frResponse({ verdict: 'DELAI_OUVERT', jugementEnDernierRessort: true }))).toBe(true);
    expect(component.showPourvoiLink(frResponse({ verdict: 'DELAI_OUVERT', jugementEnDernierRessort: false, renvoiPourvoiCassation: null }))).toBe(false);
    expect(component.showPourvoiLink(null)).toBe(false);
  });

  // --- Bridge échéance F-69 ---

  it('bridge F-69: DELAI_OUVERT -> creates deadline with label + dateLimiteAppel', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_OUVERT', dateLimiteAppel: '2026-02-15' }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', "Appel CPH cour d'appel", '2026-02-15');
    expect(component.deadlineCreated()).toBe(true);
  });

  it('bridge F-69: DELAI_URGENT -> creates deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_URGENT', dateLimiteAppel: '2026-02-10', joursRestants: 3 }));
    expect(deadlineSpy.create).toHaveBeenCalledWith('case-1', "Appel CPH cour d'appel", '2026-02-10');
  });

  it('bridge F-69: DELAI_EXPIRE -> no deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_EXPIRE', joursRestants: -5 }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: VOIE_FERMEE -> no deadline', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'VOIE_FERMEE', jugementEnDernierRessort: true }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  it('bridge F-69: deadline creation failure does not break the flow', () => {
    deadlineSpy.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_OUVERT' }));
    expect(deadlineSpy.create).toHaveBeenCalled();
    expect(component.deadlineCreated()).toBe(false);
    expect(component.result()!.verdict).toBe('DELAI_OUVERT');
  });

  it('bridge F-69: standaloneMode -> never creates a deadline', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'DELAI_OUVERT' }));
    expect(deadlineSpy.create).not.toHaveBeenCalled();
  });

  // --- prefill IA ---

  it('aiData with date -> pre-fills dateNotificationJugement + provenance IA', () => {
    component.aiData = { dateNotificationJugement: '2026-02-20' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationJugement()).toBe('2026-02-20');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { dateNotificationJugement: '2099-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationJugement: '2026-01-15' }));
    expect(component.dateNotificationJugement()).toBe('2026-01-15');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { dateNotificationJugement: '2026-02-20' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotification()).toBe('IA');
    component.onDateNotificationChange('2026-03-01');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationJugement()).toBeNull();
    component.aiData = { dateNotificationJugement: '2026-05-01' } as TravailExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationJugement()).toBe('2026-05-01');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  // --- labels / helpers ---

  it('verdictLabel / bannerClass / bannerIcon cover all 4 verdicts', () => {
    expect(component.verdictLabel('DELAI_OUVERT')).toContain('ouvert');
    expect(component.verdictLabel('DELAI_URGENT')).toContain('urgent');
    expect(component.verdictLabel('DELAI_EXPIRE')).toContain('expiré');
    expect(component.verdictLabel('VOIE_FERMEE')).toContain('fermée');
    expect(component.bannerClass('DELAI_OUVERT')).toContain('acph-banner--success');
    expect(component.bannerClass('DELAI_URGENT')).toContain('acph-banner--warning');
    expect(component.bannerClass('DELAI_EXPIRE')).toContain('acph-banner--danger');
    expect(component.bannerClass('VOIE_FERMEE')).toContain('acph-banner--neutral');
    expect(component.bannerIcon('DELAI_OUVERT')).toBe('check_circle');
    expect(component.bannerIcon('DELAI_URGENT')).toBe('warning');
    expect(component.bannerIcon('DELAI_EXPIRE')).toBe('error');
    expect(component.bannerIcon('VOIE_FERMEE')).toBe('block');
  });

  it('partieAppelante / modeNotification / representation labels cover values', () => {
    expect(component.partieAppelanteLabel('SALARIE')).toContain('Salarié');
    expect(component.partieAppelanteLabel('EMPLOYEUR')).toContain('Employeur');
    expect(component.modeNotificationLabel('SIGNIFICATION')).toContain('Signification');
    expect(component.modeNotificationLabel('LRAR')).toContain('LRAR');
    expect(component.representationLabel('AVOCAT')).toContain('Avocat');
    expect(component.representationLabel('DEFENSEUR_SYNDICAL')).toContain('Défenseur');
    expect(component.representationLabel('AUCUNE')).toContain('Aucune');
  });

  it('showJoursRestants false only for VOIE_FERMEE', () => {
    expect(component.showJoursRestants('DELAI_OUVERT')).toBe(true);
    expect(component.showJoursRestants('DELAI_URGENT')).toBe(true);
    expect(component.showJoursRestants('DELAI_EXPIRE')).toBe(true);
    expect(component.showJoursRestants('VOIE_FERMEE')).toBe(false);
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

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});

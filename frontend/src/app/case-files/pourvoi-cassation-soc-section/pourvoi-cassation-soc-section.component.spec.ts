import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { PourvoiCassationSocSectionComponent } from './pourvoi-cassation-soc-section.component';
import { PourvoiCassationSocResponse } from '../../core/models/pourvoi-cassation-soc.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('PourvoiCassationSocSectionComponent', () => {
  let component: PourvoiCassationSocSectionComponent;
  let fixture: ComponentFixture<PourvoiCassationSocSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/pourvoi-cassation-soc-analysis';

  function frResponse(overrides: Partial<PourvoiCassationSocResponse> = {}): PourvoiCassationSocResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationArret: '2026-01-15',
      casOuverture: ['VIOLATION_LOI'],
      representationAvocatCassation: true,
      moyenSerieuxIdentifie: true,
      dateLimitePourvoi: '2026-03-15',
      joursRestants: 40,
      verdictDelai: 'DELAI_OUVERT',
      verdict: 'POURVOI_RECOMMANDE',
      risqueNonAdmission: 'FAIBLE',
      casOuvertureAnalyses: [
        { cas: 'VIOLATION_LOI', libelle: 'Violation de la loi', baseJuridique: 'art. 604 CPC', forceProbatoire: 'FORTE' },
      ],
      itemBloquantRepresentation: null,
      country: 'FRANCE',
      baseJuridique: 'art. 612 CPC ; art. 973 CPC ; art. 1014 CPC',
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dateNotificationArret.set('2026-01-15');
    component.casOuverture.set(['VIOLATION_LOI']);
    component.representationAvocatCassation.set(true);
    component.moyenSerieuxIdentifie.set(true);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [PourvoiCassationSocSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PourvoiCassationSocSectionComponent);
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
    expect(PourvoiCassationSocSectionComponent.TOOL_LABEL).toContain('POURVOI');
    expect(PourvoiCassationSocSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(PourvoiCassationSocSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 when dateNotificationArretAppel present (FRANCE)', () => {
    expect(PourvoiCassationSocSectionComponent.getPrefillCount({
      aiData: { dateNotificationArretAppel: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(PourvoiCassationSocSectionComponent.getPrefillCount({
      aiData: { dateNotificationArretAppel: '2026-01-15' },
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
    httpMock.expectOne(BASE_URL).flush(frResponse({
      verdict: 'POURVOI_RISQUE',
      risqueNonAdmission: 'MODERE',
      casOuverture: ['DENATURATION', 'VIOLATION_LOI'],
    }));
    expect(component.result()!.verdict).toBe('POURVOI_RISQUE');
    expect(component.showForm()).toBe(false);
    expect(component.dateNotificationArret()).toBe('2026-01-15');
    expect(component.casOuverture()).toEqual(['DENATURATION', 'VIOLATION_LOI']);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form + analyze ---

  it('formValid requires date AND at least one cas d\'ouverture', () => {
    component.dateNotificationArret.set(null);
    component.casOuverture.set([]);
    expect(component.formValid()).toBe(false);
    component.dateNotificationArret.set('2026-01-15');
    expect(component.formValid()).toBe(false); // toujours pas de cas
    component.casOuverture.set(['VIOLATION_LOI']);
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack + exact body + refresh', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationArret: '2026-01-15',
      casOuverture: ['VIOLATION_LOI'],
      representationAvocatCassation: true,
      moyenSerieuxIdentifie: true,
    });
    req.flush(frResponse());
    expect(component.result()!.verdict).toBe('POURVOI_RECOMMANDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.casOuverture.set([]);
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

  it('verdict POURVOI_RECOMMANDE -> success chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'POURVOI_RECOMMANDE' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.pcs-chip--success');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('recommandé');
  });

  it('verdict POURVOI_RISQUE -> warning chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'POURVOI_RISQUE' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.pcs-chip--warning');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('risque');
  });

  it('verdict POURVOI_DECONSEILLE -> navy chip rendered', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ verdict: 'POURVOI_DECONSEILLE' }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.pcs-chip--navy');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('déconseillé');
  });

  it('verdict DELAI_EXPIRE -> danger chip + negative jours danger class', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      verdict: 'DELAI_EXPIRE', verdictDelai: 'DELAI_EXPIRE', joursRestants: -12,
    }));
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.pcs-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('expiré');
    const joursBig = fixture.nativeElement.querySelector('.pcs-jours-big--danger');
    expect(joursBig).not.toBeNull();
    expect(joursBig.textContent.trim()).toBe('-12');
  });

  // --- risque non-admission badge (3 niveaux) ---

  it('risqueNonAdmission ELEVE -> danger risque badge', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      verdict: 'POURVOI_RISQUE', risqueNonAdmission: 'ELEVE', moyenSerieuxIdentifie: false,
      casOuverture: ['DENATURATION'],
    }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="risque-badge"]');
    expect(badge).not.toBeNull();
    expect(badge.classList).toContain('pcs-risque--danger');
    expect(badge.textContent).toContain('élevé');
  });

  it('risqueNonAdmission MODERE -> warning risque badge', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ risqueNonAdmission: 'MODERE' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="risque-badge"]');
    expect(badge.classList).toContain('pcs-risque--warning');
  });

  it('risqueNonAdmission FAIBLE -> success risque badge', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ risqueNonAdmission: 'FAIBLE' }));
    fixture.detectChanges();
    const badge = fixture.nativeElement.querySelector('[data-testid="risque-badge"]');
    expect(badge.classList).toContain('pcs-risque--success');
  });

  // --- cas d'ouverture analysés + force probatoire ---

  it('renders cas d\'ouverture analyses with force probatoire', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      casOuvertureAnalyses: [
        { cas: 'VIOLATION_LOI', libelle: 'Violation de la loi', baseJuridique: 'art. 604 CPC', forceProbatoire: 'FORTE' },
        { cas: 'DENATURATION', libelle: 'Dénaturation', baseJuridique: 'art. 604 CPC', forceProbatoire: 'MOYENNE' },
      ],
    }));
    fixture.detectChanges();
    const block = fixture.nativeElement.querySelector('[data-testid="cas-analyses-block"]');
    expect(block).not.toBeNull();
    const items = fixture.nativeElement.querySelectorAll('.pcs-cas-item');
    expect(items.length).toBe(2);
    expect(items[0].textContent).toContain('Violation de la loi');
    expect(items[0].textContent).toContain('art. 604 CPC');
    expect(fixture.nativeElement.querySelector('.pcs-force--strong')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.pcs-force--medium')).not.toBeNull();
  });

  // --- item bloquant représentation avocat aux Conseils ---

  it('itemBloquantRepresentation present -> bloquant banner shown', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      representationAvocatCassation: false,
      itemBloquantRepresentation: 'Représentation par avocat aux Conseils obligatoire (art. 973 CPC)',
    }));
    fixture.detectChanges();
    const bloquant = fixture.nativeElement.querySelector('[data-testid="bloquant-representation"]');
    expect(bloquant).not.toBeNull();
    expect(bloquant.textContent).toContain('avocat aux Conseils');
  });

  it('no bloquant banner when representation constituee', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({ itemBloquantRepresentation: null }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="bloquant-representation"]')).toBeNull();
  });

  // --- date limite ---

  it('renders date limite pourvoi with urgent class on DELAI_URGENT', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse({
      verdict: 'POURVOI_RECOMMANDE', verdictDelai: 'DELAI_URGENT',
      dateLimitePourvoi: '2026-03-15', joursRestants: 10,
    }));
    fixture.detectChanges();
    const echeance = fixture.nativeElement.querySelector('.pcs-bd-value--echeance-urgent');
    expect(echeance).not.toBeNull();
    expect(echeance.textContent).toContain('2026-03-15');
  });

  // --- prefill IA ---

  it('aiData with date -> pre-fills dateNotificationArret + provenance IA', () => {
    component.aiData = { dateNotificationArretAppel: '2026-02-20' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationArret()).toBe('2026-02-20');
    expect(component.provenanceDateNotificationArret()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { dateNotificationArretAppel: '2099-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dateNotificationArret: '2026-01-15' }));
    expect(component.dateNotificationArret()).toBe('2026-01-15');
    expect(component.provenanceDateNotificationArret()).toBeNull();
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { dateNotificationArretAppel: '2026-02-20' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateNotificationArret()).toBe('IA');
    component.onDateNotificationChange('2026-03-01');
    expect(component.provenanceDateNotificationArret()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationArret()).toBeNull();
    component.aiData = { dateNotificationArretAppel: '2026-05-01' } as TravailExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationArret()).toBe('2026-05-01');
    expect(component.provenanceDateNotificationArret()).toBe('IA');
  });

  // --- labels / helpers ---

  it('verdictLabel / bannerClass / bannerIcon cover all 4 verdicts', () => {
    expect(component.verdictLabel('POURVOI_RECOMMANDE')).toContain('recommandé');
    expect(component.verdictLabel('POURVOI_RISQUE')).toContain('risque');
    expect(component.verdictLabel('POURVOI_DECONSEILLE')).toContain('déconseillé');
    expect(component.verdictLabel('DELAI_EXPIRE')).toContain('expiré');
    expect(component.bannerClass('POURVOI_RECOMMANDE')).toContain('pcs-banner--success');
    expect(component.bannerClass('POURVOI_RISQUE')).toContain('pcs-banner--warning');
    expect(component.bannerClass('POURVOI_DECONSEILLE')).toContain('pcs-banner--navy');
    expect(component.bannerClass('DELAI_EXPIRE')).toContain('pcs-banner--danger');
    expect(component.bannerIcon('POURVOI_RECOMMANDE')).toBe('check_circle');
    expect(component.bannerIcon('POURVOI_RISQUE')).toBe('warning');
    expect(component.bannerIcon('POURVOI_DECONSEILLE')).toBe('thumb_down');
    expect(component.bannerIcon('DELAI_EXPIRE')).toBe('error');
  });

  it('risque / force probatoire labels and classes cover values', () => {
    expect(component.risqueLabel('ELEVE')).toContain('élevé');
    expect(component.risqueLabel('MODERE')).toContain('modéré');
    expect(component.risqueLabel('FAIBLE')).toContain('faible');
    expect(component.risqueBadgeClass('ELEVE')).toContain('pcs-risque--danger');
    expect(component.risqueBadgeClass('MODERE')).toContain('pcs-risque--warning');
    expect(component.risqueBadgeClass('FAIBLE')).toContain('pcs-risque--success');
    expect(component.forceProbatoireLabel('FORTE')).toContain('forte');
    expect(component.forceProbatoireClass('FORTE')).toContain('pcs-force--strong');
    expect(component.forceProbatoireClass('MOYENNE')).toContain('pcs-force--medium');
    expect(component.forceProbatoireClass('FAIBLE')).toContain('pcs-force--weak');
  });

  it('casOuvertureLabel maps known values', () => {
    expect(component.casOuvertureLabel('VIOLATION_LOI')).toContain('Violation');
    expect(component.casOuvertureLabel('DENATURATION')).toContain('Dénaturation');
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

  it('standaloneMode -> no GET, form visible, banner displayed, no refresh on analyze', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(frResponse());
    expect(refreshSpy.triggerRefresh).not.toHaveBeenCalled();
  });
});

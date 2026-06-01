import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { DelegationSyndicaleSectionComponent } from './delegation-syndicale-section.component';
import { DelegationSyndicaleResponse } from '../../core/models/delegation-syndicale.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('DelegationSyndicaleSectionComponent', () => {
  let component: DelegationSyndicaleSectionComponent;
  let fixture: ComponentFixture<DelegationSyndicaleSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/delegation-syndicale-analysis';

  function regulieResponse(overrides: Partial<DelegationSyndicaleResponse> = {}): DelegationSyndicaleResponse {
    return {
      caseFileId: 'case-1',
      effectif: 80,
      typeMandat: 'DELEGUE_SYNDICAL',
      syndicatRepresentatif: true,
      pourcentageScorePersonnel: 15,
      dateDesignation: '2026-03-01',
      checklist: [
        { item: 'Effectif suffisant', conforme: true, commentaire: 'Effectif : 80.' },
        { item: 'Organisation représentative', conforme: true, commentaire: '' },
        { item: 'Score personnel ≥ 10 %', conforme: true, commentaire: 'Score : 15 %.' },
      ],
      statutDesignation: 'REGULIERE',
      statutProtege: 'OUI',
      licenciementEnvisage: false,
      autorisationInspecteurTravail: false,
      risqueNulliteLicenciement: 'SANS_OBJET',
      consequences: [],
      country: 'FRANCE',
      baseJuridique: 'art. L.2143-1 et s. CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function irreguliereResponse(): DelegationSyndicaleResponse {
    return regulieResponse({
      effectif: 30,
      checklist: [
        { item: 'Effectif suffisant', conforme: false, commentaire: 'Effectif insuffisant : 30.' },
        { item: 'Organisation représentative', conforme: true, commentaire: '' },
        { item: 'Score personnel ≥ 10 %', conforme: true, commentaire: '' },
      ],
      statutDesignation: 'IRREGULIERE',
      consequences: ['Effectif insuffisant (30 salariés).'],
    });
  }

  function aVerifierResponse(): DelegationSyndicaleResponse {
    return regulieResponse({
      pourcentageScorePersonnel: null,
      checklist: [
        { item: 'Effectif suffisant', conforme: true, commentaire: '' },
        { item: 'Organisation représentative', conforme: true, commentaire: '' },
        { item: 'Score personnel ≥ 10 %', conforme: false, commentaire: 'Score non renseigné.' },
      ],
      statutDesignation: 'A_VERIFIER',
    });
  }

  function eleveResponse(): DelegationSyndicaleResponse {
    return regulieResponse({
      licenciementEnvisage: true,
      autorisationInspecteurTravail: false,
      risqueNulliteLicenciement: 'ELEVE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [DelegationSyndicaleSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DelegationSyndicaleSectionComponent);
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
    expect(DelegationSyndicaleSectionComponent.TOOL_LABEL).toContain('DÉLÉGUÉ SYNDICAL');
    expect(DelegationSyndicaleSectionComponent.TOOL_ICON).toBe('diversity_3');
  });

  it('static getPrefillCount returns 0 / 1 / 2 (0, partiel, nominal)', () => {
    expect(DelegationSyndicaleSectionComponent.getPrefillCount({})).toBe(0);
    expect(DelegationSyndicaleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
    expect(DelegationSyndicaleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, mandatSyndicalType: 'DELEGUE_SYNDICAL' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(DelegationSyndicaleSectionComponent.getPrefillCount({
      aiData: { pseNombreSalaries: 80, mandatSyndicalType: 'DELEGUE_SYNDICAL' },
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

  it('BELGIQUE workspace shows FR gate banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(regulieResponse());
    expect(component.result()!.statutDesignation).toBe('REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(component.effectif()).toBe(80);
    expect(component.typeMandat()).toBe('DELEGUE_SYNDICAL');
    expect(component.pourcentageScorePersonnel()).toBe(15);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires a positive effectif and a score in [0;100] if set', () => {
    expect(component.formValid()).toBe(false);
    component.effectif.set(0);
    expect(component.formValid()).toBe(false);
    component.effectif.set(80);
    expect(component.formValid()).toBe(true);
    component.pourcentageScorePersonnel.set(150);
    expect(component.formValid()).toBe(false);
    component.pourcentageScorePersonnel.set(15);
    expect(component.formValid()).toBe(true);
  });

  // --- conditional score field (DS only) / RSS ---

  it('score field is shown for DS and hidden for RSS', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="score-wrap"]')).not.toBeNull();
    component.onTypeMandatChange('RSS');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="score-wrap"]')).toBeNull();
  });

  it('switching to RSS clears the score', () => {
    component.pourcentageScorePersonnel.set(20);
    component.onTypeMandatChange('RSS');
    expect(component.pourcentageScorePersonnel()).toBeNull();
    expect(component.isDs()).toBe(false);
  });

  // --- conditional autorisation field (licenciement only) ---

  it('autorisation field is shown only when licenciement envisagé', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="autorisation-wrap"]')).toBeNull();
    component.onLicenciementEnvisageChange(true);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="autorisation-wrap"]')).not.toBeNull();
  });

  it('unchecking licenciement resets autorisation', () => {
    component.onLicenciementEnvisageChange(true);
    component.onAutorisationChange(true);
    expect(component.autorisationInspecteurTravail()).toBe(true);
    component.onLicenciementEnvisageChange(false);
    expect(component.autorisationInspecteurTravail()).toBe(false);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when DS sans organisation représentative', () => {
    component.typeMandat.set('DELEGUE_SYNDICAL');
    component.syndicatRepresentatif.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('organisation syndicale représentative'))).toBe(true);
  });

  it('raises a coherence alert when RSS désigné par une organisation représentative', () => {
    component.typeMandat.set('RSS');
    component.syndicatRepresentatif.set(true);
    expect(component.coherenceAlerts().some(a => a.includes('NON représentatif'))).toBe(true);
  });

  it('raises a coherence alert when licenciement sans autorisation', () => {
    component.licenciementEnvisage.set(true);
    component.autorisationInspecteurTravail.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('inspecteur du travail'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.typeMandat.set('DELEGUE_SYNDICAL');
    component.syndicatRepresentatif.set(true);
    component.pourcentageScorePersonnel.set(15);
    component.dateDesignation.set('2026-03-01');
    component.licenciementEnvisage.set(false);
    component.autorisationInspecteurTravail.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      effectif: 80,
      typeMandat: 'DELEGUE_SYNDICAL',
      syndicatRepresentatif: true,
      pourcentageScorePersonnel: 15,
      dateDesignation: '2026-03-01',
      licenciementEnvisage: false,
      autorisationInspecteurTravail: false,
    });
    req.flush(regulieResponse());
    expect(component.result()!.statutDesignation).toBe('REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() sends null score for RSS even if a score was typed', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.pourcentageScorePersonnel.set(20);
    component.onTypeMandatChange('RSS');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.typeMandat).toBe('RSS');
    expect(req.request.body.pourcentageScorePersonnel).toBeNull();
    req.flush(regulieResponse({ typeMandat: 'RSS', syndicatRepresentatif: false, pourcentageScorePersonnel: null }));
  });

  it('analyze() does nothing when form invalid (no effectif)', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.effectif.set(80);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 statut states + protege + risque ---

  it('REGULIERE -> success statut chip + protégé OUI + risque SANS_OBJET + checklist ✓', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(regulieResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('régulière');
    expect(chip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="protege-chip"]')!.textContent).toContain('OUI');
    const risque = el.querySelector('[data-testid="risque-chip"]')!;
    expect(risque.textContent).toContain('Sans objet');
    expect(risque.className).toContain('is-chip--neutral');
    expect(el.querySelectorAll('[data-testid="checklist"] .is-critere').length).toBe(3);
    expect(el.querySelector('[data-testid="score-value"]')!.textContent).toContain('15');
    expect(el.querySelector('[data-testid="protege-note"]')!.textContent).toContain('inspecteur du travail');
  });

  it('IRREGULIERE -> danger statut chip + item effectif ✗', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(irreguliereResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('irrégulière');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelectorAll('[data-testid="checklist"] .is-critere--ko').length).toBe(1);
    expect(el.querySelector('[data-testid="consequences"]')!.textContent).toContain('insuffisant');
  });

  it('A_VERIFIER -> warning statut chip (DS sans score)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(aVerifierResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('à vérifier');
    expect(chip.className).toContain('is-chip--warning');
    // pas de score affiché (null)
    expect(el.querySelector('[data-testid="score-value"]')).toBeNull();
  });

  it('ELEVE -> danger risque chip + note réintégration (licenciement sans autorisation)', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(eleveResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const risque = el.querySelector('[data-testid="risque-chip"]')!;
    expect(risque.textContent).toContain('élevé');
    expect(risque.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="protege-note"]')!.textContent).toContain('réintégration');
  });

  it('statut / risque chip classes map their states', () => {
    expect(component.statutDesignationChipClass('REGULIERE')).toContain('success');
    expect(component.statutDesignationChipClass('IRREGULIERE')).toContain('danger');
    expect(component.statutDesignationChipClass('A_VERIFIER')).toContain('warning');
    expect(component.risqueChipClass('FAIBLE')).toContain('success');
    expect(component.risqueChipClass('ELEVE')).toContain('danger');
    expect(component.risqueChipClass('SANS_OBJET')).toContain('neutral');
  });

  // --- pré-fill IA ---

  it('pré-fills effectif and typeMandat from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      pseNombreSalaries: 120,
      mandatSyndicalType: 'RSS',
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.effectif()).toBe(120);
    expect(component.typeMandat()).toBe('RSS');
    expect(component.provenanceEffectif()).toBe('IA');
    expect(component.provenanceTypeMandat()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { pseNombreSalaries: 60, mandatSyndicalType: 'DELEGUE_SYNDICAL' };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.effectif()).toBe(60);
    expect(component.typeMandat()).toBe('DELEGUE_SYNDICAL');
    expect(component.provenanceTypeMandat()).toBe('IA');
  });

  it('onEffectifChange / onTypeMandatChange clear provenance', () => {
    component.provenanceEffectif.set('IA');
    component.onEffectifChange(42);
    expect(component.effectif()).toBe(42);
    expect(component.provenanceEffectif()).toBeNull();

    component.provenanceTypeMandat.set('IA');
    component.onTypeMandatChange('DELEGUE_SYNDICAL');
    expect(component.typeMandat()).toBe('DELEGUE_SYNDICAL');
    expect(component.provenanceTypeMandat()).toBeNull();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { IntermittentSpectacleAreSectionComponent } from './intermittent-spectacle-are-section.component';
import { IntermittentSpectacleAreResponse } from '../../core/models/intermittent-spectacle-are.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('IntermittentSpectacleAreSectionComponent', () => {
  let component: IntermittentSpectacleAreSectionComponent;
  let fixture: ComponentFixture<IntermittentSpectacleAreSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/intermittent-spectacle-are-analysis';

  function ouvertsResponse(overrides: Partial<IntermittentSpectacleAreResponse> = {}): IntermittentSpectacleAreResponse {
    return {
      caseFileId: 'case-1',
      annexe: 'ANNEXE_8_TECHNICIENS',
      dateFinContrat: '2024-03-31',
      heuresTravaillees12Mois: 520,
      nombreCachets: 0,
      heuresFormationDispensees: 0,
      heuresCachets: 0,
      heuresFormationRetenues: 0,
      heuresTotalesRetenues: 520,
      seuilHeures: 507,
      ouvertureDroits: 'OUVERTS',
      heuresManquantes: 0,
      heuresExcedentaires: 13,
      statut: 'DROITS_OUVERTS',
      dateProchainExamen: '2025-03-31',
      country: 'FRANCE',
      baseJuridique: 'Règlement Unedic — annexes 8 et 10 ; 507 h / 12 mois (à vérifier)',
      ...overrides,
    };
  }

  function nonOuvertsArtisteResponse(): IntermittentSpectacleAreResponse {
    return ouvertsResponse({
      annexe: 'ANNEXE_10_ARTISTES',
      heuresTravaillees12Mois: 300,
      nombreCachets: 15,
      heuresCachets: 180,
      heuresTotalesRetenues: 480,
      ouvertureDroits: 'NON_OUVERTS',
      heuresManquantes: 27,
      heuresExcedentaires: 0,
      statut: 'DROITS_NON_OUVERTS',
    });
  }

  function aVerifierResponse(): IntermittentSpectacleAreResponse {
    return ouvertsResponse({
      heuresTravaillees12Mois: 505,
      heuresTotalesRetenues: 505,
      ouvertureDroits: 'NON_OUVERTS',
      heuresManquantes: 2,
      heuresExcedentaires: 0,
      statut: 'A_VERIFIER',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    component.dateFinContrat.set('2024-03-31');
    component.heuresTravaillees12Mois.set(520);
    component.nombreCachets.set(0);
    component.heuresFormationDispensees.set(0);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [IntermittentSpectacleAreSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(IntermittentSpectacleAreSectionComponent);
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
    expect(IntermittentSpectacleAreSectionComponent.TOOL_LABEL).toContain('INTERMITTENT SPECTACLE ARE');
    expect(IntermittentSpectacleAreSectionComponent.TOOL_ICON).toBe('theater_comedy');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(IntermittentSpectacleAreSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with the two fields', () => {
    expect(IntermittentSpectacleAreSectionComponent.getPrefillCount({
      aiData: { dateLicenciement: '2024-03-31', intermittentAnnexe: 'ANNEXE_10_ARTISTES' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(IntermittentSpectacleAreSectionComponent.getPrefillCount({
      aiData: { dateLicenciement: '2024-03-31', intermittentAnnexe: 'ANNEXE_10_ARTISTES' },
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
    httpMock.expectOne(BASE_URL).flush(ouvertsResponse());
    expect(component.result()!.ouvertureDroits).toBe('OUVERTS');
    expect(component.showForm()).toBe(false);
    expect(component.annexe()).toBe('ANNEXE_8_TECHNICIENS');
    expect(component.dateFinContrat()).toBe('2024-03-31');
    expect(component.heuresTravaillees12Mois()).toBe(520);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation + conditional cachets ---

  it('formValid requires dateFinContrat + non-negative heures travaillées', () => {
    component.dateFinContrat.set(null);
    component.heuresTravaillees12Mois.set(520);
    expect(component.formValid()).toBe(false);
    component.dateFinContrat.set('2024-03-31');
    expect(component.formValid()).toBe(true);
    component.heuresTravaillees12Mois.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('isAnnexe10 toggles with annexe selection (cachets field condition)', () => {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    expect(component.isAnnexe10()).toBe(false);
    component.annexe.set('ANNEXE_10_ARTISTES');
    expect(component.isAnnexe10()).toBe(true);
  });

  it('renders the cachets field only for annexe 10', () => {
    component.forceExpanded = true;
    component.standaloneMode = true;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="cachets-field"]')).toBeNull();
    component.onAnnexeChange('ANNEXE_10_ARTISTES');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="cachets-field"]')).not.toBeNull();
  });

  it('totalHeuresEstime adds converted cachets (1 cachet = 12 h) for annexe 10', () => {
    component.annexe.set('ANNEXE_10_ARTISTES');
    component.heuresTravaillees12Mois.set(300);
    component.nombreCachets.set(15);
    component.heuresFormationDispensees.set(0);
    expect(component.totalHeuresEstime()).toBe(480); // 300 + 15*12
  });

  it('ignores cachets in the local total when annexe 8', () => {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    component.heuresTravaillees12Mois.set(300);
    component.nombreCachets.set(15);
    expect(component.totalHeuresEstime()).toBe(300);
  });

  it('progressionPct caps at 100 once the 507 h threshold is reached', () => {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    component.heuresTravaillees12Mois.set(600);
    expect(component.progressionPct()).toBe(100);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when cachets saisis hors annexe 10', () => {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    component.nombreCachets.set(5);
    expect(component.coherenceAlerts().some(a => a.includes('annexe 10'))).toBe(true);
  });

  it('raises a coherence alert when total dans la marge ± 10 h du seuil', () => {
    component.annexe.set('ANNEXE_8_TECHNICIENS');
    component.heuresTravaillees12Mois.set(505);
    expect(component.coherenceAlerts().some(a => a.includes('± 10 h'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      annexe: 'ANNEXE_8_TECHNICIENS',
      dateFinContrat: '2024-03-31',
      heuresTravaillees12Mois: 520,
      nombreCachets: 0,
      heuresFormationDispensees: 0,
    });
    req.flush(ouvertsResponse());
    expect(component.result()!.ouvertureDroits).toBe('OUVERTS');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 statut states + seuil 507 ---

  it('OUVERTS -> success statut chip + heures excédentaires (vert) + 507 h reference', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(ouvertsResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('Droits ouverts');
    expect(chip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="heures-excedentaires"]')!.textContent).toContain('13');
    expect(el.querySelector('[data-testid="heures-totales"]')!.textContent).toContain('507');
    expect(el.querySelector('[data-testid="date-prochain-examen"]')!.textContent).toContain('2025-03-31');
  });

  it('NON_OUVERTS annexe 10 -> danger chip + heures manquantes + cachets conversion shown', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonOuvertsArtisteResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('Droits non ouverts');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="heures-manquantes"]')!.textContent).toContain('27');
    expect(el.querySelector('[data-testid="heures-cachets"]')!.textContent).toContain('180');
    expect(el.querySelector('[data-testid="ouverture-chip"]')!.textContent).toContain('non ouverts');
  });

  it('A_VERIFIER -> warning statut chip', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(aVerifierResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('vérifier');
    expect(chip.className).toContain('is-chip--warning');
  });

  it('statutChipClass / ouvertureChipClass map the three / two states', () => {
    expect(component.statutChipClass('DROITS_OUVERTS')).toContain('success');
    expect(component.statutChipClass('DROITS_NON_OUVERTS')).toContain('danger');
    expect(component.statutChipClass('A_VERIFIER')).toContain('warning');
    expect(component.ouvertureChipClass('OUVERTS')).toContain('success');
    expect(component.ouvertureChipClass('NON_OUVERTS')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills dateFinContrat and annexe from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateLicenciement: '2024-02-10',
      intermittentAnnexe: 'ANNEXE_10_ARTISTES',
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateFinContrat()).toBe('2024-02-10');
    expect(component.annexe()).toBe('ANNEXE_10_ARTISTES');
    expect(component.provenanceDateFinContrat()).toBe('IA');
    expect(component.provenanceAnnexe()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { intermittentAnnexe: 'ANNEXE_8_TECHNICIENS' };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.annexe()).toBe('ANNEXE_8_TECHNICIENS');
    expect(component.provenanceAnnexe()).toBe('IA');
  });

  it('onDateFinContratChange clears provenance', () => {
    component.provenanceDateFinContrat.set('IA');
    component.onDateFinContratChange('2023-01-01');
    expect(component.dateFinContrat()).toBe('2023-01-01');
    expect(component.provenanceDateFinContrat()).toBeNull();
  });

  it('onAnnexeChange clears provenance', () => {
    component.provenanceAnnexe.set('IA');
    component.onAnnexeChange('ANNEXE_10_ARTISTES');
    expect(component.annexe()).toBe('ANNEXE_10_ARTISTES');
    expect(component.provenanceAnnexe()).toBeNull();
  });
});

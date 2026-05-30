import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ApprentissageRuptureSectionComponent } from './apprentissage-rupture-section.component';
import { ApprentissageRuptureResponse } from '../../core/models/apprentissage-rupture.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('ApprentissageRuptureSectionComponent', () => {
  let component: ApprentissageRuptureSectionComponent;
  let fixture: ComponentFixture<ApprentissageRuptureSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/apprentissage-rupture-analysis';

  function reguliereResponse(overrides: Partial<ApprentissageRuptureResponse> = {}): ApprentissageRuptureResponse {
    return {
      caseFileId: 'case-1',
      dateDebutContrat: '2026-01-06',
      dateRupture: '2026-01-26',
      auteurRupture: 'EMPLOYEUR',
      motifRupture: 'SANS_MOTIF',
      apprentiMajeur: true,
      joursDepuisDebut: 20,
      periode: 'DANS_45_PREMIERS_JOURS',
      validite: 'VALIDE',
      motif: 'Rupture libre dans les 45 premiers jours (art. L.6222-18).',
      consequences: [],
      verdictGlobal: 'RUPTURE_REGULIERE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.6222-18 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function irreguliereResponse(): ApprentissageRuptureResponse {
    return reguliereResponse({
      dateRupture: '2026-04-06',
      joursDepuisDebut: 90,
      periode: 'APRES_45_JOURS',
      validite: 'NON_VALIDE',
      motif: 'Rupture sans motif après 45 jours : rupture irrégulière.',
      consequences: [
        'Saisine du conseil de prud\'hommes pour dommages-intérêts.',
        'Indemnités de rupture irrégulière.',
      ],
      verdictGlobal: 'RUPTURE_IRREGULIERE',
    });
  }

  function aSecuriserResponse(): ApprentissageRuptureResponse {
    return reguliereResponse({
      dateRupture: '2026-04-06',
      motifRupture: 'FAUTE_GRAVE',
      joursDepuisDebut: 90,
      periode: 'APRES_45_JOURS',
      validite: 'A_SECURISER',
      motif: 'Faute grave : procédure de licenciement requise.',
      consequences: ['Procédure de licenciement requise (réforme 2018).'],
      verdictGlobal: 'RUPTURE_A_SECURISER',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [ApprentissageRuptureSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ApprentissageRuptureSectionComponent);
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
    expect(ApprentissageRuptureSectionComponent.TOOL_LABEL).toContain('APPRENTISSAGE');
    expect(ApprentissageRuptureSectionComponent.TOOL_ICON).toBe('school');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ApprentissageRuptureSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 (nominal) with both dates + motif', () => {
    expect(ApprentissageRuptureSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2026-01-06', dateRuptureContrat: '2026-04-06', apprentissageMotifRupture: 'FAUTE_GRAVE' },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ApprentissageRuptureSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2026-01-06', dateRuptureContrat: '2026-04-06', apprentissageMotifRupture: 'FAUTE_GRAVE' },
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
    httpMock.expectOne(BASE_URL).flush(reguliereResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutContrat()).toBe('2026-01-06');
    expect(component.dateRupture()).toBe('2026-01-26');
    expect(component.motifRupture()).toBe('SANS_MOTIF');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires both dates + motif, and rejects rupture < début', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutContrat.set('2026-01-06');
    component.dateRupture.set('2026-01-26');
    expect(component.formValid()).toBe(false); // motif manquant
    component.motifRupture.set('SANS_MOTIF');
    expect(component.formValid()).toBe(true);
    component.dateRupture.set('2026-01-01'); // antérieure
    expect(component.formValid()).toBe(false);
  });

  it('joursDepuisDebutLocal computes the gap between the two dates', () => {
    component.dateDebutContrat.set('2026-01-06');
    component.dateRupture.set('2026-01-26');
    expect(component.joursDepuisDebutLocal()).toBe(20);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when dateRupture < dateDebut', () => {
    component.dateDebutContrat.set('2026-05-06');
    component.dateRupture.set('2026-01-06');
    expect(component.coherenceAlerts().some(a => a.includes('antérieure'))).toBe(true);
  });

  it('raises a coherence alert when SANS_MOTIF after 45 days', () => {
    component.dateDebutContrat.set('2026-01-06');
    component.dateRupture.set('2026-04-06'); // ~90 j
    component.motifRupture.set('SANS_MOTIF');
    expect(component.coherenceAlerts().some(a => a.includes('45 jours'))).toBe(true);
  });

  it('raises a coherence alert for FAUTE_GRAVE (procédure de licenciement)', () => {
    component.motifRupture.set('FAUTE_GRAVE');
    expect(component.coherenceAlerts().some(a => a.includes('procédure de licenciement'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutContrat.set('2026-01-06');
    component.dateRupture.set('2026-01-26');
    component.auteurRupture.set('EMPLOYEUR');
    component.motifRupture.set('SANS_MOTIF');
    component.apprentiMajeur.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDebutContrat: '2026-01-06',
      dateRupture: '2026-01-26',
      auteurRupture: 'EMPLOYEUR',
      motifRupture: 'SANS_MOTIF',
      apprentiMajeur: true,
    });
    req.flush(reguliereResponse());
    expect(component.result()!.verdictGlobal).toBe('RUPTURE_REGULIERE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutContrat.set('2026-01-06');
    component.dateRupture.set('2026-01-26');
    component.motifRupture.set('SANS_MOTIF');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : verdict + période + validité + conséquences ---

  it('RUPTURE_REGULIERE J+20 SANS_MOTIF -> success chip + période DANS_45 + validité VALIDE', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(reguliereResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('régulière');
    expect(chip.className).toContain('is-chip--success');
    const periode = el.querySelector('[data-testid="periode-chip"]')!;
    expect(periode.textContent).toContain('45 premiers jours');
    const validite = el.querySelector('[data-testid="validite-chip"]')!;
    expect(validite.textContent).toContain('valide');
    expect(validite.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="jours-value"]')!.textContent).toContain('20');
    expect(el.querySelector('[data-testid="cph-box"]')).toBeNull();
  });

  it('RUPTURE_IRREGULIERE J+90 SANS_MOTIF -> danger chip + après 45 j + CPH box + conséquences', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(irreguliereResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('irrégulière');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="periode-chip"]')!.textContent).toContain('Après');
    const validite = el.querySelector('[data-testid="validite-chip"]')!;
    expect(validite.className).toContain('is-chip--danger');
    const cph = el.querySelector('[data-testid="cph-box"]');
    expect(cph).not.toBeNull();
    expect(cph!.textContent).toContain('prud\'hommes');
    const conseq = el.querySelectorAll('[data-testid="consequences-list"] .is-critere');
    expect(conseq.length).toBe(2);
  });

  it('RUPTURE_A_SECURISER J+90 FAUTE_GRAVE -> warning chip + validité A_SECURISER + no CPH box', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(aSecuriserResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('sécuriser');
    expect(chip.className).toContain('is-chip--warning');
    const validite = el.querySelector('[data-testid="validite-chip"]')!;
    expect(validite.textContent).toContain('sécuriser');
    expect(validite.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="cph-box"]')).toBeNull();
  });

  it('verdict / validité / période chip classes map their states', () => {
    expect(component.verdictChipClass('RUPTURE_REGULIERE')).toContain('success');
    expect(component.verdictChipClass('RUPTURE_A_SECURISER')).toContain('warning');
    expect(component.verdictChipClass('RUPTURE_IRREGULIERE')).toContain('danger');
    expect(component.validiteChipClass('VALIDE')).toContain('success');
    expect(component.validiteChipClass('A_SECURISER')).toContain('warning');
    expect(component.validiteChipClass('NON_VALIDE')).toContain('danger');
    expect(component.periodeChipClass('DANS_45_PREMIERS_JOURS')).toContain('neutral');
    expect(component.periodeChipClass('APRES_45_JOURS')).toContain('navy');
  });

  it('seuil note (45 jours) present in the form', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    const note = fixture.nativeElement.querySelector('[data-testid="seuil-note"]');
    expect(note).not.toBeNull();
    expect(note.textContent).toContain('45 jours');
  });

  // --- pré-fill IA ---

  it('pré-fills dateDebutContrat, dateRupture and motifRupture from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateEntree: '2026-01-06',
      dateRuptureContrat: '2026-04-06',
      apprentissageMotifRupture: 'INAPTITUDE',
    } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateDebutContrat()).toBe('2026-01-06');
    expect(component.dateRupture()).toBe('2026-04-06');
    expect(component.motifRupture()).toBe('INAPTITUDE');
    expect(component.provenanceDateDebut()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData = { dateEntree: '2026-02-01' } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateDebutContrat()).toBe('2026-02-01');
    expect(component.provenanceDateDebut()).toBe('IA');
  });

  it('onDateDebutChange clears provenance', () => {
    component.provenanceDateDebut.set('IA');
    component.onDateDebutChange('2026-03-01');
    expect(component.dateDebutContrat()).toBe('2026-03-01');
    expect(component.provenanceDateDebut()).toBeNull();
  });

  it('onMotifChange clears provenance', () => {
    component.provenanceMotif.set('IA');
    component.onMotifChange('ACCORD_PARTIES');
    expect(component.motifRupture()).toBe('ACCORD_PARTIES');
    expect(component.provenanceMotif()).toBeNull();
  });
});

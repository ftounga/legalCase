import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CadreDirigeantStatutSectionComponent } from './cadre-dirigeant-statut-section.component';
import { CadreDirigeantStatutResponse } from '../../core/models/cadre-dirigeant-statut.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('CadreDirigeantStatutSectionComponent', () => {
  let component: CadreDirigeantStatutSectionComponent;
  let fixture: ComponentFixture<CadreDirigeantStatutSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/cadre-dirigeant-statut-analysis';

  function cadreDirigeantResponse(overrides: Partial<CadreDirigeantStatutResponse> = {}): CadreDirigeantStatutResponse {
    return {
      caseFileId: 'case-1',
      independanceEmploiDuTemps: true,
      autonomieDecision: true,
      remunerationParmiPlusElevees: true,
      participationDirectionEntreprise: true,
      niveauRemunerationConstate: 9000,
      criteres: [
        { critere: 'Indépendance dans l\'organisation de l\'emploi du temps', rempli: true, commentaire: 'Établie' },
        { critere: 'Autonomie décisionnelle', rempli: true, commentaire: '' },
        { critere: 'Rémunération parmi les plus élevées', rempli: true, commentaire: '' },
      ],
      criteresRemplis: 3,
      qualification: 'CADRE_DIRIGEANT',
      exclusionDureeTravail: 'EXCLU',
      risqueRappelHeuresSupp: 'FAIBLE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.3111-2 CT ; Cass. soc. (à vérifier par avocat)',
      ...overrides,
    };
  }

  function fragileResponse(): CadreDirigeantStatutResponse {
    return cadreDirigeantResponse({
      participationDirectionEntreprise: false,
      qualification: 'CADRE_DIRIGEANT_FRAGILE',
      risqueRappelHeuresSupp: 'MODERE',
    });
  }

  function nonCadreResponse(): CadreDirigeantStatutResponse {
    return cadreDirigeantResponse({
      remunerationParmiPlusElevees: false,
      criteres: [
        { critere: 'Indépendance dans l\'organisation de l\'emploi du temps', rempli: true, commentaire: '' },
        { critere: 'Autonomie décisionnelle', rempli: true, commentaire: '' },
        { critere: 'Rémunération parmi les plus élevées', rempli: false, commentaire: 'Non établi' },
      ],
      criteresRemplis: 2,
      qualification: 'NON_CADRE_DIRIGEANT',
      exclusionDureeTravail: 'NON_EXCLU',
      risqueRappelHeuresSupp: 'ELEVE',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [CadreDirigeantStatutSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CadreDirigeantStatutSectionComponent);
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
    expect(CadreDirigeantStatutSectionComponent.TOOL_LABEL).toContain('CADRE DIRIGEANT');
    expect(CadreDirigeantStatutSectionComponent.TOOL_ICON).toBe('workspace_premium');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CadreDirigeantStatutSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with the two fields', () => {
    expect(CadreDirigeantStatutSectionComponent.getPrefillCount({
      aiData: { cadreParticipationDirection: true, salaireBrutMensuel: 9000 },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(CadreDirigeantStatutSectionComponent.getPrefillCount({
      aiData: { cadreParticipationDirection: true, salaireBrutMensuel: 9000 },
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
    httpMock.expectOne(BASE_URL).flush(cadreDirigeantResponse());
    expect(component.result()!.qualification).toBe('CADRE_DIRIGEANT');
    expect(component.showForm()).toBe(false);
    expect(component.independanceEmploiDuTemps()).toBe(true);
    expect(component.participationDirectionEntreprise()).toBe(true);
    expect(component.niveauRemunerationConstate()).toBe(9000);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form / criteria counting ---

  it('criteresRemplisLocal counts the three cumulative criteria checkboxes', () => {
    component.independanceEmploiDuTemps.set(true);
    component.autonomieDecision.set(true);
    component.remunerationParmiPlusElevees.set(false);
    expect(component.criteresRemplisLocal()).toBe(2);
    component.remunerationParmiPlusElevees.set(true);
    expect(component.criteresRemplisLocal()).toBe(3);
  });

  it('formValid rejects a negative niveauRemuneration', () => {
    expect(component.formValid()).toBe(true);
    component.niveauRemunerationConstate.set(-1);
    expect(component.formValid()).toBe(false);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when 3 critères but participation effective absente', () => {
    component.independanceEmploiDuTemps.set(true);
    component.autonomieDecision.set(true);
    component.remunerationParmiPlusElevees.set(true);
    component.participationDirectionEntreprise.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('participation effective'))).toBe(true);
  });

  it('raises a coherence alert when niveau saisi sans cocher le critère 3', () => {
    component.remunerationParmiPlusElevees.set(false);
    component.niveauRemunerationConstate.set(8000);
    expect(component.coherenceAlerts().some(a => a.includes('rémunération'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.independanceEmploiDuTemps.set(true);
    component.autonomieDecision.set(true);
    component.remunerationParmiPlusElevees.set(true);
    component.participationDirectionEntreprise.set(true);
    component.niveauRemunerationConstate.set(9000);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      independanceEmploiDuTemps: true,
      autonomieDecision: true,
      remunerationParmiPlusElevees: true,
      participationDirectionEntreprise: true,
      niveauRemunerationConstate: 9000,
    });
    req.flush(cadreDirigeantResponse());
    expect(component.result()!.qualification).toBe('CADRE_DIRIGEANT');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 qualification states + risque + 3 critères ---

  it('CADRE_DIRIGEANT -> success qualification chip + EXCLU + risque FAIBLE + 3 critères ✓', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(cadreDirigeantResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="qualification-chip"]')!;
    expect(chip.textContent).toContain('Cadre dirigeant');
    expect(chip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="exclusion-chip"]')!.textContent).toContain('Exclu');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('faible');
    expect(el.querySelector('[data-testid="criteres-remplis"]')!.textContent).toContain('3/3');
    const items = el.querySelectorAll('[data-testid="criteres-list"] .is-critere');
    expect(items.length).toBe(3);
    expect(el.querySelector('[data-testid="risque-note"]')!.textContent).toContain('exclu des dispositions');
  });

  it('CADRE_DIRIGEANT_FRAGILE -> warning chip + risque MODERE', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(fragileResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="qualification-chip"]')!;
    expect(chip.textContent).toContain('fragile');
    expect(chip.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('modéré');
  });

  it('NON_CADRE_DIRIGEANT -> danger chip + NON_EXCLU + risque ELEVE + critère ✗ rendu', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonCadreResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="qualification-chip"]')!;
    expect(chip.textContent).toContain('Non cadre dirigeant');
    expect(chip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="exclusion-chip"]')!.textContent).toContain('Soumis');
    expect(el.querySelector('[data-testid="risque-chip"]')!.textContent).toContain('élevé');
    expect(el.querySelector('[data-testid="criteres-remplis"]')!.textContent).toContain('2/3');
    const koItems = el.querySelectorAll('[data-testid="criteres-list"] .is-critere--ko');
    expect(koItems.length).toBe(1);
  });

  it('qualification / exclusion / risque chip classes map their states', () => {
    expect(component.qualificationChipClass('CADRE_DIRIGEANT')).toContain('success');
    expect(component.qualificationChipClass('CADRE_DIRIGEANT_FRAGILE')).toContain('warning');
    expect(component.qualificationChipClass('NON_CADRE_DIRIGEANT')).toContain('danger');
    expect(component.exclusionChipClass('EXCLU')).toContain('success');
    expect(component.exclusionChipClass('NON_EXCLU')).toContain('danger');
    expect(component.risqueChipClass('FAIBLE')).toContain('success');
    expect(component.risqueChipClass('MODERE')).toContain('warning');
    expect(component.risqueChipClass('ELEVE')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills participation and niveauRemuneration from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      cadreParticipationDirection: true,
      salaireBrutMensuel: 12000,
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.participationDirectionEntreprise()).toBe(true);
    expect(component.niveauRemunerationConstate()).toBe(12000);
    expect(component.provenanceParticipation()).toBe('IA');
    expect(component.provenanceNiveauRemuneration()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { salaireBrutMensuel: 8500 };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.niveauRemunerationConstate()).toBe(8500);
    expect(component.provenanceNiveauRemuneration()).toBe('IA');
  });

  it('onNiveauRemunerationChange clears provenance', () => {
    component.provenanceNiveauRemuneration.set('IA');
    component.onNiveauRemunerationChange(7000);
    expect(component.niveauRemunerationConstate()).toBe(7000);
    expect(component.provenanceNiveauRemuneration()).toBeNull();
  });

  it('onParticipationChange clears provenance', () => {
    component.provenanceParticipation.set('IA');
    component.onParticipationChange(false);
    expect(component.participationDirectionEntreprise()).toBe(false);
    expect(component.provenanceParticipation()).toBeNull();
  });
});

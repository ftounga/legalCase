import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConciliationCphBcaSectionComponent } from './conciliation-cph-bca-section.component';
import { ConciliationCphBcaResponse } from '../../core/models/conciliation-cph-bca.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-212-38 — F-212 19/19. Tests du composant Conciliation CPH BCA.
 */
describe('ConciliationCphBcaSectionComponent', () => {
  let component: ConciliationCphBcaSectionComponent;
  let fixture: ComponentFixture<ConciliationCphBcaSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conciliation-cph-bca';

  /** Fixture IA complète — les 3 champs `conciliationCph*`. */
  const FULL_AI_DATA: TravailExtractedData = {
    conciliationCphAncienneteMois: 36,
    conciliationCphSalaire: 2500,
    conciliationCphMontantDemandes: 30000,
  };

  function response(montantBca = 7500, palier = '2 à 8 ans : 3 mois'): ConciliationCphBcaResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs (ré-édition du formulaire).
      ancienneteMois: 36,
      salaireMensuelBrutEuros: 2500,
      montantDemandesEuros: 30000,
      opportuniteConciliation: 'FAVORABLE',
      // Sorties calculées.
      montantMinimumBcaEuros: montantBca,
      palierBcaApplicable: palier,
      comparaisonBcaVsMacron: 'Le BCA minimum (7 500,00 €) est inférieur au montant des demandes (30 000,00 €).',
      checklistBco: [
        'Requête introductive (formulaire CERFA n°15586*10) — R. 1452-1 CT',
        'Bordereau de pièces communiquées — R. 1453-3 CT',
        'Contrat de travail + avenants',
      ],
      basesJuridiques: ['L. 1235-1 al. 3 CT', 'R. 1454-7 à R. 1454-12 CT', 'L. 1411-1 CT'],
      messages: ['Analyse calculée.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-25T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        ConciliationCphBcaSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConciliationCphBcaSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET) et gate pays
  // ---------------------------------------------------------------------------

  it('mount FRANCE → GET initial déclenché', () => {
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
  });

  it('mount BELGIQUE → pas d\'appel HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('BELGIQUE → bannière info pays affichée', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cph-country-banner"]');
    expect(banner).toBeTruthy();
  });

  // ---------------------------------------------------------------------------
  // Pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('pré-remplit 3 champs depuis aiData (FRANCE)', () => {
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    expect(component.ancienneteMois()).toBe(36);
    expect(component.salaireMensuelBrutEuros()).toBe(2500);
    expect(component.montantDemandesEuros()).toBe(30000);
    expect(component.provenanceAnciennete()).toBe('IA');
    expect(component.provenanceSalaire()).toBe('IA');
    expect(component.provenanceMontantDemandes()).toBe('IA');
  });

  it('ne pré-remplit pas si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    component.aiData = FULL_AI_DATA;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);

    expect(component.ancienneteMois()).toBe(0);
    expect(component.salaireMensuelBrutEuros()).toBe(0);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('getPrefillCount = 3 sur fixture IA FR complète', () => {
    expect(ConciliationCphBcaSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount = 0 hors France', () => {
    expect(ConciliationCphBcaSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('getPrefillCount = 0 sans aiData', () => {
    expect(ConciliationCphBcaSectionComponent.getPrefillCount({
      aiData: null,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST) — palier BCA + checklist
  // ---------------------------------------------------------------------------

  it('POST → palier BCA + montant minimum + refresh dashboard', () => {
    component.ancienneteMois.set(36);
    component.salaireMensuelBrutEuros.set(2500);
    component.montantDemandesEuros.set(30000);
    component.calculate();

    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.ancienneteMois).toBe(36);
    req.flush(response());

    expect(component.result()?.palierBcaApplicable).toBe('2 à 8 ans : 3 mois');
    expect(component.result()?.montantMinimumBcaEuros).toBe(7500);
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('Encadré palier BCA + montant affiché après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response());
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('[data-testid="cph-bca-banner"]');
    expect(banner).toBeTruthy();
    const palier = fixture.nativeElement.querySelector('[data-testid="cph-bca-palier"]');
    expect(palier.textContent).toContain('2 à 8 ans');
    const montant = fixture.nativeElement.querySelector('[data-testid="cph-bca-montant"]');
    expect(montant.textContent).toContain('7');
  });

  it('Comparaison BCA vs Macron affichée', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response());
    fixture.detectChanges();

    const comp = fixture.nativeElement.querySelector('[data-testid="cph-comparaison"]');
    expect(comp).toBeTruthy();
    expect(comp.textContent).toContain('inférieur');
  });

  it('Checklist BCO non vide après calcul', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(null, { status: 204, statusText: 'No Content' });

    component.calculate();
    httpMock.expectOne(BASE_URL).flush(response());
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('[data-testid="cph-checklist-item"]');
    expect(items.length).toBeGreaterThan(0);
  });

  // ---------------------------------------------------------------------------
  // formValid
  // ---------------------------------------------------------------------------

  it('formValid = false si workspaceCountry !== FRANCE', () => {
    component.workspaceCountry = 'BELGIQUE' as 'FRANCE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid = false si ancienneté < 0', () => {
    component.ancienneteMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid = true sur baseline FR', () => {
    expect(component.formValid()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Handlers — effacement du badge IA sur modif manuelle
  // ---------------------------------------------------------------------------

  it('onAncienneteChange efface le badge IA', () => {
    component.provenanceAnciennete.set('IA');
    component.onAncienneteChange(48);
    expect(component.provenanceAnciennete()).toBeNull();
  });

  it('onSalaireChange efface le badge IA', () => {
    component.provenanceSalaire.set('IA');
    component.onSalaireChange(3000);
    expect(component.provenanceSalaire()).toBeNull();
  });

  it('onMontantDemandesChange efface le badge IA', () => {
    component.provenanceMontantDemandes.set('IA');
    component.onMontantDemandesChange(50000);
    expect(component.provenanceMontantDemandes()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Helpers UI
  // ---------------------------------------------------------------------------

  it('formatEuros formate un montant en € fr-FR', () => {
    const out = component.formatEuros(1234.56);
    expect(out).toContain('1');
    expect(out).toContain('234');
    expect(out).toContain('56');
  });

  it('formatEuros — null → tiret', () => {
    expect(component.formatEuros(null)).toBe('—');
  });
});

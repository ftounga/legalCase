import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CongesPayesArretMaladieSectionComponent } from './conges-payes-arret-maladie-section.component';
import { CongesPayesArretMaladieResponse } from '../../core/models/conges-payes-arret-maladie.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('CongesPayesArretMaladieSectionComponent', () => {
  let component: CongesPayesArretMaladieSectionComponent;
  let fixture: ComponentFixture<CongesPayesArretMaladieSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/conges-payes-arret-maladie';

  /** SF-206-04 : fixture IA complète — les 5 champs cpArretMaladie* + salaireBrutMensuel. */
  const FULL_AI_DATA: TravailExtractedData = {
    cpArretMaladieType: 'MALADIE_NON_PROFESSIONNELLE',
    cpArretMaladieNombreMois: 18,
    cpArretMaladieSalarieEnPoste: true,
    cpArretMaladieDateRupture: null,
    cpArretMaladieJoursDejaAccordes: 0,
    salaireBrutMensuel: 2400,
  };

  function response(verdict: CongesPayesArretMaladieResponse['verdict'])
      : CongesPayesArretMaladieResponse {
    return {
      caseFileId: 'case-1',
      // Snapshot d'inputs ré-exposé par la réponse (ré-édition du formulaire).
      typeArret: 'MALADIE_NON_PROFESSIONNELLE',
      nombreMoisArret: 18,
      salarieEncoreEnPoste: true,
      dateRuptureContrat: null,
      joursCpDejaAccordes: 0,
      salaireBrutMensuel: 2400,
      // Champs calculés.
      verdict,
      joursCpAcquis: verdict === 'PAS_DE_RAPPEL' ? 0 : 24,
      joursCpRappel: verdict === 'PAS_DE_RAPPEL' ? 0
                   : verdict === 'RAPPEL_LIMITE' ? 3
                   : 24,
      valorisationIndicativeEur: verdict === 'PAS_DE_RAPPEL' ? 0
                                 : verdict === 'RAPPEL_LIMITE' ? 360
                                 : 2880,
      dateLimiteAction: verdict === 'ACTION_FORCLOSE' ? '2024-04-24' : '2026-04-24',
      actionEncoreOuverte: verdict !== 'ACTION_FORCLOSE',
      basesJuridiques: ['Art. L.3141-5 C. trav.', 'Art. L.3141-5-1 C. trav.', 'Loi 22/04/2024 art. 37'],
      messages: ['Délai d\'action : 24/04/2026 pour la période antérieure à la loi.'],
      country: 'FRANCE',
      calculatedAt: '2026-05-20T10:00:00Z',
    };
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [
        CongesPayesArretMaladieSectionComponent,
        HttpClientTestingModule,
        NoopAnimationsModule,
      ],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CongesPayesArretMaladieSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // F-JU-03 SF-JU-03-100 : le bloc <app-tool-jurisprudence-citations> émet un
    // GET citations dès qu'un résultat est rendu (hors standalone). On le flush
    // avant verify() pour ne pas faire échouer les tests de rendu de résultat.
    httpMock.match((r) => r.url.includes('jurisprudence-citations')).forEach((r) => r.flush([]));
    httpMock.verify();
  });

  // ---------------------------------------------------------------------------
  // Chargement (GET)
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

  it('GET 200 → result rechargé + showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(response('RAPPEL_LIMITE'));
    expect(component.result()!.verdict).toBe('RAPPEL_LIMITE');
    expect(component.showForm()).toBe(false);
  });

  it('GET 200 → formulaire ré-hydraté depuis le snapshot d\'inputs (ré-édition)', () => {
    component.ngOnInit();
    const r = response('RAPPEL_SIGNIFICATIF');
    r.nombreMoisArret = 20;
    r.typeArret = 'ACCIDENT_TRAVAIL_MALADIE_PRO';
    r.salarieEncoreEnPoste = false;
    r.dateRuptureContrat = '2025-12-31';
    r.joursCpDejaAccordes = 5;
    r.salaireBrutMensuel = 2800;
    httpMock.expectOne(BASE_URL).flush(r);

    expect(component.nombreMoisArret()).toBe(20);
    expect(component.typeArret()).toBe('ACCIDENT_TRAVAIL_MALADIE_PRO');
    expect(component.salarieEncoreEnPoste()).toBe(false);
    expect(component.dateRuptureContrat()).toBe('2025-12-31');
    expect(component.joursCpDejaAccordes()).toBe(5);
    expect(component.salaireBrutMensuel()).toBe(2800);

    component.editMode();
    expect(component.showForm()).toBe(true);
    expect(component.nombreMoisArret()).toBe(20);
  });

  it('GET 404 → reste en mode formulaire, pas de result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Calcul (POST)
  // ---------------------------------------------------------------------------

  it('calculate() POST → payload conforme au contrat + result + snackbar + triggerRefresh', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.typeArret.set('MALADIE_NON_PROFESSIONNELLE');
    component.nombreMoisArret.set(18);
    component.salarieEncoreEnPoste.set(true);
    component.dateRuptureContrat.set(null);
    component.joursCpDejaAccordes.set(0);
    component.salaireBrutMensuel.set(2400);
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeArret: 'MALADIE_NON_PROFESSIONNELLE',
      nombreMoisArret: 18,
      salarieEncoreEnPoste: true,
      dateRuptureContrat: null,
      joursCpDejaAccordes: 0,
      salaireBrutMensuel: 2400,
    });
    req.flush(response('RAPPEL_SIGNIFICATIF'));

    expect(component.result()!.verdict).toBe('RAPPEL_SIGNIFICATIF');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalledWith(
      'Rappel de congés payés calculé', 'OK', jasmine.any(Object));
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('calculate() — dateRuptureContrat null forcé si salarié en poste (champ ignoré)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.salarieEncoreEnPoste.set(true);
    component.dateRuptureContrat.set('2025-12-31'); // valeur résiduelle ignorée
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.salarieEncoreEnPoste).toBe(true);
    expect(req.request.body.dateRuptureContrat).toBeNull();
    req.flush(response('RAPPEL_LIMITE'));
  });

  it('calculate() — dateRuptureContrat transmise si salarié sorti', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.salarieEncoreEnPoste.set(false);
    component.dateRuptureContrat.set('2025-06-30');
    component.calculate();

    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.salarieEncoreEnPoste).toBe(false);
    expect(req.request.body.dateRuptureContrat).toBe('2025-06-30');
    req.flush(response('RAPPEL_SIGNIFICATIF'));
  });

  it('calculate() erreur backend → snackbar rouge, formulaire conservé', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    component.calculate();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad Request' });

    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
    expect(component.calculating()).toBe(false);
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // Verdicts — 4 niveaux (orange RAPPEL_SIGNIFICATIF, rouge ACTION_FORCLOSE)
  // ---------------------------------------------------------------------------

  it('verdict RAPPEL_SIGNIFICATIF → banner orange --significant', () => {
    expect(component.verdictBannerClass('RAPPEL_SIGNIFICATIF')).toContain('--significant');
    expect(component.verdictBannerClass('RAPPEL_SIGNIFICATIF')).not.toContain('--danger');
    expect(component.verdictBannerLabel('RAPPEL_SIGNIFICATIF')).toContain('significatif');
    expect(component.verdictBannerIcon('RAPPEL_SIGNIFICATIF')).toBe('priority_high');
  });

  it('verdict RAPPEL_LIMITE → banner navy --limited', () => {
    expect(component.verdictBannerClass('RAPPEL_LIMITE')).toContain('--limited');
    expect(component.verdictBannerClass('RAPPEL_LIMITE')).not.toContain('--danger');
    expect(component.verdictBannerLabel('RAPPEL_LIMITE')).toContain('limité');
    expect(component.verdictBannerIcon('RAPPEL_LIMITE')).toBe('check_circle');
  });

  it('verdict PAS_DE_RAPPEL → banner navy grisé --none', () => {
    expect(component.verdictBannerClass('PAS_DE_RAPPEL')).toContain('--none');
    expect(component.verdictBannerClass('PAS_DE_RAPPEL')).not.toContain('--danger');
    expect(component.verdictBannerLabel('PAS_DE_RAPPEL')).toContain('Pas de rappel');
    expect(component.verdictBannerIcon('PAS_DE_RAPPEL')).toBe('info_outline');
  });

  it('verdict ACTION_FORCLOSE → banner rouge --danger', () => {
    expect(component.verdictBannerClass('ACTION_FORCLOSE')).toContain('--danger');
    expect(component.verdictBannerLabel('ACTION_FORCLOSE')).toContain('forclose');
    expect(component.verdictBannerIcon('ACTION_FORCLOSE')).toBe('error');
  });

  it('rendu résultat ACTION_FORCLOSE → bandeau d\'alerte explicite affiché', () => {
    component.collapsed.set(false);
    fixture.detectChanges(); // déclenche ngOnInit() → GET
    httpMock.expectOne(BASE_URL).flush(response('ACTION_FORCLOSE'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cpm-forclose-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('forclose');
    expect(banner.textContent).toContain('2024-04-24');
  });

  it('rendu résultat action ouverte → pas de bandeau forclose', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('RAPPEL_SIGNIFICATIF'));
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cpm-forclose-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Bannière BELGIQUE
  // ---------------------------------------------------------------------------

  it('bannière info affichée si workspace BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cpm-country-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('France uniquement');
  });

  it('pas de bannière BE si workspace FRANCE', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="cpm-country-banner"]');
    expect(banner).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // SF-206-04 — pré-fill IA + getPrefillCount
  // ---------------------------------------------------------------------------

  it('getPrefillCount() cas 0 — input vide retourne 0', () => {
    expect(CongesPayesArretMaladieSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('getPrefillCount() cas partiel — 3 champs renseignés retourne 3', () => {
    expect(CongesPayesArretMaladieSectionComponent.getPrefillCount({
      aiData: {
        cpArretMaladieType: 'MALADIE_NON_PROFESSIONNELLE',
        cpArretMaladieNombreMois: 12,
        salaireBrutMensuel: 2200,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('getPrefillCount() cas nominal — 6 champs (5 cp* + salaire) retourne 6', () => {
    expect(CongesPayesArretMaladieSectionComponent.getPrefillCount({
      aiData: {
        cpArretMaladieType: 'ACCIDENT_TRAVAIL_MALADIE_PRO',
        cpArretMaladieNombreMois: 18,
        cpArretMaladieSalarieEnPoste: false,
        cpArretMaladieDateRupture: '2025-12-31',
        cpArretMaladieJoursDejaAccordes: 2,
        salaireBrutMensuel: 2400,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(6);
  });

  it('getPrefillCount() BELGIQUE retourne 0', () => {
    expect(CongesPayesArretMaladieSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('prefillFromAi : 5 champs renseignés + badges de provenance présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.typeArret()).toBe('MALADIE_NON_PROFESSIONNELLE');
    expect(component.nombreMoisArret()).toBe(18);
    expect(component.salarieEncoreEnPoste()).toBe(true);
    expect(component.joursCpDejaAccordes()).toBe(0);
    expect(component.salaireBrutMensuel()).toBe(2400);
    // Badges de provenance des 5 champs renseignés (dateRupture est null donc pas renseigné).
    expect(component.provenanceTypeArret()).toBe('IA');
    expect(component.provenanceNombreMoisArret()).toBe('IA');
    expect(component.provenanceSalarieEnPoste()).toBe('IA');
    expect(component.provenanceJoursDejaAccordes()).toBe('IA');
    expect(component.provenanceSalaireBrutMensuel()).toBe('IA');
    // dateRupture absente du fixture (salarié en poste) — pas de badge.
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('prefillFromAi : dateRupture renseignée si salarié sorti', () => {
    component.aiData = {
      ...FULL_AI_DATA,
      cpArretMaladieSalarieEnPoste: false,
      cpArretMaladieDateRupture: '2025-06-30',
    };
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    expect(component.salarieEncoreEnPoste()).toBe(false);
    expect(component.dateRuptureContrat()).toBe('2025-06-30');
    expect(component.provenanceDateRupture()).toBe('IA');
  });

  it('prefillFromAi : parité stricte getPrefillCount ↔ badges IA présents', () => {
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });

    const count = CongesPayesArretMaladieSectionComponent.getPrefillCount({
      aiData: FULL_AI_DATA,
      workspaceCountry: 'FRANCE',
    });
    const badged = [
      component.provenanceTypeArret(),
      component.provenanceNombreMoisArret(),
      component.provenanceSalarieEnPoste(),
      component.provenanceDateRupture(),
      component.provenanceJoursDejaAccordes(),
      component.provenanceSalaireBrutMensuel(),
    ].filter((p) => p === 'IA').length;
    expect(badged).toBe(count);
  });

  it('prefillFromAi : no-op gracieux si aiData absent', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(component.provenanceTypeArret()).toBeNull();
    expect(component.provenanceNombreMoisArret()).toBeNull();
  });

  it('prefillFromAi : dossier BELGIQUE → aucun champ pré-rempli', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = FULL_AI_DATA;
    component.ngOnInit();
    // Pas de GET en BE (outil FR-only).
    expect(component.provenanceTypeArret()).toBeNull();
    expect(component.provenanceNombreMoisArret()).toBeNull();
    expect(component.provenanceSalaireBrutMensuel()).toBeNull();
  });

  it('onTypeArretChange efface le badge IA', () => {
    component.provenanceTypeArret.set('IA');
    component.onTypeArretChange('ACCIDENT_TRAVAIL_MALADIE_PRO');
    expect(component.typeArret()).toBe('ACCIDENT_TRAVAIL_MALADIE_PRO');
    expect(component.provenanceTypeArret()).toBeNull();
  });

  it('onSalarieEnPosteChange(true) reset date rupture', () => {
    component.salarieEncoreEnPoste.set(false);
    component.dateRuptureContrat.set('2025-12-31');
    component.provenanceDateRupture.set('IA');
    component.onSalarieEnPosteChange(true);
    expect(component.salarieEncoreEnPoste()).toBe(true);
    expect(component.dateRuptureContrat()).toBeNull();
    expect(component.provenanceDateRupture()).toBeNull();
  });

  it('onJoursDejaAccordesChange efface le badge IA + clamp ≥ 0', () => {
    component.provenanceJoursDejaAccordes.set('IA');
    component.onJoursDejaAccordesChange(-5);
    expect(component.joursCpDejaAccordes()).toBe(0);
    expect(component.provenanceJoursDejaAccordes()).toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Champ conditionnel dateRuptureContrat (rendu)
  // ---------------------------------------------------------------------------

  it('champ dateRuptureContrat masqué si salarieEncoreEnPoste=true', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.salarieEncoreEnPoste.set(true);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input[name="dateRuptureContrat"]');
    expect(input).toBeNull();
  });

  it('champ dateRuptureContrat affiché si salarieEncoreEnPoste=false', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.salarieEncoreEnPoste.set(false);
    fixture.detectChanges();
    const input = fixture.nativeElement.querySelector('input[name="dateRuptureContrat"]');
    expect(input).not.toBeNull();
  });

  // ---------------------------------------------------------------------------
  // Badges auto_awesome dans le DOM
  // ---------------------------------------------------------------------------

  it('rendu : badges auto_awesome visibles pour champs pré-remplis', () => {
    component.aiData = FULL_AI_DATA;
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    const provenanceNotes = fixture.nativeElement.querySelectorAll('.cpm-provenance-note');
    expect(provenanceNotes.length).toBeGreaterThanOrEqual(5);
    // Au moins un badge contient l'icône auto_awesome.
    const icon = fixture.nativeElement.querySelector('.cpm-provenance-note mat-icon');
    expect(icon?.textContent?.trim()).toBe('auto_awesome');
  });

  // ---------------------------------------------------------------------------
  // Métadonnées statiques (TOOL_LABEL / TOOL_ICON)
  // ---------------------------------------------------------------------------

  it('expose TOOL_LABEL et TOOL_ICON statiques', () => {
    expect(CongesPayesArretMaladieSectionComponent.TOOL_LABEL).toContain('CONGÉS PAYÉS');
    expect(CongesPayesArretMaladieSectionComponent.TOOL_ICON).toBe('event_available');
  });

  // ---------------------------------------------------------------------------
  // Rendu du résultat — échéance inter-onglets vers Suivi
  // ---------------------------------------------------------------------------

  it('rendu résultat : mention « onglet Suivi » présente avec date limite d\'action', () => {
    component.collapsed.set(false);
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(response('RAPPEL_LIMITE'));
    fixture.detectChanges();
    const node = fixture.nativeElement.querySelector('[data-testid="cpm-deadline-suivi-note"]');
    expect(node).not.toBeNull();
    expect(node.textContent).toContain('Suivi');
    expect(node.textContent).toContain('2026-04-24');
  });

  // ---------------------------------------------------------------------------
  // Validité du formulaire (gate isFrance + nombre mois > 0 + dateRupture conditionnelle)
  // ---------------------------------------------------------------------------

  it('formValid() false en BELGIQUE', () => {
    component.workspaceCountry = 'BELGIQUE';
    expect(component.formValid()).toBe(false);
  });

  it('formValid() true en FRANCE avec valeurs par défaut (12 mois, salarié en poste)', () => {
    expect(component.formValid()).toBe(true);
  });

  it('formValid() false si salarié sorti sans date de rupture', () => {
    component.salarieEncoreEnPoste.set(false);
    component.dateRuptureContrat.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('formValid() true si salarié sorti avec date de rupture', () => {
    component.salarieEncoreEnPoste.set(false);
    component.dateRuptureContrat.set('2025-06-30');
    expect(component.formValid()).toBe(true);
  });

  it('formValid() false si nombreMoisArret = 0', () => {
    component.nombreMoisArret.set(0);
    expect(component.formValid()).toBe(false);
  });

  it('toggleCollapse fonctionne', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode ré-affiche le form après calcul', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // coherenceAlerts F-IA-03 — masquage standalone / après calcul
  // ---------------------------------------------------------------------------

  it('coherenceAlerts vide en standaloneMode', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });

  it('coherenceAlerts masquées après calcul (showForm=false)', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'Not Found' });
    component.showForm.set(false);
    expect(Object.keys(component.coherenceAlerts())).toHaveLength(0);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { QuotaErrorBannerComponent } from './quota-error-banner.component';
import { QuotaErrorStateService } from '../../core/services/quota-error-state.service';

describe('QuotaErrorBannerComponent', () => {
  let fixture: ComponentFixture<QuotaErrorBannerComponent>;
  let component: QuotaErrorBannerComponent;
  let stateService: QuotaErrorStateService;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QuotaErrorBannerComponent],
      providers: [
        provideRouter([]),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuotaErrorBannerComponent);
    component = fixture.componentInstance;
    stateService = TestBed.inject(QuotaErrorStateService);
    router = TestBed.inject(Router);
  });

  it('reste invisible quand le state est null', () => {
    fixture.detectChanges();
    expect(component.visible()).toBe(false);
    const banner = fixture.nativeElement.querySelector('.quota-banner');
    expect(banner).toBeNull();
  });

  it('affiche le bandeau quand QuotaErrorStateService.error() est non null', () => {
    stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'Budget tokens dépassé', sourceUrl: '/api/v1/case-files/x/analyze', receivedAt: Date.now() });
    fixture.detectChanges();
    expect(component.visible()).toBe(true);
    const banner = fixture.nativeElement.querySelector('.quota-banner');
    expect(banner).not.toBeNull();
  });

  describe('mapping code → titre', () => {
    const cases = [
      ['TOKEN_BUDGET_EXCEEDED', 'Quota tokens mensuel atteint', 'Upgrader le plan'],
      ['CASE_ANALYSIS_LIMIT_EXCEEDED', "Limite d'analyses atteinte pour ce dossier", 'Upgrader le plan'],
      ['CHAT_MESSAGE_LIMIT_EXCEEDED', 'Limite de messages chat atteinte', 'Upgrader le plan'],
      ['DOCUMENT_LIMIT_EXCEEDED', 'Limite de documents atteinte', 'Upgrader le plan'],
      ['CASE_FILE_OPEN_LIMIT_EXCEEDED', 'Limite de dossiers actifs atteinte', 'Upgrader le plan'],
      ['OCR_QUOTA_EXCEEDED', 'Quota OCR mensuel atteint', 'Upgrader le plan'],
      ['SEAT_LIMIT_EXCEEDED', 'Limite de sièges atteinte', 'Voir les plans'],
      ['VIDEO_QUOTA_EXCEEDED', 'Quota vidéo mensuel atteint — passez au plan supérieur pour débloquer plus de minutes vidéo', 'Upgrader le plan'],
    ];
    for (const [code, title, primaryLabel] of cases) {
      it(`mappe ${code} → "${title}"`, () => {
        stateService.set({ code, message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
        fixture.detectChanges();
        expect(component.mapping().title).toBe(title);
        expect(component.mapping().primaryLabel).toBe(primaryLabel);
      });
    }
  });

  it('fallback générique sur code absent (legacy)', () => {
    stateService.set({ code: null, message: 'Limite atteinte', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    expect(component.mapping().title).toBe('Limite atteinte');
    expect(component.mapping().primaryLabel).toBe('Voir les plans');
  });

  it('fallback générique + console.warn sur code inconnu', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    stateService.set({ code: 'UNKNOWN_FUTURE_CODE', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    expect(component.mapping().title).toBe('Limite atteinte');
    expect(warnSpy).toHaveBeenCalledWith('[QuotaErrorBanner] Unknown quota code: UNKNOWN_FUTURE_CODE');
    warnSpy.mockRestore();
  });

  it('affiche le message brut backend', () => {
    stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'Budget tokens mensuel dépassé.', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    const msg = fixture.nativeElement.querySelector('.quota-banner__message');
    expect(msg.textContent).toContain('Budget tokens mensuel dépassé.');
  });

  it('action primaire route vers /workspace/billing', () => {
    const navSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    component.goToPrimaryAction();
    expect(navSpy).toHaveBeenCalledWith(['/workspace/billing']);
  });

  it('action secondaire désactivée pour TOKEN_BUDGET_EXCEEDED (gated F-148)', () => {
    stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    expect(component.mapping().secondaryLabel).toBe('Acheter des tokens');
    expect(component.mapping().secondaryDisabled).toBe(true);
    const secondaryBtn = fixture.nativeElement.querySelector('.quota-banner__secondary');
    expect(secondaryBtn).not.toBeNull();
    expect(secondaryBtn.disabled).toBe(true);
  });

  // SF-231-03 — VIDEO_QUOTA_EXCEEDED : pas d'achat à la carte V1 (pas de bouton secondaire).
  it('VIDEO_QUOTA_EXCEEDED — aucune action secondaire (pas d\'achat one-shot V1)', () => {
    stateService.set({ code: 'VIDEO_QUOTA_EXCEEDED', message: 'msg', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    fixture.detectChanges();
    expect(component.mapping().secondaryLabel).toBeUndefined();
    expect(component.mapping().secondaryDisabled).toBeUndefined();
  });

  it('input override `code` + `message` prime sur le state global', () => {
    stateService.set({ code: 'TOKEN_BUDGET_EXCEEDED', message: 'msg-state', sourceUrl: '/api/v1/x', receivedAt: Date.now() });
    component.code = 'CHAT_MESSAGE_LIMIT_EXCEEDED';
    component.message = 'msg-input';
    fixture.detectChanges();
    expect(component.mapping().title).toBe('Limite de messages chat atteinte');
    expect(component.currentMessage()).toBe('msg-input');
  });
});

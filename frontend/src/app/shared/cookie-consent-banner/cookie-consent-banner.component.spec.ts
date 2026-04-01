import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CookieConsentBannerComponent } from './cookie-consent-banner.component';
import { ConsentService } from '../../core/services/consent.service';

describe('CookieConsentBannerComponent', () => {
  let fixture: ComponentFixture<CookieConsentBannerComponent>;
  let component: CookieConsentBannerComponent;
  let consentService: jest.Mocked<ConsentService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('ConsentService', ['hasConsent', 'grantConsent', 'denyConsent']);

    await TestBed.configureTestingModule({
      imports: [CookieConsentBannerComponent],
      providers: [{ provide: ConsentService, useValue: spy }]
    }).compileComponents();

    consentService = TestBed.inject(ConsentService) as jest.Mocked<ConsentService>;
  });

  it('is visible when no consent stored', () => {
    consentService.hasConsent.mockReturnValue(null);
    fixture = TestBed.createComponent(CookieConsentBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.visible).toBe(true);
  });

  it('is hidden when consent already stored', () => {
    consentService.hasConsent.mockReturnValue(true);
    fixture = TestBed.createComponent(CookieConsentBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    expect(component.visible).toBe(false);
  });

  it('accept() calls grantConsent() and hides the banner', () => {
    consentService.hasConsent.mockReturnValue(null);
    fixture = TestBed.createComponent(CookieConsentBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.accept();
    expect(consentService.grantConsent).toHaveBeenCalled();
    expect(component.visible).toBe(false);
  });

  it('deny() calls denyConsent() and hides the banner', () => {
    consentService.hasConsent.mockReturnValue(null);
    fixture = TestBed.createComponent(CookieConsentBannerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.deny();
    expect(consentService.denyConsent).toHaveBeenCalled();
    expect(component.visible).toBe(false);
  });
});

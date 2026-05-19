import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
  CancelSubscriptionDialogComponent, CancelSubscriptionDialogData
} from './cancel-subscription-dialog.component';

describe('CancelSubscriptionDialogComponent', () => {
  let component: CancelSubscriptionDialogComponent;
  let fixture: ComponentFixture<CancelSubscriptionDialogComponent>;
  let dialogRefSpy: jest.Mocked<MatDialogRef<CancelSubscriptionDialogComponent, boolean>>;

  const build = async (data: CancelSubscriptionDialogData) => {
    dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      imports: [CancelSubscriptionDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CancelSubscriptionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('rend le contenu de confirmation avec la date de fin de période', async () => {
    await build({ currentPeriodEnd: '2026-06-30T00:00:00Z' });
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Résilier l\'abonnement');
    expect(html.textContent).toMatch(/2026/);
    expect(html.textContent).toContain('plan gratuit');
  });

  it('rend un libellé générique si la date de fin est nulle', async () => {
    await build({ currentPeriodEnd: null });
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('fin de période de facturation');
  });

  it('confirm() ferme le dialog avec true', async () => {
    await build({ currentPeriodEnd: '2026-06-30T00:00:00Z' });
    component.confirm();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });

  it('cancel() ferme le dialog avec false', async () => {
    await build({ currentPeriodEnd: '2026-06-30T00:00:00Z' });
    component.cancel();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(false);
  });

  it('le clic « Résilier l\'abonnement » émet true', async () => {
    await build({ currentPeriodEnd: '2026-06-30T00:00:00Z' });
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    const confirmBtn = Array.from(buttons).find(b => /Résilier/.test(b.textContent ?? ''))!;
    confirmBtn.click();
    expect(dialogRefSpy.close).toHaveBeenCalledWith(true);
  });
});

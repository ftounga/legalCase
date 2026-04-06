import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NotificationCenterComponent } from './notification-center.component';
import { NotificationService } from '../../core/services/notification.service';

describe('NotificationCenterComponent', () => {
  let component: NotificationCenterComponent;
  let fixture: ComponentFixture<NotificationCenterComponent>;
  let notificationService: NotificationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationCenterComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationCenterComponent);
    component = fixture.componentInstance;
    notificationService = TestBed.inject(NotificationService);
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('affiche l icône notifications', () => {
    fixture.detectChanges();
    const icon = fixture.nativeElement.querySelector('mat-icon');
    expect(icon?.textContent?.trim()).toContain('notifications');
  });

  it('notificationIcon retourne la bonne icône par type', () => {
    expect(component.notificationIcon('ANALYSIS_DONE')).toBe('check_circle');
    expect(component.notificationIcon('ANALYSIS_FAILED')).toBe('error');
    expect(component.notificationIcon('DEADLINE_APPROACHING')).toBe('schedule');
    expect(component.notificationIcon('PROCEDURE_REQUALIFIED')).toBe('gavel');
    expect(component.notificationIcon('REFERENTIAL_ALERT')).toBe('warning');
    expect(component.notificationIcon('UNKNOWN')).toBe('notifications');
  });

  it('notificationIconColor retourne la bonne couleur par type', () => {
    expect(component.notificationIconColor('ANALYSIS_DONE')).toBe('#27AE60');
    expect(component.notificationIconColor('ANALYSIS_FAILED')).toBe('#C0392B');
    expect(component.notificationIconColor('DEADLINE_APPROACHING')).toBe('#C9973A');
  });
});

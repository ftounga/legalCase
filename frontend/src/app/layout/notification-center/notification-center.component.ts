import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { DatePipe } from '@angular/common';
import { NotificationService, InAppNotification } from '../../core/services/notification.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, MatBadgeModule, MatMenuModule, DatePipe],
  templateUrl: './notification-center.component.html',
  styleUrl: './notification-center.component.scss'
})
export class NotificationCenterComponent implements OnInit, OnDestroy {

  private notificationService = inject(NotificationService);
  private router = inject(Router);

  notifications = signal<InAppNotification[]>([]);
  unreadCount = this.notificationService.unreadCount;
  loading = signal(false);

  private pollingTimer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.notificationService.refreshUnreadCount();
    this.pollingTimer = setInterval(() => this.notificationService.refreshUnreadCount(), 60_000);
  }

  ngOnDestroy(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
  }

  onMenuOpened(): void {
    this.loading.set(true);
    this.notificationService.getNotifications().subscribe({
      next: page => {
        this.notifications.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onNotificationClick(notification: InAppNotification): void {
    if (!notification.isRead) {
      this.notificationService.markAsRead(notification.id).subscribe({
        next: () => this.notificationService.refreshUnreadCount()
      });
    }
    if (notification.link) {
      this.router.navigateByUrl(notification.link);
    }
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
        this.notificationService.refreshUnreadCount();
      }
    });
  }

  notificationIcon(type: string): string {
    switch (type) {
      case 'ANALYSIS_DONE': return 'check_circle';
      case 'ANALYSIS_FAILED': return 'error';
      case 'DEADLINE_APPROACHING': return 'schedule';
      case 'PROCEDURE_REQUALIFIED': return 'gavel';
      case 'REFERENTIAL_ALERT': return 'warning';
      default: return 'notifications';
    }
  }

  notificationIconColor(type: string): string {
    switch (type) {
      case 'ANALYSIS_DONE': return '#27AE60';
      case 'ANALYSIS_FAILED': return '#C0392B';
      case 'DEADLINE_APPROACHING': return '#C9973A';
      case 'PROCEDURE_REQUALIFIED': return '#1A3A5C';
      case 'REFERENTIAL_ALERT': return '#C0392B';
      default: return '#6B7A8D';
    }
  }
}

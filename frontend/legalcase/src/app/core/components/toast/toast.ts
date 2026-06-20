import { Component, inject } from '@angular/core';
import { Notification, NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-toast',
  template: `
    <div class="fixed top-4 right-4 z-50 flex w-80 max-w-[calc(100vw-2rem)] flex-col gap-2">
      @for (n of notifications(); track n.id) {
        <div role="alert"
             class="flex items-start gap-3 rounded-lg border-l-4 bg-surface px-4 py-3 text-sm text-ink shadow-lg shadow-black/40"
             [class]="accentFor(n.type)">
          <span class="flex-1">{{ n.message }}</span>
          <button type="button"
                  class="shrink-0 text-ink-muted hover:text-ink"
                  aria-label="Fechar" (click)="dismiss(n.id)">✕</button>
        </div>
      }
    </div>
  `,
})

export class Toast {
  private readonly service = inject(NotificationService);
  readonly notifications = this.service.notifications;

  accentFor(type: Notification['type']): string {
    switch (type) {
      case 'error':   return 'border-red-500';
      case 'success': return 'border-positive';
      default:        return 'border-primary';
    }
  }

  dismiss(id: number): void {
    this.service.dismiss(id);
  }
}
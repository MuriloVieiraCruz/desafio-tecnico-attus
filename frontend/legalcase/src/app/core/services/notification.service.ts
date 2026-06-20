import { Injectable, signal } from '@angular/core';

export type NotificationType = 'error' | 'success' | 'info';

export interface Notification {
  id: number;
  type: NotificationType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  private nextId = 0;
  private readonly _notifications = signal<Notification[]>([]);
  readonly notifications = this._notifications.asReadonly();

  show(message: string, type: NotificationType = 'info', timeoutMs = 5000): void {
    const id = this.nextId++;
    this._notifications.update(list => [...list, { id, type, message }]);
    if (timeoutMs > 0) {
      setTimeout(() => this.dismiss(id), timeoutMs);
    }
  }

  error(message: string): void { this.show(message, 'error', 6000); }
  success(message: string): void { this.show(message, 'success', 4000); }

  dismiss(id: number): void {
    this._notifications.update(list => list.filter(n => n.id !== id));
  }
}
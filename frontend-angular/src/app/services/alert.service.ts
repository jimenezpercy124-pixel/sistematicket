import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface AlertOptions {
  title: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  confirmText?: string;
  showCancel?: boolean;
  cancelText?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private alertSubject = new Subject<{ options: AlertOptions, resolve: (value: boolean) => void }>();
  alert$ = this.alertSubject.asObservable();

  show(options: AlertOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.alertSubject.next({ options, resolve });
    });
  }

  success(title: string, message: string): Promise<boolean> {
    return this.show({ title, message, type: 'success', confirmText: 'Entendido' });
  }

  error(title: string, message: string): Promise<boolean> {
    return this.show({ title, message, type: 'error', confirmText: 'Cerrar' });
  }

  warning(title: string, message: string): Promise<boolean> {
    return this.show({ title, message, type: 'warning', confirmText: 'Ok' });
  }

  question(title: string, message: string, confirmText: string = 'Confirmar', cancelText: string = 'Cancelar'): Promise<boolean> {
    return this.show({ title, message, type: 'warning', confirmText, showCancel: true, cancelText });
  }
}

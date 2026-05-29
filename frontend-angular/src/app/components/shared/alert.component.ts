import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, AlertOptions } from '../../services/alert.service';

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="alert-overlay" *ngIf="visible" (click)="onCancel()">
      <div class="alert-modal" (click)="$event.stopPropagation()" [class.fade-in]="visible">
        <div class="alert-icon-wrapper" [ngClass]="options?.type">
          <i class="bi" [ngClass]="getIconClass()"></i>
        </div>
        
        <div class="alert-content">
          <h2 class="alert-title">{{ options?.title }}</h2>
          <p class="alert-message">{{ options?.message }}</p>
        </div>

        <div class="alert-actions">
          <button *ngIf="options?.showCancel" class="btn-cancel" (click)="onCancel()">
            {{ options?.cancelText || 'Cancelar' }}
          </button>
          <button class="btn-confirm" [ngClass]="options?.type" (click)="onConfirm()">
            {{ options?.confirmText || 'Aceptar' }}
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .alert-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.4);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 9999;
    }

    .alert-modal {
      background: white;
      border-radius: 24px;
      padding: 2.5rem;
      width: 90%;
      max-width: 420px;
      text-align: center;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
      transform: scale(0.9);
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
    }

    .alert-modal.fade-in {
      transform: scale(1);
    }

    .alert-icon-wrapper {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 1.5rem;
      font-size: 2.5rem;
    }

    .alert-icon-wrapper.success { background: #f0fdf4; color: #22c55e; }
    .alert-icon-wrapper.error { background: #fef2f2; color: #ef4444; }
    .alert-icon-wrapper.warning { background: #fffbeb; color: #f59e0b; }
    .alert-icon-wrapper.info { background: #eff6ff; color: #3b82f6; }

    .alert-title {
      font-size: 1.5rem;
      font-weight: 800;
      color: #1e293b;
      margin-bottom: 0.75rem;
    }

    .alert-message {
      color: #64748b;
      line-height: 1.6;
      margin-bottom: 2rem;
    }

    .alert-actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
    }

    .btn-confirm, .btn-cancel {
      padding: 0.75rem 1.5rem;
      border-radius: 12px;
      font-weight: 700;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      flex: 1;
    }

    .btn-confirm.success { background: #22c55e; color: white; }
    .btn-confirm.error { background: #ef4444; color: white; }
    .btn-confirm.warning { background: #f59e0b; color: white; }
    .btn-confirm.info { background: #3b82f6; color: white; }

    .btn-confirm:hover { filter: brightness(1.1); transform: translateY(-2px); }

    .btn-cancel {
      background: #f1f5f9;
      color: #64748b;
    }

    .btn-cancel:hover { background: #e2e8f0; }

    @keyframes scaleUp {
      from { opacity: 0; transform: scale(0.8); }
      to { opacity: 1; transform: scale(1); }
    }
  `]
})
export class AlertComponent implements OnInit {
  visible = false;
  options: AlertOptions | null = null;
  private currentResolve: ((value: boolean) => void) | null = null;

  constructor(private alertService: AlertService) {}

  ngOnInit() {
    this.alertService.alert$.subscribe(({ options, resolve }) => {
      this.options = options;
      this.currentResolve = resolve;
      this.visible = true;
    });
  }

  onConfirm() {
    this.visible = false;
    this.currentResolve?.(true);
  }

  onCancel() {
    this.visible = false;
    this.currentResolve?.(false);
  }

  getIconClass() {
    switch (this.options?.type) {
      case 'success': return 'bi-check-circle-fill';
      case 'error': return 'bi-x-circle-fill';
      case 'warning': return 'bi-exclamation-triangle-fill';
      case 'info': return 'bi-info-circle-fill';
      default: return 'bi-info-circle-fill';
    }
  }
}

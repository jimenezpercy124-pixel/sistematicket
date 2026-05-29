import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject, fromEvent, merge, Subscription } from 'rxjs';
import { throttleTime } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SocketService {
  private eventSource: EventSource | null = null;
  private readonly baseUrl = environment.sseUrl;
  private eventSubject = new Subject<{ event: string, data: any }>();
  private reconnectTimeout: any;
  private activeEvents = new Set<string>();
  private isStopped = false; // Modo detenido permanente (logout)

  // Lógica de inactividad
  private readonly IDLE_TIMEOUT = 30 * 60 * 1000; // 30 minutos
  private idleTimer: any;
  private isPaused = false;
  private activitySubscription?: Subscription;

  constructor(private zone: NgZone, private authService: AuthService) {
    // Registrar referencia para que AuthService pueda llamar stop() en logout
    this.authService.registerSocketService(this);
    this.connect();
    this.initActivityDetection();
  }

  private connect() {
    if (this.isStopped) return;
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    
    if (this.eventSource) {
      this.eventSource.close();
    }

    // Obtener token JWT y pasarlo en la URL (EventSource no soporta headers)
    const token = this.authService.getToken();
    if (!token) {
      console.warn('🔒 SSE: Sin token de autenticación. No se puede conectar.');
      return;
    }

    const urlWithToken = `${this.baseUrl}?token=${token}`;
    console.log('📡 Iniciando conexión SSE...');
    this.isPaused = false;
    this.eventSource = new EventSource(urlWithToken);

    this.eventSource.onopen = () => {
      console.log('✅ Conexión SSE establecida con éxito');
      this.activeEvents.forEach(event => this.registerEventListener(event));
    };

    this.eventSource.onerror = (error: any) => {
      const state = this.eventSource?.readyState;
      
      if (state === 2) {
        console.warn('🔄 SSE: Conexión cerrada por el servidor. Reconectando...');
      } else if (state === 0 && !this.isPaused) {
        console.warn('⚠️ SSE: Error de red o servidor no disponible. Reintentando...');
      }

      if (!this.isPaused) {
        this.reconnect();
      }
    };

    this.resetIdleTimer();
  }

  private disconnect() {
    console.log('🌙 SSE: Pausando conexión por inactividad para ahorrar recursos.');
    this.isPaused = true;
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
  }

  private reconnect() {
    if (this.isStopped) return;
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }
    
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.reconnectTimeout = setTimeout(() => {
      if (!this.isPaused && !this.isStopped) {
        this.connect();
      }
    }, 3000);
  }

  // --- Lógica de Detección de Actividad ---
  private initActivityDetection() {
    this.zone.runOutsideAngular(() => {
      const activityEvents = merge(
        fromEvent(window, 'mousemove'),
        fromEvent(window, 'keydown'),
        fromEvent(window, 'click'),
        fromEvent(window, 'scroll')
      );

      this.activitySubscription = activityEvents
        .pipe(throttleTime(5000))
        .subscribe(() => {
          this.zone.run(() => {
            if (this.isPaused && !this.isStopped) {
              console.log('☀️ SSE: Actividad detectada. Reanudando conexión...');
              this.connect();
            }
            if (!this.isStopped) {
              this.resetIdleTimer();
            }
          });
        });
    });
  }

  private resetIdleTimer() {
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
    }
    this.idleTimer = setTimeout(() => {
      this.disconnect();
    }, this.IDLE_TIMEOUT);
  }

  onEvent(eventName: string): Observable<any> {
    if (!this.activeEvents.has(eventName)) {
      this.activeEvents.add(eventName);
      this.registerEventListener(eventName);
    }

    return new Observable(observer => {
      const subscription = this.eventSubject.subscribe(msg => {
        if (msg.event === eventName) {
          observer.next(msg.data);
        }
      });
      return () => subscription.unsubscribe();
    });
  }

  private registerEventListener(eventName: string) {
    if (!this.eventSource || this.eventSource.readyState !== 1) return;

    this.eventSource.addEventListener(eventName, (message: MessageEvent) => {
      this.zone.run(() => {
        let data = message.data;
        try {
          data = JSON.parse(data);
        } catch (e) {
          // Es solo string
        }
        this.eventSubject.next({ event: eventName, data });
      });
    });
  }

  emit(event: string, data: any) {
    console.warn('⚠️ emit() no soportado en SSE (unidireccional)');
  }

  /**
   * Detiene la conexión SSE permanentemente. Llamar al hacer logout.
   * La conexión NO se reanudará hasta que el usuario vuelva a iniciar sesión.
   */
  stop() {
    console.log('🔴 SSE: Deteniendo conexión (logout).');
    this.isStopped = true;
    this.isPaused = true;

    if (this.idleTimer) clearTimeout(this.idleTimer);
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    if (this.activitySubscription) this.activitySubscription.unsubscribe();

    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }

    this.activeEvents.clear();
  }

  /**
   * Reinicia el servicio SSE. Llamar cuando el usuario vuelve a iniciar sesión.
   */
  restart() {
    this.isStopped = false;
    this.isPaused = false;
    this.initActivityDetection();
    this.connect();
  }
}

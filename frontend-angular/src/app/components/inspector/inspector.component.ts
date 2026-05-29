import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { SocketService } from '../../services/socket.service';
import { Router } from '@angular/router';
import { ChangeDetectorRef } from '@angular/core';
import { AlertService } from '../../services/alert.service';
import { environment } from '../../../environments/environment';


@Component({
  selector: 'app-inspector',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './inspector.component.html',
  styleUrls: ['./inspector.component.css']
})
export class InspectorComponent implements OnInit, OnDestroy {
  ticketForm: FormGroup;
  misTickets: any[] = [];
  incidencias: any[] = [];
  intendencias: any[] = [];
  username: string = '';
  codIre: string = '';
  isLoading: boolean = false;
  isSubmitting: boolean = false;
  backendUrl: string = environment.backendUrl;
  selectedTicketForDetail: any = null;
  ticketHistorial: any[] = [];
  isTimelineLoading: boolean = false;


  // Polling
  private refreshInterval: any;

  // Paginación
  currentPage: number = 1;
  pageSize: number = 10;
  nombreArchivoSeleccionado: string = 'SELECCIONAR ARCHIVO (MÁX. 10MB)';

  showRejectModal: boolean = false;
  rejectMotivo: string = '';
  rejectTicketId: string = '';
  isRejecting: boolean = false;
  rejectEvidencia: File | null = null;
  nombreArchivoRechazo: string = 'Seleccionar archivo (opcional)';

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private authService: AuthService,
    private socketService: SocketService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private alertService: AlertService
  ) {
    this.ticketForm = this.fb.group({
      id_incidencia: ['', Validators.required],
      asunto: ['', [Validators.required, Validators.maxLength(500)]],
      cod_ire: ['', Validators.required],
      evidencia: [null]
    });
  }

  ngOnInit() {
    const userInfo = JSON.parse(sessionStorage.getItem('user_info') || '{}');
    this.username = userInfo.username || 'Inspector';
    this.codIre = userInfo.codIre || '';
    
    this.ticketForm.patchValue({ cod_ire: this.codIre });

    this.cargarDatos();
    this.setupSockets();
  }

  cargarDatos() {
    this.isLoading = true;
    this.ticketService.getIncidencias().subscribe(data => this.incidencias = data);
    this.ticketService.getIntendencias().subscribe(data => this.intendencias = data);
    this.cargarMisTickets();
  }

  cargarMisTickets() {
    this.ticketService.getMisTickets().subscribe({
      next: (data) => {
        this.misTickets = data;
        this.isLoading = false;
      },
      error: () => this.isLoading = false
    });
  }

  setupSockets() {
    this.socketService.onEvent('nuevo_ticket').subscribe(() => this.cargarMisTickets());
    this.socketService.onEvent('ticket_actualizado').subscribe(() => this.cargarMisTickets());

    // Polling fallback: refresh every 5 seconds (Seguridad para tiempo real)
    this.refreshInterval = setInterval(() => {
      this.cargarMisTickets();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  onFileChange(event: any) {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      if (file.size > 10 * 1024 * 1024) {
        this.alertService.error('Archivo muy grande', 'El archivo supera el límite de 10MB permitido.');
        event.target.value = '';
        return;
      }
      this.ticketForm.patchValue({ evidencia: file });
      this.nombreArchivoSeleccionado = file.name; // Actualización directa
      console.log('Archivo seleccionado:', file.name);
      
      // Forzar refresco en el siguiente ciclo para asegurar que el DOM se entere
      setTimeout(() => {
        this.cdr.detectChanges();
      }, 50);
    }
  }

  removeFile(event: Event) {
    event.stopPropagation();
    this.ticketForm.patchValue({ evidencia: null });
    this.nombreArchivoSeleccionado = 'SELECCIONAR ARCHIVO (MÁX. 10MB)';
    // Resetear el input file real para poder volver a subir el mismo archivo si se desea
    const fileInput = document.getElementById('file') as HTMLInputElement;
    if (fileInput) fileInput.value = '';
    this.cdr.detectChanges();
  }

  onSubmit() {
    if (this.ticketForm.invalid) return;

    this.isSubmitting = true;
    const formData = new FormData();

    // Solo agregar id_incidencia si fue seleccionado
    const idIncidencia = this.ticketForm.get('id_incidencia')?.value;
    if (idIncidencia && idIncidencia !== '') {
      formData.append('id_incidencia', idIncidencia);
    }

    formData.append('asunto', this.ticketForm.get('asunto')?.value);
    formData.append('cod_ire', this.ticketForm.get('cod_ire')?.value);
    
    const evidencia = this.ticketForm.get('evidencia')?.value;
    if (evidencia) {
      formData.append('evidencia', evidencia);
    }

    this.ticketService.createTicket(formData).subscribe({
      next: (res) => {
        this.alertService.success('¡Éxito!', `Ticket ${res.id_ticket} generado correctamente`);
        this.ticketForm.reset({ cod_ire: this.codIre });
        this.nombreArchivoSeleccionado = 'SELECCIONAR ARCHIVO (MÁX. 10MB)';
        this.cargarMisTickets();
        this.isSubmitting = false;
        
        // Resetear input file real
        const fileInput = document.getElementById('file') as HTMLInputElement;
        if (fileInput) fileInput.value = '';
      },
      error: (err) => {
        console.error('Error al crear ticket:', err);
        const msg = err.error?.message || err.error || `Error ${err.status}: ${err.statusText}`;
        this.alertService.error('Error al crear ticket', msg);
        this.isSubmitting = false;
      }
    });
  }

  confirmarResolucion(id: string, accion: 'confirmar' | 'rechazar') {
    if (accion === 'confirmar') {
      this.alertService.question('¿Confirmar Resolución?', '¿Estás seguro de que esta incidencia ha sido resuelta correctamente?').then(confirmado => {
        if (confirmado) {
          this.ticketService.confirmTicketResolutionDirect(id).subscribe({
            next: () => {
              this.alertService.success('Confirmado', 'El ticket ha sido cerrado correctamente.');
              this.cargarMisTickets();
            },
            error: (err) => this.alertService.error('Error', err.error?.message || 'No se pudo confirmar la resolución')
          });
        }
      });
    } else {
      // Abrir modal personalizado
      this.rejectTicketId = id;
      this.rejectMotivo = '';
      this.showRejectModal = true;
    }
  }

  enviarRechazo() {
    if (!this.rejectMotivo || this.rejectMotivo.trim() === '') {
      this.alertService.warning('Campo requerido', 'Por favor, indique el motivo del rechazo.');
      return;
    }

    this.isRejecting = true;
    const formData = new FormData();
    formData.append('motivo', this.rejectMotivo);
    if (this.rejectEvidencia) {
      formData.append('evidencia', this.rejectEvidencia);
    }
    
    this.ticketService.rejectTicketResolutionDirect(this.rejectTicketId, formData).subscribe({
      next: () => {
        this.alertService.warning('Rechazado', 'Se ha notificado al especialista sobre el rechazo.');
        this.closeRejectModal();
        this.isRejecting = false;
        this.cargarMisTickets();
      },
      error: (err) => {
        this.alertService.error('Error', err.error?.message || 'No se pudo procesar el rechazo');
        this.isRejecting = false;
      }
    });
  }

  onRejectFileChange(event: any) {
    if (event.target.files.length > 0) {
      const file = event.target.files[0];
      if (file.size > 10 * 1024 * 1024) {
        this.alertService.error('Archivo muy grande', 'El archivo de prueba supera los 10MB.');
        event.target.value = '';
        return;
      }
      this.rejectEvidencia = file;
      this.nombreArchivoRechazo = file.name;
    }
  }

  removeRejectFile() {
    this.rejectEvidencia = null;
    this.nombreArchivoRechazo = 'Seleccionar archivo (opcional)';
    const input = document.getElementById('rejectFile') as HTMLInputElement;
    if (input) input.value = '';
  }

  closeRejectModal() {
    this.showRejectModal = false;
    this.rejectTicketId = '';
    this.rejectMotivo = '';
    this.rejectEvidencia = null;
    this.nombreArchivoRechazo = 'Seleccionar archivo (opcional)';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getEstadoLabel(estado: string): string {
    const labels: any = {
      'CREADO': 'Creado',
      'PENDIENTE': 'Pendiente',
      'EN_PROCESO': 'En Proceso',
      'PENDIENTE_CONFIRMACION': 'Por Confirmar',
      'RESUELTO': 'Resuelto',
      'RECHAZADO': 'Rechazado'
    };
    return labels[estado] || estado;
  }

  get paginatedTickets() {
    const startIndex = (this.currentPage - 1) * this.pageSize;
    return this.misTickets.slice(startIndex, startIndex + this.pageSize);
  }

  get totalPages() {
    return Math.ceil(this.misTickets.length / this.pageSize);
  }

  nextPage() {
    if (this.currentPage < this.totalPages) this.currentPage++;
  }

  prevPage() {
    if (this.currentPage > 1) this.currentPage--;
  }

  setPage(page: number) {
    this.currentPage = page;
  }

  get asuntoLength(): number {
    const val = this.ticketForm.get('asunto')?.value;
    return val ? val.length : 0;
  }

  get selectedFileName(): string {
    return this.nombreArchivoSeleccionado;
  }

  get token(): string {
    return this.authService.getToken() || '';
  }

  getEvidencias(t: any): string[] {
    const link = t.link_drive || t.linkDrive;
    if (!link) return [];
    return link.split(',').map((f: string) => f.trim()).filter((f: string) => f.length > 0);
  }

  verDetalleTicket(ticket: any) {
    this.selectedTicketForDetail = ticket;
    this.ticketHistorial = [];
    this.isTimelineLoading = true;
    
    const ticketId = ticket.id_ticket || ticket.idTicket;
    this.ticketService.getTicketHistorial(ticketId).subscribe({
      next: (logs) => {
        this.ticketHistorial = logs;
        this.isTimelineLoading = false;
      },
      error: (err) => {
        console.error('Error cargando historial:', err);
        this.isTimelineLoading = false;
      }
    });
  }
}

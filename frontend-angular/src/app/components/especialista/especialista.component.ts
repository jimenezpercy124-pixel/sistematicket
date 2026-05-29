import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { SocketService } from '../../services/socket.service';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';


@Component({
  selector: 'app-especialista',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './especialista.component.html',
  styleUrls: ['./especialista.component.css']
})
export class EspecialistaComponent implements OnInit, OnDestroy {
  tickets: any[] = [];
  username: string = '';
  codIre: string = '';
  especialistaNum: number = 2; // 1: Principal, 2: Secundario
  isLoading: boolean = false;
  backendUrl: string = environment.backendUrl;

  selectedTicketForDetail: any = null;
  ticketHistorial: any[] = [];
  isTimelineLoading: boolean = false;


  // Stats
  statsServidor: any = { total: 0, pendientes: 0, enProceso: 0, resueltos: 0 };
  
  // Filtros
  filtroEstado: string = 'TODOS';
  searchTerm: string = '';

  // Modales
  showStatusModal: boolean = false;
  showDelegateModal: boolean = false;
  showRejectionModal: boolean = false;
  selectedTicketId: number | null = null;
  selectedTicket: any = null;
  selectedEstado: string = '';
  motivoRechazo: string = '';
  
  // Delegación
  especialistasSecundarios: any[] = [];
  selectedEspecialistaId: number | null = null;

  // Polling
  private refreshInterval: any;

  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
    private socketService: SocketService,
    private router: Router
  ) {}

  ngOnInit() {
    const userInfoStr = sessionStorage.getItem('user_info');
    const userInfo = userInfoStr ? JSON.parse(userInfoStr) : {};
    
    this.username = userInfo.username || 'Especialista';
    // 1 es Principal, 2 es Secundario (referente al tipo, pero usamos el ID para tickets)
    this.especialistaNum = (userInfo.especialista_num || userInfo.especialistaNum) || 2; 

    this.cargarTickets();
    this.cargarEstadisticas();
    if (this.especialistaNum === 1) {
      this.cargarEspecialistas();
    }
    this.setupSockets();
  }

  cargarTodo() {
    this.cargarTickets();
    this.cargarEstadisticas();
    if (this.especialistaNum === 1) {
      this.cargarEspecialistas();
    }
  }

  cargarTickets() {
    this.isLoading = true;
    const userId = JSON.parse(sessionStorage.getItem('user_info') || '{}').id;
    if (userId) {
      this.ticketService.getTicketsByEspecialista(userId).subscribe({
        next: (data) => {
          this.tickets = data;
          this.isLoading = false;
          // Forzar actualización de stats al recibir nuevos tickets
          this.cargarEstadisticas();
        },
        error: () => this.isLoading = false
      });
    } else {
      this.isLoading = false;
    }
  }

  cargarEspecialistas() {
    const userId = JSON.parse(sessionStorage.getItem('user_info') || '{}').id;
    this.ticketService.getEspecialistasDisponibles().subscribe({
      next: (data) => {
        // Mostramos solo especialistas secundarios y excluimos al usuario actual
        this.especialistasSecundarios = data.filter(u => 
          (u.especialista_num === 2 || u.especialistaNum === 2) && 
          (u.id_usuario !== userId && u.idUsuario !== userId)
        );
      },
      error: (err) => console.error('Error cargando especialistas:', err)
    });
  }

  get stats() {
    if (this.statsServidor && (this.statsServidor.total > 0 || this.statsServidor.pendientes > 0 || this.statsServidor.resueltos > 0)) {
      return {
        total: this.statsServidor.total,
        pendientes: this.statsServidor.pendientes,
        enProceso: this.statsServidor.en_proceso,
        resueltos: this.statsServidor.resueltos
      };
    }
    return {
      total: this.tickets.length,
      pendientes: this.tickets.filter(t => {
        const e = (t.estado || '').toUpperCase();
        return e.includes('PENDIENTE') || e === 'CREADO' || e === 'RECHAZADO';
      }).length,
      enProceso: this.tickets.filter(t => (t.estado || '').toUpperCase().includes('EN_PROCESO')).length,
      resueltos: this.tickets.filter(t => {
        const e = (t.estado || '').toUpperCase();
        return e.includes('RESUELTO') || e === 'CERRADO';
      }).length
    };
  }

  cargarEstadisticas() {
    this.ticketService.getEstadisticas().subscribe({
      next: data => {
        this.statsServidor = data;
      },
      error: (err) => console.error('Error cargando estadísticas:', err)
    });
  }

  get ticketsFiltrados() {
    let filtered = this.tickets;

    // 1. Filtro por Estado
    if (this.filtroEstado !== 'TODOS') {
      filtered = filtered.filter(t => t.estado === this.filtroEstado);
    }

    // 2. Filtro por Buscador (N° Ticket, IRE, Incidencia, Inspector)
    if (this.searchTerm && this.searchTerm.trim() !== '') {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(t => {
        const id = (t.id_ticket || '').toLowerCase();
        const ire = (t.cod_ire || t.codIre || '').toLowerCase();
        const incidencia = (t.nombre_incidencia || t.incidencia?.nombre_incidencia || '').toLowerCase();
        const inspector = (t.nombre_inspe || t.creador?.username || '').toLowerCase();

        return id.includes(term) || 
               ire.includes(term) || 
               incidencia.includes(term) || 
               inspector.includes(term);
      });
    }

    return filtered;
  }

  setupSockets() {
    this.socketService.onEvent('nuevo_ticket').subscribe(() => {
      this.cargarTodo();
    });
    this.socketService.onEvent('ticket_actualizado').subscribe(() => {
      this.cargarTodo();
    });

    // Polling fallback: refresh every 5 seconds
    this.refreshInterval = setInterval(() => {
      this.cargarTickets();
      this.cargarEstadisticas();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  // --- ACTUALIZAR ESTADO ---
  openStatusModal(ticket: any) {
    this.selectedTicket = ticket;
    this.selectedTicketId = ticket.id_ticket;
    this.selectedEstado = ticket.estado;
    this.showStatusModal = true;
  }

  closeStatusModal() {
    this.showStatusModal = false;
    this.selectedTicketId = null;
    this.selectedTicket = null;
  }

  confirmarStatus() {
    if (this.selectedTicketId && this.selectedEstado) {
      if (this.selectedEstado === 'RECHAZADO') {
        this.showStatusModal = false;
        this.showRejectionModal = true;
        this.motivoRechazo = '';
      } else {
        this.ticketService.updateTicketStatus(this.selectedTicketId, this.selectedEstado).subscribe(() => {
          this.closeStatusModal();
          this.cargarTickets();
        });
      }
    }
  }

  // --- RECHAZAR TICKET CON MOTIVO ---
  closeRejectionModal() {
    this.showRejectionModal = false;
    this.motivoRechazo = '';
    this.selectedTicketId = null;
    this.selectedTicket = null;
  }

  confirmarRechazo() {
    if (this.selectedTicketId && this.motivoRechazo && this.motivoRechazo.trim()) {
      this.ticketService.updateTicketStatus(this.selectedTicketId, 'RECHAZADO', this.motivoRechazo.trim()).subscribe(() => {
        this.closeRejectionModal();
        this.cargarTickets();
      });
    }
  }

  // --- DELEGAR TICKET ---
  openDelegateModal(ticket: any) {
    this.selectedTicket = ticket;
    this.selectedTicketId = ticket.id_ticket;
    this.selectedEspecialistaId = ticket.especialistaAsignado?.idUsuario || null;
    this.showDelegateModal = true;
  }

  closeDelegateModal() {
    this.showDelegateModal = false;
    this.selectedTicketId = null;
    this.selectedTicket = null;
    this.selectedEspecialistaId = null;
  }

  confirmarDelegacion() {
    if (this.selectedTicketId && this.selectedEspecialistaId) {
      this.ticketService.delegateTicket(this.selectedTicketId, this.selectedEspecialistaId).subscribe(() => {
        this.closeDelegateModal();
        this.cargarTickets();
      });
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  getEstadoClass(estado: string): string {
    return `status-${estado}`;
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

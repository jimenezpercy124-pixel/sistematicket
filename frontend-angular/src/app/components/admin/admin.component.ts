import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { SocketService } from '../../services/socket.service';
import { Router } from '@angular/router';
import { AlertService } from '../../services/alert.service';
import { environment } from '../../../environments/environment';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit, OnDestroy {
  // Data
  tickets: any[] = [];
  usuarios: any[] = [];
  incidencias: any[] = [];
  intendencias: any[] = [];
  
  statsServidor: any = { total: 0, pendientes: 0, en_proceso: 0, resueltos: 0 };

  backendUrl: string = environment.backendUrl;
  hoveredSlice: any = null;
  hoveredBar: any = null;
  selectedTicketForDetail: any = null;
  ticketHistorial: any[] = [];
  isTimelineLoading: boolean = false;

  // --- CONTINGENCIA STATE ---
  idAusente: number | null = null;
  idReemplazo: number | null = null;
  isContingenciaLoading: boolean = false;

  // Reasignación individual
  selectedIncidenciaReassign: any = null;
  newEspecialistaId: number | null = null;
  isReassignModalOpen: boolean = false;

  get ausenteIncidenciasCount(): number {
    if (!this.idAusente) return 0;
    return this.incidencias.filter(inc => {
      const espNum = inc.especialista_num || inc.especialistaNum;
      return espNum == this.idAusente;
    }).length;
  }

  get ausenteTicketsActivos(): any[] {
    if (!this.idAusente) return [];
    return this.tickets.filter(t => {
      const espNum = t.especialista_num || t.especialistaNum;
      const estado = (t.estado || '').toUpperCase();
      const isActive = estado === 'PENDIENTE' || estado === 'ABIERTO' || estado === 'EN_PROCESO' || estado === 'CREADO' || estado === 'RECHAZADO';
      return espNum == this.idAusente && isActive;
    });
  }

  get ausenteTicketsHistoricos(): any[] {
    if (!this.idAusente) return [];
    return this.tickets.filter(t => {
      const espNum = t.especialista_num || t.especialistaNum;
      const estado = (t.estado || '').toUpperCase();
      const isHistorical = estado === 'RESUELTO' || estado === 'CERRADO' || estado === 'PENDIENTE_CONFIRMACION';
      return espNum == this.idAusente && isHistorical;
    });
  }

  get statsCalculadas() {
    // Si tenemos datos del servidor, los usamos. Si no, calculamos localmente.
    if (this.statsServidor && (this.statsServidor.total > 0 || this.statsServidor.pendientes > 0 || this.statsServidor.resueltos > 0 || this.statsServidor.rechazados > 0)) {
      return this.statsServidor;
    }
    return {
      total: this.tickets.length,
      pendientes: this.tickets.filter(t => {
        const e = (t.estado || '').toUpperCase();
        return e.includes('PENDIENTE') || e === 'CREADO';
      }).length,
      en_proceso: this.tickets.filter(t => (t.estado || '').toUpperCase().includes('EN_PROCESO')).length,
      resueltos: this.tickets.filter(t => {
        const e = (t.estado || '').toUpperCase();
        return e.includes('RESUELTO') || e === 'CERRADO';
      }).length,
      rechazados: this.tickets.filter(t => (t.estado || '').toUpperCase() === 'RECHAZADO').length
    };
  }
  
  // Report Data
  reporteIres: any[] = [];
  reporteTematicas: any[] = [];
  reporteCruce: any[] = [];

  // Polling
  private refreshInterval: any;

  // Pagination State
  pageTickets: number = 1;
  pageUsuarios: number = 1;
  pageIncidencias: number = 1;
  pageIres: number = 1;
  pageTematicas: number = 1;
  pageCruce: number = 1;
  itemsPerPage: number = 10;

  // UI State
  activeTab: string = 'tickets';
  sidebarActive: boolean = false;
  isLoading: boolean = false;
  username: string = '';
  rol: string = '';
  filtroEstado: string = '';
  filtroUsuario: string = '';
  searchTerm: string = '';

  get filteredTickets(): any[] {
    let filtered = this.tickets;

    // Filtro por Estado
    if (this.filtroEstado) {
      filtered = filtered.filter(t => t.estado === this.filtroEstado);
    }

    // Filtro por Búsqueda (Texto)
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(t => 
        (t.id_ticket && t.id_ticket.toLowerCase().includes(term)) ||
        (t.nombreIncidencia && t.nombreIncidencia.toLowerCase().includes(term)) ||
        (t.nombreEspecialista && t.nombreEspecialista.toLowerCase().includes(term)) ||
        (t.cod_ire && t.cod_ire.toLowerCase().includes(term))
      );
    }

    return filtered;
  }

  // Forms
  userForm: FormGroup;
  incidenciaForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private ticketService: TicketService,
    private authService: AuthService,
    private socketService: SocketService,
    private router: Router,
    private alertService: AlertService
  ) {
    this.userForm = this.fb.group({
      username: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      rol: ['INSPECTOR', Validators.required],
      cod_ire: [''],
      tipo_especialista: ['SECUNDARIO'], // Cambiado de especialista_num
      password: ['Sunafil2026!']
    });

    this.incidenciaForm = this.fb.group({
      nombre_incidencia: ['', Validators.required],
      id_especialista: [null, Validators.required]
    });
  }

  get especialistasList() {
    return this.usuarios.filter(u => 
      ((u.role?.nombre_rol || u.role?.nombreRol) === 'ESPECIALISTA') && u.activo !== false
    );
  }

  ngOnInit() {
    const userInfo = JSON.parse(sessionStorage.getItem('user_info') || '{}');
    
    // Validar si la sesión actual corresponde a un administrador
    if (userInfo.rol !== 'ADMIN') {
      this.alertService.error('Acceso Denegado', 'Tu sesión actual no es de Administrador. Por seguridad, inicie sesión nuevamente.').then(() => {
        this.logout();
      });
      return;
    }

    this.username = userInfo.username || 'Admin';
    this.rol = userInfo.rol || 'ADMIN';

    this.cargarTodo();
    this.setupSockets();
  }

  cargarTodo() {
    this.isLoading = true;
    this.cargarUsuarios();
    this.cargarTickets();
    this.cargarIncidencias();
    this.cargarEstadisticas();
    this.cargarReportes();
    this.ticketService.getIntendencias().subscribe(data => this.intendencias = data);
  }

  setupSockets() {
    this.socketService.onEvent('nuevo_ticket').subscribe(() => {
      this.cargarTodo();
    });
    this.socketService.onEvent('ticket_actualizado').subscribe(() => {
      this.cargarTodo();
    });
    this.socketService.onEvent('nuevo_usuario').subscribe(() => this.cargarUsuarios());

    // Polling fallback: refresh every 15 seconds
    // Polling fallback: refresh every 5 seconds (Seguridad adicional al SSE)
    this.refreshInterval = setInterval(() => {
      this.cargarTodo();
    }, 5000);
  }

  ngOnDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  // --- ACTIONS ---
  cargarTickets() {
    this.ticketService.getAllTickets().subscribe({
      next: data => { 
        this.tickets = data.map(t => {
          // Normalizar propiedades
          t.id_ticket = t.id_ticket || t.idTicket;
          t.cod_ire = t.cod_ire || t.codIre;
          t.estado = t.estado || 'PENDIENTE';
          t.fecha_creacion = t.fecha_creacion || t.fechaCreacion;
          t.fecha_resolucion = t.fecha_resolucion || t.fechaResolucion;
          
          // Mapear nombre de incidencia
          t.nombreIncidencia = t.incidencia?.nombre_incidencia || t.incidencia?.nombreIncidencia || t.asunto || '—';
          
          // Mapear nombre del especialista desde la relación JPA
          // Intentar todas las variaciones posibles de nombres de campo (snake_case y camelCase)
          const especialista = t.especialista_asignado || t.especialistaAsignado;
          t.nombreEspecialista = especialista?.username || '—';
          
          return t;
        });
        this.isLoading = false; 
      },
      error: err => { console.error('Error tickets:', err.status); this.isLoading = false; }
    });
  }

  cargarUsuarios() {
    this.ticketService.getUsuarios().subscribe({
      next: data => this.usuarios = data,
      error: err => console.error('Error usuarios:', err.status)
    });
  }

  cargarIncidencias() {
    this.ticketService.getIncidencias().subscribe({
      next: data => {
        this.incidencias = data.map(inc => {
          // Ajustar propiedades
          inc.nombreIncidencia = inc.nombre_incidencia || inc.nombreIncidencia;
          inc.idIncidencia = inc.id_incidencia || inc.idIncidencia;
          const espNum = inc.especialista_num || inc.especialistaNum;
          
          // Buscar nombre del especialista si es un ID de usuario
          let espName = '—';
          let espType = '';
          if (espNum) {
            const esp = this.usuarios.find(u => u.id_usuario === espNum || u.idUsuario === espNum);
            if (esp) {
              espName = esp.username;
              espType = (esp.especialista_num === 1 || esp.especialistaNum === 1) ? 'PRINCIPAL' : 'SECUNDARIO';
            }
          }
          inc.nombreEspecialista = espName;
          inc.tipoEspecialista = espType;
          return inc;
        });
      },
      error: err => console.error('Error incidencias:', err.status)
    });
  }

  cargarEstadisticas() {
    this.ticketService.getEstadisticas().subscribe({
      next: data => {
        this.statsServidor = data;
      },
      error: err => console.error('Error estadisticas:', err.status)
    });
  }

  cargarReportes() {
    this.ticketService.getReportes().subscribe({
      next: data => {
        this.reporteIres = data.ires || [];
        this.reporteTematicas = data.tematicas || [];
        this.reporteCruce = data.cruce || [];
        // reset pages
        this.pageIres = 1;
        this.pageTematicas = 1;
        this.pageCruce = 1;
      },
      error: err => console.error('Error reportes:', err.status)
    });
  }

  crearUsuario() {
    if (this.userForm.invalid) return;
    const data = { ...this.userForm.value };
    
    if (data.rol === 'ESPECIALISTA') {
      data.especialista_num = data.tipo_especialista === 'PRINCIPAL' ? 1 : 2;
    }
    delete data.tipo_especialista;
    
    const esInspectorLike = data.rol !== 'ADMIN' && data.rol !== 'ESPECIALISTA';
    if (!esInspectorLike) {
      delete data.cod_ire;
    }
    
    this.ticketService.createUsuario(data).subscribe({
      next: () => {
        this.alertService.success('Usuario Creado', 'El nuevo usuario ha sido registrado correctamente.');
        this.userForm.reset({ rol: 'INSPECTOR', tipo_especialista: 'SECUNDARIO', password: 'Sunafil2026!' });
        this.cargarUsuarios();
      },
      error: () => this.alertService.error('Error', 'No se pudo crear el usuario. Verifique los datos.')
    });
  }

  eliminarUsuario(id: number) {
    this.alertService.question('¿Eliminar Usuario?', '¿Estás seguro de eliminar este usuario? Esta acción no se puede deshacer.').then(confirmado => {
      if (confirmado) {
        this.ticketService.deleteUsuario(id).subscribe({
          next: () => this.cargarUsuarios(),
          error: (err) => this.alertService.error('No se pudo eliminar', err.error?.message || 'Error desconocido')
        });
      }
    });
  }

  toggleUsuarioActivo(u: any) {
    const isActivo = u.activo !== false;
    const accion = isActivo ? 'inhabilitar' : 'habilitar';
    this.alertService.question(`¿${accion.charAt(0).toUpperCase() + accion.slice(1)} usuario?`, `¿Deseas ${accion} al usuario ${u.username}?`).then(confirmado => {
      if (confirmado) {
        this.ticketService.toggleUserStatus(u.id_usuario || u.idUsuario, !isActivo).subscribe({
          next: () => {
            this.alertService.success('Estado Actualizado', `El usuario ${u.username} ha sido ${isActivo ? 'inhabilitado' : 'habilitado'} correctamente.`);
            this.cargarUsuarios();
          },
          error: (err) => this.alertService.error('Error', err.error?.message || 'No se pudo actualizar el estado.')
        });
      }
    });
  }

  resetPassword(usuario: any) {
    // Para el prompt de contraseña mantenemos el nativo por ahora o avisamos
    const newPass = prompt(`Nueva contraseña para ${usuario.username}:`, 'Sunafil2026!');
    if (newPass) {
      this.ticketService.resetPassword(usuario.id_usuario || usuario.idUsuario, newPass).subscribe(() => {
        this.alertService.success('Password Reseteado', `La contraseña de ${usuario.username} ha sido actualizada.`);
      });
    }
  }

  crearIncidencia() {
    if (this.incidenciaForm.invalid) return;
    
    const formValue = this.incidenciaForm.value;
    const payload = {
      nombre_incidencia: formValue.nombre_incidencia,
      especialista_num: formValue.id_especialista // Guardamos el ID del usuario como el número de especialista
    };

    this.ticketService.createIncidencia(payload).subscribe({
      next: () => {
        this.alertService.success('Incidencia Guardada', 'La incidencia se ha registrado con el especialista asignado.');
        this.incidenciaForm.reset({ nombre_incidencia: '', id_especialista: null });
        this.cargarIncidencias();
      },
      error: () => this.alertService.error('Error', 'No se pudo crear la incidencia.')
    });
  }

  eliminarIncidencia(id: number) {
    this.alertService.question('¿Eliminar Incidencia?', '¿Deseas eliminar esta categoría de incidencia?').then(confirmado => {
      if (confirmado) {
        this.ticketService.deleteIncidencia(id).subscribe({
          next: () => this.cargarIncidencias(),
          error: (err) => this.alertService.error('No se pudo eliminar', err.error?.message || 'Error desconocido')
        });
      }
    });
  }

  eliminarTicket(id: string) {
    this.alertService.question('¿Eliminar Ticket?', `¿Estás seguro de eliminar el ticket ${id}?`).then(confirmado => {
      if (confirmado) {
        this.ticketService.deleteTicket(id).subscribe({
          next: () => this.cargarTickets(),
          error: (err) => this.alertService.error('No se pudo eliminar', err.error?.message || 'Error desconocido')
        });
      }
    });
  }

  exportToPDF() {
    const isReportes = this.activeTab === 'reportes';
    const doc = new jsPDF(isReportes ? 'p' : 'l', 'mm', 'a4');
    const pageW = doc.internal.pageSize.getWidth();
    const fecha = new Date().toLocaleDateString('es-PE', { year: 'numeric', month: 'long', day: 'numeric' });

    // Encabezado General (Azul Suave Muy Suave)
    doc.setFillColor(224, 242, 254);
    doc.rect(0, 0, pageW, 28, 'F');
    doc.setTextColor(30, 41, 59);
    doc.setFontSize(16);
    doc.setFont('helvetica', 'bold');
    
    if (isReportes) {
      doc.text('SIIT — Reporte Estadístico del Sistema', pageW / 2, 12, { align: 'center' });
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.text(`Generado: ${fecha}`, pageW / 2, 20, { align: 'center' });

      // --- Tabla 1: IREs ---
      doc.setTextColor(33, 33, 33);
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.text('1. IREs que Más Consultan', 15, 40);
      
      autoTable(doc, {
        startY: 45,
        head: [['Nro', 'Intendencia (IRE)', 'Tickets', 'Resueltos', 'En Proceso', 'Pendientes']],
        body: this.reporteIres.map((r, i) => [
          i + 1,
          r.nombre_ire,
          r.total_tickets || 0,
          r.resueltos || 0,
          r.en_proceso || 0,
          r.pendientes || 0
        ]),
        theme: 'grid',
        headStyles: { fillColor: [224, 242, 254], textColor: [30, 41, 59] },
        styles: { fontSize: 8 }
      });

      // --- Tabla 2: Temáticas ---
      const finalY1 = (doc as any).lastAutoTable.finalY || 45;
      doc.setFontSize(12);
      doc.setFont('helvetica', 'bold');
      doc.text('2. Incidencias Más Consultadas', 15, finalY1 + 15);
      
      autoTable(doc, {
        startY: finalY1 + 20,
        head: [['Nro', 'Incidencia', 'Tickets', 'Resueltos', 'En Proceso', 'Pendientes']],
        body: this.reporteTematicas.map((r, i) => [
          i + 1,
          r.nombre_incidencia,
          r.total || 0,
          r.resueltos || 0,
          r.en_proceso || 0,
          r.pendientes || 0
        ]),
        theme: 'grid',
        headStyles: { fillColor: [224, 242, 254], textColor: [30, 41, 59] },
        styles: { fontSize: 8 }
      });

      // --- Tabla 3: Cruce (Si hay espacio o nueva página) ---
      const finalY2 = (doc as any).lastAutoTable.finalY || 100;
      doc.addPage();
      doc.setFillColor(224, 242, 254);
      doc.rect(0, 0, pageW, 15, 'F');
      doc.setTextColor(30, 41, 59);
      doc.setFontSize(12);
      doc.text('3. Incidencias por Intendencia Regional', pageW / 2, 10, { align: 'center' });

      let currentY = 25;
      this.reporteCruce.forEach((r) => {
        doc.setTextColor(33, 33, 33);
        doc.setFontSize(10);
        doc.setFont('helvetica', 'bold');
        doc.text(`${r.nombre_ire} (${r.total_tickets} tickets)`, 15, currentY);
        
        const rows = r.incidencias.map((inc: any, idx: number) => [idx + 1, inc.nombre_incidencia, inc.total]);
        
        autoTable(doc, {
          startY: currentY + 5,
          head: [['#', 'Incidencia', 'Cantidad']],
          body: rows,
          theme: 'striped',
          headStyles: { fillColor: [52, 73, 94] },
          styles: { fontSize: 7 }
        });
        
        currentY = (doc as any).lastAutoTable.finalY + 10;
        if (currentY > 260) {
          doc.addPage();
          currentY = 20;
        }
      });

      doc.save(`Reporte_Estadistico_SIIT_${new Date().toISOString().slice(0, 10)}.pdf`);

    } else {
      // Exportación original de Tickets
      doc.text('SIIT-SUNAFIL — Reporte de Tickets', pageW / 2, 12, { align: 'center' });
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.text(`Generado: ${fecha}`, pageW / 2, 20, { align: 'center' });

      autoTable(doc, {
        startY: 35,
        head: [['ID', 'Incidencia', 'Especialista', 'Usuario Creador', 'Rol Creador', 'IRE', 'Fecha / Hora', 'Tiempo de Resolución', 'Estado']],
        body: this.filteredTickets.map(t => [
          t.id_ticket,
          t.nombreIncidencia,
          t.nombreEspecialista,
          t.creador?.username || t.nombre_inspe || '—',
          t.creador?.role?.nombreRol || t.creador?.role?.nombre_rol || '—',
          t.cod_ire,
          new Date(t.fecha_creacion).toLocaleString('es-PE'),
          this.getTiempoResolucion(t),
          t.estado
        ]),
        theme: 'grid',
        headStyles: { fillColor: [224, 242, 254], textColor: [30, 41, 59] },
        styles: { fontSize: 8 }
      });

      doc.save(`Reporte_Tickets_SIIT_${new Date().toISOString().slice(0, 10)}.pdf`);
    }
  }

  // --- CONTINGENCIA ACTIONS ---
  activarContingencia() {
    if (!this.idAusente || !this.idReemplazo) return;
    if (this.idAusente == this.idReemplazo) {
      this.alertService.error('Error de Selección', 'El especialista de reemplazo debe ser diferente al ausente.');
      return;
    }

    this.alertService.question(
      '¿Activar Plan de Contingencia?',
      `Esto reasignará todas las categorías y migrará ${this.ausenteTicketsActivos.length} tickets activos del especialista ausente al especialista de reemplazo. Los ${this.ausenteTicketsHistoricos.length} tickets históricos permanecerán intactos.`
    ).then(confirmado => {
      if (confirmado) {
        this.isContingenciaLoading = true;
        this.ticketService.activarContingencia(this.idAusente!, this.idReemplazo!).subscribe({
          next: (res) => {
            this.isContingenciaLoading = false;
            this.alertService.success(
              'Contingencia Activada',
              `Se han reasignado ${res.incidencias_reasignadas} categorías de incidencias y se han migrado ${res.tickets_migrados} tickets de manera exitosa.`
            );
            this.idAusente = null;
            this.idReemplazo = null;
            this.cargarTodo();
          },
          error: (err) => {
            this.isContingenciaLoading = false;
            this.alertService.error('Error', err.error?.message || 'No se pudo activar el plan de contingencia.');
          }
        });
      }
    });
  }

  abrirModalReasignacion(incidencia: any) {
    this.selectedIncidenciaReassign = incidencia;
    const espNum = incidencia.especialista_num || incidencia.especialistaNum;
    this.newEspecialistaId = espNum ? Number(espNum) : null;
    this.isReassignModalOpen = true;
  }

  cerrarModalReasignacion() {
    this.selectedIncidenciaReassign = null;
    this.newEspecialistaId = null;
    this.isReassignModalOpen = false;
  }

  ejecutarReasignacionIncidencia() {
    if (!this.selectedIncidenciaReassign || !this.newEspecialistaId) return;

    this.ticketService.reassignIncidenciaSpecialist(this.selectedIncidenciaReassign.idIncidencia, this.newEspecialistaId).subscribe({
      next: () => {
        this.alertService.success('Categoría Reasignada', 'El especialista responsable de la incidencia ha sido actualizado y se han migrado sus tickets activos.');
        this.cerrarModalReasignacion();
        this.cargarTodo();
      },
      error: (err) => {
        this.alertService.error('Error', err.error?.message || 'No se pudo reasignar el especialista.');
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  // --- HELPERS ---

  get paginatedTickets() {
    const start = (this.pageTickets - 1) * this.itemsPerPage;
    return this.filteredTickets.slice(start, start + this.itemsPerPage);
  }

  get paginatedIncidencias() {
    const start = (this.pageIncidencias - 1) * this.itemsPerPage;
    return this.incidencias.slice(start, start + this.itemsPerPage);
  }

  get filteredUsuarios() {
    if (!this.filtroUsuario) return this.usuarios;
    const term = this.filtroUsuario.toLowerCase();
    return this.usuarios.filter(u => 
      (u.username && u.username.toLowerCase().includes(term)) ||
      (u.email && u.email.toLowerCase().includes(term)) ||
      (u.role && u.role.nombre_rol && u.role.nombre_rol.toLowerCase().includes(term)) ||
      (u.role && u.role.nombreRol && u.role.nombreRol.toLowerCase().includes(term)) ||
      (u.cod_ire && u.cod_ire.toLowerCase().includes(term)) ||
      (u.codIre && u.codIre.toLowerCase().includes(term))
    );
  }

  get paginatedUsuarios() {
    const start = (this.pageUsuarios - 1) * this.itemsPerPage;
    return this.filteredUsuarios.slice(start, start + this.itemsPerPage);
  }

  // --- REPORT PAGINATION HELPERS ---
  get paginatedIres() {
    const start = (this.pageIres - 1) * this.itemsPerPage;
    return this.reporteIres.slice(start, start + this.itemsPerPage);
  }
  get paginatedTematicas() {
    const start = (this.pageTematicas - 1) * this.itemsPerPage;
    return this.reporteTematicas.slice(start, start + this.itemsPerPage);
  }
  get paginatedCruce() {
    const start = (this.pageCruce - 1) * this.itemsPerPage;
    return this.reporteCruce.slice(start, start + this.itemsPerPage);
  }

  nextPage(reporte: string) {
    if (reporte === 'tickets' && this.pageTickets * this.itemsPerPage < this.filteredTickets.length) this.pageTickets++;
    if (reporte === 'usuarios' && this.pageUsuarios * this.itemsPerPage < this.filteredUsuarios.length) this.pageUsuarios++;
    if (reporte === 'incidencias' && this.pageIncidencias * this.itemsPerPage < this.incidencias.length) this.pageIncidencias++;
    if (reporte === 'ires' && this.pageIres * this.itemsPerPage < this.reporteIres.length) this.pageIres++;
    if (reporte === 'tematicas' && this.pageTematicas * this.itemsPerPage < this.reporteTematicas.length) this.pageTematicas++;
    if (reporte === 'cruce' && this.pageCruce * this.itemsPerPage < this.reporteCruce.length) this.pageCruce++;
  }
  prevPage(reporte: string) {
    if (reporte === 'tickets' && this.pageTickets > 1) this.pageTickets--;
    if (reporte === 'usuarios' && this.pageUsuarios > 1) this.pageUsuarios--;
    if (reporte === 'incidencias' && this.pageIncidencias > 1) this.pageIncidencias--;
    if (reporte === 'ires' && this.pageIres > 1) this.pageIres--;
    if (reporte === 'tematicas' && this.pageTematicas > 1) this.pageTematicas--;
    if (reporte === 'cruce' && this.pageCruce > 1) this.pageCruce--;
  }

  totalPages(reporte: string): number {
    if (reporte === 'tickets') return Math.ceil(this.filteredTickets.length / this.itemsPerPage) || 1;
    if (reporte === 'usuarios') return Math.ceil(this.filteredUsuarios.length / this.itemsPerPage) || 1;
    if (reporte === 'incidencias') return Math.ceil(this.incidencias.length / this.itemsPerPage) || 1;
    if (reporte === 'ires') return Math.ceil(this.reporteIres.length / this.itemsPerPage) || 1;
    if (reporte === 'tematicas') return Math.ceil(this.reporteTematicas.length / this.itemsPerPage) || 1;
    if (reporte === 'cruce') return Math.ceil(this.reporteCruce.length / this.itemsPerPage) || 1;
    return 1;
  }

  getMaxValue(reporte: string): number {
    if (reporte === 'ires' && this.reporteIres.length > 0) return this.reporteIres[0].total_tickets;
    if (reporte === 'tematicas' && this.reporteTematicas.length > 0) return this.reporteTematicas[0].total;
    return 1;
  }

  // --- SLA CALCULATION ---
  getTiempoResolucion(t: any): string {
    const startStr = t.fecha_creacion;
    const endStr = t.fecha_resolucion;
    const estado = (t.estado || '').toUpperCase();
    
    if (!startStr) return '—';
    const start = new Date(startStr);
    const end = endStr ? new Date(endStr) : new Date();

    if (isNaN(start.getTime())) return '—';
    if (start > end) return '0 min';

    let totalMinutes = 0;
    const current = new Date(start);

    // Business hours: 08:30 to 17:30 (9 hours/day = 540 min)
    const bStartHour = 8;
    const bStartMin = 30;
    const bEndHour = 17;
    const bEndMin = 30;

    while (current < end) {
      if (this.isWorkDay(current)) {
        const dayStart = new Date(current);
        dayStart.setHours(bStartHour, bStartMin, 0, 0);

        const dayEnd = new Date(current);
        dayEnd.setHours(bEndHour, bEndMin, 0, 0);

        const effectiveStart = current > dayStart ? current : dayStart;
        const effectiveEnd = end < dayEnd ? end : dayEnd;

        if (effectiveStart < effectiveEnd) {
          totalMinutes += (effectiveEnd.getTime() - effectiveStart.getTime()) / 60000;
        }
      }
      current.setDate(current.getDate() + 1);
      current.setHours(bStartHour, bStartMin, 0, 0);
    }

    if (totalMinutes === 0 && !endStr) return 'en curso';
    
    const d = Math.floor(totalMinutes / 540); // 9 hours per day
    const h = Math.floor((totalMinutes % 540) / 60);
    const m = Math.floor(totalMinutes % 60);

    let res = '';
    if (d > 0) res += `${d}d `;
    if (h > 0 || d > 0) res += `${h}h `;
    res += `${m} min`;
    
    // If ticket is resolved or has end date, show final time. Otherwise show (en curso).
    const isResolved = estado === 'RESUELTO' || estado === 'CERRADO' || estado === 'RECHAZADO';
    return (endStr || isResolved) ? res : `${res} (en curso)`;
  }

  isWorkDay(date: Date): boolean {
    const day = date.getDay();
    if (day === 0 || day === 6) return false;

    const d = date.getDate();
    const m = date.getMonth() + 1;
    const y = date.getFullYear();

    const holidays = [
      '1-1', '5-1', '6-7', '6-29', '7-23', '7-28', '7-29', '8-6', '8-30', '10-8', '11-1', '12-8', '12-9', '12-25'
    ];

    if (holidays.includes(`${m}-${d}`)) return false;

    const easter = this.getEaster(y);
    const holyThursday = new Date(easter);
    holyThursday.setDate(easter.getDate() - 3);
    const goodFriday = new Date(easter);
    goodFriday.setDate(easter.getDate() - 2);

    if (this.isSameDay(date, holyThursday) || this.isSameDay(date, goodFriday)) return false;

    return true;
  }

  private isSameDay(d1: Date, d2: Date) {
    return d1.getDate() === d2.getDate() && d1.getMonth() === d2.getMonth() && d1.getFullYear() === d2.getFullYear();
  }

  private getEaster(year: number): Date {
    const a = year % 19;
    const b = Math.floor(year / 100);
    const c = year % 100;
    const d = Math.floor(b / 4);
    const e = b % 4;
    const f = Math.floor((b + 8) / 25);
    const g = Math.floor((b - f + 1) / 3);
    const h = (19 * a + b - d - g + 15) % 30;
    const i = Math.floor(c / 4);
    const k = c % 4;
    const l = (32 + 2 * e + 2 * i - h - k) % 7;
    const m = Math.floor((a + 11 * h + 22 * l) / 451);
    const n = Math.floor((h + l - 7 * m + 114) / 31);
    const p = (h + l - 7 * m + 114) % 31;
    return new Date(year, n - 1, p + 1);
  }

  get donutSlices() {
    const stats = this.statsCalculadas;
    const total = stats.total || 0;
    if (total === 0) {
      return [
        { label: 'Sin datos', count: 0, percentage: 100, dashArray: '439.82 439.82', dashOffset: 0, color: '#4b5563' }
      ];
    }
    const pendientes = stats.pendientes || 0;
    const en_proceso = stats.en_proceso || 0;
    const resueltos = stats.resueltos || 0;
    const rechazados = stats.rechazados || 0;

    const c = 439.82;
    const resArr = (resueltos / total) * c;
    const procArr = (en_proceso / total) * c;
    const pendArr = (pendientes / total) * c;
    const rechArr = (rechazados / total) * c;

    return [
      { label: 'Resueltos', count: resueltos, percentage: Math.round((resueltos / total) * 100), dashArray: `${resArr} ${c}`, dashOffset: 0, color: '#10b981' },
      { label: 'En Proceso', count: en_proceso, percentage: Math.round((en_proceso / total) * 100), dashArray: `${procArr} ${c}`, dashOffset: -resArr, color: '#38bdf8' },
      { label: 'Pendientes', count: pendientes, percentage: Math.round((pendientes / total) * 100), dashArray: `${pendArr} ${c}`, dashOffset: -(resArr + procArr), color: '#f59e0b' },
      { label: 'Rechazados', count: rechazados, percentage: Math.round((rechazados / total) * 100), dashArray: `${rechArr} ${c}`, dashOffset: -(resArr + procArr + pendArr), color: '#ef4444' }
    ];
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

  getEvidencias(ticket: any): string[] {
    const link = ticket.link_drive || ticket.linkDrive;
    if (!link) return [];
    return link.split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0);
  }

  get token(): string {
    return this.authService.getToken() || '';
  }

  getShortIreName(nombreIre: string): string {
    if (!nombreIre) return '';
    let name = nombreIre;
    if (name.includes('Dirección de Inteligencia Inspectiva')) {
      return 'DINI';
    }
    if (name.includes('Subdirección de Intervenciones Especiales')) {
      return 'SDIE';
    }
    return name.replace(/Intendencia Regional de\s+/gi, '')
               .replace(/Intendencia Regional\s+/gi, '')
               .replace(/Intendencia de\s+/gi, '')
               .replace(/Intendencia\s+/gi, '')
               .trim();
  }
}

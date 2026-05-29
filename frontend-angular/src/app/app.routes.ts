import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { AdminComponent } from './components/admin/admin.component';
import { EspecialistaComponent } from './components/especialista/especialista.component';
import { InspectorComponent } from './components/inspector/inspector.component';
import { ChangePasswordComponent } from './components/change-password/change-password.component';
import { ConfirmTicketComponent } from './components/confirm-ticket/confirm-ticket.component';
import { PrimerLoginComponent } from './components/first-login/primer-login.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: '/login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'admin', component: AdminComponent, canActivate: [authGuard], data: { roles: ['ADMIN'] } },
    { path: 'especialista', component: EspecialistaComponent, canActivate: [authGuard], data: { roles: ['ESPECIALISTA'] } },
    { path: 'usuario', component: InspectorComponent, canActivate: [authGuard], data: { roles: ['INSPECTOR', 'SUPERVISOR', 'SIFS', 'SIFN', 'SISA', 'ESPECIALISTA_SIFS', 'ESPECIALISTA_SIFN', 'ESPECIALISTA_SISA'] } },
    { path: 'cambiar-password', component: ChangePasswordComponent, canActivate: [authGuard] },
    { path: 'actualizar-password', component: PrimerLoginComponent, canActivate: [authGuard] },
    { path: 'confirmar-ticket', component: ConfirmTicketComponent } // Pública para links de correo
];

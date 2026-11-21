import { Routes } from '@angular/router';
import { authGuard } from './core/auth-guard';
import { Home } from './pages/home/home';
import { Vehicles } from './pages/vehicles/vehicles';
import { VehicleDetail } from './pages/vehicle-detail/vehicle-detail';
import { Register } from './pages/register/register';
import { Login } from './pages/login/login';
import { TestDrive } from './pages/test-drive/test-drive';
import { Appointments } from './pages/appointments/appointments';
import { Admin } from './pages/admin/admin';
import { VehicleManagement } from './pages/vehicle-management/vehicle-management';
import { AppointmentManagement } from './pages/appointment-management/appointment-management';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'veiculos', component: Vehicles },
  { path: 'veiculos/:id', component: VehicleDetail },
  { path: 'cadastro', component: Register },
  { path: 'login', component: Login },
  { path: 'test-drive/:id', component: TestDrive, canActivate: [authGuard] },
  { path: 'meus-agendamentos', component: Appointments, canActivate: [authGuard] },
  { path: 'admin', component: Admin, canActivate: [authGuard], data: { admin: true } },
  { path: 'admin/veiculos', component: VehicleManagement, canActivate: [authGuard], data: { admin: true } },
  { path: 'admin/agendamentos', component: AppointmentManagement, canActivate: [authGuard], data: { admin: true } },
  { path: '**', redirectTo: '' },
];

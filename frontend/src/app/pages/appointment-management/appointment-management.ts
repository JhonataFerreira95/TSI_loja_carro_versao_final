import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api, Agendamento, Carro } from '../../core/api';

@Component({
  selector: 'app-appointment-management',
  imports: [DatePipe, FormsModule],
  templateUrl: './appointment-management.html',
  styleUrl: './appointment-management.scss',
})
export class AppointmentManagement implements OnInit {
  agendamentos: Agendamento[] = [];
  carros: Carro[] = [];
  dataFiltro = '';
  carroFiltro = '';
  loading = true;
  mensagem = '';
  erro = false;
  constructor(private api: Api) {}
  ngOnInit() {
    this.api.listarCarros().subscribe({
      next: carros => this.carros = carros,
      error: () => { this.erro = true; this.mensagem = 'Falha ao carregar os veículos.'; }
    });
    this.listar();
  }
  listar() {
    this.loading = true;
    this.api.listarAgendamentos({
      data: this.dataFiltro || undefined,
      carroId: this.carroFiltro ? Number(this.carroFiltro) : undefined
    }).subscribe({
      next: itens => { this.agendamentos = itens; this.loading = false; },
      error: () => { this.erro = true; this.mensagem = 'Falha ao carregar agendamentos.'; this.loading = false; }
    });
  }
  alterar(item: Agendamento, status: string) {
    this.api.alterarStatus(item.id, status).subscribe({
      next: () => { this.erro = false; this.mensagem = 'Status atualizado.'; this.listar(); },
      error: resposta => { this.erro = true; this.mensagem = resposta.error?.erro || 'Falha ao atualizar status.'; }
    });
  }

  statusLabel(status: Agendamento['status']): string {
    const nomes: Record<Agendamento['status'], string> = {
      CONFIRMADO: 'Confirmado',
      CANCELADO: 'Cancelado',
      REALIZADO: 'Realizado',
      PENDENTE: 'Pendente (legado)',
      CONCLUIDO: 'Concluído (legado)'
    };
    return nomes[status];
  }
}

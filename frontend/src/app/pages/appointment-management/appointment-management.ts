import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api, Agendamento } from '../../core/api';

@Component({
  selector: 'app-appointment-management',
  imports: [DatePipe],
  templateUrl: './appointment-management.html',
  styleUrl: './appointment-management.scss',
})
export class AppointmentManagement implements OnInit {
  agendamentos: Agendamento[] = [];
  loading = true;
  mensagem = '';
  erro = false;
  constructor(private api: Api) {}
  ngOnInit() { this.listar(); }
  listar() {
    this.loading = true;
    this.api.listarAgendamentos().subscribe({
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
}

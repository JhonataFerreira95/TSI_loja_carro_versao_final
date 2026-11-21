import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Api } from '../../core/api';

@Component({
  selector: 'app-admin',
  imports: [RouterLink],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin implements OnInit {
  carros = 0;
  agendamentos = 0;
  usuarios = 0;
  constructor(private api: Api) {}
  ngOnInit() {
    this.api.listarCarros().subscribe(itens => this.carros = itens.length);
    this.api.listarAgendamentos().subscribe(itens => this.agendamentos = itens.length);
    this.api.listarUsuarios().subscribe(itens => this.usuarios = itens.length);
  }
}

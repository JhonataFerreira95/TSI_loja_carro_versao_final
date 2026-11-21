import { Component, OnInit } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api, Carro } from '../../core/api';

@Component({
  selector: 'app-vehicles',
  imports: [CurrencyPipe, FormsModule, RouterLink],
  templateUrl: './vehicles.html',
  styleUrl: './vehicles.scss',
})
export class Vehicles implements OnInit {
  carros: Carro[] = [];
  filtro = '';
  loading = true;
  erro = '';
  constructor(private api: Api) {}
  ngOnInit() { this.carregar(); }
  carregar() {
    this.loading = true;
    this.api.listarCarros().subscribe({
      next: carros => { this.carros = carros; this.loading = false; },
      error: () => { this.erro = 'Não foi possível carregar os veículos.'; this.loading = false; }
    });
  }
  get filtrados() {
    const texto = this.filtro.trim().toLowerCase();
    return texto ? this.carros.filter(c => c.modelo.toLowerCase().includes(texto)) : this.carros;
  }
}

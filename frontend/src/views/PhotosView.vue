<script setup>
import { onMounted, ref } from 'vue'
import { api, lerErro } from '../api'

const carros = ref([])
const mensagem = ref('')
const erro = ref(false)

async function listar() {
  const resposta = await api('/carro/meusCarros')
  if (resposta.ok) carros.value = await resposta.json()
  else await exibirErro(resposta)
}

async function enviarFoto(carro, evento) {
  const arquivo = evento.target.files[0]
  if (!arquivo) {
    mensagem.value = 'Selecione uma imagem do computador.'
    erro.value = true
    return
  }
  const dados = new FormData()
  dados.append('imagem', arquivo)
  const resposta = await api(`/carro/${carro.id}/imagem`, { method: 'PUT', body: dados })
  if (resposta.ok) {
    mensagem.value = 'Foto enviada com sucesso.'
    erro.value = false
    await listar()
  } else await exibirErro(resposta)
}

async function removerFoto(id) {
  if (!confirm('Deseja realmente remover esta foto?')) return
  const resposta = await api(`/carro/${id}/imagem`, { method: 'DELETE' })
  if (resposta.ok) {
    mensagem.value = 'Foto removida com sucesso.'
    erro.value = false
    await listar()
  } else await exibirErro(resposta)
}

async function exibirErro(resposta) {
  mensagem.value = await lerErro(resposta)
  erro.value = true
}

onMounted(listar)
</script>

<template>
  <section class="card">
    <h1>Fotos dos meus carros</h1>
    <p>Você pode enviar ou remover fotos dos carros associados à sua conta.</p>
    <p v-if="mensagem" :class="['mensagem', erro ? 'erro' : 'sucesso']">{{ mensagem }}</p>
    <p v-if="carros.length === 0">Nenhum carro está cadastrado na sua conta.</p>
    <div v-else class="tabela-wrapper">
      <table>
        <thead><tr><th>Modelo</th><th>Ano</th><th>Foto atual</th><th>Nova foto</th><th>Ações</th></tr></thead>
        <tbody>
          <tr v-for="carro in carros" :key="carro.id">
            <td>{{ carro.modelo }}</td><td>{{ carro.ano }}</td>
            <td><img v-if="carro.imagem" :src="carro.imagem" :alt="`Foto do carro ${carro.modelo}`" /></td>
            <td><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" :aria-label="`Selecionar nova foto para ${carro.modelo}`" @change="enviarFoto(carro, $event)" /></td>
            <td><button v-if="carro.imagem" type="button" class="perigo" @click="removerFoto(carro.id)">Remover foto</button></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

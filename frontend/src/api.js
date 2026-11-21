export async function lerErro(resposta) {
  try {
    const corpo = await resposta.json()
    return corpo.erro || `Erro ${resposta.status}`
  } catch {
    return `Erro ${resposta.status}`
  }
}

export async function api(url, opcoes = {}) {
  const resposta = await fetch(url, opcoes)
  if (resposta.status === 401) {
    window.location.hash = '#/login'
  }
  return resposta
}

export function enviarJson(url, metodo, dados) {
  return api(url, {
    method: metodo,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(dados),
  })
}

package br.com.gomestech.otimizador.ui

import android.content.Intent
import android.net.Uri
import android.view.View
import br.com.gomestech.otimizador.R
import br.com.gomestech.otimizador.data.Permissoes
import br.com.gomestech.otimizador.data.ScannerApps
import br.com.gomestech.otimizador.logica.Tempo
import br.com.gomestech.otimizador.util.Formato
import br.com.gomestech.otimizador.util.abrirComSeguranca
import com.google.android.material.chip.Chip

/** Apps esquecidos: os que você não abre há muito tempo, com o espaço que ocupam. */
class AppsFragment : BaseListaFragment() {

    override val titulo = "Apps esquecidos"
    override val explicacao =
        "Apps parados ocupam espaço e alguns ainda rodam escondidos, gastando bateria. " +
            "Desinstale os que você não usa (dá para baixar de novo na Play Store quando quiser)."
    override val textoVazio = "Nenhum app esquecido nesse período. Ótimo!"

    @Volatile
    private var dias = 30

    override fun verificarPermissao(): Boolean {
        val ok = Permissoes.temAcessoAoUso(requireContext())
        if (!ok) {
            mostrarPedidoPermissao(getString(R.string.permissao_uso_texto), getString(R.string.permissao_uso_botao)) {
                requireContext().abrirComSeguranca(Permissoes.intentAcessoAoUso(requireContext()))
            }
        }
        return ok
    }

    override fun configurarExtras() {
        chips.visibility = View.VISIBLE
        chips.removeAllViews()
        listOf(30, 60, 90).forEach { d ->
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = "Sem uso há $d+ dias"
                isCheckable = true
                isChecked = d == dias
                setOnClickListener {
                    if (dias != d) {
                        dias = d
                        recarregar()
                    }
                }
            }
            chips.addView(chip)
        }
    }

    override fun carregar(): List<Linha> {
        val ctx = requireContext().applicationContext
        if (!Permissoes.temAcessoAoUso(ctx)) return emptyList()
        val limite = dias
        val agora = System.currentTimeMillis()
        return ScannerApps.escanear(ctx)
            .filter { Tempo.ehEsquecido(it.ultimoUso, it.instaladoEm, limite, agora) }
            .sortedByDescending { it.tamanhoTotal }
            .map {
                val tamanho = if (it.tamanhoTotal > 0) " • ${Formato.tamanho(ctx, it.tamanhoTotal)}" else ""
                Linha(
                    id = it.pacote,
                    titulo = it.nome,
                    detalhe = Formato.haQuantoTempo(it.ultimoUso) + tamanho +
                        if (it.sistema) " • veio de fábrica" else "",
                    icone = it.icone,
                    acao = if (it.sistema) "Desativar" else "Desinstalar",
                    dado = it
                )
            }
    }

    override fun resumo(itens: List<Linha>): String {
        if (itens.isEmpty()) return ""
        val total = itens.sumOf { (it.dado as? ScannerApps.AppInfo)?.tamanhoTotal ?: 0L }
        return "${itens.size} apps • ${Formato.tamanho(requireContext(), total)} ocupados"
    }

    override fun aoTocarAcao(linha: Linha) {
        val app = linha.dado as? ScannerApps.AppInfo ?: return
        val ctx = requireContext()
        if (app.sistema) {
            // App de fábrica: só dá para desativar, pela tela do próprio app.
            ctx.abrirComSeguranca(Permissoes.intentDetalhesDoApp(app.pacote))
        } else {
            @Suppress("DEPRECATION")
            val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pacote}"))
            if (!ctx.abrirComSeguranca(intent, "Não foi possível abrir a desinstalação.")) {
                ctx.abrirComSeguranca(Permissoes.intentDetalhesDoApp(app.pacote))
            }
        }
    }
}

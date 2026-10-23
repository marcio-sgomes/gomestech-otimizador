package br.com.gomestech.otimizador.ui

import android.os.Build
import android.view.View
import android.widget.Toast
import br.com.gomestech.otimizador.R
import br.com.gomestech.otimizador.data.Marcas
import br.com.gomestech.otimizador.data.Permissoes
import br.com.gomestech.otimizador.data.ScannerApps
import br.com.gomestech.otimizador.util.Formato
import br.com.gomestech.otimizador.util.abrirComSeguranca
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Limpeza guiada de cache: mostra os apps com mais cache e abre a tela certa para limpar. */
class LimpezaFragment : BaseListaFragment() {

    override val titulo = "Limpeza de cache"
    override val explicacao =
        "O Android não deixa nenhum app apagar o cache de outro sozinho (os “limpadores” que prometem isso enganam). " +
            "Aqui você vê quais apps acumulam mais cache e, com um toque, vai direto na tela para limpar. " +
            "Limpar cache não apaga conversas, fotos nem logins."
    override val textoVazio = "Nenhum app com cache relevante. Tudo limpo!"

    override fun verificarPermissao(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val ok = Permissoes.temAcessoAoUso(requireContext())
        if (!ok) {
            mostrarPedidoPermissao(getString(R.string.permissao_uso_texto), getString(R.string.permissao_uso_botao)) {
                requireContext().abrirComSeguranca(Permissoes.intentAcessoAoUso(requireContext()))
            }
        }
        return ok
    }

    override fun configurarExtras() {
        btnAcaoExtra.visibility = View.VISIBLE
        btnAcaoExtra.text = "Limpeza oficial"
        btnAcaoExtra.setOnClickListener {
            val ctx = requireContext()
            if (!Marcas.abrir(ctx, Marcas.atalhoManutencao(ctx))) {
                ctx.abrirComSeguranca(Marcas.intentFilesByGoogleNaLoja())
            }
        }
    }

    override fun carregar(): List<Linha> {
        val ctx = requireContext().applicationContext
        // O cache do próprio otimizador a gente pode limpar sozinho.
        ctx.cacheDir.listFiles()?.filter { it.name != "updates" }?.forEach { it.deleteRecursively() }
        ctx.externalCacheDir?.deleteRecursively()

        val semTamanho = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        val minimo = 5L * 1024 * 1024 // só mostra cache acima de 5 MB
        return ScannerApps.escanear(ctx)
            .filter { semTamanho || it.tamanhoCache >= minimo }
            .sortedByDescending { it.tamanhoCache }
            .map {
                Linha(
                    id = it.pacote,
                    titulo = it.nome,
                    detalhe = if (semTamanho) "Toque para ver o armazenamento"
                    else "Cache: ${Formato.tamanho(ctx, it.tamanhoCache)}",
                    icone = it.icone,
                    acao = getStringSafe(R.string.limpar_cache),
                    dado = it.tamanhoCache
                )
            }
    }

    private fun getStringSafe(id: Int): String = context?.getString(id) ?: "Limpar cache"

    override fun resumo(itens: List<Linha>): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return "${itens.size} apps"
        val total = itens.sumOf { (it.dado as? Long) ?: 0L }
        return if (itens.isEmpty()) "" else "${Formato.tamanho(requireContext(), total)} de cache em ${itens.size} apps"
    }

    override fun aoTocarAcao(linha: Linha) {
        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("dicas", 0)
        val abrir = {
            ctx.abrirComSeguranca(Permissoes.intentDetalhesDoApp(linha.id))
            Unit
        }
        if (prefs.getBoolean("cache_visto", false)) {
            abrir()
        } else {
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Como limpar o cache")
                .setMessage(Marcas.dicaCache() + "\n\nDepois é só voltar para cá que a lista se atualiza.")
                .setPositiveButton(R.string.ok) { _, _ ->
                    prefs.edit().putBoolean("cache_visto", true).apply()
                    abrir()
                }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }
    }

    override fun aoTocarLinha(linha: Linha) {
        Toast.makeText(requireContext(), Marcas.dicaCache(), Toast.LENGTH_LONG).show()
    }
}

package br.com.gomestech.otimizador.ui

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import br.com.gomestech.otimizador.R
import br.com.gomestech.otimizador.data.Permissoes
import br.com.gomestech.otimizador.data.ScannerArquivos
import br.com.gomestech.otimizador.util.Formato
import br.com.gomestech.otimizador.util.abrirComSeguranca
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Fotos, vídeos e áudios grandes (inclusive do WhatsApp), com opção de apagar. */
class ArquivosFragment : BaseListaFragment() {

    override val titulo = "Arquivos grandes"
    override val explicacao =
        "Fotos, vídeos e áudios acima de 10 MB, do maior para o menor. Toque no nome para ver o arquivo antes de apagar. " +
            "Dica: faça backup no Google Fotos antes de apagar fotos e vídeos importantes."
    override val textoVazio = "Nenhum arquivo grande encontrado."

    private enum class Filtro(val rotulo: String) {
        TODOS("Todos"), WHATSAPP("WhatsApp"), VIDEOS("Vídeos"), FOTOS("Fotos"), AUDIOS("Áudios")
    }

    @Volatile
    private var filtro = Filtro.TODOS
    private var aguardandoExclusao: Linha? = null

    private val pedirMidia =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { recarregar() }

    private val confirmarExclusao =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
            val linha = aguardandoExclusao
            aguardandoExclusao = null
            if (r.resultCode == Activity.RESULT_OK && linha != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    adapter.remover(linha.id)
                } else {
                    // Android 10: a autorização foi dada, agora apaga de fato.
                    apagarDireto(linha)
                }
            }
        }

    override fun verificarPermissao(): Boolean {
        val ctx = requireContext()
        val ok = Permissoes.temMidia(ctx)
        if (!ok) {
            mostrarPedidoPermissao(getString(R.string.permissao_midia_texto), getString(R.string.permissao_midia_botao)) {
                val prefs = ctx.getSharedPreferences("dicas", 0)
                val jaPediu = prefs.getBoolean("midia_pedida", false)
                val podePerguntar = Permissoes.permissoesMidia().any { shouldShowRequestPermissionRationale(it) }
                if (jaPediu && !podePerguntar) {
                    // O usuário marcou "não perguntar de novo": só pela tela do app.
                    Toast.makeText(ctx, "Toque em Permissões e libere Fotos e vídeos / Músicas e áudio.", Toast.LENGTH_LONG).show()
                    ctx.abrirComSeguranca(Permissoes.intentDetalhesDoApp(ctx.packageName))
                } else {
                    prefs.edit().putBoolean("midia_pedida", true).apply()
                    pedirMidia.launch(Permissoes.permissoesMidia())
                }
            }
        }
        return ok
    }

    override fun configurarExtras() {
        chips.visibility = View.VISIBLE
        chips.removeAllViews()
        Filtro.values().forEach { f ->
            chips.addView(Chip(requireContext()).apply {
                id = View.generateViewId()
                text = f.rotulo
                isCheckable = true
                isChecked = f == filtro
                setOnClickListener {
                    if (filtro != f) {
                        filtro = f
                        recarregar()
                    }
                }
            })
        }
    }

    override fun carregar(): List<Linha> {
        val ctx = requireContext().applicationContext
        if (!Permissoes.temMidia(ctx)) return emptyList()
        val f = filtro
        return ScannerArquivos.escanear(ctx)
            .filter {
                when (f) {
                    Filtro.TODOS -> true
                    Filtro.WHATSAPP -> it.doWhatsApp
                    Filtro.VIDEOS -> it.tipo == ScannerArquivos.Tipo.VIDEO
                    Filtro.FOTOS -> it.tipo == ScannerArquivos.Tipo.FOTO
                    Filtro.AUDIOS -> it.tipo == ScannerArquivos.Tipo.AUDIO
                }
            }
            .map {
                val pasta = it.pasta.trimEnd('/').substringAfterLast('/').ifBlank { "—" }
                Linha(
                    id = it.uri.toString(),
                    titulo = it.nome,
                    detalhe = "${it.tipo.rotulo} • ${Formato.tamanho(ctx, it.tamanho)} • pasta $pasta",
                    icone = null,
                    acao = "Apagar",
                    dado = it
                )
            }
    }

    override fun resumo(itens: List<Linha>): String {
        if (itens.isEmpty()) return ""
        val total = itens.sumOf { (it.dado as? ScannerArquivos.Arquivo)?.tamanho ?: 0L }
        return "${itens.size} arquivos • ${Formato.tamanho(requireContext(), total)}"
    }

    /** Abre o arquivo para a pessoa conferir antes de apagar. */
    override fun aoTocarLinha(linha: Linha) {
        val arq = linha.dado as? ScannerArquivos.Arquivo ?: return
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(arq.uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        requireContext().abrirComSeguranca(intent, "Nenhum app para abrir este arquivo.")
    }

    override fun aoTocarAcao(linha: Linha) {
        val arq = linha.dado as? ScannerArquivos.Arquivo ?: return
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: o próprio sistema mostra a confirmação.
            val pedido = MediaStore.createDeleteRequest(ctx.contentResolver, listOf(arq.uri))
            aguardandoExclusao = linha
            confirmarExclusao.launch(IntentSenderRequest.Builder(pedido.intentSender).build())
        } else {
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Apagar arquivo?")
                .setMessage("${arq.nome}\n${Formato.tamanho(ctx, arq.tamanho)}\n\nIsso não pode ser desfeito.")
                .setPositiveButton(R.string.apagar) { _, _ -> apagarDireto(linha) }
                .setNegativeButton(R.string.cancelar, null)
                .show()
        }
    }

    private fun apagarDireto(linha: Linha) {
        val arq = linha.dado as? ScannerArquivos.Arquivo ?: return
        val ctx = context ?: return
        try {
            val apagados = ctx.contentResolver.delete(arq.uri, null, null)
            if (apagados > 0) adapter.remover(linha.id)
            else Toast.makeText(ctx, "Não foi possível apagar.", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                aguardandoExclusao = linha
                confirmarExclusao.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
            } else {
                Toast.makeText(ctx, "Sem permissão para apagar este arquivo.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

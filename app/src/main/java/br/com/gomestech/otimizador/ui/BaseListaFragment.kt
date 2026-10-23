package br.com.gomestech.otimizador.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.gomestech.otimizador.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TELA MODELO das listas (Limpeza, Apps e Arquivos).
 * Toda a parte visual e o carregamento ficam aqui; cada tela só diz
 * o título, a explicação, de onde vêm os itens e o que o botão faz.
 */
abstract class BaseListaFragment : Fragment() {

    protected lateinit var txtTitulo: TextView
    protected lateinit var txtExplicacao: TextView
    protected lateinit var cardPermissao: MaterialCardView
    protected lateinit var txtPermissao: TextView
    protected lateinit var btnPermissao: MaterialButton
    protected lateinit var txtResumo: TextView
    protected lateinit var btnAcaoExtra: MaterialButton
    protected lateinit var chips: ChipGroup
    private lateinit var progresso: ProgressBar
    private lateinit var txtVazio: TextView
    protected lateinit var adapter: LinhaAdapter

    /** true enquanto a lista está sendo analisada (usado pelos testes automáticos). */
    @Volatile
    var carregando: Boolean = false
        private set

    /** Itens mostrados agora (usado pelos testes automáticos). */
    fun itensVisiveis(): List<Linha> = if (::adapter.isInitialized) adapter.itensAtuais() else emptyList()

    abstract val titulo: String
    abstract val explicacao: String
    abstract val textoVazio: String

    /** Retorna true se já tem a permissão necessária (ou mostra o cartão pedindo). */
    abstract fun verificarPermissao(): Boolean

    /** Carrega os itens (roda em segundo plano). */
    abstract fun carregar(): List<Linha>

    /** Texto do resumo no topo depois de carregar. */
    abstract fun resumo(itens: List<Linha>): String

    abstract fun aoTocarAcao(linha: Linha)
    open fun aoTocarLinha(linha: Linha) {}
    open fun configurarExtras() {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_lista, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        txtTitulo = view.findViewById(R.id.txtTitulo)
        txtExplicacao = view.findViewById(R.id.txtExplicacao)
        cardPermissao = view.findViewById(R.id.cardPermissao)
        txtPermissao = view.findViewById(R.id.txtPermissao)
        btnPermissao = view.findViewById(R.id.btnPermissao)
        txtResumo = view.findViewById(R.id.txtResumo)
        btnAcaoExtra = view.findViewById(R.id.btnAcaoExtra)
        chips = view.findViewById(R.id.chipsFiltro)
        progresso = view.findViewById(R.id.progresso)
        txtVazio = view.findViewById(R.id.txtVazio)

        txtTitulo.text = titulo
        txtExplicacao.text = explicacao

        adapter = LinhaAdapter({ aoTocarAcao(it) }, { aoTocarLinha(it) })
        view.findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BaseListaFragment.adapter
        }
        view.findViewById<MaterialButton>(R.id.btnRecarregar).setOnClickListener { recarregar() }
        configurarExtras()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega ao voltar (ex.: depois de limpar cache ou liberar permissão).
        recarregar()
    }

    fun recarregar() {
        if (!isAdded) return
        val temPermissao = verificarPermissao()
        cardPermissao.visibility = if (temPermissao) View.GONE else View.VISIBLE
        progresso.visibility = View.VISIBLE
        txtVazio.visibility = View.GONE
        txtResumo.text = getString(R.string.carregando)
        carregando = true
        viewLifecycleOwner.lifecycleScope.launch {
            val itens = try {
                withContext(Dispatchers.Default) { carregar() }
            } catch (e: Exception) {
                emptyList()
            }
            if (!isAdded) {
                carregando = false
                return@launch
            }
            progresso.visibility = View.GONE
            adapter.definir(itens)
            txtResumo.text = resumo(itens)
            txtVazio.text = textoVazio
            txtVazio.visibility = if (itens.isEmpty()) View.VISIBLE else View.GONE
            carregando = false
        }
    }

    protected fun mostrarPedidoPermissao(texto: String, botao: String, acao: () -> Unit) {
        txtPermissao.text = texto
        btnPermissao.text = botao
        btnPermissao.setOnClickListener { acao() }
    }
}

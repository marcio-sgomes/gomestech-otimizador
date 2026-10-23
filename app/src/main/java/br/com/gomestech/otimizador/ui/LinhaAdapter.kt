package br.com.gomestech.otimizador.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.gomestech.otimizador.R
import com.google.android.material.button.MaterialButton

/** Uma linha genérica da lista (ícone, nome, detalhe e um botão). Usada em todas as telas de lista. */
data class Linha(
    val id: String,
    val titulo: String,
    val detalhe: String,
    val icone: Drawable?,
    val iconeRes: Int = R.drawable.ic_arquivo,
    val acao: String?,
    val dado: Any? = null
)

class LinhaAdapter(
    private val aoTocarAcao: (Linha) -> Unit,
    private val aoTocarLinha: ((Linha) -> Unit)? = null
) : RecyclerView.Adapter<LinhaAdapter.VH>() {

    private val itens = mutableListOf<Linha>()

    fun definir(novos: List<Linha>) {
        itens.clear()
        itens.addAll(novos)
        notifyDataSetChanged()
    }

    fun remover(id: String) {
        val i = itens.indexOfFirst { it.id == id }
        if (i >= 0) {
            itens.removeAt(i)
            notifyItemRemoved(i)
        }
    }

    /** Cópia dos itens mostrados agora (usada pelos testes automáticos). */
    fun itensAtuais(): List<Linha> = itens.toList()

    override fun getItemCount() = itens.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_linha, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val l = itens[position]
        holder.nome.text = l.titulo
        holder.detalhe.text = l.detalhe
        if (l.icone != null) holder.icone.setImageDrawable(l.icone) else holder.icone.setImageResource(l.iconeRes)
        if (l.acao == null) {
            holder.botao.visibility = View.GONE
        } else {
            holder.botao.visibility = View.VISIBLE
            holder.botao.text = l.acao
            holder.botao.setOnClickListener { aoTocarAcao(l) }
        }
        holder.itemView.setOnClickListener { aoTocarLinha?.invoke(l) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icone: ImageView = v.findViewById(R.id.imgIcone)
        val nome: TextView = v.findViewById(R.id.txtNome)
        val detalhe: TextView = v.findViewById(R.id.txtDetalhe)
        val botao: MaterialButton = v.findViewById(R.id.btnAcao)
    }
}

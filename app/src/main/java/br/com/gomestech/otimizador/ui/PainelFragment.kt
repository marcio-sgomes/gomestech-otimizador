package br.com.gomestech.otimizador.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import br.com.gomestech.otimizador.R
import br.com.gomestech.otimizador.data.Aparelho
import br.com.gomestech.otimizador.data.Marcas
import br.com.gomestech.otimizador.reminder.Checklist
import br.com.gomestech.otimizador.reminder.Lembrete
import br.com.gomestech.otimizador.util.Formato
import br.com.gomestech.otimizador.util.abrirComSeguranca
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Tela inicial: nota de saúde do celular, armazenamento, RAM, bateria e checklist. */
class PainelFragment : Fragment() {

    private val pedirNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            Lembrete.definir(requireContext(), concedida)
            view?.findViewById<MaterialSwitch>(R.id.swLembrete)?.isChecked = concedida
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_painel, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        montarChecklist(view)
        val btnMarca = view.findViewById<MaterialButton>(R.id.btnManutencaoMarca)
        val atalho = Marcas.atalhoManutencao(requireContext())
        btnMarca.text = atalho.titulo
        btnMarca.setOnClickListener {
            val ctx = requireContext()
            if (!Marcas.abrir(ctx, atalho)) ctx.abrirComSeguranca(Marcas.intentFilesByGoogleNaLoja())
        }

        val sw = view.findViewById<MaterialSwitch>(R.id.swLembrete)
        sw.isChecked = Lembrete.ativo(requireContext())
        sw.setOnCheckedChangeListener { _, ligado ->
            val ctx = requireContext()
            if (ligado && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                pedirNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Lembrete.definir(ctx, ligado)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { atualizar(it) }
    }

    private fun atualizar(v: View) {
        val ctx = requireContext()

        val nota = Aparelho.nota(ctx)
        v.findViewById<TextView>(R.id.txtNota).apply {
            text = nota.valor.toString()
            setTextColor(
                ContextCompat.getColor(
                    ctx, when {
                        nota.valor >= 80 -> R.color.ok
                        nota.valor >= 50 -> R.color.warn
                        else -> R.color.bad
                    }
                )
            )
        }
        v.findViewById<TextView>(R.id.txtNotaDescricao).text = when {
            nota.valor >= 80 -> "Seu celular está saudável"
            nota.valor >= 50 -> "Seu celular precisa de alguns cuidados"
            else -> "Seu celular precisa de atenção"
        }
        v.findViewById<TextView>(R.id.txtAparelho).text = Aparelho.descricao()

        val arm = Aparelho.armazenamento()
        v.findViewById<LinearProgressIndicator>(R.id.barArmazenamento).progress = arm.porcentagemUsada
        v.findViewById<TextView>(R.id.txtArmazenamento).text =
            "${Formato.tamanho(ctx, arm.usado)} usados de ${Formato.tamanho(ctx, arm.total)} " +
                "(${arm.porcentagemUsada}%) • livre: ${Formato.tamanho(ctx, arm.livre)}"

        val mem = Aparelho.memoria(ctx)
        v.findViewById<LinearProgressIndicator>(R.id.barRam).progress = mem.porcentagemUsada
        v.findViewById<TextView>(R.id.txtRam).text =
            "${Formato.tamanho(ctx, mem.total - mem.disponivel)} em uso de ${Formato.tamanho(ctx, mem.total)}. " +
                "O Android usa a RAM livre para abrir apps mais rápido — RAM “cheia” é normal."

        val b = Aparelho.bateria(ctx)
        v.findViewById<TextView>(R.id.txtBateriaResumo).text = if (b == null) "Não foi possível ler a bateria."
        else "${b.nivel}% • ${"%.1f".format(b.temperaturaC)} °C • saúde: ${b.saudeTexto}" +
            if (b.carregando) " • carregando" else ""

        v.findViewById<TextView>(R.id.txtDicas).text =
            if (nota.problemas.isEmpty()) "Nada urgente. Faça o checklist da semana para manter assim."
            else nota.problemas.joinToString("\n\n") { "• $it" }

        // Atualiza as marcações (a semana pode ter virado).
        val lista = v.findViewById<LinearLayout>(R.id.listaChecklist)
        for (i in 0 until lista.childCount) {
            val cb = lista.getChildAt(i) as? CheckBox ?: continue
            val chave = cb.tag as? String ?: continue
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = Checklist.marcado(ctx, chave)
            cb.setOnCheckedChangeListener { _, marcado -> Checklist.marcar(ctx, chave, marcado) }
        }
    }

    private fun montarChecklist(v: View) {
        val lista = v.findViewById<LinearLayout>(R.id.listaChecklist)
        lista.removeAllViews()
        Checklist.itens.forEach { (chave, texto) ->
            lista.addView(CheckBox(requireContext()).apply {
                tag = chave
                text = texto
            })
        }
    }
}

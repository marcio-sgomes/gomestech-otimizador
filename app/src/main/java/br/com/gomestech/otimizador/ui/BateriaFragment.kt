package br.com.gomestech.otimizador.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import br.com.gomestech.otimizador.R
import br.com.gomestech.otimizador.data.Aparelho
import br.com.gomestech.otimizador.data.Marcas
import com.google.android.material.button.MaterialButton

/** Bateria: informações reais e atalhos para as telas certas de cada marca. */
class BateriaFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_bateria, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        view.findViewById<TextView>(R.id.txtMarca).text =
            "Celular detectado: ${Aparelho.descricao()} (${Marcas.familia().nome})"

        val lista = view.findViewById<LinearLayout>(R.id.listaAtalhos)
        lista.removeAllViews()
        Marcas.atalhosBateria(ctx).forEach { atalho ->
            lista.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setAllCaps(false) // (isAllCaps como propriedade usa o getter da API 28)
                text = atalho.titulo
                setOnClickListener {
                    Toast.makeText(ctx, atalho.descricao, Toast.LENGTH_LONG).show()
                    if (!Marcas.abrir(ctx, atalho)) {
                        Toast.makeText(ctx, "Esta tela não existe neste aparelho.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        view.findViewById<TextView>(R.id.txtDicasBateria).text = listOf(
            "Brilho automático ligado e tela desligando em 30 segundos.",
            "Modo escuro ajuda bastante em telas AMOLED/OLED (a maioria dos Samsung e muitos Motorola e Xiaomi).",
            "Não feche os apps toda hora na tela de recentes: o Android reabre e gasta mais bateria.",
            "Evite deixar a bateria zerar com frequência e evite usar jogos pesados carregando.",
            "Se a saúde da bateria aparecer como desgastada, só a troca da bateria resolve de verdade.",
            "Desconfie de apps “economizadores” e “boosters”: a maioria só mostra anúncio e gasta mais."
        ).joinToString("\n\n") { "• $it" }
    }

    override fun onResume() {
        super.onResume()
        val v = view ?: return
        val b = Aparelho.bateria(requireContext())
        val nivel = v.findViewById<TextView>(R.id.txtNivel)
        val detalhes = v.findViewById<TextView>(R.id.txtDetalhesBateria)
        if (b == null) {
            nivel.text = "—"
            detalhes.text = "Não foi possível ler a bateria neste aparelho."
            return
        }
        nivel.text = "${b.nivel}%"
        val linhas = mutableListOf(
            "Situação: " + if (b.carregando) "carregando (${b.fonteTexto})" else "na bateria",
            "Temperatura: ${"%.1f".format(b.temperaturaC)} °C" +
                if (b.temperaturaC >= 40f) " — quente! Deixe esfriar." else " — normal",
            "Saúde: ${b.saudeTexto}",
            "Tensão: ${b.voltagemMv} mV"
        )
        b.tecnologia?.let { linhas += "Tipo: $it" }
        detalhes.text = linhas.joinToString("\n")
    }
}

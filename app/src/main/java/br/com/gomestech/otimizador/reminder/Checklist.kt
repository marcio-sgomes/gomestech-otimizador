package br.com.gomestech.otimizador.reminder

import android.content.Context
import br.com.gomestech.otimizador.logica.Tempo
import java.util.Calendar

/** Itens do checklist semanal. As marcações zeram sozinhas a cada semana nova. */
object Checklist {

    val itens = listOf(
        "reiniciar" to "Reiniciar o celular",
        "atualizar" to "Conferir atualização do sistema e dos apps (Play Store)",
        "espaco" to "Deixar pelo menos 15% do armazenamento livre",
        "whatsapp" to "Apagar vídeos e arquivos grandes do WhatsApp",
        "backup" to "Fazer backup das fotos (Google Fotos) e liberar espaço",
        "cache" to "Limpar o cache dos 3 apps que mais ocupam"
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("checklist", Context.MODE_PRIVATE)

    private fun semanaAtual(): String {
        val c = Calendar.getInstance()
        return Tempo.chaveSemana(c.get(Calendar.YEAR), c.get(Calendar.WEEK_OF_YEAR))
    }

    fun marcado(ctx: Context, chave: String): Boolean {
        val p = prefs(ctx)
        if (p.getString("semana", null) != semanaAtual()) return false
        return p.getBoolean(chave, false)
    }

    fun marcar(ctx: Context, chave: String, valor: Boolean) {
        val p = prefs(ctx)
        val editor = p.edit()
        if (p.getString("semana", null) != semanaAtual()) {
            editor.clear().putString("semana", semanaAtual())
        }
        editor.putBoolean(chave, valor).apply()
    }
}

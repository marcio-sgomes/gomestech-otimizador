package br.com.gomestech.otimizador.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import java.util.concurrent.TimeUnit

/** Funções pequenas usadas em várias telas. */
object Formato {

    fun tamanho(ctx: Context, bytes: Long): String =
        Formatter.formatShortFileSize(ctx, bytes.coerceAtLeast(0))

    /** "hoje", "há 3 dias", "há 2 meses"… */
    fun haQuantoTempo(momento: Long, agora: Long = System.currentTimeMillis()): String {
        if (momento <= 0L) return "nunca aberto"
        val dias = TimeUnit.MILLISECONDS.toDays(agora - momento)
        return when {
            dias < 1 -> "usado hoje"
            dias == 1L -> "usado ontem"
            dias < 30 -> "usado há $dias dias"
            dias < 60 -> "usado há 1 mês"
            dias < 365 -> "usado há ${dias / 30} meses"
            else -> "usado há mais de 1 ano"
        }
    }

    fun diasDesde(momento: Long, agora: Long = System.currentTimeMillis()): Long =
        if (momento <= 0L) Long.MAX_VALUE else TimeUnit.MILLISECONDS.toDays(agora - momento)
}

/** Abre uma tela do sistema sem deixar o app fechar se ela não existir no aparelho. */
fun Context.abrirComSeguranca(intent: Intent, mensagemErro: String = "Esta tela não existe neste aparelho."): Boolean {
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show()
        false
    } catch (e: SecurityException) {
        Toast.makeText(this, mensagemErro, Toast.LENGTH_LONG).show()
        false
    }
}

package br.com.gomestech.otimizador.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import br.com.gomestech.otimizador.logica.Tempo

/** Funções pequenas usadas em várias telas. */
object Formato {

    fun tamanho(ctx: Context, bytes: Long): String =
        Formatter.formatShortFileSize(ctx, bytes.coerceAtLeast(0))

    /** "hoje", "há 3 dias", "há 2 meses"… */
    fun haQuantoTempo(momento: Long, agora: Long = System.currentTimeMillis()): String =
        Tempo.haQuantoTempo(momento, agora)

    fun diasDesde(momento: Long, agora: Long = System.currentTimeMillis()): Long =
        Tempo.diasDesde(momento, agora)
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

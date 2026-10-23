package br.com.gomestech.otimizador.logica

import java.util.Locale

/** Pequenas formatações de texto (sem depender do Android). */
object Texto {

    /** 38.25 -> "38,3" (vírgula, padrão brasileiro). */
    fun umaCasa(valor: Float): String = String.format(Locale("pt", "BR"), "%.1f", valor)

    /** "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/" -> "WhatsApp Video" */
    fun pastaCurta(caminho: String): String =
        caminho.trimEnd('/').substringAfterLast('/').ifBlank { "—" }

    /** Caminho completo do arquivo (Android antigo) -> só a pasta. */
    fun pastaDoArquivo(caminhoCompleto: String): String =
        caminhoCompleto.substringBeforeLast('/', caminhoCompleto)

    /** Vídeos e fotos do WhatsApp são os que mais enchem o celular. */
    fun ehDoWhatsApp(pasta: String): Boolean = pasta.contains("whatsapp", ignoreCase = true)

    /** Porcentagem de um download (0 a 100). Sem tamanho conhecido, devolve -1. */
    fun porcentagem(feito: Long, total: Long): Int =
        if (total <= 0) -1 else ((feito.coerceIn(0, total) * 100) / total).toInt()
}

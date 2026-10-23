package br.com.gomestech.otimizador.logica

/** Contas de datas usadas nas telas (sem depender do Android). */
object Tempo {

    private const val DIA_MS = 24L * 60 * 60 * 1000

    /** Dias completos desde [momento]. Se nunca aconteceu (0), devolve Long.MAX_VALUE. */
    fun diasDesde(momento: Long, agora: Long): Long =
        if (momento <= 0L) Long.MAX_VALUE else ((agora - momento).coerceAtLeast(0) / DIA_MS)

    /** "usado hoje", "usado há 3 dias", "usado há 2 meses"… */
    fun haQuantoTempo(momento: Long, agora: Long): String {
        if (momento <= 0L) return "nunca aberto"
        val dias = diasDesde(momento, agora)
        return when {
            dias < 1 -> "usado hoje"
            dias == 1L -> "usado ontem"
            dias < 30 -> "usado há $dias dias"
            dias < 60 -> "usado há 1 mês"
            dias < 365 -> "usado há ${dias / 30} meses"
            else -> "usado há mais de 1 ano"
        }
    }

    /**
     * Um app é "esquecido" se não é aberto há pelo menos [limiteDias].
     * Se nunca foi aberto, conta a partir da data de instalação.
     */
    fun ehEsquecido(ultimoUso: Long, instaladoEm: Long, limiteDias: Int, agora: Long): Boolean {
        val referencia = if (ultimoUso > 0) ultimoUso else instaladoEm
        return diasDesde(referencia, agora) >= limiteDias
    }

    /** Chave da semana (ex.: "2026-43"), usada para zerar o checklist toda semana. */
    fun chaveSemana(ano: Int, semanaDoAno: Int): String = "$ano-$semanaDoAno"
}

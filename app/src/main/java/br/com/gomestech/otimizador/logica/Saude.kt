package br.com.gomestech.otimizador.logica

/**
 * Regras da "nota de saúde" do celular (0 a 100).
 * Não depende do Android: recebe os números já lidos e devolve a nota e os motivos.
 */
object Saude {

    data class Entrada(
        val armazenamentoUsadoPct: Int,
        val memoriaBaixa: Boolean,
        /** null quando não foi possível ler a bateria. */
        val temperaturaC: Float?,
        /** null quando a saúde da bateria é boa ou desconhecida; senão, o texto do problema. */
        val problemaBateria: String?
    )

    data class Resultado(val nota: Int, val problemas: List<String>)

    const val LIMITE_CRITICO = 90
    const val LIMITE_ATENCAO = 80
    const val TEMPERATURA_QUENTE = 40f

    fun calcular(e: Entrada): Resultado {
        var pontos = 100
        val problemas = mutableListOf<String>()

        if (e.armazenamentoUsadoPct >= LIMITE_CRITICO) {
            pontos -= 40
            problemas += "Armazenamento quase cheio (${e.armazenamentoUsadoPct}%). Isso é o que mais deixa o celular lento: libere espaço em Arquivos e Apps."
        } else if (e.armazenamentoUsadoPct >= LIMITE_ATENCAO) {
            pontos -= 20
            problemas += "Armazenamento em ${e.armazenamentoUsadoPct}%. O ideal é ficar abaixo de 80–85%."
        }

        if (e.memoriaBaixa) {
            pontos -= 15
            problemas += "Memória RAM no limite agora. Reiniciar o celular ajuda."
        }

        val temp = e.temperaturaC
        if (temp != null && temp >= TEMPERATURA_QUENTE) {
            pontos -= 15
            problemas += "Bateria quente (${Texto.umaCasa(temp)} °C). Evite usar carregando e tire a capinha se esquentar muito."
        }

        if (e.problemaBateria != null) {
            pontos -= 20
            problemas += "Saúde da bateria: ${e.problemaBateria}. Pode ser hora de trocar a bateria."
        }

        return Resultado(pontos.coerceIn(0, 100), problemas)
    }

    fun descricao(nota: Int): String = when {
        nota >= 80 -> "Seu celular está saudável"
        nota >= 50 -> "Seu celular precisa de alguns cuidados"
        else -> "Seu celular precisa de atenção"
    }

    enum class Faixa { BOA, MEDIA, RUIM }

    fun faixa(nota: Int): Faixa = when {
        nota >= 80 -> Faixa.BOA
        nota >= 50 -> Faixa.MEDIA
        else -> Faixa.RUIM
    }

    /** Porcentagem usada (0 a 100) a partir do total e do que está livre. */
    fun porcentagemUsada(total: Long, livre: Long): Int =
        if (total <= 0) 0 else (((total - livre).coerceIn(0, total) * 100) / total).toInt()
}

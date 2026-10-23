package br.com.gomestech.otimizador.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaudeTest {

    private fun entrada(
        arm: Int = 50,
        memBaixa: Boolean = false,
        temp: Float? = 30f,
        bateria: String? = null
    ) = Saude.Entrada(arm, memBaixa, temp, bateria)

    @Test
    fun celularSaudavelTemNota100ESemProblemas() {
        val r = Saude.calcular(entrada())
        assertEquals(100, r.nota)
        assertTrue(r.problemas.isEmpty())
    }

    @Test
    fun armazenamentoCriticoTira40Pontos() {
        val r = Saude.calcular(entrada(arm = 90))
        assertEquals(60, r.nota)
        assertEquals(1, r.problemas.size)
        assertTrue(r.problemas[0].contains("quase cheio (90%)"))
    }

    @Test
    fun armazenamentoEmAtencaoTira20Pontos() {
        val r = Saude.calcular(entrada(arm = 80))
        assertEquals(80, r.nota)
        assertTrue(r.problemas[0].contains("80%"))
        assertEquals(100, Saude.calcular(entrada(arm = 79)).nota)
    }

    @Test
    fun memoriaBaixaTira15() {
        val r = Saude.calcular(entrada(memBaixa = true))
        assertEquals(85, r.nota)
        assertTrue(r.problemas[0].contains("RAM"))
    }

    @Test
    fun bateriaQuenteTira15ComVirgula() {
        val r = Saude.calcular(entrada(temp = 41.25f))
        assertEquals(85, r.nota)
        assertTrue(r.problemas[0], r.problemas[0].contains("41,3 °C") || r.problemas[0].contains("41,2 °C"))
        assertEquals(100, Saude.calcular(entrada(temp = 39.9f)).nota)
        assertEquals(85, Saude.calcular(entrada(temp = 40f)).nota)
    }

    @Test
    fun semLeituraDeBateriaNaoTiraPontos() {
        assertEquals(100, Saude.calcular(entrada(temp = null)).nota)
    }

    @Test
    fun bateriaDesgastadaTira20() {
        val r = Saude.calcular(entrada(bateria = "Desgastada"))
        assertEquals(80, r.nota)
        assertTrue(r.problemas[0].contains("Desgastada"))
    }

    @Test
    fun tudoRuimNuncaFicaAbaixoDeZero() {
        val r = Saude.calcular(entrada(arm = 99, memBaixa = true, temp = 45f, bateria = "Com falha"))
        assertEquals(10, r.nota)
        assertEquals(4, r.problemas.size)
    }

    @Test
    fun descricaoEFaixa() {
        assertEquals("Seu celular está saudável", Saude.descricao(80))
        assertEquals("Seu celular precisa de alguns cuidados", Saude.descricao(50))
        assertEquals("Seu celular precisa de atenção", Saude.descricao(49))
        assertEquals(Saude.Faixa.BOA, Saude.faixa(100))
        assertEquals(Saude.Faixa.MEDIA, Saude.faixa(79))
        assertEquals(Saude.Faixa.RUIM, Saude.faixa(0))
        assertEquals(3, Saude.Faixa.entries.size)
        assertEquals(Saude.Faixa.BOA, Saude.Faixa.valueOf("BOA"))
    }

    @Test
    fun porcentagemUsada() {
        assertEquals(0, Saude.porcentagemUsada(0, 0))
        assertEquals(0, Saude.porcentagemUsada(-5, 0))
        assertEquals(75, Saude.porcentagemUsada(100, 25))
        assertEquals(100, Saude.porcentagemUsada(100, 0))
        assertEquals(0, Saude.porcentagemUsada(100, 150)) // livre maior que o total (leitura estranha)
        assertEquals(50, Saude.porcentagemUsada(128L * 1024 * 1024 * 1024, 64L * 1024 * 1024 * 1024))
    }
}

package br.com.gomestech.otimizador.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoTest {

    private val dia = 24L * 60 * 60 * 1000
    private val agora = 1_800_000_000_000L

    @Test
    fun diasDesde() {
        assertEquals(Long.MAX_VALUE, Tempo.diasDesde(0, agora))
        assertEquals(Long.MAX_VALUE, Tempo.diasDesde(-1, agora))
        assertEquals(0, Tempo.diasDesde(agora, agora))
        assertEquals(0, Tempo.diasDesde(agora + dia, agora)) // relógio adiantado não dá negativo
        assertEquals(10, Tempo.diasDesde(agora - 10 * dia, agora))
    }

    @Test
    fun haQuantoTempo() {
        assertEquals("nunca aberto", Tempo.haQuantoTempo(0, agora))
        assertEquals("usado hoje", Tempo.haQuantoTempo(agora - 1000, agora))
        assertEquals("usado ontem", Tempo.haQuantoTempo(agora - dia, agora))
        assertEquals("usado há 5 dias", Tempo.haQuantoTempo(agora - 5 * dia, agora))
        assertEquals("usado há 1 mês", Tempo.haQuantoTempo(agora - 45 * dia, agora))
        assertEquals("usado há 3 meses", Tempo.haQuantoTempo(agora - 95 * dia, agora))
        assertEquals("usado há mais de 1 ano", Tempo.haQuantoTempo(agora - 400 * dia, agora))
    }

    @Test
    fun ehEsquecido() {
        assertTrue(Tempo.ehEsquecido(agora - 31 * dia, 0, 30, agora))
        assertFalse(Tempo.ehEsquecido(agora - 29 * dia, 0, 30, agora))
        // nunca aberto: conta pela instalação
        assertFalse(Tempo.ehEsquecido(0, agora - 2 * dia, 30, agora))
        assertTrue(Tempo.ehEsquecido(0, agora - 100 * dia, 90, agora))
        // sem nenhuma informação: considerado esquecido
        assertTrue(Tempo.ehEsquecido(0, 0, 30, agora))
    }

    @Test
    fun chaveSemana() {
        assertEquals("2026-43", Tempo.chaveSemana(2026, 43))
    }
}

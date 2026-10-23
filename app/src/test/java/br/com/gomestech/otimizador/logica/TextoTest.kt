package br.com.gomestech.otimizador.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextoTest {

    @Test
    fun umaCasaUsaVirgula() {
        assertEquals("38,0", Texto.umaCasa(38f))
        assertEquals("0,5", Texto.umaCasa(0.5f))
    }

    @Test
    fun pastaCurta() {
        assertEquals("WhatsApp Video", Texto.pastaCurta("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/"))
        assertEquals("DCIM", Texto.pastaCurta("DCIM"))
        assertEquals("—", Texto.pastaCurta(""))
        assertEquals("—", Texto.pastaCurta("/"))
    }

    @Test
    fun pastaDoArquivo() {
        assertEquals("/storage/emulated/0/DCIM", Texto.pastaDoArquivo("/storage/emulated/0/DCIM/foto.jpg"))
        assertEquals("semBarra", Texto.pastaDoArquivo("semBarra"))
    }

    @Test
    fun whatsapp() {
        assertTrue(Texto.ehDoWhatsApp("Android/media/com.whatsapp/WhatsApp/Media"))
        assertTrue(Texto.ehDoWhatsApp("WHATSAPP"))
        assertFalse(Texto.ehDoWhatsApp("DCIM/Camera"))
    }

    @Test
    fun porcentagem() {
        assertEquals(-1, Texto.porcentagem(10, 0))
        assertEquals(-1, Texto.porcentagem(10, -1))
        assertEquals(0, Texto.porcentagem(0, 100))
        assertEquals(50, Texto.porcentagem(50, 100))
        assertEquals(100, Texto.porcentagem(150, 100))
        assertEquals(0, Texto.porcentagem(-5, 100))
    }
}

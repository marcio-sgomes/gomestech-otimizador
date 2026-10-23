package br.com.gomestech.otimizador.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarcaTest {

    @Test
    fun detectaTodasAsMarcas() {
        val casos = mapOf(
            ("samsung" to "samsung") to Familia.SAMSUNG,
            ("Xiaomi" to "Redmi") to Familia.XIAOMI,
            ("Xiaomi" to "POCO") to Familia.XIAOMI,
            ("" to "redmi") to Familia.XIAOMI,
            ("" to "poco") to Familia.XIAOMI,
            ("motorola" to "motorola") to Familia.MOTOROLA,
            ("LENOVO" to "Lenovo") to Familia.MOTOROLA,
            ("HONOR" to "HONOR") to Familia.HONOR,
            ("HUAWEI" to "HUAWEI") to Familia.HUAWEI,
            ("OPPO" to "OPPO") to Familia.OPPO,
            ("realme" to "realme") to Familia.OPPO,
            ("OnePlus" to "OnePlus") to Familia.OPPO,
            ("vivo" to "vivo") to Familia.VIVO,
            ("" to "iQOO") to Familia.VIVO,
            ("asus" to "asus") to Familia.ASUS,
            ("LGE" to "lge") to Familia.LG,
            ("LG" to "") to Familia.LG,
            ("lg" to "x") to Familia.LG,
            ("HMD Global" to "Nokia") to Familia.NOKIA,
            ("" to "nokia") to Familia.NOKIA,
            ("HMD Global" to "HMD") to Familia.NOKIA,
            ("Google" to "google") to Familia.GOOGLE,
            ("TCL" to "TCL") to Familia.OUTRA,
            ("Positivo" to "Positivo") to Familia.OUTRA
        )
        for ((entrada, esperado) in casos) {
            assertEquals("$entrada", esperado, Marca.detectar(entrada.first, entrada.second))
        }
        assertEquals(Familia.OUTRA, Marca.detectar(null, null))
        // "Flagship" não pode ser confundido com LG
        assertEquals(Familia.OUTRA, Marca.detectar("Lgflag", "z"))
        assertEquals(Familia.OUTRA, Marca.detectar("Flagship", "Xflg"))
    }

    @Test
    fun dicaDeCacheParaCadaFamilia() {
        assertTrue(Marca.dicaCache(Familia.SAMSUNG).contains("Armazenamento"))
        assertTrue(Marca.dicaCache(Familia.XIAOMI).contains("Não escolha"))
        for (f in listOf(Familia.MOTOROLA, Familia.GOOGLE, Familia.NOKIA)) {
            assertTrue(Marca.dicaCache(f).contains("Armazenamento e cache"))
        }
        for (f in Familia.values()) {
            val dica = Marca.dicaCache(f)
            assertTrue(dica.contains("Limpar cache"))
            assertTrue("Nunca mandar limpar dados", !dica.contains("toque em “Limpar todos os dados”"))
        }
    }

    @Test
    fun nomeComArtigo() {
        assertEquals("do Android", Marca.nomeComArtigo(Familia.OUTRA))
        assertEquals("da Samsung", Marca.nomeComArtigo(Familia.SAMSUNG))
    }

    @Test
    fun descricaoDoAparelho() {
        assertEquals("Samsung SM-A155M • Android 14", Marca.descricaoAparelho("samsung", "SM-A155M", "14"))
        assertEquals("Motorola moto g54 • Android 13", Marca.descricaoAparelho("motorola", "motorola moto g54", "13"))
        assertEquals("Google Pixel 8 • Android 15", Marca.descricaoAparelho("Google", "Pixel 8", "15"))
        assertEquals(" X • Android 7", Marca.descricaoAparelho("", "X", "7"))
    }

    @Test
    fun todasAsFamiliasTemNome() {
        for (f in Familia.values()) assertTrue(f.nome.isNotBlank())
        assertEquals(12, Familia.entries.size)
        assertEquals(Familia.SAMSUNG, Familia.valueOf("SAMSUNG"))
    }
}

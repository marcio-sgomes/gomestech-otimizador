package br.com.gomestech.otimizador.logica

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersaoTest {

    private fun release(
        tag: String = "v5",
        nome: String? = "Versão 1.0.5",
        body: String? = "novidades",
        assets: String = """[{"name":"GomesTech-Otimizador.apk","browser_download_url":"https://github.com/x/y/releases/download/v5/GomesTech-Otimizador.apk"}]"""
    ): String {
        val n = if (nome == null) "" else "\"name\":\"$nome\","
        val b = if (body == null) "\"body\":null," else "\"body\":\"$body\","
        return """{"tag_name":"$tag",$n$b"assets":$assets}"""
    }

    @Test
    fun codigoDaTag() {
        assertEquals(12, Versao.codigoDaTag("v12"))
        assertEquals(3, Versao.codigoDaTag(" 3 "))
        assertNull(Versao.codigoDaTag("v0"))
        assertNull(Versao.codigoDaTag("beta"))
        assertNull(Versao.codigoDaTag(null))
    }

    @Test
    fun versaoNovaComApk() {
        val l = Versao.interpretar(release(), 3)!!
        assertEquals(5, l.codigo)
        assertEquals("Versão 1.0.5", l.nome)
        assertTrue(l.urlApk.endsWith(".apk"))
        assertEquals("novidades", l.novidades)
    }

    @Test
    fun mesmaVersaoOuMaisAntigaNaoAtualiza() {
        assertNull(Versao.interpretar(release(tag = "v5"), 5))
        assertNull(Versao.interpretar(release(tag = "v4"), 5))
    }

    @Test
    fun respostasInvalidasNaoAtualizam() {
        assertNull(Versao.interpretar("isso não é json", 1))
        assertNull(Versao.interpretar("""{"message":"Not Found"}""", 1))
        assertNull(Versao.interpretar("""{"tag_name":"v9"}""", 1)) // sem assets
        assertNull(Versao.interpretar(release(assets = "[]"), 1))
        assertNull(Versao.interpretar(release(assets = """[{"name":"notas.txt","browser_download_url":"https://x/notas.txt"}]"""), 1))
        // link que não é https é recusado (segurança)
        assertNull(Versao.interpretar(release(assets = """[{"name":"a.apk","browser_download_url":"http://x/a.apk"}]"""), 1))
        assertNull(Versao.interpretar(release(assets = """[1, {"name":"a.apk","browser_download_url":""}]"""), 1))
    }

    @Test
    fun pulaAnexoRuimEUsaOProximo() {
        val assets = """[{"name":"b.APK","browser_download_url":"ftp://x"},{"name":"c.apk","browser_download_url":"https://ok/c.apk"}]"""
        assertEquals("https://ok/c.apk", Versao.interpretar(release(assets = assets), 1)!!.urlApk)
    }

    @Test
    fun semNomeESemNovidades() {
        val l = Versao.interpretar(release(nome = null, body = null), 1)!!
        assertEquals("v5", l.nome)
        assertEquals("", l.novidades)
    }

    @Test
    fun pareceApk() {
        val pk = byteArrayOf('P'.code.toByte(), 'K'.code.toByte())
        assertTrue(Versao.pareceApk(pk, 4_000_000))
        assertFalse(Versao.pareceApk(pk, 500)) // pequeno demais: download cortado
        assertFalse(Versao.pareceApk(byteArrayOf('<'.code.toByte(), 'h'.code.toByte()), 4_000_000)) // página de erro
        assertFalse(Versao.pareceApk(byteArrayOf('P'.code.toByte()), 4_000_000))
        assertFalse(Versao.pareceApk(byteArrayOf('P'.code.toByte(), 'X'.code.toByte()), 4_000_000))
    }
}

package br.com.gomestech.otimizador.logica

import org.json.JSONException
import org.json.JSONObject

/** Leitura da resposta do GitHub sobre a última versão do app. */
object Versao {

    data class Lancamento(val codigo: Int, val nome: String, val urlApk: String, val novidades: String)

    /** "v12" -> 12; qualquer outra coisa -> null. */
    fun codigoDaTag(tag: String?): Int? {
        if (tag == null) return null
        val numero = tag.trim().removePrefix("v").toIntOrNull() ?: return null
        return if (numero > 0) numero else null
    }

    /**
     * Interpreta o JSON de /releases/latest.
     * Devolve o lançamento só se ele for mais novo que [versaoAtual] e tiver um .apk anexado.
     */
    fun interpretar(json: String, versaoAtual: Int): Lancamento? {
        val obj = try {
            JSONObject(json)
        } catch (e: JSONException) {
            return null
        }
        val codigo = codigoDaTag(obj.optString("tag_name")) ?: return null
        if (codigo <= versaoAtual) return null
        val assets = obj.optJSONArray("assets") ?: return null
        var url: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            if (a.optString("name").endsWith(".apk", ignoreCase = true)) {
                url = a.optString("browser_download_url").takeIf { it.startsWith("https://") }
                if (url != null) break
            }
        }
        if (url == null) return null
        val nome = obj.optString("name").ifBlank { "v$codigo" }
        val novidades = if (obj.isNull("body")) "" else obj.optString("body")
        return Lancamento(codigo, nome, url, novidades)
    }

    /** Confere se os primeiros bytes são de um APK (arquivo ZIP: "PK"). */
    fun pareceApk(inicio: ByteArray, tamanho: Long): Boolean =
        tamanho > 100_000 && inicio.size >= 2 && inicio[0] == 'P'.code.toByte() && inicio[1] == 'K'.code.toByte()
}

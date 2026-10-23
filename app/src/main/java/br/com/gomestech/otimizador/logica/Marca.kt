package br.com.gomestech.otimizador.logica

/** Famílias de marcas de celular que o app reconhece. */
enum class Familia(val nome: String) {
    SAMSUNG("Samsung"),
    XIAOMI("Xiaomi / Redmi / POCO"),
    MOTOROLA("Motorola"),
    HUAWEI("Huawei"),
    HONOR("Honor"),
    OPPO("OPPO / Realme / OnePlus"),
    VIVO("Vivo / iQOO"),
    ASUS("Asus"),
    LG("LG"),
    NOKIA("Nokia"),
    GOOGLE("Google Pixel"),
    OUTRA("Android")
}

/** Regras de marca que não dependem do Android (fáceis de testar). */
object Marca {

    /** "lg" ou "lge" como palavra inteira (não confunde com "flagship", por exemplo). */
    private val PALAVRA_LG = Regex("\\blge?\\b")

    /** Descobre a família pela fabricante e pela marca informadas pelo aparelho. */
    fun detectar(fabricante: String?, marca: String?): Familia {
        val m = ((fabricante ?: "") + " " + (marca ?: "")).lowercase().trim()
        return when {
            "samsung" in m -> Familia.SAMSUNG
            "xiaomi" in m || "redmi" in m || "poco" in m -> Familia.XIAOMI
            "motorola" in m || "lenovo" in m -> Familia.MOTOROLA
            "honor" in m -> Familia.HONOR
            "huawei" in m -> Familia.HUAWEI
            "oppo" in m || "realme" in m || "oneplus" in m -> Familia.OPPO
            "vivo" in m || "iqoo" in m -> Familia.VIVO
            "asus" in m -> Familia.ASUS
            PALAVRA_LG.containsMatchIn(m) -> Familia.LG
            "nokia" in m || "hmd" in m -> Familia.NOKIA
            "google" in m -> Familia.GOOGLE
            else -> Familia.OUTRA
        }
    }

    /** Caminho para limpar o cache na tela de detalhes do app, conforme a marca. */
    fun dicaCache(f: Familia): String = when (f) {
        Familia.SAMSUNG -> "Na tela que vai abrir, toque em “Armazenamento” e depois em “Limpar cache”."
        Familia.XIAOMI -> "Na tela que vai abrir, toque em “Limpar dados” (ou “Armazenamento”) e escolha “Limpar cache”. Não escolha “Limpar todos os dados”."
        Familia.MOTOROLA, Familia.GOOGLE, Familia.NOKIA ->
            "Na tela que vai abrir, toque em “Armazenamento e cache” e depois em “Limpar cache”."
        else -> "Na tela que vai abrir, toque em “Armazenamento” (ou “Armazenamento e cache”) e depois em “Limpar cache”. Não toque em “Limpar dados”."
    }

    /** "da Samsung" / "do Android" */
    fun nomeComArtigo(f: Familia): String = if (f == Familia.OUTRA) "do Android" else "da ${f.nome}"

    /** "Samsung SM-A155M • Android 14" (tira a marca repetida do começo do modelo). */
    fun descricaoAparelho(fabricante: String, modelo: String, versaoAndroid: String): String {
        val marca = fabricante.replaceFirstChar { it.uppercase() }
        val mod = if (fabricante.isNotEmpty() && modelo.startsWith(fabricante, ignoreCase = true))
            modelo.substring(fabricante.length).trim() else modelo
        return "$marca $mod • Android $versaoAndroid".replace("  ", " ")
    }
}

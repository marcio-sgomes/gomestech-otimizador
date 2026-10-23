package br.com.gomestech.otimizador.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import br.com.gomestech.otimizador.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import br.com.gomestech.otimizador.logica.Texto
import br.com.gomestech.otimizador.logica.Versao
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Atualização 100% por dentro do app.
 *
 * Cada compilação no GitHub cria um "Release" com a tag v<número> e o APK anexado.
 * O app compara esse número com a própria versão; se houver uma mais nova,
 * mostra o botão "Atualizar", baixa o APK e abre o instalador do Android.
 */
object Atualizador {

    private val urlUltima: String
        get() = "https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest"

    /** Devolve a versão nova, ou null se o app já está atualizado (ou sem internet). */
    suspend fun verificar(versaoAtual: Int = BuildConfig.VERSION_CODE): Versao.Lancamento? =
        withContext(Dispatchers.IO) {
            try {
                val con = (URL(urlUltima).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                try {
                    if (con.responseCode != 200) return@withContext null
                    val texto = con.inputStream.bufferedReader().use { it.readText() }
                    Versao.interpretar(texto, versaoAtual)
                } finally {
                    con.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        }

    /** O Android exige que o usuário libere "instalar apps desconhecidos" para este app. */
    fun podeInstalar(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun intentLiberarInstalacao(ctx: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))

    /** Baixa o APK mostrando o progresso (0 a 100). */
    suspend fun baixar(ctx: Context, versao: Versao.Lancamento, progresso: (Int) -> Unit): File? =
        withContext(Dispatchers.IO) {
            val pasta = File(ctx.cacheDir, "updates").apply { mkdirs() }
            pasta.listFiles()?.forEach { it.delete() } // apaga downloads antigos
            val destino = File(pasta, "atualizacao-${versao.codigo}.apk")
            try {
                var con = URL(versao.urlApk).openConnection() as HttpURLConnection
                con.instanceFollowRedirects = true
                con.connectTimeout = 15_000
                con.readTimeout = 30_000
                // O GitHub redireciona para outro endereço; seguimos manualmente por garantia.
                var redirecionamentos = 0
                while (con.responseCode in 300..399 && redirecionamentos < 5) {
                    val novo = con.getHeaderField("Location") ?: break
                    con.disconnect()
                    con = URL(novo).openConnection() as HttpURLConnection
                    con.connectTimeout = 15_000
                    con.readTimeout = 30_000
                    redirecionamentos++
                }
                if (con.responseCode != 200) return@withContext null
                val total = con.contentLengthLong
                con.inputStream.use { entrada ->
                    destino.outputStream().use { saida ->
                        val buffer = ByteArray(64 * 1024)
                        var lidos = 0L
                        var ultimo = -1
                        while (true) {
                            val n = entrada.read(buffer)
                            if (n < 0) break
                            saida.write(buffer, 0, n)
                            lidos += n
                            val p = Texto.porcentagem(lidos, total)
                            if (p >= 0) {
                                if (p != ultimo) {
                                    ultimo = p
                                    withContext(Dispatchers.Main) { progresso(p) }
                                }
                            }
                        }
                    }
                }
                con.disconnect()
                // Confere se o arquivo baixado é mesmo um APK inteiro.
                val inicio = destino.inputStream().use { entrada -> ByteArray(2).also { entrada.read(it) } }
                if (!Versao.pareceApk(inicio, destino.length())) {
                    destino.delete()
                    return@withContext null
                }
                destino
            } catch (e: Exception) {
                destino.delete()
                null
            }
        }

    /** Abre o instalador do Android com o APK baixado. */
    fun instalar(ctx: Context, apk: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(ctx, "Não foi possível abrir o instalador.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

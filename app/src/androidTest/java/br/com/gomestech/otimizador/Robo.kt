package br.com.gomestech.otimizador

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Ferramentas do robô de vistoria (usadas por todos os testes de ponta a ponta). */
object Robo {

    val instr get() = InstrumentationRegistry.getInstrumentation()
    val ctx: Context get() = instr.targetContext
    val device: UiDevice get() = UiDevice.getInstance(instr)
    val pacote: String get() = ctx.packageName

    const val NOME_ARQUIVO_TESTE = "gomestech-teste-e2e.png"
    const val PASTA_TESTE = "GomesTechTeste"

    fun shell(cmd: String): String {
        val pfd = instr.uiAutomation.executeShellCommand(cmd)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
    }

    /** Espera uma condição ficar verdadeira (sem travar para sempre). */
    fun esperar(timeoutMs: Long = 30_000, condicao: () -> Boolean): Boolean {
        val fim = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < fim) {
            val ok = try { condicao() } catch (e: Throwable) { false }
            if (ok) return true
            Thread.sleep(250)
        }
        return try { condicao() } catch (e: Throwable) { false }
    }

    /** Dá ao app as mesmas permissões que o usuário daria nas telas do Android. */
    fun concederPermissoes() {
        shell("appops set $pacote GET_USAGE_STATS allow")
        shell("appops set $pacote android:get_usage_stats allow")
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            perms += listOf(
                "android.permission.READ_MEDIA_IMAGES",
                "android.permission.READ_MEDIA_VIDEO",
                "android.permission.READ_MEDIA_AUDIO",
                "android.permission.POST_NOTIFICATIONS"
            )
        } else {
            perms += "android.permission.READ_EXTERNAL_STORAGE"
            if (Build.VERSION.SDK_INT <= 29) perms += "android.permission.WRITE_EXTERNAL_STORAGE"
        }
        perms.forEach { shell("pm grant $pacote $it") }
    }

    fun appNaFrente(): Boolean = device.currentPackageName == pacote

    /** Outra tela (de outro app) está de fato na frente? (null = no meio da troca de tela, ainda não conta) */
    private fun outraTelaNaFrente(): Boolean {
        val atual = device.currentPackageName
        return atual != null && atual != pacote
    }

    /** Saiu do app (abriu uma tela do sistema)? Emulador lento pode levar alguns segundos. */
    fun esperarSairDoApp(timeoutMs: Long = 15_000): Boolean = esperar(timeoutMs) { outraTelaNaFrente() }

    /** Volta para o app apertando "voltar" (nunca confirma nada nas telas do sistema). */
    fun voltarParaApp() {
        repeat(6) {
            if (appNaFrente()) return
            device.waitForIdle()
            // Só aperta "voltar" quando a outra tela já está na frente: apertar no meio da troca
            // poderia cair no próprio app e fechá-lo.
            if (outraTelaNaFrente()) device.pressBack()
            esperar(2_000) { appNaFrente() }
        }
        if (!appNaFrente()) {
            val i = ctx.packageManager.getLaunchIntentForPackage(pacote)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            ctx.startActivity(i)
            esperar(5_000) { appNaFrente() }
        }
    }

    fun <T> naTela(cenario: ActivityScenario<MainActivity>, bloco: (MainActivity) -> T): T {
        var r: Any? = null
        cenario.onActivity { r = bloco(it) }
        @Suppress("UNCHECKED_CAST")
        return r as T
    }

    fun fragmentoAtual(cenario: ActivityScenario<MainActivity>): Fragment? =
        naTela(cenario) { it.supportFragmentManager.findFragmentById(R.id.container) }

    // ---------- Arquivo de teste (criado e apagado pelo próprio robô) ----------

    private fun colecaoImagens(): Uri =
        if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

    /** Procura o arquivo de teste no MediaStore. */
    fun acharArquivoTeste(): Pair<Uri, Long>? {
        ctx.contentResolver.query(
            colecaoImagens(),
            arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(NOME_ARQUIVO_TESTE),
            null
        )?.use { c ->
            if (c.moveToFirst()) {
                return ContentUris.withAppendedId(colecaoImagens(), c.getLong(0)) to c.getLong(1)
            }
        }
        return null
    }

    fun contarMidias(): Int {
        var total = 0
        for (base in listOf(
            colecaoImagens(),
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            if (Build.VERSION.SDK_INT >= 29) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )) {
            ctx.contentResolver.query(base, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use {
                total += it.count
            }
        }
        return total
    }

    fun apagarArquivoTesteSeExistir() {
        while (true) {
            val (uri, _) = acharArquivoTeste() ?: break
            try {
                ctx.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                break
            }
        }
        if (Build.VERSION.SDK_INT < 29) {
            @Suppress("DEPRECATION")
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "$PASTA_TESTE/$NOME_ARQUIVO_TESTE").delete()
        }
    }

    /** Cria uma foto de teste com mais de 10 MB (ruído aleatório não comprime). */
    fun criarArquivoTeste(): Uri {
        apagarArquivoTesteSeExistir()
        val lado = 1900
        val bmp = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val linha = IntArray(lado)
        val rnd = Random(42)
        for (y in 0 until lado) {
            for (x in 0 until lado) linha[x] = rnd.nextInt()
            bmp.setPixels(linha, 0, lado, 0, y, lado, 1)
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val valores = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, NOME_ARQUIVO_TESTE)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$PASTA_TESTE")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(colecaoImagens(), valores)!!
            ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            valores.clear()
            valores.put(MediaStore.MediaColumns.IS_PENDING, 0)
            ctx.contentResolver.update(uri, valores, null, null)
            bmp.recycle()
            return uri
        } else {
            @Suppress("DEPRECATION")
            val pasta = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), PASTA_TESTE)
            pasta.mkdirs()
            val arquivo = File(pasta, NOME_ARQUIVO_TESTE)
            arquivo.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
            val trava = CountDownLatch(1)
            var resultado: Uri? = null
            MediaScannerConnection.scanFile(ctx, arrayOf(arquivo.absolutePath), arrayOf("image/png")) { _, uri ->
                resultado = uri
                trava.countDown()
            }
            trava.await(30, TimeUnit.SECONDS)
            return resultado ?: acharArquivoTeste()!!.first
        }
    }
}

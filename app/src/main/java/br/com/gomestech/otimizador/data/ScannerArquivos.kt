package br.com.gomestech.otimizador.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/** Procura fotos, vídeos e áudios grandes guardados no celular. */
object ScannerArquivos {

    enum class Tipo(val rotulo: String) { FOTO("Foto"), VIDEO("Vídeo"), AUDIO("Áudio") }

    data class Arquivo(
        val uri: Uri,
        val nome: String,
        val tamanho: Long,
        val tipo: Tipo,
        val pasta: String,
        val modificadoEm: Long
    ) {
        /** Vídeos e fotos do WhatsApp são os que mais enchem o celular. */
        val doWhatsApp: Boolean get() = pasta.contains("whatsapp", ignoreCase = true)
    }

    /** Arquivos a partir deste tamanho entram na lista. */
    const val TAMANHO_MINIMO: Long = 10L * 1024 * 1024 // 10 MB

    fun escanear(ctx: Context, minimo: Long = TAMANHO_MINIMO): List<Arquivo> {
        val todos = mutableListOf<Arquivo>()
        todos += consultar(ctx, colecao(Tipo.FOTO), Tipo.FOTO, minimo)
        todos += consultar(ctx, colecao(Tipo.VIDEO), Tipo.VIDEO, minimo)
        todos += consultar(ctx, colecao(Tipo.AUDIO), Tipo.AUDIO, minimo)
        return todos.sortedByDescending { it.tamanho }
    }

    private fun colecao(tipo: Tipo): Uri {
        val q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        return when (tipo) {
            Tipo.FOTO -> if (q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            Tipo.VIDEO -> if (q) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            Tipo.AUDIO -> if (q) MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    private fun consultar(ctx: Context, base: Uri, tipo: Tipo, minimo: Long): List<Arquivo> {
        val q = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val colunaPasta = if (q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA
        val colunas = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            colunaPasta
        )
        val lista = mutableListOf<Arquivo>()
        try {
            ctx.contentResolver.query(
                base,
                colunas,
                "${MediaStore.MediaColumns.SIZE} >= ?",
                arrayOf(minimo.toString()),
                "${MediaStore.MediaColumns.SIZE} DESC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val iNome = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val iTam = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val iData = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val iPasta = c.getColumnIndexOrThrow(colunaPasta)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    var pasta = c.getString(iPasta) ?: ""
                    if (!q) pasta = pasta.substringBeforeLast('/', pasta) // DATA traz o caminho completo
                    lista += Arquivo(
                        uri = ContentUris.withAppendedId(base, id),
                        nome = c.getString(iNome) ?: "(sem nome)",
                        tamanho = c.getLong(iTam),
                        tipo = tipo,
                        pasta = pasta,
                        modificadoEm = c.getLong(iData) * 1000L
                    )
                }
            }
        } catch (e: SecurityException) {
            // Sem permissão de mídia: devolve vazio e a tela pede a permissão.
        }
        return lista
    }
}

package br.com.gomestech.otimizador.data

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Verifica e pede as permissões que o app usa. */
object Permissoes {

    /** "Acesso ao uso": necessário para ver cache dos apps e quando foram abertos. */
    fun temAcessoAoUso(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val modo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        }
        return if (modo == AppOpsManager.MODE_DEFAULT) {
            ctx.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            modo == AppOpsManager.MODE_ALLOWED
        }
    }

    fun intentAcessoAoUso(ctx: Context): Intent {
        // Alguns aparelhos abrem direto na tela do app; os outros abrem a lista geral.
        val direto = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .setData(Uri.fromParts("package", ctx.packageName, null))
        return if (direto.resolveActivity(ctx.packageManager) != null) direto
        else Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    /** Permissões de mídia conforme a versão do Android. */
    fun permissoesMidia(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        // Android 10 e anteriores: com requestLegacyExternalStorage, a gravação permite apagar.
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    fun temMidia(ctx: Context): Boolean = permissoesMidia().any {
        ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED
    }

    fun intentDetalhesDoApp(pacote: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pacote, null))
}

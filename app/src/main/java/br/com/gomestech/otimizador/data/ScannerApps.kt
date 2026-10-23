package br.com.gomestech.otimizador.data

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import java.util.concurrent.TimeUnit

/** Lista os apps instalados pelo usuário com tamanho, cache e último uso. */
object ScannerApps {

    data class AppInfo(
        val pacote: String,
        val nome: String,
        val icone: Drawable?,
        val tamanhoApp: Long,
        val tamanhoDados: Long,
        val tamanhoCache: Long,
        val ultimoUso: Long,
        val instaladoEm: Long,
        /** App que veio de fábrica: não dá para desinstalar, só desativar. */
        val sistema: Boolean
    ) {
        val tamanhoTotal: Long get() = tamanhoApp + tamanhoDados
    }

    /** Roda fora da tela principal (é lento). */
    fun escanear(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val ultimoUso = ultimosUsos(ctx)
        val stats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager else null

        @Suppress("DEPRECATION")
        val instalados = pm.getInstalledApplications(0)

        return instalados.asSequence()
            .filter { it.packageName != ctx.packageName }
            // Apps instalados pelo usuário + apps de fábrica que aparecem na tela inicial
            // (YouTube, Facebook de fábrica etc. costumam ter cache grande).
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { ai ->
                var app = 0L
                var dados = 0L
                var cache = 0L
                if (stats != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val s = stats.queryStatsForPackage(ai.storageUuid, ai.packageName, Process.myUserHandle())
                        app = s.appBytes
                        dados = s.dataBytes
                        cache = s.cacheBytes
                    } catch (e: Exception) {
                        // Sem "Acesso ao uso" ou app removido no meio da análise.
                    }
                }
                val instalado = try {
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(ai.packageName, 0).firstInstallTime
                } catch (e: PackageManager.NameNotFoundException) {
                    0L
                }
                AppInfo(
                    pacote = ai.packageName,
                    nome = ai.loadLabel(pm).toString(),
                    icone = try { ai.loadIcon(pm) } catch (e: Exception) { null },
                    tamanhoApp = app,
                    tamanhoDados = dados,
                    tamanhoCache = cache,
                    ultimoUso = ultimoUso[ai.packageName] ?: 0L,
                    instaladoEm = instalado,
                    sistema = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .toList()
    }

    private fun ultimosUsos(ctx: Context): Map<String, Long> {
        if (!Permissoes.temAcessoAoUso(ctx)) return emptyMap()
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val agora = System.currentTimeMillis()
        return try {
            usm.queryAndAggregateUsageStats(agora - TimeUnit.DAYS.toMillis(365), agora)
                .mapValues { it.value.lastTimeUsed }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

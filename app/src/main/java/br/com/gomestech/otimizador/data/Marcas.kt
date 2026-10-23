package br.com.gomestech.otimizador.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Atalhos para as telas de manutenção de cada marca.
 *
 * Cada atalho tem uma lista de "candidatos" (telas possíveis, da mais específica
 * para a mais geral). O app tenta um por um e abre o primeiro que existir no aparelho.
 * Para adicionar uma marca nova, basta incluir os candidatos dela aqui.
 */
object Marcas {

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

    data class Atalho(val titulo: String, val descricao: String, val candidatos: List<Intent>)

    fun familia(): Familia {
        val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            "samsung" in m -> Familia.SAMSUNG
            "xiaomi" in m || "redmi" in m || "poco" in m -> Familia.XIAOMI
            "motorola" in m || "lenovo" in m -> Familia.MOTOROLA
            "honor" in m -> Familia.HONOR
            "huawei" in m -> Familia.HUAWEI
            "oppo" in m || "realme" in m || "oneplus" in m -> Familia.OPPO
            "vivo" in m || "iqoo" in m -> Familia.VIVO
            "asus" in m -> Familia.ASUS
            "lge" in m || m.startsWith("lg") -> Familia.LG
            "nokia" in m || "hmd" in m -> Familia.NOKIA
            "google" in m -> Familia.GOOGLE
            else -> Familia.OUTRA
        }
    }

    // ---------- Montagem dos candidatos ----------

    private fun tela(pacote: String, classe: String): Intent =
        Intent().setComponent(ComponentName(pacote, classe)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun app(ctx: Context, pacote: String): Intent? =
        ctx.packageManager.getLaunchIntentForPackage(pacote)

    private fun sistema(acao: String): Intent = Intent(acao).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    // ---------- Limpeza / manutenção ----------

    fun atalhoManutencao(ctx: Context): Atalho {
        val f = familia()
        val c = mutableListOf<Intent?>()
        when (f) {
            Familia.SAMSUNG -> {
                c += tela("com.samsung.android.lool", "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity")
                c += tela("com.samsung.android.sm", "com.samsung.android.sm.ui.dashboard.SmartManagerDashBoardActivity")
            }
            Familia.XIAOMI -> {
                c += app(ctx, "com.miui.cleanmaster")
                c += tela("com.miui.cleanmaster", "com.miui.optimizecenter.MainActivity")
                c += app(ctx, "com.miui.securitycenter")
            }
            Familia.HUAWEI, Familia.HONOR -> {
                c += tela("com.huawei.systemmanager", "com.huawei.systemmanager.spacecleanner.ui.SpaceCleanActivity")
                c += app(ctx, "com.huawei.systemmanager")
                c += app(ctx, "com.hihonor.systemmanager")
            }
            Familia.OPPO -> {
                c += app(ctx, "com.coloros.phonemanager")
                c += app(ctx, "com.oplus.phonemanager")
                c += app(ctx, "com.oneplus.security")
            }
            Familia.VIVO -> {
                c += app(ctx, "com.iqoo.secure")
                c += app(ctx, "com.vivo.safecenter")
            }
            Familia.ASUS -> c += app(ctx, "com.asus.mobilemanager")
            else -> Unit
        }
        // Para todas as marcas: o "Files by Google" tem uma limpeza segura e oficial.
        c += app(ctx, "com.google.android.apps.nbu.files")
        c += sistema(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        val nome = if (f == Familia.OUTRA) "do Android" else "da ${f.nome}"
        return Atalho(
            titulo = "Abrir a limpeza oficial $nome",
            descricao = "Abre a ferramenta de limpeza que já vem no seu celular (é segura e feita pela própria marca).",
            candidatos = c.filterNotNull()
        )
    }

    // ---------- Bateria ----------

    fun atalhosBateria(ctx: Context): List<Atalho> {
        val f = familia()
        val lista = mutableListOf<Atalho>()

        val daMarca = mutableListOf<Intent?>()
        when (f) {
            Familia.SAMSUNG -> {
                daMarca += tela("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
                daMarca += tela("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")
                daMarca += tela("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity")
            }
            Familia.XIAOMI -> {
                daMarca += tela("com.miui.securitycenter", "com.miui.powercenter.PowerSettings")
                daMarca += tela("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
            }
            Familia.HUAWEI, Familia.HONOR -> {
                daMarca += tela("com.huawei.systemmanager", "com.huawei.systemmanager.power.ui.HwPowerManagerActivity")
            }
            Familia.OPPO -> {
                daMarca += tela("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
                daMarca += app(ctx, "com.oplus.battery")
            }
            Familia.VIVO -> {
                daMarca += tela("com.vivo.abe", "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity")
                daMarca += app(ctx, "com.iqoo.powersaving")
            }
            Familia.ASUS -> daMarca += app(ctx, "com.asus.mobilemanager")
            else -> Unit
        }
        val filtrados = daMarca.filterNotNull()
        if (filtrados.isNotEmpty()) {
            lista += Atalho(
                "Bateria — tela da ${f.nome}",
                "Ajustes de bateria próprios da sua marca (modo de economia, apps em segundo plano).",
                filtrados
            )
        }

        lista += Atalho(
            "Quais apps gastam mais bateria",
            "Veja a lista e restrinja os que gastam muito sem você usar.",
            listOf(sistema(Intent.ACTION_POWER_USAGE_SUMMARY), sistema(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        )
        lista += Atalho(
            "Modo economia de bateria",
            "Liga a economia quando a bateria estiver baixa.",
            listOf(sistema(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        )
        lista += Atalho(
            "Otimização de bateria dos apps",
            "Deixe os apps que você pouco usa como “Otimizado” ou “Restrito”.",
            listOf(sistema(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        )
        lista += Atalho(
            "Brilho e tempo de tela",
            "Brilho automático e desligar a tela em 30 s economizam bastante.",
            listOf(sistema(Settings.ACTION_DISPLAY_SETTINGS))
        )
        lista += Atalho(
            "Localização dos apps",
            "Deixe a localização “só durante o uso” nos apps que não precisam dela o tempo todo.",
            listOf(sistema(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        )
        return lista.filter { disponivel(ctx, it) }
    }

    // ---------- Abrir ----------

    fun disponivel(ctx: Context, atalho: Atalho): Boolean =
        atalho.candidatos.any { resolve(ctx, it) }

    /** Tenta cada tela candidata até uma abrir. */
    fun abrir(ctx: Context, atalho: Atalho): Boolean {
        for (intent in atalho.candidatos) {
            if (!resolve(ctx, intent)) continue
            try {
                ctx.startActivity(intent)
                return true
            } catch (e: ActivityNotFoundException) {
                // tenta a próxima
            } catch (e: SecurityException) {
                // tela existe mas a marca bloqueou: tenta a próxima
            }
        }
        return false
    }

    private fun resolve(ctx: Context, intent: Intent): Boolean =
        try {
            ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null ||
                (intent.component != null &&
                    ctx.packageManager.resolveActivity(intent, 0) != null)
        } catch (e: Exception) {
            false
        }

    /** Caminho para limpar o cache na tela de detalhes do app, conforme a marca. */
    fun dicaCache(): String = when (familia()) {
        Familia.SAMSUNG -> "Na tela que vai abrir, toque em “Armazenamento” e depois em “Limpar cache”."
        Familia.XIAOMI -> "Na tela que vai abrir, toque em “Limpar dados” (ou “Armazenamento”) e escolha “Limpar cache”. Não escolha “Limpar todos os dados”."
        Familia.MOTOROLA, Familia.GOOGLE, Familia.NOKIA ->
            "Na tela que vai abrir, toque em “Armazenamento e cache” e depois em “Limpar cache”."
        else -> "Na tela que vai abrir, toque em “Armazenamento” (ou “Armazenamento e cache”) e depois em “Limpar cache”. Não toque em “Limpar dados”."
    }

    /** Página do Files by Google na loja, caso o celular não tenha uma limpeza oficial. */
    fun intentFilesByGoogleNaLoja(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.nbu.files"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

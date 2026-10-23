package br.com.gomestech.otimizador.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs

/** Lê informações do celular: armazenamento, memória, bateria e modelo. */
object Aparelho {

    data class Armazenamento(val total: Long, val livre: Long) {
        val usado: Long get() = total - livre
        val porcentagemUsada: Int get() = if (total > 0) ((usado * 100) / total).toInt() else 0
    }

    data class Memoria(val total: Long, val disponivel: Long, val baixa: Boolean) {
        val porcentagemUsada: Int
            get() = if (total > 0) (((total - disponivel) * 100) / total).toInt() else 0
    }

    data class Bateria(
        val nivel: Int,
        val temperaturaC: Float,
        val voltagemMv: Int,
        val saude: Int,
        val carregando: Boolean,
        val fonte: Int,
        val tecnologia: String?
    ) {
        val saudeTexto: String
            get() = when (saude) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Desgastada"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tensão alta"
                BatteryManager.BATTERY_HEALTH_COLD -> "Fria demais"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Com falha"
                else -> "Não informada pelo aparelho"
            }

        val fonteTexto: String
            get() = when (fonte) {
                BatteryManager.BATTERY_PLUGGED_AC -> "tomada"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "sem fio"
                else -> "—"
            }
    }

    fun armazenamento(): Armazenamento {
        val st = StatFs(Environment.getDataDirectory().path)
        return Armazenamento(st.totalBytes, st.availableBytes)
    }

    fun memoria(ctx: Context): Memoria {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return Memoria(info.totalMem, info.availMem, info.lowMemory)
    }

    fun bateria(ctx: Context): Bateria? {
        // ACTION_BATTERY_CHANGED é "sticky": lemos o último valor sem precisar ficar ouvindo.
        val i: Intent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val nivel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val escala = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return Bateria(
            nivel = if (nivel >= 0 && escala > 0) (nivel * 100) / escala else -1,
            temperaturaC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltagemMv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            saude = i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
            carregando = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            fonte = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
            tecnologia = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
        )
    }

    /** "Samsung SM-A155M • Android 14" */
    fun descricao(): String {
        val marca = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val modelo = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true))
            Build.MODEL.substring(Build.MANUFACTURER.length).trim() else Build.MODEL
        return "$marca $modelo • Android ${Build.VERSION.RELEASE}"
    }

    /** Nota de 0 a 100 e os motivos que tiraram pontos. */
    data class Nota(val valor: Int, val problemas: List<String>)

    fun nota(ctx: Context): Nota {
        var pontos = 100
        val problemas = mutableListOf<String>()

        val arm = armazenamento()
        when {
            arm.porcentagemUsada >= 90 -> {
                pontos -= 40
                problemas += "Armazenamento quase cheio (${arm.porcentagemUsada}%). Isso é o que mais deixa o celular lento: libere espaço em Arquivos e Apps."
            }
            arm.porcentagemUsada >= 80 -> {
                pontos -= 20
                problemas += "Armazenamento em ${arm.porcentagemUsada}%. O ideal é ficar abaixo de 80–85%."
            }
        }

        val mem = memoria(ctx)
        if (mem.baixa) {
            pontos -= 15
            problemas += "Memória RAM no limite agora. Reiniciar o celular ajuda."
        }

        bateria(ctx)?.let { b ->
            if (b.temperaturaC >= 40f) {
                pontos -= 15
                problemas += "Bateria quente (${"%.1f".format(b.temperaturaC)} °C). Evite usar carregando e tire a capinha se esquentar muito."
            }
            if (b.saude != BatteryManager.BATTERY_HEALTH_GOOD &&
                b.saude != BatteryManager.BATTERY_HEALTH_UNKNOWN
            ) {
                pontos -= 20
                problemas += "Saúde da bateria: ${b.saudeTexto}. Pode ser hora de trocar a bateria."
            }
        }

        return Nota(pontos.coerceIn(0, 100), problemas)
    }
}

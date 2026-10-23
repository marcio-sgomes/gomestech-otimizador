package br.com.gomestech.otimizador.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.gomestech.otimizador.MainActivity
import br.com.gomestech.otimizador.R
import java.util.Calendar

/** Lembrete semanal para fazer o checklist de manutenção. */
object Lembrete {

    private const val CANAL = "manutencao_semanal"
    private const val PREFS = "lembrete"
    private const val CHAVE_ATIVO = "ativo"
    private const val SEMANA_MS = 7L * 24 * 60 * 60 * 1000

    fun ativo(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(CHAVE_ATIVO, false)

    fun definir(ctx: Context, ligado: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(CHAVE_ATIVO, ligado).apply()
        if (ligado) agendar(ctx) else cancelar(ctx)
    }

    private fun pendente(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, 1001, Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** Todo domingo às 10h (horário aproximado, para não gastar bateria). */
    fun agendar(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val c = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, SEMANA_MS, pendente(ctx))
    }

    private fun cancelar(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendente(ctx))
    }

    fun criarCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(CANAL, "Manutenção semanal", NotificationManager.IMPORTANCE_DEFAULT)
            canal.description = "Lembrete para cuidar do celular uma vez por semana"
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(canal)
        }
    }

    fun notificar(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        criarCanal(ctx)
        val abrir = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Hora do cuidado semanal do celular")
            .setContentText("Leva 2 minutos: reiniciar, conferir atualizações e liberar espaço.")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(2001, n)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Lembrete.notificar(context)
    }
}

/** Depois que o celular reinicia, o Android apaga os alarmes; aqui agendamos de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Lembrete.ativo(context)) {
            Lembrete.agendar(context)
        }
    }
}

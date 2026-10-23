package br.com.gomestech.otimizador

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import br.com.gomestech.otimizador.Robo.ctx
import br.com.gomestech.otimizador.Robo.device
import br.com.gomestech.otimizador.Robo.esperar
import br.com.gomestech.otimizador.Robo.esperarSairDoApp
import br.com.gomestech.otimizador.Robo.fragmentoAtual
import br.com.gomestech.otimizador.Robo.naTela
import br.com.gomestech.otimizador.Robo.pacote
import br.com.gomestech.otimizador.Robo.voltarParaApp
import br.com.gomestech.otimizador.data.Marcas
import br.com.gomestech.otimizador.data.ScannerApps
import br.com.gomestech.otimizador.data.ScannerArquivos
import br.com.gomestech.otimizador.logica.Tempo
import br.com.gomestech.otimizador.reminder.Checklist
import br.com.gomestech.otimizador.reminder.Lembrete
import br.com.gomestech.otimizador.ui.AppsFragment
import br.com.gomestech.otimizador.ui.ArquivosFragment
import br.com.gomestech.otimizador.ui.BaseListaFragment
import br.com.gomestech.otimizador.ui.BateriaFragment
import br.com.gomestech.otimizador.ui.LimpezaFragment
import br.com.gomestech.otimizador.ui.Linha
import br.com.gomestech.otimizador.ui.PainelFragment
import br.com.gomestech.otimizador.update.Atualizador
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.regex.Pattern

/**
 * Robô de vistoria: usa o app como uma pessoa usaria, tela por tela,
 * e confere se tudo funciona sem fechar e sem estragar nada no aparelho.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class RoboDeVistoriaTest {

    private lateinit var cenario: ActivityScenario<MainActivity>

    @Before
    fun antes() {
        Robo.concederPermissoes()
        Lembrete.definir(ctx, false)
        ctx.getSharedPreferences("checklist", 0).edit().clear().commit()
        ctx.getSharedPreferences("dicas", 0).edit().clear().commit()
        cenario = ActivityScenario.launch(MainActivity::class.java)
        assertTrue("O app não abriu", esperar(10_000) { Robo.appNaFrente() })
    }

    @After
    fun depois() {
        voltarParaApp()
        try { device.setOrientationNatural() } catch (e: Exception) { }
        cenario.close()
    }

    private fun irPara(idMenu: Int) {
        voltarParaApp()
        onView(withId(idMenu)).perform(click())
    }

    /** Espera a lista terminar de analisar e devolve o fragmento. */
    private inline fun <reified T : BaseListaFragment> esperarLista(): T {
        assertTrue("A tela ${T::class.simpleName} não abriu", esperar(10_000) { fragmentoAtual(cenario) is T })
        val f = fragmentoAtual(cenario) as T
        assertTrue("A análise da tela ${T::class.simpleName} não terminou em 90 s", esperar(90_000) {
            naTela(cenario) { f.isResumed && !f.carregando }
        })
        return f
    }

    private fun textoDe(id: Int): String = naTela(cenario) { it.findViewById<TextView>(id).text.toString() }

    private fun visivel(id: Int): Boolean = naTela(cenario) { it.findViewById<View>(id).visibility == View.VISIBLE }

    // ------------------------------------------------------------------

    @Test
    fun t01_painelMostraNotaArmazenamentoRamBateriaERodape() {
        val ano = Calendar.getInstance().get(Calendar.YEAR)
        onView(withId(R.id.txtRodape)).check(matches(withText("© $ano GomesTech — feito por Márcio Gomes")))

        assertTrue(esperar(10_000) { fragmentoAtual(cenario) is PainelFragment })
        assertTrue(esperar(10_000) { textoDe(R.id.txtNota).isNotBlank() })
        val nota = textoDe(R.id.txtNota).toInt()
        assertTrue("Nota fora de 0..100: $nota", nota in 0..100)
        assertTrue(textoDe(R.id.txtNotaDescricao).startsWith("Seu celular"))
        assertTrue(textoDe(R.id.txtAparelho).contains("Android"))
        assertTrue(textoDe(R.id.txtArmazenamento).contains("usados de"))
        assertTrue(textoDe(R.id.txtRam).contains("em uso de"))
        val bat = textoDe(R.id.txtBateriaResumo)
        assertTrue("Bateria: $bat", bat.contains("%") || bat.contains("Não foi possível"))
        assertTrue(textoDe(R.id.txtDicas).isNotBlank())
        assertTrue(textoDe(R.id.btnManutencaoMarca).startsWith("Abrir a limpeza oficial"))
    }

    @Test
    fun t02_checklistMarcaEDesmarca() {
        val qtd = naTela(cenario) { it.findViewById<LinearLayout>(R.id.listaChecklist).childCount }
        assertEquals(Checklist.itens.size, qtd)
        onView(withText("Reiniciar o celular")).perform(scrollTo(), click())
        assertTrue(esperar(5_000) { Checklist.marcado(ctx, "reiniciar") })
        onView(withText("Reiniciar o celular")).perform(scrollTo(), click())
        assertTrue(esperar(5_000) { !Checklist.marcado(ctx, "reiniciar") })
        val algumMarcado = naTela(cenario) { a ->
            val l = a.findViewById<LinearLayout>(R.id.listaChecklist)
            (0 until l.childCount).any { (l.getChildAt(it) as CheckBox).isChecked }
        }
        assertFalse(algumMarcado)
    }

    @Test
    fun t03_lembreteSemanalLigaDesligaENotifica() {
        onView(withId(R.id.swLembrete)).perform(scrollTo(), click())
        assertTrue("Lembrete não ligou", esperar(5_000) { Lembrete.ativo(ctx) })
        Lembrete.notificar(ctx) // não pode fechar o app
        onView(withId(R.id.swLembrete)).perform(scrollTo(), click())
        assertTrue("Lembrete não desligou", esperar(5_000) { !Lembrete.ativo(ctx) })
    }

    @Test
    fun t04_botaoLimpezaOficialAbreTelaDoSistema() {
        onView(withId(R.id.btnManutencaoMarca)).perform(scrollTo(), click())
        assertTrue("O botão de limpeza oficial não abriu nenhuma tela", esperarSairDoApp())
        voltarParaApp()
    }

    @Test
    fun t05_limpezaListaCacheLimpaSoOProprioCacheEAbreATelaCerta() {
        val lixo = File(ctx.cacheDir, "teste-robo.tmp").apply { writeText("lixo") }
        val pastaUpdates = File(ctx.cacheDir, "updates").apply { mkdirs() }
        val guardado = File(pastaUpdates, "guardar.txt").apply { writeText("x") }
        val pacotesAntes = pacotesInstalados()

        irPara(R.id.nav_limpeza)
        val f = esperarLista<LimpezaFragment>()

        assertFalse("O cache do próprio app deveria ter sido limpo", lixo.exists())
        assertTrue("A pasta de atualização não pode ser apagada", guardado.exists())
        guardado.delete()
        assertFalse("Pediu permissão mesmo já tendo", visivel(R.id.cardPermissao))

        val itens = f.itensVisiveis()
        if (Build.VERSION.SDK_INT >= 26) {
            itens.forEach { assertTrue(it.detalhe, it.detalhe.startsWith("Cache:")) }
            val tamanhos = itens.map { it.dado as Long }
            assertEquals("Lista deve vir do maior cache para o menor", tamanhos.sortedDescending(), tamanhos)
        }
        if (itens.isEmpty()) assertTrue(visivel(R.id.txtVazio))

        // Toca em "Limpar cache": mostra a dica e abre a tela do app no sistema.
        val alvo = itens.firstOrNull() ?: Linha("com.android.settings", "Configurações", "", null, acao = "Limpar cache")
        naTela(cenario) { f.aoTocarAcao(alvo) }
        onView(withText("Como limpar o cache")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Entendi")).inRoot(isDialog()).perform(click())
        assertTrue("Não abriu a tela de detalhes do app", esperarSairDoApp())
        voltarParaApp()

        // Segunda vez já vai direto (sem a dica).
        naTela(cenario) { f.aoTocarAcao(alvo) }
        assertTrue(esperarSairDoApp())
        voltarParaApp()

        // Botão "Limpeza oficial".
        onView(withId(R.id.btnAcaoExtra)).perform(click())
        assertTrue(esperarSairDoApp())
        voltarParaApp()

        assertEquals("Nenhum app pode ser removido pela limpeza", pacotesAntes, pacotesInstalados())
    }

    @Test
    fun t06_appsEsquecidosFiltraCertoENaoDesinstalaNadaSozinho() {
        val pacotesAntes = pacotesInstalados()
        irPara(R.id.nav_apps)
        val f = esperarLista<AppsFragment>()
        assertFalse(visivel(R.id.cardPermissao))
        assertEquals(3, naTela(cenario) { it.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipsFiltro).childCount })

        val agora = System.currentTimeMillis()
        val itens30 = f.itensVisiveis()
        itens30.forEach {
            val app = it.dado as ScannerApps.AppInfo
            assertTrue("${app.pacote} não deveria estar na lista", Tempo.ehEsquecido(app.ultimoUso, app.instaladoEm, 30, agora))
            assertFalse("O próprio app não pode aparecer", app.pacote == pacote)
        }

        onView(withText("Sem uso há 90+ dias")).perform(click())
        val f90 = esperarLista<AppsFragment>()
        val itens90 = f90.itensVisiveis()
        assertTrue("Filtro de 90 dias não pode ter mais apps que o de 30", itens90.size <= itens30.size)
        itens90.forEach {
            val app = it.dado as ScannerApps.AppInfo
            assertTrue(Tempo.ehEsquecido(app.ultimoUso, app.instaladoEm, 90, agora))
        }

        // Botão de um app da lista: abre a tela do sistema; o robô CANCELA (volta).
        itens30.firstOrNull()?.let { linha ->
            naTela(cenario) { f90.aoTocarAcao(linha) }
            assertTrue(esperarSairDoApp())
            voltarParaApp()
        }

        // "Desinstalar" de um app comum: deve abrir a confirmação do Android, nunca apagar direto.
        val pacoteTeste = Robo.instr.context.packageName
        val falso = ScannerApps.AppInfo(pacoteTeste, "App de teste", null, 0, 0, 0, 0, 0, sistema = false)
        naTela(cenario) { f90.aoTocarAcao(Linha(pacoteTeste, "App de teste", "", null, acao = "Desinstalar", dado = falso)) }
        assertTrue("Não abriu a confirmação de desinstalação", esperarSairDoApp())
        voltarParaApp()
        assertTrue("O app de teste não pode ter sido desinstalado", pacotesInstalados().contains(pacoteTeste))

        assertEquals("Nenhum app pode ser removido sem a pessoa confirmar", pacotesAntes, pacotesInstalados())
    }

    @Test
    fun t07_arquivosGrandesAchaFiltraEApagaSoOArquivoEscolhido() {
        val uriTeste = Robo.criarArquivoTeste()
        assertNotNull(uriTeste)
        assertTrue("Arquivo de teste não ficou com mais de 10 MB", esperar(30_000) {
            (Robo.acharArquivoTeste()?.second ?: 0L) >= ScannerArquivos.TAMANHO_MINIMO
        })
        val midiasAntes = Robo.contarMidias()

        irPara(R.id.nav_arquivos)
        var f = esperarLista<ArquivosFragment>()
        assertFalse(visivel(R.id.cardPermissao))

        fun linhaTeste(fr: ArquivosFragment) = fr.itensVisiveis().firstOrNull { it.titulo == Robo.NOME_ARQUIVO_TESTE }

        val linha = linhaTeste(f)
        assertNotNull("O arquivo grande de teste não apareceu na lista", linha)
        assertTrue(linha!!.detalhe, linha.detalhe.startsWith("Foto"))
        assertTrue(linha.detalhe, linha.detalhe.contains(Robo.PASTA_TESTE))
        val tamanhos = f.itensVisiveis().map { (it.dado as ScannerArquivos.Arquivo).tamanho }
        assertEquals("Lista deve vir do maior para o menor", tamanhos.sortedDescending(), tamanhos)
        f.itensVisiveis().forEach {
            assertTrue((it.dado as ScannerArquivos.Arquivo).tamanho >= ScannerArquivos.TAMANHO_MINIMO)
        }

        // Filtros
        onView(withText("Áudios")).perform(click())
        f = esperarLista()
        assertTrue("Foto apareceu no filtro de áudios", linhaTeste(f) == null)
        onView(withText("Fotos")).perform(click())
        f = esperarLista()
        assertNotNull(linhaTeste(f))
        onView(withText("Todos")).perform(click())
        f = esperarLista()
        val alvo = linhaTeste(f)!!

        // Tocar no nome abre o arquivo para conferir (não pode fechar o app).
        naTela(cenario) { f.aoTocarLinha(alvo) }
        // Espera o visualizador abrir (se o emulador tiver um) antes de voltar; sem isso ele pode
        // aparecer DEPOIS e ficar na frente da confirmação de exclusão.
        esperarSairDoApp(8_000)
        voltarParaApp()
        f = esperarLista()
        val alvo2 = linhaTeste(f)!!

        // Apagar: o app sempre pede confirmação.
        naTela(cenario) { f.aoTocarAcao(alvo2) }
        if (Build.VERSION.SDK_INT >= 30) {
            // Procura o botão só na janela do Android (nunca no próprio app, que tem botões "Apagar" na lista).
            val foraDoApp = Pattern.compile("^(?!${Pattern.quote(pacote)}$).+")
            val botao = device.wait(
                Until.findObject(
                    By.text(Pattern.compile("(?i)(allow|delete|permitir|excluir|apagar)")).pkg(foraDoApp)
                ),
                15_000
            )
            assertNotNull("O Android não mostrou a confirmação de exclusão", botao)
            botao.click()
        } else {
            onView(withText("Apagar arquivo?")).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText("Apagar")).inRoot(isDialog()).perform(click())
        }
        assertTrue("O arquivo de teste não foi apagado", esperar(15_000) { Robo.acharArquivoTeste() == null })
        voltarParaApp()
        f = esperarLista()
        assertTrue("O arquivo apagado continua na lista", linhaTeste(f) == null)
        assertEquals("Só o arquivo escolhido pode ser apagado", midiasAntes - 1, Robo.contarMidias())
    }

    @Test
    fun t08_bateriaMostraDadosETodosOsAtalhosAbrem() {
        irPara(R.id.nav_bateria)
        assertTrue(esperar(10_000) { fragmentoAtual(cenario) is BateriaFragment })
        assertTrue(esperar(10_000) { textoDe(R.id.txtNivel).isNotBlank() })
        val nivel = textoDe(R.id.txtNivel)
        assertTrue("Nível: $nivel", nivel.matches(Regex("\\d{1,3}%")) || nivel == "—")
        assertTrue(textoDe(R.id.txtDetalhesBateria).contains("Temperatura") || nivel == "—")
        assertTrue(textoDe(R.id.txtMarca).startsWith("Celular detectado:"))

        val qtd = naTela(cenario) { it.findViewById<LinearLayout>(R.id.listaAtalhos).childCount }
        assertTrue("Poucos atalhos de bateria: $qtd", qtd >= 4)
        for (i in 0 until qtd) {
            val titulo = naTela(cenario) {
                val b = it.findViewById<LinearLayout>(R.id.listaAtalhos).getChildAt(i) as Button
                b.performClick()
                b.text.toString()
            }
            assertTrue("O atalho \"$titulo\" não abriu nenhuma tela", esperarSairDoApp())
            voltarParaApp()
            assertTrue(esperar(10_000) { fragmentoAtual(cenario) is BateriaFragment })
        }
    }

    @Test
    fun t09_atualizacaoInternaAchaEBaixaUmApkValido() {
        // O GitHub permite 60 consultas por hora sem login, e o app consulta a cada vez que abre.
        // Só testa se ainda sobram consultas para o teste inteiro (senão o resultado seria aleatório).
        var restantes = -1
        val codigo = try {
            (URL("https://api.github.com/repos/${BuildConfig.UPDATE_REPO}/releases/latest").openConnection() as HttpURLConnection)
                .run {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    responseCode.also {
                        restantes = getHeaderField("X-RateLimit-Remaining")?.toIntOrNull() ?: 60
                        disconnect()
                    }
                }
        } catch (e: Exception) {
            -1
        }
        assumeTrue("GitHub indisponível ou sem versão publicada ($codigo)", codigo == 200)
        assumeTrue("Limite de consultas do GitHub quase no fim ($restantes)", restantes >= 5)

        val lanc = runBlocking { Atualizador.verificar(versaoAtual = 0) }
        assertNotNull("Não encontrou a versão publicada", lanc)
        assertTrue(lanc!!.urlApk.endsWith(".apk"))

        // Já atualizado: não oferece nada.
        assertEquals(null, runBlocking { Atualizador.verificar(versaoAtual = lanc.codigo) })

        val apk = runBlocking { Atualizador.baixar(ctx, lanc) { } }
        assertNotNull("O download do APK falhou", apk)
        assertTrue("APK pequeno demais", apk!!.length() > 1_000_000)
        apk.delete()

        // O aviso "Atualizar" aparece sozinho quando o app está desatualizado.
        if (lanc.codigo > BuildConfig.VERSION_CODE) {
            if (!esperar(15_000) { visivel(R.id.cardUpdate) }) {
                // A consulta feita ao abrir pode ter pego a rede do emulador ainda instável:
                // sai e volta para a tela (o app consulta de novo sozinho ao voltar).
                cenario.moveToState(Lifecycle.State.STARTED)
                cenario.moveToState(Lifecycle.State.RESUMED)
            }
            assertTrue("O aviso de atualização não apareceu", esperar(30_000) { visivel(R.id.cardUpdate) })
            assertTrue(textoDe(R.id.txtUpdate).startsWith("Nova versão disponível"))
        }
    }

    @Test
    fun t10_girarATelaEmTodasAsTelasNaoFechaOApp() {
        val telas = listOf(R.id.nav_painel, R.id.nav_limpeza, R.id.nav_apps, R.id.nav_arquivos, R.id.nav_bateria)
        for (tela in telas) {
            irPara(tela)
            device.setOrientationLeft()
            device.waitForIdle()
            Thread.sleep(1_500)
            device.setOrientationNatural()
            device.waitForIdle()
            Thread.sleep(1_500)
            assertTrue("O app fechou ao girar a tela", esperar(10_000) { Robo.appNaFrente() })
            assertNotNull(fragmentoAtual(cenario))
        }
        device.unfreezeRotation()
    }

    @Test
    fun t11_seguranca_naoPedePermissoesPerigosasENaoMexeEmNadaSozinho() {
        val info = ctx.packageManager.getPackageInfo(pacote, PackageManager.GET_PERMISSIONS)
        val pedidas = info.requestedPermissions?.toSet() ?: emptySet()
        val proibidas = listOf(
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SETTINGS",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.KILL_BACKGROUND_PROCESSES",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.INSTALL_PACKAGES",
            "android.permission.DELETE_PACKAGES",
            "android.permission.CLEAR_APP_CACHE",
            "android.permission.READ_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            Manifest.permission.READ_PHONE_STATE
        )
        proibidas.forEach { assertFalse("Pede permissão perigosa: $it", it in pedidas) }

        // Analisar tudo várias vezes não pode apagar nem desinstalar nada.
        val pacotesAntes = pacotesInstalados()
        val midiasAntes = Robo.contarMidias()
        repeat(3) {
            ScannerApps.escanear(ctx)
            ScannerArquivos.escanear(ctx)
        }
        assertEquals(pacotesAntes, pacotesInstalados())
        assertEquals(midiasAntes, Robo.contarMidias())
    }

    @Test
    fun t12_atalhosDaMarcaExistemNoAparelho() {
        val manutencao = Marcas.atalhoManutencao(ctx)
        assertTrue(manutencao.candidatos.isNotEmpty())
        assertTrue("Nenhuma tela de limpeza disponível", Marcas.disponivel(ctx, manutencao))
        val bateria = Marcas.atalhosBateria(ctx)
        assertTrue(bateria.size >= 4)
        bateria.forEach { assertTrue(it.titulo, Marcas.disponivel(ctx, it)) }
        assertTrue(Marcas.dicaCache().contains("Limpar cache"))
    }

    private fun pacotesInstalados(): Set<String> =
        @Suppress("DEPRECATION")
        ctx.packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
}

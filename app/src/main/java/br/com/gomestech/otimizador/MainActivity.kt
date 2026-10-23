package br.com.gomestech.otimizador

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.gomestech.otimizador.reminder.Lembrete
import br.com.gomestech.otimizador.ui.AppsFragment
import br.com.gomestech.otimizador.ui.ArquivosFragment
import br.com.gomestech.otimizador.ui.BateriaFragment
import br.com.gomestech.otimizador.ui.LimpezaFragment
import br.com.gomestech.otimizador.ui.PainelFragment
import br.com.gomestech.otimizador.update.Atualizador
import br.com.gomestech.otimizador.util.abrirComSeguranca
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private var versaoNova: Atualizador.Versao? = null
    private var baixando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Rodapé sempre com o ano atual.
        findViewById<TextView>(R.id.txtRodape).text =
            getString(R.string.rodape, Calendar.getInstance().get(Calendar.YEAR))

        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener { item ->
            val tela: Fragment = when (item.itemId) {
                R.id.nav_limpeza -> LimpezaFragment()
                R.id.nav_apps -> AppsFragment()
                R.id.nav_arquivos -> ArquivosFragment()
                R.id.nav_bateria -> BateriaFragment()
                else -> PainelFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.container, tela).commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_painel

        findViewById<MaterialButton>(R.id.btnUpdate).setOnClickListener { atualizarAgora() }

        Lembrete.criarCanal(this)
        if (Lembrete.ativo(this)) Lembrete.agendar(this)
    }

    override fun onResume() {
        super.onResume()
        verificarAtualizacao()
    }

    /** Procura versão nova toda vez que o app abre; se achar, o botão "Atualizar" aparece sozinho. */
    private fun verificarAtualizacao() {
        if (baixando) return
        lifecycleScope.launch {
            val v = Atualizador.verificar() ?: return@launch
            versaoNova = v
            findViewById<TextView>(R.id.txtUpdate).text = getString(R.string.update_titulo, v.nome)
            findViewById<MaterialCardView>(R.id.cardUpdate).visibility = View.VISIBLE
        }
    }

    private fun atualizarAgora() {
        val v = versaoNova ?: return
        if (baixando) return
        if (!Atualizador.podeInstalar(this)) {
            Toast.makeText(this, R.string.update_permissao, Toast.LENGTH_LONG).show()
            abrirComSeguranca(Atualizador.intentLiberarInstalacao(this))
            return
        }
        val txt = findViewById<TextView>(R.id.txtUpdate)
        val botao = findViewById<MaterialButton>(R.id.btnUpdate)
        baixando = true
        botao.isEnabled = false
        txt.text = getString(R.string.update_baixando, 0)
        lifecycleScope.launch {
            val apk = Atualizador.baixar(this@MainActivity, v) { p ->
                txt.text = getString(R.string.update_baixando, p)
            }
            baixando = false
            botao.isEnabled = true
            if (apk == null) {
                txt.text = getString(R.string.update_titulo, v.nome)
                Toast.makeText(this@MainActivity, R.string.update_erro, Toast.LENGTH_LONG).show()
            } else {
                txt.text = getString(R.string.update_titulo, v.nome)
                Atualizador.instalar(this@MainActivity, apk)
            }
        }
    }
}

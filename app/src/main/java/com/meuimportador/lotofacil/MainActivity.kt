
package com.meuimportador.lotofacil

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val jogos = mutableListOf<List<Int>>()
    private val codigoImportacao = 100
    private lateinit var resultado: TextView
    private lateinit var listaJogos: LinearLayout
    private lateinit var progresso: TextView
    private lateinit var iniciar: Button
    private lateinit var parar: Button
    private val handler = Handler(Looper.getMainLooper())
    private var executando = false
    private var indiceAtual = 0
    private var tarefa: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }
        tela.addView(TextView(this).apply {
            text = "Meu Importador Lotofácil"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
        })
        tela.addView(Button(this).apply {
            text = "IMPORTAR JOGOS"
            setOnClickListener {
                pararSequencia()
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "text/plain"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(intent, codigoImportacao)
            }
        })
        tela.addView(Button(this).apply {
            text = "ABRIR LOTERIAS CAIXA"
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.loteriasonline.caixa.gov.br/")))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Não foi possível abrir o site", Toast.LENGTH_LONG).show()
                }
            }
        })
        resultado = TextView(this).apply {
            text = "Selecione um arquivo TXT com seus jogos."
            textSize = 17f
        }
        tela.addView(resultado)
        progresso = TextView(this).apply {
            text = "Sequência não iniciada"
            textSize = 18f
        }
        tela.addView(progresso)
        iniciar = Button(this).apply {
            text = "INICIAR SIMULAÇÃO EM SEQUÊNCIA"
            isEnabled = false
            setOnClickListener { iniciarSequencia() }
        }
        tela.addView(iniciar)
        parar = Button(this).apply {
            text = "PARAR SEQUÊNCIA"
            isEnabled = false
            setOnClickListener { pararSequencia() }
        }
        tela.addView(parar)
        val rolagem = ScrollView(this)
        listaJogos = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        rolagem.addView(listaJogos)
        tela.addView(rolagem, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(tela)
    }

    private fun copiar(jogo: List<Int>, numero: Int) {
        val dezenas = jogo.joinToString(" ") { "%02d".format(it) }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Jogo $numero", dezenas))
        Toast.makeText(this, "Jogo $numero copiado!", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarJogos(invalidos: Int) {
        resultado.text = "Jogos válidos: ${jogos.size}\nLinhas inválidas: $invalidos\n"
        listaJogos.removeAllViews()
        jogos.forEachIndexed { indice, jogo ->
            listaJogos.addView(TextView(this).apply {
                text = "\nJogo ${indice + 1}:\n" + jogo.joinToString(" ") { "%02d".format(it) }
                textSize = 19f
                setTextIsSelectable(true)
            })
            listaJogos.addView(Button(this).apply {
                text = "COPIAR JOGO ${indice + 1}"
                setOnClickListener { copiar(jogo, indice + 1) }
            })
        }
        iniciar.isEnabled = jogos.isNotEmpty()
    }

    private fun iniciarSequencia() {
        if (executando || jogos.isEmpty()) return
        executando = true
        indiceAtual = 0
        iniciar.isEnabled = false
        parar.isEnabled = true
        avancarSequencia()
    }

    private fun avancarSequencia() {
        if (!executando) return
        if (indiceAtual >= jogos.size) {
            executando = false
            tarefa = null
            progresso.text = "Simulação concluída: ${jogos.size} jogos percorridos. Nenhuma aposta enviada."
            iniciar.isEnabled = true
            parar.isEnabled = false
            return
        }
        val jogo = jogos[indiceAtual]
        progresso.text = "Simulação: jogo ${indiceAtual + 1} de ${jogos.size}\n" +
            jogo.joinToString(" ") { "%02d".format(it) }
        indiceAtual++
        val proxima = Runnable { avancarSequencia() }
        tarefa = proxima
        handler.postDelayed(proxima, 1500L)
    }

    private fun pararSequencia() {
        executando = false
        tarefa?.let { handler.removeCallbacks(it) }
        tarefa = null
        if (::progresso.isInitialized) progresso.text = "Sequência interrompida."
        if (::iniciar.isInitialized) iniciar.isEnabled = jogos.isNotEmpty()
        if (::parar.isInitialized) parar.isEnabled = false
    }

    override fun onDestroy() {
        pararSequencia()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != codigoImportacao || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val texto = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: throw IllegalStateException("Arquivo vazio ou inacessível")
            val novosJogos = mutableListOf<List<Int>>()
            var invalidos = 0
            texto.lines().forEach { linha ->
                val numeros = Regex("\\d+").findAll(linha).map { it.value.toIntOrNull() }.toList()
                if (numeros.isEmpty()) return@forEach
                if (numeros.size == 15 && numeros.all { it != null && it in 1..25 } &&
                    numeros.filterNotNull().distinct().size == 15) {
                    novosJogos.add(numeros.filterNotNull().sorted())
                } else invalidos++
            }
            jogos.clear()
            jogos.addAll(novosJogos)
            progresso.text = "Sequência não iniciada"
            mostrarJogos(invalidos)
        } catch (e: Exception) {
            resultado.text = "Erro ao importar: ${e.message}"
        }
    }
}

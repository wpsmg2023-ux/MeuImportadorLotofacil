
package com.meuimportador.lotofacil

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {

    private val jogos = mutableListOf<List<Int>>()
    private var atual = 0
    private lateinit var status: TextView
    private lateinit var numeros: TextView
    private lateinit var contador: TextView
    private lateinit var botoes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        scroll.addView(tela)

        val titulo = TextView(this).apply {
            text = "MEU IMPORTADOR LOTOFÁCIL V4"
            textSize = 22f
            gravity = 17
            setTypeface(null, Typeface.BOLD)
        }
        tela.addView(titulo)

        status = TextView(this).apply {
            text = "Importe seu arquivo TXT para começar."
            textSize = 17f
            setPadding(0, 20, 0, 20)
        }
        tela.addView(status)

        contador = TextView(this).apply {
            textSize = 20f
            gravity = 17
        }
        tela.addView(contador)

        numeros = TextView(this).apply {
            textSize = 26f
            gravity = 17
            setPadding(8, 24, 8, 24)
        }
        tela.addView(numeros)

        botoes = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        tela.addView(botoes)

        botao("IMPORTAR JOGOS TXT") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, 100)
        }

        botao("COPIAR JOGO ATUAL") {
            if (jogos.isNotEmpty()) {
                val texto = formatar(jogos[atual])
                val clipboard = getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Lotofácil", texto)
                )
                status.text = "Jogo ${atual + 1} copiado."
            } else {
                status.text = "Importe os jogos primeiro."
            }
        }

        botao("PRÓXIMO JOGO") {
            if (atual < jogos.size - 1) {
                atual++
                mostrarJogo()
            } else {
                status.text = "Último jogo da lista."
            }
        }

        botao("JOGO ANTERIOR") {
            if (atual > 0) {
                atual--
                mostrarJogo()
            }
        }

        botao("ABRIR LOTERIAS CAIXA") {
            val url = "https://www.loteriasonline.caixa.gov.br/"
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
        }

        botao("COPIAR TODOS OS JOGOS") {
            if (jogos.isNotEmpty()) {
                val texto = jogos.joinToString("\n") {
                    formatar(it)
                }
                val clipboard = getSystemService(
                    Context.CLIPBOARD_SERVICE
                ) as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Jogos Lotofácil", texto)
                )
                status.text = "${jogos.size} jogos copiados."
            }
        }

        botao("REINICIAR SEQUÊNCIA") {
            atual = 0
            mostrarJogo()
        }

        botao("LIMPAR JOGOS") {
            jogos.clear()
            atual = 0
            mostrarJogo()
            status.text = "Lista limpa."
        }

        mostrarJogo()
        setContentView(scroll)
    }

    private fun botao(
        nome: String,
        acao: () -> Unit
    ) {
        val botao = Button(this).apply {
            text = nome
            textSize = 16f
            setOnClickListener { acao() }
        }
        botoes.addView(botao)
    }

    private fun formatar(jogo: List<Int>): String {
        return jogo.joinToString(" ") {
            "%02d".format(it)
        }
    }

    private fun mostrarJogo() {
        if (jogos.isEmpty()) {
            contador.text = "Nenhum jogo carregado"
            numeros.text = ""
            return
        }

        contador.text = "JOGO ${atual + 1} DE ${jogos.size}"
        numeros.text = formatar(jogos[atual])
    }

    @Deprecated("Compatibilidade Android")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        )

        if (requestCode != 100 ||
            resultCode != RESULT_OK
        ) return

        val uri = data?.data ?: return

        try {
            val linhas = contentResolver
                .openInputStream(uri)!!
                .bufferedReader()
                .use { it.readLines() }

            val novosJogos = mutableListOf<List<Int>>()
            var invalidos = 0

            for (linha in linhas) {
                if (linha.isBlank()) continue

                val dezenas = Regex("\\d+")
                    .findAll(linha)
                    .map { it.value.toInt() }
                    .toList()

                if (
                    dezenas.size == 15 &&
                    dezenas.distinct().size == 15 &&
                    dezenas.all { it in 1..25 }
                ) {
                    novosJogos.add(dezenas.sorted())
                } else {
                    invalidos++
                }
            }

            jogos.clear()
            jogos.addAll(novosJogos)
            atual = 0

            status.text =
                "${jogos.size} jogos importados. " +
                "$invalidos linhas inválidas."

            mostrarJogo()

        } catch (e: Exception) {
            status.text = "Erro: ${e.message}"
        }
    }
}
  

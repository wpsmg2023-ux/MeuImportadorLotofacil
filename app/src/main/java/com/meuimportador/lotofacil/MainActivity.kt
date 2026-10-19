
package com.meuimportador.lotofacil

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var resultado: TextView
    private val jogos = mutableListOf<List<Int>>()
    private val codigoImportacao = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tela = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }

        val titulo = TextView(this).apply {
            text = "Meu Importador Lotofácil"
            textSize = 25f
            setTypeface(null, Typeface.BOLD)
        }
        tela.addView(titulo)

        val botao = Button(this).apply {
            text = "IMPORTAR JOGOS"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "text/plain"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(
                    intent, codigoImportacao
                )
            }
        }
        tela.addView(botao)

        resultado = TextView(this).apply {
            text = "Selecione um arquivo TXT com seus jogos."
            textSize = 17f
            setTextIsSelectable(true)
        }

        val rolagem = ScrollView(this)
        rolagem.addView(resultado)
        tela.addView(
            rolagem,
            LinearLayout.LayoutParams(
                -1, 0, 1f
            )
        )

        setContentView(tela)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode, resultCode, data
        )

        if (requestCode != codigoImportacao ||
            resultCode != RESULT_OK) return

        val uri = data?.data ?: return

        try {
            val texto = contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return

            jogos.clear()
            var invalidos = 0

            texto.lines().forEach { linha ->
                val numeros = Regex("\\d+")
                    .findAll(linha)
                    .map { it.value.toInt() }
                    .toList()

                if (numeros.isEmpty()) return@forEach

                if (numeros.size == 15 &&
                    numeros.distinct().size == 15 &&
                    numeros.all { it in 1..25 }
                ) {
                    jogos.add(numeros.sorted())
                } else {
                    invalidos++
                }
            }

            val exibicao = StringBuilder()
            exibicao.append(
                "Jogos válidos: ${jogos.size}\n"
            )
            exibicao.append(
                "Linhas inválidas: $invalidos\n\n"
            )

            jogos.forEachIndexed { indice, jogo ->
                exibicao.append(
                    "Jogo ${indice + 1}: "
                )
                exibicao.append(
                    jogo.joinToString(" ") {
                        "%02d".format(it)
                    }
                )
                exibicao.append("\n\n")
            }

            resultado.text = exibicao.toString()

        } catch (e: Exception) {
            resultado.text =
                "Erro ao importar: ${e.message}"
        }
    }
}

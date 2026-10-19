
package com.meuimportador.lotofacil

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.Typeface
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var resultado: TextView
    private lateinit var listaJogos: LinearLayout
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

        val importar = Button(this).apply {
            text = "IMPORTAR JOGOS"
            setOnClickListener {
                val intent = Intent(
                    Intent.ACTION_OPEN_DOCUMENT
                ).apply {
                    type = "text/plain"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(
                    intent, codigoImportacao
                )
            }
        }
        tela.addView(importar)

        val abrirSite = Button(this).apply {
            text = "ABRIR LOTERIAS CAIXA"
            setOnClickListener {
                try {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://www.loteriasonline.caixa.gov.br/"
                        )
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "Não foi possível abrir o site",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        tela.addView(abrirSite)

        resultado = TextView(this).apply {
            text = "Selecione um arquivo TXT com seus jogos."
            textSize = 17f
        }
        tela.addView(resultado)

        val rolagem = ScrollView(this)
        listaJogos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rolagem.addView(listaJogos)

        tela.addView(
            rolagem,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(tela)
    }

    private fun mostrarJogos(invalidos: Int) {
        resultado.text =
            "Jogos válidos: ${jogos.size}\n" +
            "Linhas inválidas: $invalidos\n"

        listaJogos.removeAllViews()

        jogos.forEachIndexed { indice, jogo ->

            val dezenas = jogo.joinToString(" ") {
                "%02d".format(it)
            }

            val textoJogo = TextView(this).apply {
                text = "\nJogo ${indice + 1}:\n$dezenas"
                textSize = 19f
                setTextIsSelectable(true)
            }
            listaJogos.addView(textoJogo)

            val copiar = Button(this).apply {
                text = "COPIAR JOGO ${indice + 1}"
                setOnClickListener {
                    val clipboard = getSystemService(
                        Context.CLIPBOARD_SERVICE
                    ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "Jogo ${indice + 1}",
                            dezenas
                        )
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Jogo ${indice + 1} copiado!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            listaJogos.addView(copiar)
        }
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
            resultCode != RESULT_OK
        ) return

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
                    .map { it.value.toIntOrNull() }
                    .toList()

                if (numeros.isEmpty()) {
                    return@forEach
                }

                if (numeros.size == 15 &&
                    numeros.all { it != null && it in 1..25 } &&
                    numeros.filterNotNull().distinct().size == 15
                ) {
                    jogos.add(
                        numeros.filterNotNull().sorted()
                    )
                } else {
                    invalidos++
                }
            }

            mostrarJogos(invalidos)

        } catch (e: Exception) {
            resultado.text =
                "Erro ao importar: ${e.message}"
        }
    }
}

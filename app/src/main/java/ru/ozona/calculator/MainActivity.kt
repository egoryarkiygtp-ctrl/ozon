package ru.ozona.calculator

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.util.Locale

class MainActivity : Activity() {
    private val fields = linkedMapOf<String, EditText>()
    private lateinit var schemeSpinner: Spinner
    private lateinit var output: TextView
    private var lastInput: CalculationInput? = null
    private var lastResult: CalculationResult? = null
    private var lastTariff: Tariff = Calculator.fbo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
            setBackgroundColor(Color.rgb(247, 248, 252))
        }

        root.addView(TextView(this).apply {
            text = "Ozona"
            textSize = 30f
            setTextColor(Color.rgb(0, 91, 255))
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        })
        root.addView(TextView(this).apply {
            text = "Калькулятор юнит-экономики Ozon"
            textSize = 16f
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(16))
        })

        root.addView(label("Схема работы"))
        schemeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("FBO", "FBS")
            )
        }
        root.addView(schemeSpinner, fullWidth(dp(50)))

        addField(root, "price", "Цена продажи, ₽", "1990")
        addField(root, "cost", "Себестоимость, ₽", "740")
        addField(root, "length", "Длина, см", "20")
        addField(root, "width", "Ширина, см", "15")
        addField(root, "height", "Высота, см", "8")
        addField(root, "weight", "Вес, кг", "0.5")
        addField(root, "buyout", "Процент выкупа", "90")
        addField(root, "tax", "Налог, %", "6")
        addField(root, "packaging", "Упаковка, ₽", "0")
        addField(root, "advertising", "Реклама на заказ, ₽", "0")
        addField(root, "other", "Прочие расходы, ₽", "0")

        root.addView(Button(this).apply {
            text = "Рассчитать"
            setOnClickListener { calculateAndShow() }
        }, fullWidth(dp(56)))

        output = TextView(this).apply {
            text = "Введите данные и нажмите «Рассчитать»"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(dp(12), dp(18), dp(12), dp(18))
            setBackgroundColor(Color.WHITE)
        }
        root.addView(output, fullWidth(LinearLayout.LayoutParams.WRAP_CONTENT))

        val exportRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        exportRow.addView(Button(this).apply {
            text = "CSV / Excel"
            setOnClickListener { exportCsv() }
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        exportRow.addView(Button(this).apply {
            text = "PDF"
            setOnClickListener { exportPdf() }
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        root.addView(exportRow)

        root.addView(TextView(this).apply {
            text = "Важно: стартовые тарифы демонстрационные. Перед коммерческим использованием их нужно сверить с действующими тарифами Ozon."
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(12), 0, 0)
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun addField(root: LinearLayout, key: String, title: String, defaultValue: String) {
        root.addView(label(title))
        val edit = EditText(this).apply {
            setText(defaultValue)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(12), 0, dp(12), 0)
        }
        fields[key] = edit
        root.addView(edit, fullWidth(dp(50)))
    }

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue
        textSize = 14f
        setTextColor(Color.DKGRAY)
        setPadding(0, dp(10), 0, dp(4))
    }

    private fun number(key: String): Double =
        fields.getValue(key).text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

    private fun calculateAndShow() {
        val input = CalculationInput(
            price = number("price"),
            cost = number("cost"),
            lengthCm = number("length"),
            widthCm = number("width"),
            heightCm = number("height"),
            weightKg = number("weight"),
            buyoutPercent = number("buyout"),
            taxPercent = number("tax"),
            packaging = number("packaging"),
            advertising = number("advertising"),
            other = number("other")
        )
        val tariff = if (schemeSpinner.selectedItem.toString() == "FBS") Calculator.fbs else Calculator.fbo
        val r = Calculator.calculate(input, tariff)
        lastInput = input
        lastTariff = tariff
        lastResult = r

        output.text = buildString {
            appendLine("Результат")
            appendLine()
            appendLine("Ожидаемая выручка: ${money(r.revenue)}")
            appendLine("Комиссия Ozon: ${money(r.commission)}")
            appendLine("Логистика: ${money(r.logistics)}")
            appendLine("Последняя миля: ${money(r.lastMile)}")
            appendLine("Обработка: ${money(r.processing)}")
            appendLine("Возвратная логистика: ${money(r.returns)}")
            appendLine("Налог: ${money(r.tax)}")
            appendLine("Всего расходов: ${money(r.totalExpenses)}")
            appendLine()
            appendLine("Прибыль на заказ: ${money(r.profitPerOrder)}")
            appendLine("Прибыль на выкупленную единицу: ${money(r.profitPerBoughtUnit)}")
            appendLine("Маржинальность: ${percent(r.marginPercent)}")
            appendLine("Рентабельность расходов: ${percent(r.roiPercent)}")
            appendLine("Цена безубыточности: ${money(r.breakEvenPrice)}")
            appendLine()
            appendLine("Профиль: ${tariff.name}")
        }
    }

    private fun exportCsv() {
        ensureResult()
        val input = lastInput ?: return
        val result = lastResult ?: return
        try {
            val file = Exporters.exportCsv(this, input, lastTariff, result)
            showExported(file)
        } catch (e: Exception) {
            errorDialog(e.message ?: "Не удалось создать CSV")
        }
    }

    private fun exportPdf() {
        ensureResult()
        val input = lastInput ?: return
        val result = lastResult ?: return
        try {
            val file = Exporters.exportPdf(this, input, lastTariff, result)
            showExported(file)
        } catch (e: Exception) {
            errorDialog(e.message ?: "Не удалось создать PDF")
        }
    }

    private fun ensureResult() {
        if (lastResult == null) calculateAndShow()
    }

    private fun showExported(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Экспорт завершён")
            .setMessage("Файл сохранён:\n${file.absolutePath}")
            .setPositiveButton("ОК", null)
            .show()
    }

    private fun errorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Ошибка")
            .setMessage(message)
            .setPositiveButton("ОК", null)
            .show()
    }

    private fun money(value: Double) = String.format(Locale("ru", "RU"), "%.2f ₽", value)
    private fun percent(value: Double) = String.format(Locale("ru", "RU"), "%.2f %%", value)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun fullWidth(height: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height).apply {
            bottomMargin = dp(6)
        }
}

package ru.ozona.calculator

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporters {
    private fun rows(input: CalculationInput, tariff: Tariff, r: CalculationResult): List<Pair<String, String>> =
        listOf(
            "Тарифный профиль" to tariff.name,
            "Цена продажи" to "%.2f ₽".format(russianLocale(), input.price),
            "Себестоимость" to "%.2f ₽".format(russianLocale(), input.cost),
            "Выкуп" to "%.2f %%".format(russianLocale(), input.buyoutPercent),
            "Налог" to "%.2f %%".format(russianLocale(), input.taxPercent),
            "Ожидаемая выручка" to "%.2f ₽".format(russianLocale(), r.revenue),
            "Комиссия Ozon" to "%.2f ₽".format(russianLocale(), r.commission),
            "Налог" to "%.2f ₽".format(russianLocale(), r.tax),
            "Логистика" to "%.2f ₽".format(russianLocale(), r.logistics),
            "Обработка" to "%.2f ₽".format(russianLocale(), r.processing),
            "Последняя миля" to "%.2f ₽".format(russianLocale(), r.lastMile),
            "Возвратная логистика" to "%.2f ₽".format(russianLocale(), r.returns),
            "Всего расходов" to "%.2f ₽".format(russianLocale(), r.totalExpenses),
            "Прибыль на заказ" to "%.2f ₽".format(russianLocale(), r.profitPerOrder),
            "Прибыль на выкупленную единицу" to "%.2f ₽".format(russianLocale(), r.profitPerBoughtUnit),
            "Маржинальность" to "%.2f %%".format(russianLocale(), r.marginPercent),
            "Рентабельность расходов" to "%.2f %%".format(russianLocale(), r.roiPercent),
            "Цена безубыточности" to "%.2f ₽".format(russianLocale(), r.breakEvenPrice)
        )

    private fun russianLocale() = Locale("ru", "RU")

    private fun exportDir(context: Context): File =
        File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    fun exportCsv(context: Context, input: CalculationInput, tariff: Tariff, r: CalculationResult): File {
        val file = File(exportDir(context), "ozona_${stamp()}.csv")
        file.outputStream().bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("\uFEFFПоказатель;Значение\n")
            rows(input, tariff, r).forEach { (label, value) ->
                writer.write("\"${label.replace("\"", "\"\"")}\";\"${value.replace("\"", "\"\"")}\"\n")
            }
        }
        return file
    }

    fun exportPdf(context: Context, input: CalculationInput, tariff: Tariff, r: CalculationResult): File {
        val file = File(exportDir(context), "ozona_${stamp()}.pdf")
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val paint = Paint().apply { textSize = 11f }
        var y = 45f
        canvas.drawText("Ozona — юнит-экономика", 35f, y, titlePaint)
        y += 30f
        rows(input, tariff, r).forEach { (label, value) ->
            canvas.drawText(label.take(48), 35f, y, paint)
            canvas.drawText(value, 390f, y, paint)
            y += 23f
        }
        document.finishPage(page)
        FileOutputStream(file).use(document::writeTo)
        document.close()
        return file
    }
}

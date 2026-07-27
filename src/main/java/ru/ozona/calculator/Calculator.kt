package ru.ozona.calculator

import kotlin.math.max
import kotlin.math.min

data class Tariff(
    val name: String,
    val commissionPercent: Double,
    val processingFee: Double,
    val logisticsBase: Double,
    val logisticsPerLiter: Double,
    val lastMilePercent: Double,
    val lastMileMin: Double,
    val lastMileMax: Double,
    val returnFee: Double
)

data class CalculationInput(
    val price: Double,
    val cost: Double,
    val lengthCm: Double,
    val widthCm: Double,
    val heightCm: Double,
    val weightKg: Double,
    val buyoutPercent: Double,
    val taxPercent: Double,
    val packaging: Double,
    val advertising: Double,
    val other: Double
)

data class CalculationResult(
    val volumeLiters: Double,
    val revenue: Double,
    val commission: Double,
    val tax: Double,
    val logistics: Double,
    val processing: Double,
    val lastMile: Double,
    val returns: Double,
    val productCost: Double,
    val totalExpenses: Double,
    val profitPerOrder: Double,
    val profitPerBoughtUnit: Double,
    val marginPercent: Double,
    val roiPercent: Double,
    val breakEvenPrice: Double
)

object Calculator {
    val fbo = Tariff(
        name = "FBO — стартовый профиль",
        commissionPercent = 20.0,
        processingFee = 0.0,
        logisticsBase = 75.0,
        logisticsPerLiter = 12.0,
        lastMilePercent = 5.5,
        lastMileMin = 20.0,
        lastMileMax = 500.0,
        returnFee = 50.0
    )

    val fbs = Tariff(
        name = "FBS — стартовый профиль",
        commissionPercent = 20.0,
        processingFee = 30.0,
        logisticsBase = 85.0,
        logisticsPerLiter = 12.0,
        lastMilePercent = 5.5,
        lastMileMin = 20.0,
        lastMileMax = 500.0,
        returnFee = 60.0
    )

    fun calculate(input: CalculationInput, tariff: Tariff): CalculationResult {
        val p = (input.buyoutPercent / 100.0).coerceIn(0.0, 1.0)
        val volume = max(0.0, input.lengthCm * input.widthCm * input.heightCm / 1000.0)
        val chargeableVolume = max(volume, max(0.0, input.weightKg) * 5.0)

        val revenue = p * input.price
        val commission = p * input.price * tariff.commissionPercent / 100.0
        val tax = p * input.price * input.taxPercent / 100.0
        val logistics = tariff.logisticsBase + chargeableVolume * tariff.logisticsPerLiter
        val processing = tariff.processingFee
        val lastMileAtBuyout = (input.price * tariff.lastMilePercent / 100.0)
            .coerceIn(tariff.lastMileMin, tariff.lastMileMax)
        val lastMile = p * lastMileAtBuyout
        val returns = (1.0 - p) * tariff.returnFee
        val productCost = p * input.cost

        val total = productCost + commission + tax + logistics + processing +
            lastMile + returns + input.packaging + input.advertising + input.other
        val profitOrder = revenue - total
        val profitBought = if (p > 0) profitOrder / p else 0.0
        val margin = if (input.price > 0) profitBought / input.price * 100.0 else 0.0
        val roi = if (total > 0) profitOrder / total * 100.0 else 0.0

        val variableRate = p * (
            1.0 -
                tariff.commissionPercent / 100.0 -
                input.taxPercent / 100.0 -
                tariff.lastMilePercent / 100.0
            )
        val fixed = productCost + logistics + processing + returns +
            input.packaging + input.advertising + input.other
        val breakEven = if (variableRate > 0) fixed / variableRate else 0.0

        return CalculationResult(
            volumeLiters = volume,
            revenue = revenue,
            commission = commission,
            tax = tax,
            logistics = logistics,
            processing = processing,
            lastMile = lastMile,
            returns = returns,
            productCost = productCost,
            totalExpenses = total,
            profitPerOrder = profitOrder,
            profitPerBoughtUnit = profitBought,
            marginPercent = margin,
            roiPercent = roi,
            breakEvenPrice = breakEven
        )
    }
}

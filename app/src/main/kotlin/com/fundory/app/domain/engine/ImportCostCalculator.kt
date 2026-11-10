package com.fundory.app.domain.engine

import com.fundory.app.domain.model.ImportCalculationConfig
import com.fundory.app.domain.model.ImportCalculationResult
import java.math.BigDecimal
import java.math.RoundingMode

class ImportCostCalculator {

    fun calculate(
        productsSubtotalGel: BigDecimal,
        config: ImportCalculationConfig,
        ratesToGel: Map<String, BigDecimal>
    ): ImportCalculationResult {
        // 1. Calculate shipping total in GEL
        val shippingRate = ratesToGel[config.shippingCurrency.uppercase()] ?: BigDecimal.ONE
        val weightRateAmount = config.ratePerKg.multiply(BigDecimal.valueOf(config.weightKg))
        val rawShippingForeign = weightRateAmount.add(config.fixedShippingFee)
        val shippingTotalGel = rawShippingForeign.multiply(shippingRate).setScale(2, RoundingMode.HALF_UP)

        // 2. Transaction fees (percentage of products subtotal)
        val txFeeRate = BigDecimal.valueOf(config.transactionFeePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        val transactionFeesGel = productsSubtotalGel.multiply(txFeeRate).setScale(2, RoundingMode.HALF_UP)

        // 3. Currency conversion fee (percentage of products subtotal + shipping)
        val fxFeeRate = BigDecimal.valueOf(config.currencyConversionFeePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
        val currencyConversionFeesGel = productsSubtotalGel.add(shippingTotalGel)
            .multiply(fxFeeRate).setScale(2, RoundingMode.HALF_UP)

        // 4. Customs / Import Taxes (Georgian Customs rule: items + shipping > threshold)
        val taxableBase = productsSubtotalGel.add(shippingTotalGel)
        val isTaxable = config.isGeorgianImportTaxMode &&
                !config.isCustomsExempt &&
                (taxableBase > config.customsThresholdGel)

        val estimatedVatGel: BigDecimal
        val customsDeclarationFeeGel: BigDecimal

        if (isTaxable) {
            val vatRate = BigDecimal.valueOf(config.vatRatePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            estimatedVatGel = taxableBase.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
            customsDeclarationFeeGel = config.customsDeclarationFeeGel.setScale(2, RoundingMode.HALF_UP)
        } else {
            estimatedVatGel = BigDecimal.ZERO
            customsDeclarationFeeGel = BigDecimal.ZERO
        }

        val totalTaxesAndFeesGel = transactionFeesGel
            .add(currencyConversionFeesGel)
            .add(estimatedVatGel)
            .add(customsDeclarationFeeGel)
            .setScale(2, RoundingMode.HALF_UP)

        val grandTotalGel = productsSubtotalGel
            .add(shippingTotalGel)
            .add(totalTaxesAndFeesGel)
            .setScale(2, RoundingMode.HALF_UP)

        val explanation = buildString {
            append("Products: ${productsSubtotalGel} GEL. ")
            append("Shipping (${config.weightKg} kg via ${config.shippingMethod}): ${shippingTotalGel} GEL. ")
            if (isTaxable) {
                append("Import taxable because CIF total (${taxableBase} GEL) exceeds ${config.customsThresholdGel} GEL. ")
                append("18% VAT = ${estimatedVatGel} GEL + ${customsDeclarationFeeGel} GEL customs declaration fee.")
            } else if (config.isGeorgianImportTaxMode) {
                append("CIF total (${taxableBase} GEL) is within the tax-free limit (${config.customsThresholdGel} GEL). No import VAT.")
            }
        }

        return ImportCalculationResult(
            productsSubtotalGel = productsSubtotalGel,
            shippingTotalGel = shippingTotalGel,
            transactionFeesGel = transactionFeesGel,
            currencyConversionFeesGel = currencyConversionFeesGel,
            isTaxableUnderCustoms = isTaxable,
            estimatedVatGel = estimatedVatGel,
            customsDeclarationFeeGel = customsDeclarationFeeGel,
            totalTaxesAndFeesGel = totalTaxesAndFeesGel,
            grandTotalGel = grandTotalGel,
            breakdownExplanation = explanation
        )
    }
}


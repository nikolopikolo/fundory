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
        val shippingTotalGel: BigDecimal
        if (config.isLocalPurchase) {
            shippingTotalGel = config.fixedShippingFee.setScale(2, RoundingMode.HALF_UP)
        } else {
            val shippingRate = ratesToGel[config.shippingCurrency.uppercase()] ?: BigDecimal.ONE
            val weightRateAmount = config.ratePerKg.multiply(BigDecimal.valueOf(config.weightKg))
            val rawShippingForeign = weightRateAmount.add(config.fixedShippingFee)
            shippingTotalGel = rawShippingForeign.multiply(shippingRate).setScale(2, RoundingMode.HALF_UP)
        }

        // 2. Transaction fees (only if card fee toggle is enabled or percent > 0)
        val transactionFeesGel = if ((config.applyCardTransactionFee || config.transactionFeePercent > 0.0) && config.transactionFeePercent > 0.0) {
            val txFeeRate = BigDecimal.valueOf(config.transactionFeePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            productsSubtotalGel.multiply(txFeeRate).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        // 3. Currency conversion fee (only for international & if enabled)
        val currencyConversionFeesGel = if (!config.isLocalPurchase && (config.applyCurrencyConversionFee || config.currencyConversionFeePercent > 0.0) && config.currencyConversionFeePercent > 0.0) {
            val fxFeeRate = BigDecimal.valueOf(config.currencyConversionFeePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            productsSubtotalGel.add(shippingTotalGel)
                .multiply(fxFeeRate).setScale(2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
        }

        // 4. Customs / Import Taxes (only for international purchases when toggle is enabled)
        val taxableBase = productsSubtotalGel.add(shippingTotalGel)
        val isTaxable = !config.isLocalPurchase &&
                config.isGeorgianImportTaxMode &&
                config.applyImportVatAndCustoms &&
                !config.isCustomsExempt &&
                (taxableBase > config.customsThresholdGel)

        val estimatedVatGel: BigDecimal
        val customsDeclarationFeeGel: BigDecimal

        if (isTaxable) {
            val vatRate = BigDecimal.valueOf(config.vatRatePercent).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
            estimatedVatGel = taxableBase.multiply(vatRate).setScale(2, RoundingMode.HALF_UP)
            customsDeclarationFeeGel = config.customsDeclarationFeeGel.setScale(2, RoundingMode.HALF_UP)
        } else {
            estimatedVatGel = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            customsDeclarationFeeGel = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
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
            if (config.isLocalPurchase) {
                append("Local Purchase in Georgia. ")
                append("Products: ${productsSubtotalGel} GEL. ")
                if (shippingTotalGel > BigDecimal.ZERO) append("Local delivery: ${shippingTotalGel} GEL. ")
                if (transactionFeesGel > BigDecimal.ZERO) append("Payment fee: ${transactionFeesGel} GEL. ")
                append("No customs duties or international shipping.")
            } else {
                append("Products: ${productsSubtotalGel} GEL. ")
                if (shippingTotalGel > BigDecimal.ZERO) {
                    append("Shipping: ${shippingTotalGel} GEL. ")
                }
                if (isTaxable) {
                    append("Import taxable: declared value (${taxableBase} GEL) exceeds ${config.customsThresholdGel} GEL. ")
                    append("18% VAT = ${estimatedVatGel} GEL + ${customsDeclarationFeeGel} GEL customs declaration fee.")
                } else if (!config.applyImportVatAndCustoms) {
                    append("Import VAT and customs fees excluded by user preference.")
                } else {
                    append("CIF total (${taxableBase} GEL) is within the tax-free limit (${config.customsThresholdGel} GEL). No import VAT.")
                }
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

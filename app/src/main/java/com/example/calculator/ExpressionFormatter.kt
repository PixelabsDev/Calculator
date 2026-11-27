package com.example.calculator

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Formats arithmetic expressions with standard thousands comma grouping on the display layer
 * (e.g. "1000×1000" -> "1,000×1,000") while keeping the underlying raw string untouched
 * for [ExpressionParser] and maintaining exact bidirectional cursor offset mapping.
 */
object ExpressionFormatter {

    data class FormattedMapping(
        val formattedText: String,
        val origToTrans: IntArray,
        val transToOrig: IntArray
    )

    fun formatForDisplay(raw: String): String {
        if (raw.isEmpty()) return raw
        return formatWithMapping(raw).formattedText
    }

    /**
     * Formats evaluated preview/result strings while skipping prose error messages
     * (e.g. "Division by zero", "tan(90°) is undefined").
     */
    fun formatResultForDisplay(rawResult: String): String {
        if (rawResult.isEmpty()) return rawResult
        if (rawResult.any { it.isLetter() && it != 'e' && it != 'E' }) {
            return rawResult
        }
        return formatForDisplay(rawResult)
    }

    fun formatWithMapping(raw: String): FormattedMapping {
        val len = raw.length
        if (len == 0) {
            return FormattedMapping("", intArrayOf(0), intArrayOf(0))
        }

        val out = StringBuilder(len + len / 3)
        val origToTrans = IntArray(len + 1)
        val transToOrigList = ArrayList<Int>(len + len / 3 + 1)

        var i = 0
        while (i < len) {
            val ch = raw[i]
            if (ch.isDigit()) {
                var runEnd = i
                while (runEnd < len && raw[runEnd].isDigit()) {
                    runEnd++
                }
                val runLen = runEnd - i
                val isFractional = i > 0 && raw[i - 1] == '.'

                if (isFractional || runLen <= 3) {
                    for (k in i until runEnd) {
                        origToTrans[k] = out.length
                        transToOrigList.add(k)
                        out.append(raw[k])
                    }
                } else {
                    for (k in i until runEnd) {
                        origToTrans[k] = out.length
                        transToOrigList.add(k)
                        out.append(raw[k])

                        val remaining = runEnd - 1 - k
                        if (remaining > 0 && remaining % 3 == 0) {
                            transToOrigList.add(k + 1)
                            out.append(',')
                        }
                    }
                }
                i = runEnd
            } else {
                origToTrans[i] = out.length
                transToOrigList.add(i)
                out.append(ch)
                i++
            }
        }

        origToTrans[len] = out.length
        transToOrigList.add(len)

        return FormattedMapping(
            formattedText = out.toString(),
            origToTrans = origToTrans,
            transToOrig = transToOrigList.toIntArray()
        )
    }
}

class ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val mappingResult = ExpressionFormatter.formatWithMapping(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, raw.length)
                return mappingResult.origToTrans[safe]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, mappingResult.formattedText.length)
                return mappingResult.transToOrig[safe]
            }
        }

        return TransformedText(
            text = AnnotatedString(mappingResult.formattedText),
            offsetMapping = offsetMapping
        )
    }
}

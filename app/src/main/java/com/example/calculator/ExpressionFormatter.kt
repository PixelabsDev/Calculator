package com.example.calculator

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
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

    /**
     * Returns ascending raw character indices where thousands separator commas should be inserted.
     * Skips fractional decimal runs and numbers with 3 or fewer digits.
     */
    fun findCommaInsertionPositions(raw: CharSequence): List<Int> {
        val len = raw.length
        if (len <= 3) return emptyList()

        val positions = ArrayList<Int>(len / 3)
        var i = 0
        while (i < len) {
            if (raw[i].isDigit()) {
                var runEnd = i
                while (runEnd < len && raw[runEnd].isDigit()) {
                    runEnd++
                }
                val runLen = runEnd - i
                val isFractional = i > 0 && raw[i - 1] == '.'

                if (!isFractional && runLen > 3) {
                    for (k in i until runEnd) {
                        val remaining = runEnd - 1 - k
                        if (remaining > 0 && remaining % 3 == 0) {
                            positions.add(k + 1)
                        }
                    }
                }
                i = runEnd
            } else {
                i++
            }
        }
        return positions
    }

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

        val commaPositions = findCommaInsertionPositions(raw)
        val out = StringBuilder(len + commaPositions.size)
        val origToTrans = IntArray(len + 1)
        val transToOrigList = ArrayList<Int>(len + commaPositions.size + 1)
        var commaIdx = 0

        for (i in 0 until len) {
            origToTrans[i] = out.length
            transToOrigList.add(i)
            out.append(raw[i])

            val boundary = i + 1
            while (commaIdx < commaPositions.size && commaPositions[commaIdx] == boundary) {
                transToOrigList.add(boundary)
                out.append(',')
                commaIdx++
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

/**
 * Resolves graceful single-line font downscaling and far-edge tap snapping coordinates.
 */
object DisplayTypographyScaler {
    val PORTRAIT_INPUT_SIZES_SP = floatArrayOf(56f, 44f, 36f, 28f)
    val PORTRAIT_PREVIEW_SIZES_SP = floatArrayOf(32f, 26f, 22f, 18f)

    val LANDSCAPE_INPUT_SIZES_SP = floatArrayOf(34f, 28f, 23f, 18f)
    val LANDSCAPE_PREVIEW_SIZES_SP = floatArrayOf(22f, 18f, 15f, 13f)

    /**
     * Picks the largest size in [tiersSp] (descending) whose measured single-line pixel width
     * fits inside [maxWidthPx], flooring at the final tier so horizontal scrolling stays legible.
     */
    fun selectFittingFontSizeSp(
        tiersSp: FloatArray,
        maxWidthPx: Float,
        measureWidthPx: (fontSizeSp: Float) -> Float
    ): Float {
        if (tiersSp.isEmpty()) return 28f
        if (maxWidthPx <= 0f) return tiersSp.first()
        for (tierSp in tiersSp) {
            if (measureWidthPx(tierSp) <= maxWidthPx) {
                return tierSp
            }
        }
        return tiersSp.last()
    }

    /**
     * Determines whether a single quick tap landed on the far-left or far-right edge zone
     * (outside active digits) so the cursor snaps cleanly to 0 or [textLength] without locking a handle.
     * Returns null when tapping interior digits cleanly.
     */
    fun resolveEdgeTapCursorIndex(
        tapX: Float,
        fieldWidthPx: Float,
        edgeZonePx: Float,
        lineLeftPx: Float?,
        lineRightPx: Float?,
        textLength: Int
    ): Int? {
        if (textLength <= 0) return 0

        val glyphMargin = edgeZonePx * 0.25f
        val hitLeftEdge = tapX <= edgeZonePx ||
            (lineLeftPx != null && lineLeftPx > edgeZonePx && tapX < lineLeftPx - glyphMargin)
        if (hitLeftEdge) return 0

        val rightBoundary = if (lineRightPx != null) {
            minOf(fieldWidthPx, lineRightPx + glyphMargin)
        } else {
            fieldWidthPx - edgeZonePx
        }
        val hitRightEdge = tapX >= fieldWidthPx - edgeZonePx * 0.35f ||
            (lineRightPx != null && tapX >= lineRightPx + glyphMargin) ||
            (lineRightPx == null && tapX >= rightBoundary)
        if (hitRightEdge) return textLength

        return null
    }
}

/**
 * OutputTransformation for Compose Foundation `BasicTextField(state = TextFieldState)`
 * that renders thousands commas cleanly while preserving native cursor drag & selection offsets.
 */
class ThousandsOutputTransformation : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        val positions = ExpressionFormatter.findCommaInsertionPositions(asCharSequence())
        for (i in positions.indices.reversed()) {
            val pos = positions[i]
            replace(pos, pos, ",")
        }
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

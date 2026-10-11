package com.example.mediscannerai.domain.usecase

import com.example.mediscannerai.data.remote.GroqExplanationService
import com.example.mediscannerai.domain.model.ParsedReport

class GenerateAiExplanationUseCase(
    private val service: GroqExplanationService = GroqExplanationService()
) {
    suspend operator fun invoke(report: ParsedReport): String {
        return service.generateExplanation(
            systemInstruction = SYSTEM_INSTRUCTION,
            userPrompt = buildUserPrompt(report)
        )
    }

    private fun buildUserPrompt(report: ParsedReport): String {
        val checked = report.results.map { it to RangeCheck.evaluate(it.value, it.referenceRange, it.flag) }

        val structuredSection = if (checked.isNotEmpty()) {
            val outside = checked.count {
                it.second == RangeStatus.ABOVE || it.second == RangeStatus.BELOW
            }
            val within = checked.count { it.second == RangeStatus.WITHIN }
            val unknown = checked.count { it.second == RangeStatus.UNKNOWN }
            buildString {
                appendLine(
                    "Values the app found. The app has already compared each one with the range " +
                            "printed on the report. Do not redo the comparison:"
                )
                checked.forEach { (r, status) ->
                    append("- ${r.testName}: value ${r.value ?: "?"}")
                    r.unit?.let { append(" $it") }
                    append("; printed range: ${r.referenceRange ?: "not found"}")
                    append("; status: ${status.name}")
                    appendLine()
                }
                appendLine(
                    "Totals: $outside outside the printed range, $within within it, " +
                            "$unknown could not be compared."
                )
            }
        } else {
            "The app could not identify individual test values from this report's layout. " +
                    "Read the raw text carefully and take extra care not to guess."
        }

        // Personal details are removed before the text leaves the phone.
        val cleanedText = ReportTextRedactor.redact(report.rawText)

        return """
            Here is the text read from a user's lab report by OCR. It may contain reading
            errors (misread characters, garbled layout). If something looks unclear, say so
            instead of guessing.

            $structuredSection

            Full raw OCR text:
            ---
            $cleanedText
            ---

            Please write the explanation following your instructions exactly.
        """.trimIndent()
    }

    private companion object {
        const val SYSTEM_INSTRUCTION = """
You are an educational assistant inside a lab report app called MediScanner AI. You help a person
with no medical background understand what is written on their lab report. You are not a doctor.

STRICT RULES. Never break these:
1. Never diagnose, and never name a disease or condition the person might have. Never say or
   imply "you have X".
2. Never suggest or comment on any medicine, supplement, diet, food, exercise, lifestyle change,
   treatment, or extra test. Explaining what a value measures is allowed. Suggesting what to do
   about it is not.
3. Never give a conclusion about the person's health. Do not call them healthy, unwell, fine,
   concerning, worrying, serious or normal. Do not comment on their age or sex. Use only the
   phrases "within the range printed on the report" and "outside the range printed on the report".
4. Do not list or guess possible causes. For a value outside the printed range, say only that
   there can be several reasons and that a doctor can look at it together with the full picture.
5. The app has already compared each value with the printed range (the status in the data).
   Trust it and do not redo it. If a status is UNKNOWN, say the range could not be read and do
   not guess.
6. Use only tests, values, ranges and units that appear in the data or the text. Never invent
   any. If the text looks garbled or a value looks physically implausible, say so plainly.
7. Write in simple everyday words. Every medical term or abbreviation (for example RDW, MCV,
   bilirubin, ALT) must be explained in everyday words in the same sentence where it first
   appears. Group related tests together.
8. Keep the whole answer under about 280 words.

Write exactly these three sections. Put each header on its own line in bold Markdown, as shown.

**From your report**
Facts only. One short line per test that is outside the printed range, written like
"- Test name: your value, printed range, above or below". If no test is outside the range, say
so in one sentence. Then one sentence about how many other tests were within the range.

**What these tests mean**
Short plain paragraphs, only for the tests outside the printed range. If many tests are
outside, cover the two or three most important groups. If every test is within the range, explain
the one or two main tests instead. Say what each test measures and why doctors look at it, in
everyday words. No causes and no advice.

**Talk to your doctor**
One short, warm sentence encouraging the person to discuss this report with a doctor.
"""
    }
}
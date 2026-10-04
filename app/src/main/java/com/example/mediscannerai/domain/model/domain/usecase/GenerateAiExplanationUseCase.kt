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
        val structuredSection = if (report.results.isNotEmpty()) {
            buildString {
                appendLine("Values already shown to the user elsewhere in the app (for your context only — do not repeat this list back to them):")
                report.results.forEach { r ->
                    append("- ${r.testName}: ${r.value ?: "?"}")
                    r.unit?.let { append(" $it") }
                    r.referenceRange?.let { append(" (reference range: $it)") }
                    r.flag?.let { append(" [flag: $it]") }
                    appendLine()
                }
            }
        } else {
            "No individual test values could be automatically identified from this report's layout."
        }

        // Personal details are removed before the text leaves the phone.
        val cleanedText = ReportTextRedactor.redact(report.rawText)

        return """
            Here is the raw text extracted from a user's lab report via OCR. It may
            contain OCR errors (misread characters, garbled formatting) — use your
            judgement about what looks like a genuine value versus a likely OCR error,
            and note if something looks unclear rather than guessing.

            $structuredSection

            Full raw OCR text:
            ---
            $cleanedText
            ---

            Please explain this report to the user following your instructions exactly.
        """.trimIndent()
    }

    private companion object {
        const val SYSTEM_INSTRUCTION = """
You are an educational assistant inside a medical lab report app called MediScanner AI.
Your job is to help a layperson with NO medical background genuinely understand what
their lab report means — not to summarize it back to them as another table.

The user has already seen every raw value, unit, and reference range elsewhere in the
app (in a separate structured card view). Your job is different: EXPLAIN, don't relist.

STRICT RULES — never break these:
1. NEVER diagnose a disease or medical condition. Do not say or imply "you have X."
2. NEVER recommend, suggest, start, stop, or change any medication, diet, food, supplement,
   exercise, home remedy, or any other treatment or lifestyle intervention — even something
   that sounds harmless like "eat more iron-rich foods." Explaining what a value means is
   allowed; suggesting what to do about it is not, no matter how the suggestion is framed.
3. NEVER give a definitive medical conclusion about the user's health.
4. If a value is outside the reference range, mention this factually and briefly, then
   move on to explaining what the test itself measures and why it matters in general —
   do not dwell on listing every off-range number one by one.
5. If OCR text looks garbled or a value seems physically implausible, say so plainly
   rather than guessing a "corrected" number.
6. Do not fabricate any test, value, or range that is not present in the provided text.
7. Write in flowing conversational paragraphs, the way you'd actually talk out loud to
   a friend over chai — NOT as a bulleted list of findings. Use a bullet list only when
   grouping 4+ closely related, similarly-behaving items together as a single list item
   each (e.g. naming three normal white cell types in one sentence), never one bullet
   per individual test result. Most of your response should read as ordinary prose.
8. Every single medical term or abbreviation you use (RDW, MCV, bilirubin, AST, ALT,
   GGT, lymphocytes, platelets — all of them, no exceptions) must be explained in the
   very same sentence it first appears in, in everyday words, before you use it again.
   If you can't explain a term simply, don't use the term — describe the idea instead.
   Group related tests together (e.g. explain "red blood cell health" as one idea
   covering Haemoglobin, RBC count, and PCV together) rather than going test-by-test.
9. Keep your entire response under about 300 words. Prioritize the 2-3 groups of
   results most worth understanding; a group where every value is normal can be
   covered in one brief, reassuring sentence rather than a full paragraph.
10. Structure your response in exactly two sections, using these exact headers:
    - "What Your Report Is Telling You" — the plain-language explanation described above.
    - "Talk to Your Doctor" — ONE short, warm sentence encouraging the user to bring this
      report to a healthcare professional. Do NOT list specific questions here — a
      separate part of the app generates a detailed question list for the user.
11. Do not use raw Markdown table syntax. Avoid repeating numeric values/ranges verbatim
    — describe them in words (e.g. "noticeably lower than typical" rather than restating
    the number) unless a specific number is essential to explain a concept.
"""
    }
}
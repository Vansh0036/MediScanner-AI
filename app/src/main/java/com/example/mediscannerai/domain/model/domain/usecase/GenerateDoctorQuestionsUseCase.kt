package com.example.mediscannerai.domain.usecase

import com.example.mediscannerai.data.remote.GroqExplanationService
import com.example.mediscannerai.domain.model.ParsedReport

class GenerateDoctorQuestionsUseCase(
    private val service: GroqExplanationService = GroqExplanationService()
) {
    suspend operator fun invoke(report: ParsedReport): List<String> {
        val responseText = service.generateExplanation(
            systemInstruction = SYSTEM_INSTRUCTION,
            userPrompt = buildUserPrompt(report)
        )
        return parseQuestions(responseText)
    }

    private fun buildUserPrompt(report: ParsedReport): String {
        val structuredSection = if (report.results.isNotEmpty()) {
            buildString {
                appendLine("Values identified from this report:")
                report.results.forEach { r ->
                    append("- ${r.testName}: ${r.value ?: "?"}")
                    r.unit?.let { append(" $it") }
                    r.referenceRange?.let { append(" (reference range: $it)") }
                    r.flag?.let { append(" [flag: $it]") }
                    appendLine()
                }
            }
        } else {
            "No individual test values could be automatically identified; base your questions on the raw text below."
        }

        // Personal details are removed before the text leaves the phone.
        val cleanedText = ReportTextRedactor.redact(report.rawText)

        return """
            Here is a user's lab report. Generate discussion questions for their doctor
            visit following your instructions exactly.

            $structuredSection

            Full raw OCR text:
            ---
            $cleanedText
            ---
        """.trimIndent()
    }

    /**
     * Expects one question per line, each starting with "- ". Falls back to a
     * generic safe question set if parsing finds nothing usable, so the UI
     * never shows an empty screen.
     */
    private fun parseQuestions(responseText: String): List<String> {
        val questions = responseText.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
            .filter { it.isNotBlank() }

        return questions.ifEmpty {
            listOf(
                "What does this result mean in my situation?",
                "Is this result significantly different from what's typically expected?",
                "Could anything recent have affected this result?",
                "Should I consider repeating this test?",
                "Are there other questions I should be asking about this report?"
            )
        }
    }

    private companion object {
        const val SYSTEM_INSTRUCTION = """
You generate a short list of discussion questions a patient can bring to their doctor
about their own lab report, for an educational app called MediScanner AI.

STRICT RULES — never break these:
1. Generate QUESTIONS ONLY — never statements, conclusions, advice, or recommendations.
   Every single line must grammatically be a question a patient could ask out loud.
2. NEVER embed a diagnosis, treatment suggestion, or implied conclusion inside a
   question. "Should I start taking iron supplements?" is NOT acceptable — that smuggles
   a treatment suggestion into question form. "What might explain this result?" is fine.
3. Base questions on the SPECIFIC values and flags in this report, not generic advice
   that could apply to any report. Reference the actual test names involved.
4. Write 5 to 8 questions total.
5. Keep each question short, plain, and in everyday language — something a person with
   no medical background would feel comfortable asking.
6. Do not fabricate any test or value not present in the provided report.
7. Output ONLY the questions, one per line, each starting with "- ". No headers, no
   introduction, no closing remarks, nothing else.
"""
    }
}
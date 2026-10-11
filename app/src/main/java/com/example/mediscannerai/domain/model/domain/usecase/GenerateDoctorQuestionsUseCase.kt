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
        val checked = report.results.map { it to RangeCheck.evaluate(it.value, it.referenceRange, it.flag) }

        val structuredSection = if (checked.isNotEmpty()) {
            buildString {
                appendLine("Values found in this report, already compared by the app with the printed range:")
                checked.forEach { (r, status) ->
                    append("- ${r.testName}: value ${r.value ?: "?"}")
                    r.unit?.let { append(" $it") }
                    append("; printed range: ${r.referenceRange ?: "not found"}")
                    append("; status: ${status.name}")
                    appendLine()
                }
            }
        } else {
            "No individual test values could be identified; base your questions on the raw text below."
        }

        // Personal details are removed before the text leaves the phone.
        val cleanedText = ReportTextRedactor.redact(report.rawText)

        return """
            Here is a user's lab report. Write discussion questions for their doctor visit,
            following your instructions exactly.

            $structuredSection

            Full raw OCR text:
            ---
            $cleanedText
            ---
        """.trimIndent()
    }

    /**
     * Expects one question per line, each starting with "- ". Drops anything that is not a
     * question or that drifts into diet, lifestyle, supplements or treatment, then tops the list
     * up with safe general questions so the screen is never empty or too short.
     */
    private fun parseQuestions(responseText: String): List<String> {
        val questions = responseText.lines()
            .map { it.trim() }
            .filter { it.startsWith("- ") }
            .map { it.removePrefix("- ").trim() }
            .filter { it.isNotBlank() && it.contains('?') }
            .filterNot { BLOCKED.containsMatchIn(it) }
            .distinct()
            .toMutableList()

        FALLBACK.forEach { question ->
            if (questions.size < 5 && question !in questions) questions.add(question)
        }
        return questions
    }

    private companion object {
        val BLOCKED = Regex(
            """(?i)\b(diet|dietary|lifestyle|supplement|supplements|exercise|treat|treatment|therapy|cure|should i (start|stop|take|avoid|eat))\b"""
        )

        val FALLBACK = listOf(
            "What do these results mean in my situation?",
            "How do these results compare with my earlier ones?",
            "Could anything recent have affected these results?",
            "Should we discuss repeating any of these tests?",
            "Are there other questions I should be asking about this report?"
        )

        const val SYSTEM_INSTRUCTION = """
You write a short list of discussion questions that a patient can bring to their doctor about
their own lab report, for an educational app called MediScanner AI.

STRICT RULES. Never break these:
1. Write QUESTIONS ONLY. Every line must be a question the patient could ask out loud. Never
   write statements, conclusions, advice or recommendations.
2. Never mention or hint at diet, food, lifestyle, exercise, supplements, medicines, treatment or
   therapy. Never name or suggest a disease or condition. Never assume a cause.
3. Never mention the patient's age or sex.
4. Base the questions on the SPECIFIC tests in this report. Start with the tests whose status is
   ABOVE or BELOW the printed range, using the test name and the value. Then add one or two
   general questions, for example about comparing with earlier results, anything that might have
   affected the result, or whether to repeat the test.
5. Write 5 to 8 questions. Keep each one short, plain and in everyday words.
6. Never invent a test or a value that is not in the report.
7. Output ONLY the questions, one per line, each starting with "- ". No headers, no introduction,
   no closing remarks, nothing else.
"""
    }
}
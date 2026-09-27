package com.example.mediscannerai.domain.model

data class TestResult(
    val testName: String,
    val value: String?,
    val unit: String?,
    val referenceRange: String?,
    val flag: String?
)

data class ParsedReport(
    val rawText: String,
    val results: List<TestResult>,
    val reportDate: String?
)


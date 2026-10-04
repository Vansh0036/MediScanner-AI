package com.example.mediscannerai.domain.usecase

/**
 * Removes personal details from report text before it is sent to the AI service.
 * It works on text patterns, so it is a best-effort filter, not a guarantee.
 * The user is shown the cleaned text before it is sent.
 */
object ReportTextRedactor {

    // "UHID: 208724", "Registration No : 0006813", "CIN-U85100PB2015PTC039242".
    // The value must contain a digit, so a label with no real number is left alone.
    private val idNumberRegex = Regex(
        """(?i)\b(UHID|Registration[ ]*No\.?|Reg\.?[ ]*No\.?|Permanent[ ]*No\.?|Patient[ ]*ID|Lab[ ]*No\.?|Sample[ ]*(?:ID|No\.?)|Barcode|MRN|CIN|Bill[ ]*No\.?|Invoice[ ]*No\.?|Accession[ ]*No\.?|Visit[ ]*(?:ID|No\.?)|Case[ ]*No\.?)\s*[:\-]?\s*(?=[A-Za-z0-9\-/]*\d)[A-Za-z0-9\-/]+"""
    )

    // "Patient Name : ...", "Ref. By : ...", "Approved By : ..." (value runs to
    // the end of the line or to the next column gap).
    private val personLabelRegex = Regex(
        """(?im)\b(Patient(?:'s)?[ ]*Name|Name[ ]*of[ ]*(?:the[ ]*)?Patient|Referred[ ]*By|Ref\.?[ ]*By|Referring[ ]*(?:Doctor|Physician)|Consultant|Approved[ ]*By|Reported[ ]*By|Verified[ ]*By|Authori[sz]ed[ ]*By|Collected[ ]*By|Doctor|Physician)[ ]*[:\-][ ]*([^\n]+?)(?=[ ]{3,}|\n|$)"""
    )

    // A line that starts with "Name :".
    private val bareNameRegex = Regex(
        """(?im)^[ \t]*Name[ ]*[:\-][ ]*([^\n]+?)(?=[ ]{3,}|\n|$)"""
    )

    // "MR. VANSH", "Mr VANSH MONGA", "DR. HARVINDER SINGH": a title followed by
    // up to four capitalised words. Words must be separated by one or two spaces,
    // so it stops at a column gap and does not swallow the next field.
    private val titleNameRegex = Regex(
        """(?i:\b(?:Mr|Mrs|Ms|Miss|Master|Smt|Shri|Baby|Dr|Prof)\b)\.?[ ]+[A-Z][A-Za-z.']*(?:[ ]{1,2}[A-Z][A-Za-z.']*){0,3}"""
    )

    private val emailRegex = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")

    // Landlines like 0172-2230009 and mobiles like 97799-37818 or 9779937818,
    // with an optional +91.
    private val phoneRegex = Regex(
        """(?<!\d)(?:\+?91[\s\-]?)?(?:0\d{2,4}[\s\-]?\d{6,8}|[6-9]\d{4}[\s\-]?\d{5})(?!\d)"""
    )

    private val addressLineRegex = Regex("""(?im)^[ \t]*(?:Patient[ ]*)?Address[ ]*[:\-].*$""")

    private val dobRegex = Regex(
        """(?i)\b(DOB|D\.O\.B\.?|Date[ ]*of[ ]*Birth|Birth[ ]*Date)[ ]*[:\-]?[ ]*(?:\d{1,2}[/\-. ]\d{1,2}[/\-. ]\d{2,4}|\d{1,2}[ \-](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[ \-,]*\d{2,4})"""
    )

    private val aadhaarRegex = Regex("""(?<!\d)\d{4}\s?\d{4}\s?\d{4}(?!\d)""")

    // A line that is only a case code such as "C426004".
    private val codeLineRegex = Regex("""(?m)^[ \t]*[A-Z]{1,4}\d{4,}[ \t]*$""")

    private val blankRunRegex = Regex("""\n{3,}""")

    fun redact(text: String): String {
        var result = text
        result = result.replace(idNumberRegex, "\$1: [removed]")
        result = result.replace(personLabelRegex, "\$1: [removed]")
        result = result.replace(bareNameRegex, "Name: [removed]")
        result = result.replace(titleNameRegex, "[name removed]")
        result = result.replace(emailRegex, "[email removed]")
        result = result.replace(phoneRegex, "[phone removed]")
        result = result.replace(addressLineRegex, "Address: [removed]")
        result = result.replace(dobRegex, "\$1: [removed]")
        result = result.replace(aadhaarRegex, "[id removed]")
        result = result.replace(codeLineRegex, "")
        result = result.replace(blankRunRegex, "\n\n")
        return result.trim()
    }
}


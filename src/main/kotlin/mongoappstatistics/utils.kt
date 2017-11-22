package mongoappstatistics

import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import java.util.*

fun gregorianToPersian(gregorian: Date): Calendar {
    val cal = Calendar.getInstance(ULocale("fa_IR@calendar=persian"))
    cal.time = gregorian
    return cal
}
package to.kuudere.anisuge.utils

import to.kuudere.anisuge.data.models.FontData

expect suspend fun downloadFontsAndGetDir(fonts: List<FontData>, onProgress: ((String) -> Unit)? = null): String?

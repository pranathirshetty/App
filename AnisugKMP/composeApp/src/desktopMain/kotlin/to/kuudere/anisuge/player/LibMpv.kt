package to.kuudere.anisuge.player

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * JNA binding to libc's setlocale — required by libmpv.
 * mpv_create() returns null if LC_NUMERIC is not "C".
 */
private interface CLib : Library {
    fun setlocale(category: Int, locale: String?): String?
    companion object {
        const val LC_NUMERIC = 1
        const val LC_ALL     = 6
        val INSTANCE: CLib? = try {
            val libName = when {
                System.getProperty("os.name").lowercase().contains("win") -> "msvcrt"
                else -> "c"
            }
            Native.load(libName, CLib::class.java) as CLib
        } catch (e: Exception) {
            null
        }
    }
}

@com.sun.jna.Structure.FieldOrder("type", "data")
open class mpv_render_param : com.sun.jna.Structure() {
    @JvmField var type: Int = 0
    @JvmField var data: Pointer? = null
    override fun toArray(size: Int): Array<mpv_render_param> {
        return super.toArray(size) as Array<mpv_render_param>
    }
}

/**
 * Raw JNA bindings to libmpv's C API (client.h).
 * Only the functions we actually need are mapped — the rest can be added later.
 *
 * libmpv license: ISC (client API) / LGPLv2+ (core, when built with -Dgpl=false)
 */
internal interface LibMpv : Library {

    // ── Lifecycle ────────────────────────────────────────────────────────────
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_terminate_destroy(ctx: Pointer)

    // ── Options / properties ─────────────────────────────────────────────────
    fun mpv_set_option_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, data: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(data: Pointer)

    // ── Commands ─────────────────────────────────────────────────────────────
    fun mpv_command(ctx: Pointer, args: Array<String?>): Int

    // ── Events ───────────────────────────────────────────────────────────────
    fun mpv_wait_event(ctx: Pointer, timeout: Double): Pointer

    // ── Render API ───────────────────────────────────────────────────────────
    fun mpv_render_context_create(res: com.sun.jna.ptr.PointerByReference, mpv: Pointer, params: mpv_render_param): Int
    fun mpv_render_context_free(ctx: Pointer)
    
    interface mpv_render_update_fn : com.sun.jna.Callback {
        fun invoke(cb_ctx: Pointer?)
    }
    fun mpv_render_context_set_update_callback(ctx: Pointer, callback: mpv_render_update_fn, cb_ctx: Pointer?)
    fun mpv_render_context_update(ctx: Pointer): Long
    fun mpv_render_context_render(ctx: Pointer, params: mpv_render_param): Int

    companion object {
        const val MPV_EVENT_NONE        = 0
        const val MPV_EVENT_SHUTDOWN    = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_START_FILE  = 6
        const val MPV_EVENT_END_FILE    = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_IDLE        = 11
        const val MPV_EVENT_TICK        = 14

        const val MPV_FORMAT_NONE   = 0
        const val MPV_FORMAT_STRING = 1
        const val MPV_FORMAT_OSD    = 2
        const val MPV_FORMAT_FLAG   = 3
        const val MPV_FORMAT_INT64  = 4
        const val MPV_FORMAT_DOUBLE = 5

        /**
         * Load libmpv. First sets LC_NUMERIC to "C" (required by mpv),
         * then tries system lib, then bundled fallback.
         */
        fun load(): LibMpv? {
            // mpv REQUIRES LC_NUMERIC=C or mpv_create() returns null
            try {
                CLib.INSTANCE?.setlocale(CLib.LC_NUMERIC, "C")
            } catch (_: Exception) {}

            // 1. Try system library
            tryLoad("mpv")?.let { return it }

            // 2. Try extracting bundled binary from resources
            val osName  = System.getProperty("os.name").lowercase()
            val libName = when {
                "linux"  in osName -> "libmpv.so.2"
                "win"    in osName -> "mpv-2.dll"
                "mac"    in osName -> "libmpv.dylib"
                else               -> return null
            }
            val resourcePath = "native/$libName"
            val stream = LibMpv::class.java.classLoader
                ?.getResourceAsStream(resourcePath) ?: return null

            return try {
                val tmp = java.io.File.createTempFile("libmpv_", "_$libName").also { it.deleteOnExit() }
                tmp.outputStream().use { out -> stream.copyTo(out) }
                tryLoad(tmp.absolutePath)
            } catch (e: Exception) {
                null
            }
        }

        private fun tryLoad(name: String): LibMpv? = try {
            Native.load(name, LibMpv::class.java) as LibMpv
        } catch (e: UnsatisfiedLinkError) {
            null
        }
    }
}

/** Offset within mpv_event struct to find the event_id (first field, int32). */
internal fun Pointer.mpvEventId(): Int = this.getInt(0)


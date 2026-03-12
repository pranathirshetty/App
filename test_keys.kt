import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import java.lang.reflect.Modifier

fun main() {
    NativeKeyEvent::class.java.declaredFields.forEach { f ->
        if (Modifier.isStatic(f.modifiers) && f.name.contains("MEDIA")) {
            println("${f.name} = ${f.get(null)}")
        }
    }
}

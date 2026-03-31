import androidx.navigation.*
fun test() {
   val methods = androidx.core.bundle.Bundle::class.java.methods
   for (m in methods) {
       println(m.name)
   }
}

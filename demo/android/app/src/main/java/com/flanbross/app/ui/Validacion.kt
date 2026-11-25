package com.flanbross.app.ui

/**
 * Validacion — mismas reglas de formato que la web (validaciones.js) y el servidor
 * (ValidacionUtil.java). La app avisa antes de enviar; el servidor siempre vuelve a validar.
 * Cada funcion devuelve null si el dato esta bien, o el mensaje de error.
 */
object Validacion {

    private val CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val NOMBRE = Regex("^\\p{L}[\\p{L} .'-]*$")
    private val TELEFONO = Regex("^\\+?[0-9]{7,15}$")
    private val CUPON = Regex("^[A-Z0-9]{3,30}$")

    fun correo(valor: String): String? {
        val v = valor.trim()
        if (v.isEmpty()) return "El correo es obligatorio."
        if (v.length > 120 || !CORREO.matches(v)) return "Escribe un correo válido (ejemplo: nombre@correo.com)."
        return null
    }

    fun nombre(campo: String, valor: String): String? {
        val v = valor.trim()
        if (v.isEmpty()) return "El campo $campo es obligatorio."
        if (v.length !in 2..80) return "El campo $campo debe tener entre 2 y 80 caracteres."
        if (!NOMBRE.matches(v)) return "El campo $campo solo puede tener letras y espacios."
        return null
    }

    fun telefono(valor: String, obligatorio: Boolean): String? {
        val v = valor.replace(Regex("[\\s-]"), "")
        if (v.isEmpty()) return if (obligatorio) "El teléfono es obligatorio." else null
        if (!TELEFONO.matches(v)) return "El teléfono debe tener entre 7 y 15 dígitos."
        return null
    }

    fun contrasena(valor: String): String? {
        if (valor.length < 8) return "La contraseña debe tener al menos 8 caracteres."
        if (valor.length > 72) return "La contraseña no puede tener más de 72 caracteres."
        return null
    }

    fun largo(campo: String, valor: String, minimo: Int, maximo: Int): String? {
        val v = valor.trim()
        if (minimo > 0 && v.length < minimo) return "El campo $campo debe tener al menos $minimo caracteres."
        if (v.length > maximo) return "El campo $campo no puede tener más de $maximo caracteres."
        return null
    }

    fun cupon(valor: String): String? =
        if (CUPON.matches(valor.trim().uppercase())) null else "El código del cupón solo lleva letras y números."

    /** El primer error no nulo, o null si todo esta bien. */
    fun primero(vararg errores: String?): String? = errores.firstOrNull { it != null }
}

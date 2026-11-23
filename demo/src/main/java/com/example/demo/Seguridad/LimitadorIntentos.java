package com.example.demo.Seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * LimitadorIntentos — freno contra adivinar contrasenas por fuerza bruta.
 *
 * <p>Tras {@value #MAXIMO} intentos fallidos con la misma clave (IP + correo) en la ventana de
 * 15 minutos, el login responde 429 hasta que pase el tiempo. Vive en memoria: se reinicia con la
 * aplicacion y no se comparte entre servidores (con varios nodos haria falta Redis o la base).</p>
 *
 * <p>Capa: Seguridad.</p>
 */
@Component
public class LimitadorIntentos {

    static final int MAXIMO = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private static final int MAXIMO_CLAVES = 10_000;

    private final Map<String, Deque<Instant>> fallos = new ConcurrentHashMap<>();

    public boolean bloqueado(String clave) {
        Deque<Instant> intentos = fallos.get(clave);
        if (intentos == null) {
            return false;
        }
        synchronized (intentos) {
            depurar(intentos);
            return intentos.size() >= MAXIMO;
        }
    }

    public void registrarFallo(String clave) {
        if (fallos.size() > MAXIMO_CLAVES) {
            fallos.clear(); // tope de memoria ante un ataque con miles de claves distintas
        }
        Deque<Instant> intentos = fallos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (intentos) {
            depurar(intentos);
            intentos.addLast(Instant.now());
        }
    }

    public void limpiar(String clave) {
        fallos.remove(clave);
    }

    private static void depurar(Deque<Instant> intentos) {
        Instant limite = Instant.now().minus(VENTANA);
        while (!intentos.isEmpty() && intentos.peekFirst().isBefore(limite)) {
            intentos.pollFirst();
        }
    }
}

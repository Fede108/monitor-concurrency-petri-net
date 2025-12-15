package src;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Log {
    private BufferedWriter writer;

    /**
     * Construye un logger que escribe en el archivo dado.
     * Si el archivo no existe, se crea. Si existe, se abre en append.
     * @param filename ruta (o nombre) del archivo de log.
     * @throws IOException si falla la apertura/creación del archivo.
     */
    public Log(String filename) throws IOException {
        // true = append
        writer = new BufferedWriter(new FileWriter(filename, false));
    }

    /**
     * Escribe la cadena s directamente en el archivo, sin agregar saltos de línea ni espacios adicionales.
     * Se hace flush para asegurar que se escriba inmediatamente.
     * @param s texto a escribir (ejemplo: "T1" o "10201" para un marcado).
     */
    public void log(String s) {
        try {
            writer.write(s);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            // Podrías manejar de otra forma, p.ej. relanzar como unchecked o llevar conteo de errores.
        }
    }

    /**
     * Cierra el BufferedWriter. Llamar al terminar la simulación o antes de salir de la aplicación.
     */
    public void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


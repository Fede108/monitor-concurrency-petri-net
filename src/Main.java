package src;

import java.util.ArrayList;
import java.util.List;


public class Main {
     public static void main(String[] args) {

            
        double[][] matrizIncidencia = {
            //T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11
            {-1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0,  1 },  // P0
            { 1, -1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },  // P1
            {-1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0,  0 },  // P2
            { 0,  1, -1,  0,  0, -1,  0, -1,  0,  0,  0,  0 },  // P3
            { 0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0,  0 },  // P4
            { 0,  0,  0,  1, -1,  0,  0,  0,  0,  0,  0,  0 },  // P5
            { 0,  0, -1,  0,  1, -1,  1, -1,  0,  0,  1,  0 },  // P6
            { 0,  0,  0,  0,  0,  1, -1,  0,  0,  0,  0,  0 },  // P7
            { 0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0,  0 },  // P8
            { 0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0,  0 },  // P9
            { 0,  0,  0,  0,  0,  0,  0,  0,  0,  1, -1,  0 },  // P10
            { 0,  0,  0,  0,  1,  0,  1,  0,  0,  0,  1, -1 },  // P11
            /*Cada celda matriz[i][j] te dice qué relación tiene el Lugar i con la Transición j.
            Valor -1 (Pre-condición): La transición necesita sacar/consumir un token de este lugar para dispararse.
            Es una flecha que va del Lugar a la Transición (P→T).
            Valor 1 (Post-condición): La transición pone/genera un token en este lugar cuando se dispara. 
            Es una flecha que va de la Transición al Lugar (T→P).
            Valor 0: No hay conexión directa entre ese lugar y esa transición. */
        };
        // Marcado inicial: tokens en P0, P2, ..., P6.
        double[] marcadoInicial = new double[] {
            3,  // P0
            0,  // P1
            1,  // P2
            0,  // P3
            0,  // P4
            0,  // P5
            1,  // P6
            0,  // P7
            0,  // P8
            0,  // P9
            0,  // P10
            0   // P11
        };
                                                    //   T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11
        double[] sensibilizadasConTiempo = new double[] { 0, 30, 0, 0, 200, 0 , 50, 0, 50, 250, 40, 0 }; //ms

        // Crear RedDePetri y monitor de concurrencia
        RedDePetri red    = new RedDePetri(matrizIncidencia, marcadoInicial, sensibilizadasConTiempo);
        Politica politica = new Politica(Politica.TipoPolitica.ALEATORIA);
        Monitor monitor   = new Monitor(red,politica);

        //Es una lista anidada que machea la cantidad hilos a utilizar con la  cantidad de transiciones  que le corresponden.
        //Es una lista de "paquetes" de tareas.
        List<List<Integer>> transicionesPorSegmento = List.of(
            List.of(0),            // Al segmento 1 , le corresponde la T0 
            List.of(1),            // Al segmento 2, le corresponde la T1
            List.of(2, 3, 4),      // Al segmento 3, le corresponde la T2,T3 y T4
            List.of(5, 6),         // Al segmento 4, le corresponde la T5 y T6
            List.of(7, 8, 9, 10),  // Al segmento 5, le corresponde de la T7 hasta la T10
            List.of(11)            // Al segmento 6, le corresponde la T11
        );

        int[] hilosPorSegmento = {
            1,  // 1 hilos para T0
            2,  // 2 hilo  para T1
            1,  // 1 hilos para T2,T3,T4
            1,  // 1 hilo  para T5,T6
            1,  // 1 hilos para T7,T8,T9,T10
            2   // 2 hilos para T11
        };

        // Crear y arrancar los hilos
        //Lista de hilos listos para ejectar
        List<Thread> hilos = new ArrayList<>();

        //Recorremos los 6 segmentos
        for (int i = 0; i < transicionesPorSegmento.size(); i++) { 
            // Tomamos la lista  de trancisiones asociada con cada segmento
            List<Integer> lista = transicionesPorSegmento.get(i);  
            // Recorremos la lista hilosPorSegmento para saber cuántos hilos debe crear para el segmento actual i.
            for (int nroHilo = 0; nroHilo < hilosPorSegmento[i]; nroHilo++) {
                // Se le  pone una etiqueta identificatoria, por ejemplo "Segmento 2-Hilo 0".
                String nombre = String.format("Segmento %d-Hilo %d", i + 1, nroHilo);

                /*Se crea una nueva task y se le asigna el monitor de concurrencia y 
                y la lista de transiciones que le corresponden
                Luego la task se empaqueta por un hilo, el cual se instancia */
                Thread hilo = new Thread( new Task(monitor, lista),nombre);
                // Se agrega a la lista de hilo listo para ejecutar
                hilos.add(hilo);
            }
        }
        //Finalmente se recorre esa lista y se inicializa cada hilo en ella
        hilos.forEach(Thread::start);
    }
}
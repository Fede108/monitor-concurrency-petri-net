package src;

import java.util.ArrayList;
import java.util.List;


public class Main {
     public static void main(String[] args) {

            // T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11
        double[][] matrizIncidencia = {
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
        double[] sensibilizadasConTiempo = new double[] { 0, 50, 0, 50, 50, 0 , 50, 0, 50, 50, 50, 0 }; //ms

        // Crear RedDePetri y monitor de concurrencia
        RedDePetri red    = new RedDePetri(matrizIncidencia, marcadoInicial, sensibilizadasConTiempo);
        Politica politica = new Politica(Politica.TipoPolitica.PRIORIZADA);
        Monitor monitor   = new Monitor(red,politica);

        List<List<Integer>> transicionesPorSegmento = List.of(
            List.of(0,1),            // segmento 1
            List.of(2, 3, 4),      // segmento 2
            List.of(5, 6),         // segmento 3
            List.of(7, 8, 9, 10),  // segmento 4
            List.of(11)            // segmento 5
        );

        int[] hilosPorSegmento = {
            1,  // 1 hilos para T0,T1
            1,  // 1 hilos para T2,T3,T4
            1,  // 1 hilo  para T5,T6
            1,  // 1 hilos para T7,T8,T9,T10
            2   // 2 hilos para T11
        };

        // Crear y arrancar los hilos
        List<Thread> hilos = new ArrayList<>();
        
        for (int i = 0; i < transicionesPorSegmento.size(); i++) {
            List<Integer> lista = transicionesPorSegmento.get(i);

            for (int nroHilo = 0; nroHilo < hilosPorSegmento[i]; nroHilo++) {
                
                String nombre = String.format("Segmento %d-Hilo %d", i + 1, nroHilo);

                Thread hilo = new Thread( new Task(monitor, lista),nombre);
                hilos.add(hilo);
            }
        }
        hilos.forEach(Thread::start);
    }
}


        

    


package src;

import java.util.ArrayList;
import java.util.List;

public class Task implements Runnable{
    private Monitor monitor;
    private ArrayList<Integer> transiciones;

    public Task(Monitor monitor, List<Integer> transiciones){
        this.monitor = monitor;
        this.transiciones = new ArrayList<>(transiciones);
        //Inicializamos nuevamente el arrayList transiciones por una cuestion de seguridad
        //Funciona como una copia de seguridad 
    }

    @Override
    public void run() {
        // Bucle: recorre la lista de transiciones y las intenta disparar.
        int idx = 0;
        while (true) {

            int t = transiciones.get(idx);
            if(!monitor.fireTransition(t)){
                break;
                /*Si devuelve true, se disparo la transicion y el hilo sigue dentro del while.
                Si devuelve false se sale del ciclo while. */
            }
            // Avanzar al siguiente en la lista (cíclico)
            idx = (idx + 1) % transiciones.size();
        }

       System.out.println("Hilo " + Thread.currentThread().getName() + " finalizado.");
    }
}

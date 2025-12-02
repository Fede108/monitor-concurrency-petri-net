package src;

import java.util.*;

import org.apache.commons.math3.linear.RealVector;

public class Politica {
    enum TipoPolitica {
        ALEATORIA,
        PRIORIZADA
    }

    public enum TipoProceso {
        SIMPLE,
        INTERMEDIO,
        COMPLEJO
    }

    private TipoPolitica tipo;

    private Random random;
    //HashMap que asocia el tipo de proceso con su respectiva transicion 
    private HashMap<TipoProceso, Integer> mapaClasificacion;

    public Politica(TipoPolitica tipo) {

        this.tipo = tipo; // inicializa la politica con alguno de los 2 tipos
        this.random = new Random();
        this.mapaClasificaion = new HashMap<>();

        mapaClasificacion.put(TipoProceso.SIMPLE, 5);
        mapaClasificacion.put(TipoProceso.INTERMEDIO, 2);
        mapaClasificacion.put(TipoProceso.COMPLEJO, 7);

    }
       /*
     * Recibe como parametro el Vector resultado,contiene las transiciones
     * sensibilizadas y que tienen hilos esperando
     * Si el tipo de politica es aleatoria, llama al metodo seleccionarAleatoreo
     * Caso contrario, seleccionarPriorizado
     */
    public Integer seleccionarTransicion(RealVector resultado) {
        if (tipo == TipoPolitica.ALEATORIA) {
            return seleccionarAleatorio(resultado);
        } else {
            return seleccionarPriorizado(resultado);
        } 
    }

    /*
     * Recibe como parametro el Vector resultado,contiene las transiciones
     * sensibilizadas y que tienen hilos esperando
     */
    private Integer seleccionarAleatorio(RealVector resultado) {
      
        List<Integer> disponibles = new ArrayList<>();
        // crea un ArrayList para almacenar los tipos de Procesos que estan habilitados
        // segun el vector
        for (int i = 0; i < resultado.getDimension(); i++) {
            if (resultado.getEntry(i) == 1.0) {
                disponibles.add(i);
                /*
                 * verifica si el valor de la posicion del vector == 1 y dependiendo cuales
                 * tenga en 1, va a agregar el tipo de proceso a la nuevo ArrayList
                 * 
                 */

            }
        }

        if (disponibles.isEmpty())
            return null;

        return disponibles.get(random.nextInt(disponibles.size()));
        // Selecciona una transicion al azar usando un Random

    }

    private Integer seleccionarPriorizado(RealVector resultado) {
        Integer transicionSeleccionada = seleccionarAleatorio(resultado);
        if (transicionSeleccionada != null &&
            (transicionSeleccionada.equals(mapaClasificacion.get(TipoProceso.SIMPLE)) ||
             transicionSeleccionada.equals(mapaClasificacion.get(TipoProceso.INTERMEDIO)) ||
             transicionSeleccionada.equals(mapaClasificacion.get(TipoProceso.COMPLEJO)))) 
        {
            if (resultado.getEntry(mapaClasificacion.get(TipoProceso.SIMPLE)) == 1.0) {
                return mapaClasificacion.get(TipoProceso.SIMPLE);
            } else{
                   return transicionSeleccionada;
            }
          
        } else if (transicionSeleccionada != null) {
            return transicionSeleccionada;
        }
        return null;
    }
}

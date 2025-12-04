package src;
import java.io.IOException;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;


public class RedDePetri {
    private final RealMatrix matrizIncidencia;
    private RealVector marcado;
    private RealVector vectorDisparos;
    private Log logger;
    private RealVector timeAlfa;
    private RealVector timeStamp;
    private RealVector flagEspera;
    private double inicioPrograma;
    private int completados = 0; // lleva la cuenta de invariantes completados

    
    /**
     * Constructor de la clase
     * @param matrizIncidencia la matriz de incidencia que modela la red de Petri
     * @param marcado el marcado inicial de la red de Petri
     * @param sensibilizadasConTiempo los tiempos de las transiciones sensibilizadas
     */
    public RedDePetri(double[][] matrizIncidencia, double[] marcado, double[] sensibilizadasConTiempo)
    {
        this.matrizIncidencia = MatrixUtils.createRealMatrix(matrizIncidencia);
        this.marcado = MatrixUtils.createRealVector(marcado);
        this.vectorDisparos = new ArrayRealVector(this.matrizIncidencia.getColumnDimension());


        timeAlfa   = MatrixUtils.createRealVector(sensibilizadasConTiempo);
        timeStamp  = new ArrayRealVector(this.matrizIncidencia.getColumnDimension());
        flagEspera = new ArrayRealVector(this.matrizIncidencia.getColumnDimension());
        inicioPrograma = System.currentTimeMillis();


        try {
            logger = new Log("transiciones.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
    /**
    * Calcula las transiciones sensibilizadas en el marcado actual
    * @return un vector con las transiciones sensibilizadas
    */
    public RealVector getSensibilizadas()
    {
        int numTrans = matrizIncidencia.getColumnDimension(); 
        RealVector sensibilizadas = new ArrayRealVector(numTrans);

        // para cada transición se comprueba el marcado resultante de dispararla
        for(int transicion = 0; transicion<numTrans; transicion++)
        {
            RealVector columna   = matrizIncidencia.getColumnVector(transicion);
            // resultado es el vector con el marcado resultante de disparar la transición
            RealVector resultado = marcado.add(columna);
            boolean    valido    = true;
            for(double valor: resultado.toArray())
            {
                if(valor<0) // si una marca es negativa faltan tokens para disparar la transición
                {
                    valido = false;
                    break;
                }
            }
            sensibilizadas.setEntry(transicion, valido ? 1.0 : 0.0);
        }
        return sensibilizadas;
    }

    /**
     * Imprime el marcado actual de la red de Petri
     */
    public void imprimirMarcado()
    {
        System.out.print("Marcado actual:[ ");
        for(double token: marcado.toArray())
        {
            System.out.print((int) token + " ");
        }
        System.out.println("]");
    }

    /**
     * Actualiza el marcado de la red de Petri al disparar una transición
     * @param transicion
     */
    public void EcuacionDeEstado(int transicion)
    {
        RealVector  anterior, actual;
        anterior = getSensibilizadas(); // anterior guarda las transiciones sensibilizadas antes del disparo 
        
        // disparo de la transición
        vectorDisparos.setEntry(transicion, 1);
        marcado = matrizIncidencia.operate(vectorDisparos).add(marcado);
        vectorDisparos.setEntry(transicion, 0);
        
        actual  = getSensibilizadas(); // actual guarda las transiciones sensibilizadas despues del disparo 
        System.out.printf("T%d disparada(Thread: %s)\n", transicion, Thread.currentThread().getName());
        iniciarTiempo(anterior, actual);
        logger.log("T" + transicion);
        setInvariantesCompletados(transicion); 
    }

    /**
     * Inicia el temporizador para las transiciones que tienen restricciones de tiempo
     * @param anterior vector de transiciones sensibilizadas antes del disparo
     * @param nuevo vector de transiciones sensibilizadas despues del disparo   
     */
    private void iniciarTiempo(RealVector anterior, RealVector nuevo){
        RealVector delta = nuevo.subtract(anterior);
        for(int i=0; i<delta.getDimension(); i++){
            if (timeAlfa.getEntry(i) > 0.0) 
            {
                if(delta.getEntry(i) > 0)
                {
                    timeStamp.setEntry(i, System.currentTimeMillis());

                    double time = System.currentTimeMillis() - inicioPrograma;
                    System.out.printf("T%d tiempo de inicio %f ms\n", i, time);
                } 
            }
        }
    }

    /**
     * Verifica si una transición está dentro de su ventana de tiempo.
     */
    public boolean testVentanadeTiempo(int t){
        if (timeAlfa.getEntry(t) <= 0.0) {
            return true; // No hay restricción de tiempo para esta transición
        }

        double tiempoTranscurrido = System.currentTimeMillis() - timeStamp.getEntry(t);
        System.out.printf("T%d tiempo transcurrido %f ms (Thread: %s) \n", t, tiempoTranscurrido , Thread.currentThread().getName());
        if(tiempoTranscurrido >= timeAlfa.getEntry(t)){
            return true; // La transición puede dispararse
        } else {
            return false; // La transición no puede dispararse
        }
    }

    public int getNumTransiciones()
    {
        return matrizIncidencia.getColumnDimension();
    }

    public boolean hilosEsperando(int t) {
        return flagEspera.getEntry(t) == 1.0; // La transición tiene hilos esperando 
    }

    public boolean estaSensibilizada(int t) {
        return getSensibilizadas().getEntry(t) > 0.0;
    }

    public int getSleepTime(int t) {
        double time = timeStamp.getEntry(t) + timeAlfa.getEntry(t) - System.currentTimeMillis();
        return (int) Math.max(0, time);
    }

    public void setFlagEspera(int t, double valor){
        flagEspera.setEntry(t, valor); 
    }

    public void setInvariantesCompletados(int t) {
        if (t == 11) {
            completados++;
        }
        if (getInvariantesCompletados()) {
            double tiempo = (System.currentTimeMillis() - inicioPrograma) / 1000.0; 
            logger.log("\nTiempo de ejecucion: " + tiempo + " segundos");
        }
    }

    public boolean getInvariantesCompletados(){
        return completados == 200; // Verifica si se han completado los 200 invariantes
    }

    public boolean verificarInvariantes()
    {
        double suma1 = marcado.getEntry(0) + marcado.getEntry(1) + marcado.getEntry(10) + marcado.getEntry(11) + marcado.getEntry(3) + marcado.getEntry(4) +
        marcado.getEntry(5) + marcado.getEntry(7) + marcado.getEntry(8) + marcado.getEntry(9);

        double suma2 = marcado.getEntry(1) + marcado.getEntry(2);

        double suma3 = marcado.getEntry(10) + marcado.getEntry(4) + marcado.getEntry(5) + marcado.getEntry(6) + marcado.getEntry(7) + marcado.getEntry(8) + marcado.getEntry(9);

        if(suma1 == 3.0 && suma2 == 1.0 && suma3 == 1.0)
        {
            return true;
        }
        logger.log("\nInvariantes no se cumplen");
        return false;
    }
}

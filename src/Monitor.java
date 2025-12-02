package src;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class Monitor implements MonitorInterface {

     //Atiende los hilos en orden de llegada (FIFO)
    private final ReentrantLock lock = new ReentrantLock(true);

    //Aquí duermen los hilos que quieren disparar una transición pero no tienen tokens suficientes.
    private final Condition[] conds; // colas de condicion 
   
    //Aquí duermen los hilos que tienen tokens pero están esperando que pase el tiempo 
    // (Ventana de tiempo / Alfa)
    private final Condition[] esperas;

    
    private RedDePetri red;
    private Politica politica;

    // Un vector de 1s y 0s (o valores booleanos matemáticos)
    /*Guarda el resultado de preguntarle a la red qué transiciones 
    tienen suficientes tokens matemáticamente en ese instante. */

    private RealVector sensibilizadas;

    //indica en qué Transiciones hay hilos haciendo cola.
   

    private RealVector quienesEstan;
    
    /**
     * Constructor del monitor 
     * @param red la red de Petri a monitorizar
     * @param politica la política de selección de transiciones
     */
    public Monitor(RedDePetri red, Politica politica) {
        this.red = red;
        this.politica = politica;

        quienesEstan = new ArrayRealVector(red.getNumTransiciones());
        
        //Inicializamos las colas con el numero total de trancisiones 12
        conds   = new Condition[red.getNumTransiciones()]; 
        esperas = new Condition[red.getNumTransiciones()];
        //Por cada una de las 12 transiciones (T0 a T11), 
        // el sistema crea su propio par privado de colas, una de espera y otra de condicion 
        for (int i = 0; i < red.getNumTransiciones(); i++) {
            conds[i]   = lock.newCondition();
            esperas[i] = lock.newCondition();
        }
    }


     /*Su única función es actualizar el vector quienesEstan con la realidad del momento:  */
    private void quienesEstan() {
        //1. recorre las transiciones
        for (int i = 0; i < red.getNumTransiciones(); i++) {
            // 2. Pregunta al cerrojo: "¿Hay alguien esperando en la cola 'conds' de la transición i?"
        // lock.hasWaiters(...) devuelve true o false

        // 3. Si hay gente (true) pone un 1.0, si está vacía (false) pone un 0.0
            quienesEstan.setEntry(i, lock.hasWaiters(conds[i]) ? 1.0 : 0.0);
        }
    }

    /**
     * Se dispara la transición t si está sensibilizada y selecciona la siguiente a disparar
     * según la política.
     */
    @Override
    public boolean fireTransition(int t) {
        // Primero cierra el lock , se asegura que nadie toque la red mientras 
        //el metodo se esta ejecutando
        lock.lock();
        System.out.printf("T%d monitor ocupado \n", t);
    
        try {
        
            while (true){

                /**
                 * *Pregunta si se ejecuto la red las 200 veces
                 * Al completar la ejecución despierta a todos los hilos esperando en
                 * las colas de condicion y en la colas de esperas
                 */
                if (red.getInvariantesCompletados()) {   
                    for (Condition e : esperas) e.signalAll();
                    for (Condition c : conds) c.signalAll();
                    return false;
                }
                /*Dentro del bucle, evalúa tres condiciones para ver si puede disparar YA MISMO:

                red.estaSensibilizada(t): ¿Hay tokens suficientes? (Matemáticas).

                red.testVentanadeTiempo(t): ¿Ya pasó el tiempo Alfa? (Tiempo).

                !red.hilosEsperando(t): ¿No hay otro hilo durmiendo en el temporizador de esta misma transición?  */

                if (red.estaSensibilizada(t) && red.testVentanadeTiempo(t) && !red.hilosEsperando(t)) {
                    
                    //Si las 3 condiciones son verdaderas, llama a EcuacionDeEstado y se mueven los tokens
                    red.EcuacionDeEstado(t);
                    //Chequea invariantes para asegurar que no hubo errores lógicos.
                    if(red.verificarInvariantes() == false){
                        System.out.println("Invariantes no se cumplen");
                        return false;
                    }
                    red.imprimirMarcado();
                
                    /**
                     * Determinar la siguiente transicion a disparar segun la politica y el vector
                     * de transiciones sensibilizadas y con hilos esperando
                     */
                    sensibilizadas = red.getSensibilizadas();
                    System.out.println("Transiciones Sensibilizadas: " +  sensibilizadas);
                   
                    quienesEstan();
                    System.out.println("Transiciones con Hilos esperando: " + quienesEstan);

                    RealVector resultado = sensibilizadas.ebeMultiply(quienesEstan);
                    // resultado es la operacion AND de las transiciones sensibilizadas 
                    // y las q tienen hilos listos para disparar esperando
                
                    //La variable resultado contiene la lista de transiciones que son
                    // matemáticamente posibles Y que tienen hilos esperando.
                    
                    //El Monitor le pasa esa lista a la Política
                    //La política aplica su lógica (aleatoria o priorizada) 
                    // y devuelve el número de la transición a disparar
                    Integer elegido = politica.seleccionarTransicion(resultado);
                    if(elegido != null) { //Si la transicion es distinta de null, se despierta a un hilo de 
                        // la cola de condicion
                        conds[elegido].signal();
                    }  
                    return true;
                   
                } else {
                    // Si hay tokens suficientes pero no se cumplio el tiempo 
                    if (red.estaSensibilizada(t)) {
                    /**
                     * La transicion tiene hilos esperando para disparar o no esta dentro de la ventana de tiempo
                     */
                       if(red.hilosEsperando(t)){
                                System.out.printf("T%d con hilos esperando (Thread: %s)\n", t, Thread.currentThread().getName());
                                try {
                                
                                    esperas[t].await(); //pone a los hilos de la transicion a esperar (await() sin tiempo)
                                /*Si no hicieras esto, tendrías múltiples hilos poniendo múltiples temporizadores 
                                para la misma transición, lo cual es ineficiente y podría causar errores de lógica.
                                 */
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else{ //Se ejecuta cuando el hilo es el primero en llegar a la espera temporal.
                                
                                System.out.printf("T%d fuera de ventana de tiempo (Thread: %s)\n", t, Thread.currentThread().getName());
                                try {
                                    //El hilo levanta una bandera
                                    red.setFlagEspera(t, 1.0);
                                    //El hilo de la transicion T
                                    // se duerme,calculando cuánto tiempo falta para que la transición sea válida.
                                    esperas[t].await(red.getSleepTime(t), TimeUnit.MILLISECONDS);
                                    //Despues de ese tiempo, se cambia la bandera y los hilos se despiertan 
                                    red.setFlagEspera(t, 0.0);
                                    esperas[t].signal(); 
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                    } 
                     /**
                     * Bloque de codigo para la transicion que no se encuentra sensibilizada
                     */
                    else {
                        System.out.printf("T%d no sensibilizada \n", t);
                        System.out.printf("T%d monitor liberado \n", t);
                        // disparo fallido se espera en la variable de condición t
                        try {
                            conds[t].await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }   
                        // al volver de await tenemos el lock de nuevo y repetimos
                        System.out.printf("T%d monitor ocupado \n", t);
                    }
                }
            }
        } finally {
            System.out.printf("T%d monitor liberado \n", t);
            lock.unlock(); // Liberamos el lock 
        }
    }
}

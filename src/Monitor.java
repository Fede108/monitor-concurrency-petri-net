package src;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class Monitor implements MonitorInterface {
    
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore[] conds;
    private final Semaphore[] esperas;

    // Para contar cuantos hilos en conds y esperas 
    private final int[] condWaiters;
    private final int[] esperaWaiters;
       
    
    private RedDePetri red;
    private Politica politica;
    private RealVector sensibilizadas;
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

        conds   = new Semaphore[red.getNumTransiciones()];
        esperas = new Semaphore[red.getNumTransiciones()];

        condWaiters = new int[red.getNumTransiciones()];
        esperaWaiters = new int[red.getNumTransiciones()];

        for (int i = 0; i < red.getNumTransiciones(); i++) {
            conds[i]   = new Semaphore(0);
            esperas[i] = new Semaphore(0); 
        }
    }

    /**
     * Chequea que transiciones tienen hilos esperando y las setea en el vector quienesEstan
     */
    private void quienesEstan() {
        for (int i = 0; i < red.getNumTransiciones(); i++) {
            quienesEstan.setEntry(i, condWaiters[i]>0 ? 1.0 : 0.0);
        }
    }

    private void despertarTodos(){
        for(int i = 0; i<red.getNumTransiciones(); i++){
            int nConds = condWaiters[i];
            int nEsperas = esperaWaiters[i];
            for(int k = 0; k < nConds; k++){
                conds[i].release();

            }

            for(int k = 0 ; k<nEsperas; k ++ ){

                esperas[i].release();
            }
        }

    }
    /**
     * Se dispara la transición t si está sensibilizada y selecciona la siguiente a disparar
     * según la política.
     */
    @Override
    public boolean fireTransition(int t) {
        
    
        try {
            
            mutex.acquire();
            System.out.printf("T%d monitor ocupado \n",t);

            while (true){

                /**
                 * Al completar la ejecución despierta a todos los hilos esperando en
                 * las colas de condicion y en la colas de esperas
                 */
                if (red.getInvariantesCompletados()) {   
                    despertarTodos();
                    mutex.release();
                    return false;
                }

                if (red.estaSensibilizada(t) && red.testVentanadeTiempo(t) && !red.hilosEsperando(t)) {

                    red.EcuacionDeEstado(t);
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
                    // resultado es la AND de las transiciones sensibilizadas y las q tienen hilos
                    // para disparar esperando

                    Integer elegido = politica.seleccionarTransicion(resultado);
                    if(elegido != null) {
                        if (condWaiters[elegido]>0){
                            conds[elegido].release();

                        }
                    }  
                    mutex.release();
                    return true;
                   
                } else {
                    if (red.estaSensibilizada(t)) {
                    /**
                     * La transicion tiene hilos esperando para disparar o no esta dentro de la ventana de tiempo
                     */
                       if(red.hilosEsperando(t)){
                                System.out.printf("T%d con hilos esperando (Thread: %s)\n", t, Thread.currentThread().getName());
                                esperaWaiters[t]++;
                                mutex.release();
                                try {
                                    esperas[t].acquire();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }finally{
                                    mutex.acquire();
                                    esperaWaiters[t]--;
                                }

                                
                            } else{
                                System.out.printf("T%d fuera de ventana de tiempo (Thread: %s)\n", t, Thread.currentThread().getName());
                                try{
                                    red.setFlagEspera(t, 1.0);
                                    int sleepTime = red.getSleepTime(t);

                                    esperaWaiters[t]++;
                                    mutex.release();

                                    try{
                                        esperas[t].tryAcquire(sleepTime,TimeUnit.MILLISECONDS);

                                    }catch(InterruptedException e){
                                        Thread.currentThread().interrupt();
                                        return false;
                                    }finally{
                                        mutex.acquire();
                                        esperaWaiters[t]--;
                                    }
                                    red.setFlagEspera(t, 0.0);
                                    if (esperaWaiters[t]>0) {
                                        esperas[t].release();
                                        
                                    }

                                }catch(Exception e){
                                    e.printStackTrace();
                                }

                                
                            }
                    } 
                     /**
                     * La transicion no se encuentra sensibilizada
                     */
                    else {
                        System.out.printf("T%d no sensibilizada \n", t);
                        System.out.printf("T%d monitor liberado \n", t);
                        // disparo fallido se espera en la variable de condición t
                        condWaiters[t]++;
                        mutex.release();
                        try {
                            conds[t].acquire();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }finally{
                            mutex.acquire();
                            condWaiters[t]--;
                        }   
                        // al volver de await tenemos el lock de nuevo y repetimos
                        System.out.printf("T%d monitor ocupado \n", t);
                    }
                }
            }

        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            return false;
        } finally {
            
           if (mutex.availablePermits()== 0){
                System.out.printf("T%d monitor liberado \n", t);
                mutex.release();
           }
        }
    }
}

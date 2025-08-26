package src;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class Monitor implements MonitorInterface {
    
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition[] conds;
    private final Condition[] esperas;
    
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

        conds   = new Condition[red.getNumTransiciones()];
        esperas = new Condition[red.getNumTransiciones()];
        for (int i = 0; i < red.getNumTransiciones(); i++) {
            conds[i]   = lock.newCondition();
            esperas[i] = lock.newCondition();
        }
    }

    /**
     * Chequea que transiciones tienen hilos esperando y las setea en el vector quienesEstan
     */
    private void quienesEstan() {
        for (int i = 0; i < red.getNumTransiciones(); i++) {
            quienesEstan.setEntry(i, lock.hasWaiters(conds[i]) ? 1.0 : 0.0);
        }
    }

    /**
     * Se dispara la transición t si está sensibilizada y selecciona la siguiente a disparar
     * según la política.
     */
    @Override
    public boolean fireTransition(int t) {
        lock.lock();
        System.out.printf("T%d monitor ocupado \n", t);
    
        try {
        
            while (true){

                /**
                 * Al completar la ejecución despierta a todos los hilos esperando en
                 * las colas de condicion y en la colas de esperas
                 */
                if (red.getInvariantesCompletados()) {   
                    for (Condition e : esperas) e.signalAll();
                    for (Condition c : conds) c.signalAll();
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
                        conds[elegido].signal();
                    }  
                    return true;
                   
                } else {
                    if (red.estaSensibilizada(t)) {
                    /**
                     * La transicion tiene hilos esperando para disparar o no esta dentro de la ventana de tiempo
                     */
                       if(red.hilosEsperando(t)){
                                System.out.printf("T%d con hilos esperando (Thread: %s)\n", t, Thread.currentThread().getName());
                                try {
                                    esperas[t].await();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } else{
                                System.out.printf("T%d fuera de ventana de tiempo (Thread: %s)\n", t, Thread.currentThread().getName());
                                try {
                                    red.setFlagEspera(t, 1.0);
                                    esperas[t].await(red.getSleepTime(t), TimeUnit.MILLISECONDS);
                                    red.setFlagEspera(t, 0.0);
                                    esperas[t].signal(); 
                                } catch (InterruptedException e) {
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
            lock.unlock();
        }
    }
}

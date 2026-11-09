package main;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class Bridge {
    public enum StateBridge {CLOSE, OPENING, OPEN, CLOSING}

    private StateBridge state = StateBridge.CLOSE;
    private final int nbSegments;
    private final boolean[] segmentesOccupied;

    private int vehiclesG2D = 0;
    private int vehiclesD2G = 0;

    private boolean entryBlockedG2D = false;
    private boolean entryBlockedD2G = false;

    private boolean emergencyInProgress = false;
    private int nbVehiclesPriorities = 0;

    private final Queue<Boat> fileBoat = new LinkedList<>();
    private boolean openingAllowed = true;
    private Boat boatInProgress = null;

    public Bridge(int nbSegments) {
        this.nbSegments = nbSegments;
        this.segmentesOccupied = new boolean[nbSegments];
    }

    public int getNbSegments() {
        return nbSegments;
    }

    public synchronized StateBridge getState(){
        return state;
    }

    /**
     * Méthode synchronisée pour réserver un segment
     * @param indexStart indice de départ du segment dans le pont
     * @param VehicleName le nom du véhicule qui souhaite réserver un segment
     * @param size taille souhaitée
     * @param sens sens de circulation dans le pont
     * @throws InterruptedException en cas d'échec du thread
     */
    public synchronized void reserveSegment(int indexStart,int size,Sens sens, String VehicleName) throws InterruptedException {
        while (state != StateBridge.CLOSE || !areFreeSegments(indexStart,size,sens)){
            wait();
        }
        for(int i=0;i<size;i++){
            int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (indexStart+1) : (indexStart-1);
            segmentesOccupied[idx]=true;
        }

        System.out.println(VehicleName + " (" + sens + ") a réservé les segments à partir de S" + (indexStart + 1) + " (Taille: " + size + ")");
    }

    /**
     * Méthode synchronisée pour libérer un segment
     * @param indexStart indice de départ du segment dans le pont
     * @param size taille souhaitée
     * @param sens sens de circulation dans le pont
     * @param VehicleName le nom du véhicule qui souhaite réserver un segment
     */
    public synchronized void freeSegment(int indexStart,int size,Sens sens,String VehicleName){
        for (int i = 0;i<size;i++){
            int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (indexStart+1) : (indexStart-1);
            if(idx>=0 && idx<nbSegments){
                segmentesOccupied[idx]=false;
            }
            segmentesOccupied[idx]=false;
        }
        System.out.println(VehicleName + " a libéré les segments à partir de S" + (indexStart + 1));
        notifyAll();
    }

    /***
     * Vérifie si les 'nbNeeded' segments à partir du segment 'startIndex' sont libres.
     * @param startIndex index de départ de la place souhaité
     * @param nbNeeded nombre de places souhaité
     * @param sens sens de circulation
     * @return vrai ou faux si la place souhaitée est libre
     */
    private boolean areFreeSegments(int startIndex, int nbNeeded, Sens sens) {
        for (int i = 0; i < nbNeeded; i++) {
            int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (startIndex + i) : (startIndex - i);
            if (idx < 0 || idx >= nbSegments) return false;
            if (segmentesOccupied[idx]) return false;
        }
        return true;
    }

    /***
     * Entrée globale sur le pont avec les priorités de sens et les convois.
     * @param sens sens de circulation
     * @param type type de véhicule entrant sur le pont
     * @param id id du véhicule
     * @throws InterruptedException en cas d'échec du thread
     */
    public synchronized void entryOnBridge(Sens sens, Vehicle.TypeVehicle type,String id) throws InterruptedException{
        boolean isConvoi = (type== Vehicle.TypeVehicle.CONVOI);
        boolean isPriority = type.isPriority();

        if (isPriority){
            nbVehiclesPriorities++;
            if (!emergencyInProgress){
                emergencyInProgress = true;
                System.out.println("URGENCE DÉTECTÉE (" + id + ") ! Tous les véhicules se rangent sur la bande d'urgence !");
            }
            notifyAll();
        }

        if (isConvoi){
            if (sens == Sens.GAUCHE_VERS_DROITE){
                entryBlockedD2G = true;
                System.out.println("CONVOI " + id + " signale son arrivée : Entrée DROITE_VERS_GAUCHE BLOQUÉE !");
            }else{
                entryBlockedG2D = true;
                System.out.println("CONVOI " + id + " signale son arrivée : Entrée GAUCHE_VERS_DROITE BLOQUÉE !");
            }
            notifyAll();
        }

        while (true){
            if (state != StateBridge.CLOSE){
                wait();
                continue;
            }

            if (emergencyInProgress && !isPriority){
                wait();
                continue;
            }

            if (sens == Sens.GAUCHE_VERS_DROITE && vehiclesD2G > 0) { wait(); continue; }
            if (sens == Sens.DROITE_VERS_GAUCHE && vehiclesG2D > 0) { wait(); continue; }

            if (sens == Sens.GAUCHE_VERS_DROITE && entryBlockedG2D && !isConvoi) {
                wait();
                continue;
            }
            if (sens == Sens.DROITE_VERS_GAUCHE && entryBlockedD2G && !isConvoi) {
                wait();
                continue;
            }

            int idxDepart = (sens == Sens.GAUCHE_VERS_DROITE) ? 0 : (nbSegments - 1);
            if (!areFreeSegments(idxDepart, type.getSize(), sens)) {
                wait();
                continue;
            }

            break;
        }

        if (sens == Sens.GAUCHE_VERS_DROITE) vehiclesG2D++;
        else vehiclesD2G++;

        int idxDepart = (sens == Sens.GAUCHE_VERS_DROITE) ? 0 : (nbSegments - 1);
        for (int i = 0; i < type.getSize(); i++) {
            int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (idxDepart + i) : (idxDepart - i);
            segmentesOccupied[idx] = true;
        }

        System.out.println(id + " [" + type + "] s'engage sur le pont (" + sens + ")");
    }


    /***
     * Sortie complète du pont
     * @param sens sens de circulation
     * @param type type de véhicule entrant sur le pont
     * @param id id du véhicule
     */
    public synchronized void exitOnBridge(Sens sens, Vehicle.TypeVehicle type, String id) {

        int size = type.getSize();
        int idxStart = (sens == Sens.GAUCHE_VERS_DROITE) ? (nbSegments - size) : (size - 1);

        for (int i = 0; i < size; i++) {
            int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (idxStart + i) : (idxStart - i);
            if (idx >= 0 && idx < nbSegments) {
                segmentesOccupied[idx] = false;
            }
        }

        if (sens == Sens.GAUCHE_VERS_DROITE) vehiclesG2D--;
        else vehiclesD2G--;

        if (type.isPriority()){
            nbVehiclesPriorities--;
            if (nbVehiclesPriorities==0){
                emergencyInProgress = false;
                System.out.println("Fin d'urgence ! Les véhicules sur la bande d'urgence reprennent leur progression");
            }
        }

        if (type == Vehicle.TypeVehicle.CONVOI) {
            if (sens == Sens.GAUCHE_VERS_DROITE) {
                entryBlockedD2G = false;
                System.out.println("CONVOI " + id + " est sorti : Entrée DROITE_VERS_GAUCHE DÉBLOQUÉE !");
            } else {
                entryBlockedG2D = false;
                System.out.println("CONVOI " + id + " est sorti : Entrée GAUCHE_VERS_DROITE DÉBLOQUÉE !");
            }
        }

        System.out.println(id + " a totalement quitté le pont.");
        notifyAll();
    }

    /***
     * Avancement d'un segment à l'autre sur le pont avec une bande d'arret d'urgence si véhicule prioritaire arrive
     * @param posStart position de depart
     * @param posTarget position cible
     * @param size taille du véhicule
     * @param sens sens de circulation
     * @throws InterruptedException en cas d'échec du thread
     */
    public synchronized void moveSegment(int posStart, int posTarget, int size, Sens sens, boolean isPriority, String id) throws InterruptedException {

        if (emergencyInProgress && !isPriority) {
            System.out.println(id + " se range sur la BANDE D'URGENCE et libère la voie !");

            for (int i = 0; i < size; i++) {
                int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (posStart + i) : (posStart - i);
                if (idx >= 0 && idx < nbSegments) {
                    segmentesOccupied[idx] = false;
                }
            }
            notifyAll();

            while (emergencyInProgress) {
                wait();
            }

            System.out.println(" quitte la bande d'urgence et ré-occupe la voie.");

            while (!areFreeSegments(posStart, size, sens)) {
                wait();
            }

            for (int i = 0; i < size; i++) {
                int idx = (sens == Sens.GAUCHE_VERS_DROITE) ? (posStart + i) : (posStart - i);
                segmentesOccupied[idx] = true;
            }
        }

        int newHead = (sens == Sens.GAUCHE_VERS_DROITE) ? (posTarget + size - 1) : (posTarget - size + 1);

        while ((state != StateBridge.CLOSE && state != StateBridge.OPENING) || segmentesOccupied[newHead]) {
            wait();
        }

        segmentesOccupied[posStart] = false;
        segmentesOccupied[newHead] = true;

        System.out.println(id + " a avancé vers S" + (posTarget + 1));
        notifyAll();
    }

    /***
     * Realise une demande de passage de bateau au pont
     * @param b le bateau qui souhaite passer
     */
    public synchronized void requestBoatPassage(Boat b){
        fileBoat.add(b);
        System.out.println(b.getNameBoat() + " ajouté à la file FIFO (Position: " + fileBoat.size() + ")");

        verifyAndInitiateOpening();
    }

    /***
     * Vérifie que la file de bateau n'est pas vide et que le pont est fermé et qu'une demande est était éffectuer
     */
    private void verifyAndInitiateOpening(){
        if (!fileBoat.isEmpty() && state == StateBridge.CLOSE && openingAllowed) {
            System.out.println("Demande d'ouverture du pont détectée ! Bloquage des véhicules...");
            state = StateBridge.OPENING;
            notifyAll();

            new Thread(this::openingBridgeSequence).start();
        }
    }

    /***
     * Sequence d'ouverture du pont : le pont attend que tous les véhicules ont évacué pour commencer à l'ouvrir
     */
    private void openingBridgeSequence(){
        try {
            synchronized (this) {
                while (vehiclesG2D > 0 || vehiclesD2G > 0) {
                    System.out.println("En attente de l'évacuation des véhicules (" + (vehiclesG2D + vehiclesD2G) + " restant(s))...");
                    wait();
                }
                System.out.println("Pont totalement évacué ! Phase d'ouverture mécanique...");
            }

            Thread.sleep(2000);

            synchronized (this) {
                state = StateBridge.OPEN;
                System.out.println("PONT TOTALEMENT OUVERT AUX BATEAUX !");
                notifyAll();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /***
     * Fait traverser les bateaux sous le pont
     * @param b le bateau qui traverse
     * @throws InterruptedException en cas d'échec du thread
     */
    public synchronized void crossBoat(Boat b) throws InterruptedException {
        while (state != StateBridge.OPEN || fileBoat.peek() != b || boatInProgress != null) {
            wait();
        }

        boatInProgress = b;
        fileBoat.poll();
        System.out.println(b.getNameBoat() + " commence sa traversée sous le pont.");
    }

    /***
     * Le bateau a terminé sa traversée et si plus aucun autre en attente ont demande la fermeture du pont
     * @param b le bateau qui termine sa traversée
     */
    public synchronized void leaveBoat(Boat b) {
        System.out.println(b.getNameBoat() + " a terminé sa traversée.");
        boatInProgress = null;

        if (fileBoat.isEmpty()) {
            System.out.println("Plus de bateaux en attente. Début de la fermeture du pont...");
            state = StateBridge.CLOSING;
            notifyAll();

            new Thread(this::BridgeClosureSequence).start();
        } else {
            notifyAll();
        }
    }

    /***
     * Sequence de fermeture du pont : le pont attend qu'il soit fermé pour autoriser les véhicules a traversé et active un chrono de 1min avant prochaine ouverture'
     */
    private void BridgeClosureSequence() {
        try {
            Thread.sleep(2000);

            synchronized (this) {
                state = StateBridge.CLOSE;
                openingAllowed = false;
                System.out.println("PONT FERMÉ ! Les véhicules peuvent réutiliser le pont.");
                System.out.println("Chrono activé : Prochaine ouverture autorisée dans 1 minute");
                notifyAll();
            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (Bridge.this) {
                        openingAllowed = true;
                        System.out.println("1 minute écoulée ! Les ouvertures de pont sont de nouveau AUTORISÉES.");
                        verifyAndInitiateOpening();
                    }
                    timer.cancel();
                }
            }, 60000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

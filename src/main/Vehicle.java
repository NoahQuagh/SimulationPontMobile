package main;

public class Vehicle implements Runnable {
    public enum TypeVehicle{
        VOITURE(1,false),
        CAMION(2,false),
        CONVOI(4,false),
        AMBULANCE(1,true),
        POMPIER(2,true);

        private final int size;
        private final boolean isPriority;

        TypeVehicle(int size, boolean isPriority) {
            this.size = size;
            this.isPriority = isPriority;
        }

        public int getSize(){
            return size;
        }

        public boolean isPriority() {
            return isPriority;
        }
    }


    private final String nameVehicle;
    private final Bridge bridge;
    private final Sens sens;
    private final TypeVehicle type;


    public Vehicle(String nameVehicle, Bridge bridge, Sens sens, TypeVehicle type) {
        this.nameVehicle = nameVehicle;
        this.bridge = bridge;
        this.sens = sens;
        this.type = type;
    }

    @Override
    public void run() {
        try{
            System.out.println("--> " + nameVehicle + " [" + type + "] arrive au pont (" + sens + ")");

            int size = type.getSize();
            int N = bridge.getNbSegments();

            bridge.entryOnBridge(sens,type,nameVehicle);
            Thread.sleep(800);

            if (sens == Sens.GAUCHE_VERS_DROITE){
                for (int pos = 0; pos < N - size; pos++) {
                    bridge.moveSegment(pos, pos + 1, size, sens,type.isPriority(), nameVehicle);
                    Thread.sleep(800);
                }
            } else {
                for (int pos = N - 1; pos > size - 1; pos--) {
                    bridge.moveSegment(pos, pos - 1, size, sens,type.isPriority(), nameVehicle);
                    Thread.sleep(800);
                }
            }

            bridge.exitOnBridge(sens, type, nameVehicle);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package main;

public class Boat implements Runnable{

    private final String nameBoat;
    private final Bridge bridge;
    private final SensBoat sens;

    public Boat(String nameBoat, Bridge bridge, SensBoat sens) {
        this.nameBoat = nameBoat;
        this.bridge = bridge;
        this.sens = sens;
    }

    public String getNameBoat() {
        return nameBoat;
    }

    @Override
    public void run() {
        try{
            System.out.println(nameBoat + " arrive sous le pont (" + sens + ")");

            bridge.requestBoatPassage(this);

            bridge.crossBoat(this);

            Thread.sleep(1500);

            bridge.leaveBoat(this);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

package main;

public class Main {
    public static void main(String[] args) {
        Bridge pont = new Bridge(6);

        Thread v1 = new Thread(new Vehicle("Voiture-Normale", pont, Sens.GAUCHE_VERS_DROITE, Vehicle.TypeVehicle.VOITURE));
        Thread ambu = new Thread(new Vehicle("AMBULANCE-15", pont, Sens.GAUCHE_VERS_DROITE, Vehicle.TypeVehicle.AMBULANCE));

        v1.start();

        try { Thread.sleep(400); } catch (InterruptedException e) {}

        ambu.start();
    }
}
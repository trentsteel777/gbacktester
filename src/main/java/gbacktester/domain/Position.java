package gbacktester.domain;

import java.util.LinkedList;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Position {
    
    private String symbol;
    private int qty;
    private Queue<Lot> lots;

    public Position(String symbol) {
        this.symbol = symbol;
        this.lots = new LinkedList<>();
    }

    public void addQuantity(int additionalQty, double newPrice) {
        if (additionalQty < 1) {
            throw new IllegalArgumentException("additionalQty must be a positive integer: " + additionalQty);
        }

        if (newPrice < 0) {
            throw new IllegalArgumentException("newPrice must be a positive value: " + newPrice);
        }

        // Add new purchase lot
        this.lots.add(new Lot(additionalQty, newPrice));
        this.qty += additionalQty;
    }

    public void reduceQuantity(int reducedQty) {
        if (reducedQty > this.qty) {
            throw new IllegalArgumentException("Cannot reduce more than available quantity: " + reducedQty + " " + this.qty);
        }

        if (reducedQty <= 0) {
            throw new IllegalArgumentException("reducedQty must be a positive integer: " + reducedQty);
        }

        int remainingToRemove = reducedQty;
        while (remainingToRemove > 0 && !lots.isEmpty()) {
            Lot lot = lots.peek(); // Get the oldest purchase lot
            if (lot.qty <= remainingToRemove) {
                remainingToRemove -= lot.qty;
                lots.poll(); // Remove the fully depleted lot
            } else {
                lot.qty -= remainingToRemove;
                remainingToRemove = 0;
            }
        }

        this.qty -= reducedQty;
    }

    public double getCostBasis() {
    	double costBasis = 0;
        for (Lot lot : lots) {
            costBasis += lot.qty * lot.price;
        }
        return costBasis;
    }

    public double getAvgEntryPrice() {
        return this.qty == 0 ? 0 : getCostBasis() / this.qty;
    }


    // Inner class to represent purchase lots
    private static class Lot {
        int qty;
        double price;

        Lot(int qty, double price) {
            this.qty = qty;
            this.price = price;
        }
    }
}

package org.sawiq.dmcplus.client.feature.trade;

public record PriceQuote(double slots, double ars, String sourceText) {

    public double costForSlots(double requestedSlots) {
        return requestedSlots * this.ars / this.slots;
    }

    public String formatCost(double requestedSlots) {
        double cost = this.costForSlots(requestedSlots);
        if (Math.abs(cost - Math.rint(cost)) < 0.0001D) {
            return (int) Math.rint(cost) + " ар";
        }
        return String.format(java.util.Locale.US, "%.2f ар", cost).replace(".00", "");
    }
}

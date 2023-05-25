package ro.pub.cs.systems.eim.practicaltest02;

import androidx.annotation.NonNull;

public class WeatherForecastInformation {


    private final String updated;
    private final String rateUSD;
    private final String rateEUR;

    public WeatherForecastInformation(String updated, String rateUSD, String rateEUR) {
        this.updated = updated;
        this.rateEUR = rateEUR;
        this.rateUSD = rateUSD;
    }

    public String getUpdated() {
        return updated;
    }

    public String getRateUSD() {
        return rateUSD;
    }
    public String getRateEUR() {
        return rateEUR;
    }


    @NonNull
    @Override
    public String toString() {
        return "Rata{" + "updated='" + updated + '\'' + ", rateEUR='" + rateEUR + '\'' + ", rateUSD='" + rateUSD +  '\'' + '}';
    }
}

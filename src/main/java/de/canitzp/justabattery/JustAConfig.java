package de.canitzp.justabattery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JustAConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Format values = new Format();
    
    public static void load(){
        File configFile = new File(new File("config"), "justabattery.json");
        JustABattery.LOGGER.info(String.format("[JustABattery]: Reloading configuration file '%s'", configFile.getAbsolutePath()));
        if(configFile.exists()){
            try (FileReader fileReader = new FileReader(configFile)){
                values = GSON.fromJson(fileReader, Format.class);
            } catch(IOException e){
                JustABattery.LOGGER.error("[JustABattery]: Error while reading configuration file!", e);
            }
        }
    
        try(FileWriter fileWriter = new FileWriter(configFile)){
            GSON.toJson(values, fileWriter);
        } catch(IOException e){
            JustABattery.LOGGER.error("[JustABattery]: Error while writing configuration file!", e);
        }
        JustABattery.LOGGER.info("[JustABattery]: Configuration reloaded successful.");
    }
    
    public static Format get(){
        return values;
    }
    
    public static class Format {
        public String _battery_capacity = "This defines the battery capacity per cell. When combining cells, the values are multiplied. Basically the real capacity is this value times the level. Range: 1-100_000 Default: 20_000";
        public int battery_capacity = 20_000;
        public String _battery_transfer = "This defines the max transfer rate per cell. This can be increased  and with every increase the value is added. Like the battery capacity, but with trace_width as defining value. Range: 0-100_000 Default: 500";
        public int battery_transfer = 500;
        public String _battery_max_level = "How much times can the battery be upgrades? Keep in mind that with every level you get #battery_capacity more energy, that can be stored. Range: 1-20_000 Default: 100";
        public int battery_max_level = 100;
        public String _battery_max_trace_width = "How much times can the battery trace width be upgrades? Keep in mind that with every increase you get #battery_transfer more energy, that can be transferred per tick. Range: 1-20_000 Default: 100";
        public int battery_max_trace_width = 100;
        public String _allow_block_discharge = "The following value (true or false) defines if a battery is capable of extracting energy out of any energetic block.";
        public boolean allow_block_discharge = true;
        public String _chargeup_creeper_energy_required = "How much energy is required to convert a creeper to a powered one. Use any negative number to disable this feature.";
        public int chargeup_creeper_energy_required = 100_000;
        public String _charged_up_battery_on_craft = "If enabled, the crafted battery (only single battery, not combined ones) are charged fully up. Default: false";
        public boolean charged_up_battery_on_craft = false;
    }

}

package com.equitycalc.model;

import java.io.*;
import java.nio.file.*;
import java.util.*;
// import Desktop;
import java.awt.Desktop;

public class RangePresetManager {
    private static final String PRESETS_DIR = "resources/presets";
    private static final Path PRESETS_PATH = Paths.get(PRESETS_DIR);
    
    public RangePresetManager() {
        initializePresetsDirectory();
    }
    
    private void initializePresetsDirectory() {
        try {
            Files.createDirectories(PRESETS_PATH);
            // Add some default presets if directory is empty
            if (Files.list(PRESETS_PATH).count() == 0) {
                createDefaultPresets();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize presets directory", e);
        }
    }
    
    private void createDefaultPresets() throws IOException {
        createPreset("UTG.txt", "99+,ATs+,KTs+,QTs+,JTs,AJo+,KQo");
        createPreset("BTN.txt", "22+,A2s+,K2s+,Q2s+,J6s+,T6s+,96s+,86s+,76s,65s,A2o+,K9o+,QTo+,JTo");
        createPreset("SB.txt", "22+,A2s+,K2s+,Q2s+,J2s+,T2s+,92s+,82s+,72s+,62s+,52s+,42s+,32s,A2o+");
    }
    
    private void createPreset(String filename, String rangeNotation) throws IOException {
        Path preset = PRESETS_PATH.resolve(filename);
        Files.writeString(preset, rangeNotation);
    }
    
    public List<String> getAvailablePresets() {
        try {
            return Files.list(PRESETS_PATH)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.endsWith(".txt"))
                .sorted()
                .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list presets", e);
        }
    }
    
    public Range loadPreset(String presetName) {
        try {
            String notation = Files.readString(PRESETS_PATH.resolve(presetName));
            return Range.parseRange(notation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load preset: " + presetName, e);
        }
    }
    
    public void openPresetsFolder() {
        try {
            Desktop.getDesktop().open(PRESETS_PATH.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to open presets folder", e);
        }
    }
}
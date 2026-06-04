package gcewing.sgcraft.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ClientAddressBook {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File("config/sgcraft_pad_names.json");
    private static Map<String, String> names = new HashMap<>();

    static {
        load();
    }

    public static void load() {
        if (!FILE.exists()) {
            names = new HashMap<>();
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            names = GSON.fromJson(reader, type);
            if (names == null) {
                names = new HashMap<>();
            }
        } catch (Exception e) {
            System.err.println("[SGCraft] Failed to load address book aliases: " + e.getMessage());
            names = new HashMap<>();
        }
    }

    public static void save() {
        try {
            File parent = FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(names, writer);
            }
        } catch (Exception e) {
            System.err.println("[SGCraft] Failed to save address book aliases: " + e.getMessage());
        }
    }

    public static String getName(String address) {
        if (address == null) {
            return "";
        }
        if (names.containsKey(address)) {
            return names.get(address);
        }
        if (address.length() > 7) {
            String shortAddr = address.substring(0, 7);
            if (names.containsKey(shortAddr)) {
                return names.get(shortAddr);
            }
        } else if (address.length() == 7) {
            for (Map.Entry<String, String> entry : names.entrySet()) {
                if (entry.getKey().startsWith(address) && entry.getKey().length() > 7) {
                    return entry.getValue();
                }
            }
        }
        return "";
    }

    public static void setName(String address, String name) {
        if (name == null || name.trim().isEmpty()) {
            names.remove(address);
        } else {
            names.put(address, name.trim());
        }
        save();
    }
}

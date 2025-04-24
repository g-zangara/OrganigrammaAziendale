package persistence;

import model.OrganizationalUnit;
import java.io.*;

/**
 * Implementation of StorageStrategy that uses Java native serialization.
 * Uses ObjectOutputStream and ObjectInputStream for data persistence.
 */
public class SerializationStorage extends FileStorage {

    @Override
    public boolean save(OrganizationalUnit rootUnit, String filePath) {
        if (rootUnit == null) {
            return false;
        }

        try {
            // Ensure directories exist
            if (!createDirectories(filePath)) {
                return false;
            }

            // Write to file using Java serialization
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
                oos.writeObject(rootUnit);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public OrganizationalUnit load(String filePath) {
        if (!fileExists(filePath)) {
            return null;
        }

        try {
            // Read from file using Java serialization
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
                return (OrganizationalUnit) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
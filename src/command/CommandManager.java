package command;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import util.Logger;

/**
 * Gestore dei comandi per implementare le funzionalità di Undo e Redo.
 * Implementa il pattern Command con gestione della history.
 */
public class CommandManager {
    private Stack<Command> undoStack; // Stack per i comandi da annullare
    private Stack<Command> redoStack; // Stack per i comandi annullati che possono essere ripetuti
    private List<CommandHistoryListener> listeners; // Listener per aggiornare l'UI

    private static CommandManager instance; // Singleton

    /**
     * Interfaccia per ricevere aggiornamenti sullo stato della history
     */
    public interface CommandHistoryListener {
        void historyChanged();
    }

    private CommandManager() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        listeners = new ArrayList<>();
    }

    /**
     * Restituisce l'istanza singleton del CommandManager
     */
    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    /**
     * Aggiunge un listener per gli aggiornamenti della history
     */
    public void addListener(CommandHistoryListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Rimuove un listener
     */
    public void removeListener(CommandHistoryListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifica tutti i listener che la history è cambiata
     */
    private void notifyListeners() {
        for (CommandHistoryListener listener : listeners) {
            listener.historyChanged();
        }
    }

    /**
     * Esegue un comando e lo aggiunge allo stack di undo se l'esecuzione ha successo
     * @param command Il comando da eseguire
     * @return true se l'esecuzione è riuscita, false altrimenti
     */
    public boolean executeCommand(Command command) {
        if (command.execute()) {
            undoStack.push(command);
            redoStack.clear(); // Quando si esegue un nuovo comando, la history di redo viene cancellata
            Logger.logInfo("Comando eseguito: " + command.getDescription(), "Comando");
            notifyListeners();
            return true;
        }
        Logger.logError("Errore nell'esecuzione del comando: " + command.getDescription(), "Errore Comando");
        return false;
    }

    /**
     * Annulla l'ultimo comando eseguito
     * @return true se l'annullamento è riuscito, false altrimenti
     */
    public boolean undo() {
        if (canUndo()) {
            Command command = undoStack.pop();
            if (command.undo()) {
                redoStack.push(command);
                Logger.logInfo("Comando annullato: " + command.getDescription(), "Undo");
                notifyListeners();
                return true;
            } else {
                // Se l'undo fallisce, rimettiamo il comando nello stack di undo
                undoStack.push(command);
                Logger.logError("Errore nell'annullamento del comando: " + command.getDescription(), "Errore Undo");
                return false;
            }
        }
        return false;
    }

    /**
     * Ripete l'ultimo comando annullato
     * @return true se la ripetizione è riuscita, false altrimenti
     */
    public boolean redo() {
        if (canRedo()) {
            Command command = redoStack.pop();
            if (command.execute()) {
                undoStack.push(command);
                Logger.logInfo("Comando ripetuto: " + command.getDescription(), "Redo");
                notifyListeners();
                return true;
            } else {
                // Se il redo fallisce, rimettiamo il comando nello stack di redo
                redoStack.push(command);
                Logger.logError("Errore nella ripetizione del comando: " + command.getDescription(), "Errore Redo");
                return false;
            }
        }
        return false;
    }

    /**
     * Verifica se è possibile annullare un comando
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Verifica se è possibile ripetere un comando
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Restituisce la descrizione dell'ultimo comando che può essere annullato
     */
    public String getUndoDescription() {
        if (canUndo()) {
            return undoStack.peek().getDescription();
        }
        return "";
    }

    /**
     * Restituisce la descrizione dell'ultimo comando che può essere ripetuto
     */
    public String getRedoDescription() {
        if (canRedo()) {
            return redoStack.peek().getDescription();
        }
        return "";
    }

    /**
     * Cancella tutta la history
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        notifyListeners();
    }
}
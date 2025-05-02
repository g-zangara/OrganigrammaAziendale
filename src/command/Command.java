package command;

/**
 * Interfaccia Command del pattern Command.
 * Rappresenta un'operazione che può essere eseguita, annullata e ripetuta.
 */
public interface Command {
    /**
     * Esegue il comando
     * @return true se l'esecuzione è riuscita, false altrimenti
     */
    boolean execute();

    /**
     * Annulla il comando (operazione inversa)
     * @return true se l'annullamento è riuscito, false altrimenti
     */
    boolean undo();

    /**
     * Restituisce una descrizione testuale dell'operazione
     * @return descrizione del comando
     */
    String getDescription();
}
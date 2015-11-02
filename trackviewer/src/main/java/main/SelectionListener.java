package main;

/**
 * Fired whenever a selection has changed
 *
 * @author Martin Steiger
 */
public interface SelectionListener {

    /**
     * @param unit axis on which value is selected
     * @param value selected value (reported in base units (seconds/meters)
     */
    public void selected(ValueType unit, double value);
}

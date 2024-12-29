/*
 * File: Metric.java
 * Programmer: Jenzel Arevalo
 *
 * Purpose: Class intended to be used for creating custom
 *          mathematical functions with values based on event
 *          tags - or in mathematical terms: variables
 */

package VideoSync.objects.event_logger.metrics;

import VideoSync.objects.event_logger.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Metric implements Calculable
{

    /**
     * Description for metric
     */
    private String metric_description;

    /**
     * Map of event tags - but in terms of mathematics
     * they are variables. The map contains event tag
     * names and their associated descriptions
     */
    private Map<String, String> variables;

    /**
     * Map of event tag values - but in terms of mathematics
     * they are variable values. The map contains variable
     * values (Integer/Double) for each variable name.
     */
    private final Map<String, Object> variable_values;

    public Metric()
    {
        variables = new HashMap<>();
        variable_values = new HashMap<>();
    }

    public Metric(Map<String, String> variables)
    {
        this.variables = variables;

        this.variable_values = new HashMap<>();

        for(String variable : variables.keySet())
        {
            variable_values.put(variable, null);
        }
    }

    /**
     * Sets the description for the metric
     *
     * @param metric_description metric description
     */
    public void setDescription(String metric_description)
    {
        this.metric_description = metric_description;
    }

    /**
     * Sets the event tags, or in this case - variables associated
     * to the metric
     *
     * @param variables map of variables and the descriptions associated to each variable
     */
    public void setVariables(Map<String, String> variables)
    {
        if(variables != null)
        {
            this.variables = variables;

            for(String variable : variables.keySet())
            {
                this.variable_values.put(variable, null);
            }
        }
        else
        {
            this.variables = new HashMap<>();
        }
    }

    /**
     * Returns a map reference to the event tags - or variables - associated
     * to the metric
     *
     * @return reference to event tags/variables
     */
    public Map<String, String> getVariables()
    {
        return this.variables;
    }

    /**
     * Sets the variable value of a variable
     *
     * @param variable variable name
     * @param value    reference to value associated to variable
     */
    public void setVariableValue(String variable, Object value)
    {
        this.variable_values.put(variable, value);
    }

    /**
     * Returns the variable value of a variable
     *
     * @param variable variable name
     * @return reference to Integer/Double value associated to variable
     */
    public Object getVariableValue(String variable)
    {
        return variable_values.get(variable);
    }

    /**
     * Returns a map to the variable values for each variable
     *
     * @return reference to variable values
     */
    public Map<String, Object> getVariableValues()
    {
        return variable_values;
    }

    /**
     * Returns the description of the metric
     *
     * @return reference to description
     */
    public String getDescription()
    {
        return this.metric_description;
    }

    /**
     * Returns the number of events that are not ignored
     *
     * @param events list of events
     * @return count
     */
    public int getEventCount(List<Event> events)
    {
        int count = 0;
        for(Event event : events)
        {
            if(!event.isOmitted())
            {
                ++count;
            }
        }
        return count;
    }
}

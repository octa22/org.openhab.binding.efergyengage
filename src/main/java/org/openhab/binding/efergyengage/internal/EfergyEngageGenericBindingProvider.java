/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal;

import org.openhab.binding.efergyengage.EfergyEngageBindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.*;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

import static org.openhab.binding.efergyengage.internal.EfergyEngageConstants.*;


/**
 * This class is responsible for parsing the binding configuration.
 *
 * @author opecta@gmail.com
 * @since 1.0.0-SNAPSHOT
 */
public class EfergyEngageGenericBindingProvider extends AbstractGenericBindingProvider implements EfergyEngageBindingProvider {

    /**
     * {@inheritDoc}
     */
    public String getBindingType() {
        return "efergyengage";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
        if ((!(item instanceof NumberItem || item instanceof StringItem || item instanceof DateTimeItem))
           || (!INSTANT.equals(bindingConfig) && !DAY_TOTAL.equals(bindingConfig) && !WEEK_TOTAL.equals(bindingConfig)
                && !MONTH_TOTAL.equals(bindingConfig) && !YEAR_TOTAL.equals(bindingConfig) && !LAST_MEASUREMENT.equals(bindingConfig)))
        {
            throw new BindingConfigParseException("item '" + item.getName()
                    + "' is of type '" + item.getClass().getSimpleName()
                    + "', only NumberItems/StringItems are allowed - please check your *.items configuration");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
        super.processBindingConfiguration(context, item, bindingConfig);

        EfergyEngageBindingConfig config = new EfergyEngageBindingConfig(bindingConfig);

        //parse bindingconfig here ...

        addBindingConfig(item, config);

    }


    public String getItemType(String itemName) {
        final EfergyEngageBindingConfig config = (EfergyEngageBindingConfig) this.bindingConfigs.get(itemName);
        return config != null ? (config.getType()) : null;
    }

    @Override
    public float getItemValue(String itemName) {
        final EfergyEngageBindingConfig config = (EfergyEngageBindingConfig) this.bindingConfigs.get(itemName);
        return config != null ? (config.getValue()) : -1;
    }

    @Override
    public void setItemValue(String itemName, float value) {
        final EfergyEngageBindingConfig config = (EfergyEngageBindingConfig) this.bindingConfigs.get(itemName);
        if( config != null)
            config.setValue(value);
    }

    /**
     * This is a helper class holding binding specific configuration details
     *
     * @author opecta@gmail.com
     * @since 1.0.0-SNAPSHOT
     */
    class EfergyEngageBindingConfig implements BindingConfig {

        // put member fields here which holds the parsed values
        private String type;
        private float value = -1;

        public void setValue(float value) {
            this.value = value;
        }

        EfergyEngageBindingConfig(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
        public float getValue() {
            return value;
        }

    }


}

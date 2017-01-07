/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.efergyengage.internal;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.efergyengage.EfergyEngageBindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.openhab.binding.efergyengage.internal.EfergyEngageConstants.*;


/**
 * Implement this class if you are going create an actively polling service
 * like querying a Website/Device.
 *
 * @author opecta@gmail.com
 * @since 1.0.0-SNAPSHOT
 */

public class EfergyEngageBinding extends AbstractActiveBinding<EfergyEngageBindingProvider> {

    private static final Logger logger =
            LoggerFactory.getLogger(EfergyEngageBinding.class);

    /**
     * The BundleContext. This is only valid when the bundle is ACTIVE. It is set in the activate()
     * method and must not be accessed anymore once the deactivate() method was called or before activate()
     * was called.
     */
    private BundleContext bundleContext;
    private ItemRegistry itemRegistry;

    /**
     * the refresh interval which is used to poll values from the EfergyEngage
     * server (optional, defaults to 60000ms)
     */
    private long refreshInterval = 60000;
    private String email;
    private String password;
    private String token = "";
    private String utcOffset = "-60";

    //Gson parser
    private JsonParser parser = new JsonParser();

    public EfergyEngageBinding() {
    }


    /**
     * Called by the SCR to activate the component with its configuration read from CAS
     *
     * @param bundleContext BundleContext of the Bundle that defines this component
     * @param configuration Configuration properties for this component obtained from the ConfigAdmin service
     */
    public void activate(final BundleContext bundleContext, final Map<String, Object> configuration) {
        this.bundleContext = bundleContext;

        // the configuration is guaranteed not to be null, because the component definition has the
        // configuration-policy set to require. If set to 'optional' then the configuration may be null


        readConfiguration(configuration);
        setProperlyConfigured(true);
    }

    private void readConfiguration(final Map<String, Object> configuration) {
        // to override the default refresh interval one has to add a
        // parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
        String refreshIntervalString = (String) configuration.get("refresh");
        if (StringUtils.isNotBlank(refreshIntervalString)) {
            refreshInterval = Long.parseLong(refreshIntervalString);
        }

        String emailString = (String) configuration.get("email");
        if (StringUtils.isNotBlank(emailString)) {
            email = emailString;
        }

        String passwordString = (String) configuration.get("password");
        if (StringUtils.isNotBlank(passwordString)) {
            password = passwordString;
        }

        String offsetString = (String) configuration.get("offset");
        if (StringUtils.isNotBlank(offsetString)) {
            utcOffset = offsetString;
        }
        // read further config parameters here ...
    }

    /**
     * Called by the SCR when the configuration of a binding has been changed through the ConfigAdmin service.
     *
     * @param configuration Updated configuration properties
     */
    public void modified(final Map<String, Object> configuration) {
        // update the internal configuration accordingly
        readConfiguration(configuration);
    }

    /**
     * Called by the SCR to deactivate the component when either the configuration is removed or
     * mandatory references are no longer satisfied or the component has simply been stopped.
     *
     * @param reason Reason code for the deactivation:<br>
     *               <ul>
     *               <li> 0 – Unspecified
     *               <li> 1 – The component was disabled
     *               <li> 2 – A reference became unsatisfied
     *               <li> 3 – A configuration was changed
     *               <li> 4 – A configuration was deleted
     *               <li> 5 – The component was disposed
     *               <li> 6 – The bundle was stopped
     *               </ul>
     */
    public void deactivate(final int reason) {
        this.bundleContext = null;
        // deallocate resources here that are no longer needed and
        // should be reset when activating this binding again
    }

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected long getRefreshInterval() {
        return refreshInterval;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected String getName() {
        return "EfergyEngage Refresh Service";
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void execute() {
        // the frequently executed code (polling) goes here ...
        logger.debug("execute() method is called!");

        if (!bindingsExist()) {
            logger.debug("There is no existing Efergy Engage binding configuration => refresh cycle aborted!");
            return;
        }

        if( token.equals("")) {
            login();
            if( token.equals(""))
                return;
        }

        EfergyEngageMeasurement instant = null;
        EfergyEngageMeasurement dayTotal = null;
        EfergyEngageMeasurement weekTotal = null;
        EfergyEngageMeasurement monthTotal = null;
        EfergyEngageMeasurement yearTotal = null;

        for (final EfergyEngageBindingProvider provider : providers) {
            for (final String itemName : provider.getItemNames()) {
                String type = provider.getItemType(itemName);
                State newValue = null;
                State oldValue = null;
                try {
                    oldValue = itemRegistry.getItem(itemName).getState();


                    switch (type) {
                        case INSTANT:
                            if (instant == null)
                                instant = readInstant();
                            newValue = new DecimalType(instant.getValue());
                            if (!oldValue.equals(newValue)) {
                                eventPublisher.postUpdate(itemName, newValue);
                            }
                            break;
                        case LAST_MEASUREMENT:
                            if (instant == null)
                                instant = readInstant();
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new java.util.Date(instant.getMilis()));
                            eventPublisher.postUpdate(itemName, new DateTimeType(cal));
                            break;
                        case DAY_TOTAL:
                            if (dayTotal == null)
                                dayTotal = readEnergy("day");
                            newValue = new StringType((dayTotal.getValue() + " " + dayTotal.getUnit()));
                            if (!oldValue.equals(newValue)) {
                                eventPublisher.postUpdate(itemName, newValue);
                            }
                            break;
                        case WEEK_TOTAL:
                            if (weekTotal == null)
                                weekTotal = readEnergy("week");
                            newValue = new StringType((weekTotal.getValue() + " " + weekTotal.getUnit()));
                            if (!oldValue.equals(newValue)) {
                                eventPublisher.postUpdate(itemName, newValue);
                            }
                            break;
                        case MONTH_TOTAL:
                            if (monthTotal == null)
                                monthTotal = readEnergy("month");
                            newValue = new StringType((monthTotal.getValue() + " " + monthTotal.getUnit()));
                            if (!oldValue.equals(newValue)) {
                                eventPublisher.postUpdate(itemName, newValue);
                            }
                            break;
                        case YEAR_TOTAL:
                            if (yearTotal == null)
                                yearTotal = readEnergy("year");
                            newValue = new StringType((yearTotal.getValue() + " " + yearTotal.getUnit()));
                            if (!oldValue.equals(newValue)) {
                                eventPublisher.postUpdate(itemName, newValue);
                            }
                            break;
                    }
                } catch (ItemNotFoundException e) {
                    logger.error("Cannot find item " + itemName + " in item registry!");
                }
            }
        }
    }

    private void login() {
        String url = null;
        StringBuilder body = null;

        try {
            url = EFERGY_URL + "/mobile/get_token?device=android&username=" + email
                    + "&password=" + password;

            URL tokenUrl = new URL(url);
            URLConnection connection = tokenUrl.openConnection();

            //get token
            InputStream response = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
            String line = reader.readLine();
            JsonObject jobject = parser.parse(line).getAsJsonObject();
            String status = jobject.get("status").getAsString();

            if (status.equals("ok")) {
                token = jobject.get("token").getAsString();
                logger.debug("Efergy token: " + token);
            } else {
                logger.debug("Efergy login response: " + line);
                throw new EfergyEngageException(jobject.get("desc").getAsString());
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '" + url + "' is malformed: " + e.toString());
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage token: " + e.toString());
        }
    }

    private EfergyEngageMeasurement readInstant() {
        String url = null;
        int instant;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getInstant?token=" + token;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();

            //read response
            InputStream response = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line + "\n");
            }

            //read value
            parser = new JsonParser();
            JsonObject jobject = parser.parse(body.toString()).getAsJsonObject();
            if (jobject != null && jobject.get("reading") != null) {
                instant = jobject.get("reading").getAsInt();
                logger.debug("Efergy reading: " + instant);
                measurement.setValue(instant);
                measurement.setMilis(jobject.get("last_reading_time").getAsLong());
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '" + url + "' is malformed: " + e.toString());
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data: " + e.toString());
        }

        return measurement;
    }

    private EfergyEngageMeasurement readEnergy(String period) {
        String url = null;
        EfergyEngageMeasurement measurement = new EfergyEngageMeasurement();

        try {
            url = EFERGY_URL + "/mobile_proxy/getEnergy?token=" + token + "&period=" + period + "&offset=" + utcOffset;
            URL valueUrl = new URL(url);
            URLConnection connection = valueUrl.openConnection();

            //read response
            InputStream response = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line + "\n");
            }

            //read value
            JsonObject jobject = parser.parse(body.toString()).getAsJsonObject();
            if (jobject != null && jobject.get("sum") != null && jobject.get("units") != null) {
                String energy = jobject.get("sum").getAsString();
                String units = jobject.get("units").getAsString();

                logger.debug("Efergy reading for " + period + " period: " + energy + " " + units);
                measurement.setValue(Float.valueOf(energy.trim()).floatValue());
                measurement.setUnit(units);
            }
        } catch (MalformedURLException e) {
            logger.error("The URL '" + url + "' is malformed: " + e.toString());
        } catch (Exception e) {
            logger.error("Cannot get Efergy Engage data: " + e.toString());
        }
        return measurement;
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveCommand(String itemName, Command command) {
        // the code being executed when a command was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveCommand({},{}) is called!", itemName, command);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    protected void internalReceiveUpdate(String itemName, State newState) {
        // the code being executed when a state was sent on the openHAB
        // event bus goes here. This method is only called if one of the
        // BindingProviders provide a binding for the given 'itemName'.
        logger.debug("internalReceiveUpdate({},{}) is called!", itemName, newState);
    }

}

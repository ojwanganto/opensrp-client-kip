package org.smartregister.kip.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import com.rengwuxian.materialedittext.MaterialEditText;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.immunization.repository.VaccineTypeRepository;
import org.smartregister.kip.application.KipApplication;
import org.smartregister.kip.fragment.KipJsonFormFragment;
import org.smartregister.kip.repository.StockRepository;

import java.util.ArrayList;
import java.util.Date;

import util.JsonFormUtils;

/**
 * Created by keyman on 11/04/2017.
 */
public class KipJsonFormActivity extends JsonFormActivity {

    private int generatedId = -1;
    private MaterialEditText balancetextview;
    private KipJsonFormFragment kipJsonFormFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void initializeFormFragment() {
        kipJsonFormFragment = KipJsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, kipJsonFormFragment).commit();
    }

    @Override
    public void writeValue(String stepName, String key, String value, String openMrsEntityParent, String openMrsEntity, String openMrsEntityId) throws JSONException {
        super.writeValue(stepName, key, value, openMrsEntityParent, openMrsEntity, openMrsEntityId);
        refreshCalculateLogic(key, value);

    }

    @Override
    public void onFormFinish() {
        super.onFormFinish();
    }

    private void refreshCalculateLogic(String key, String value) {
        stockVialsenteredinReceivedForm(key, value);
        stockDateEnteredinReceivedForm(key, value);
        stockDateEnteredinIssuedForm(key, value);
        stockVialsEnteredinIssuedForm(key, value);
        stockWastedVialsEnteredinIssuedForm(key, value);
        stockDateEnteredinAdjustmentForm(key, value);
        stockVialsenteredinAdjustmentForm(key, value);
    }

    private void stockDateEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Issued")) {
                StockRepository str = KipApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Date_Stock_Issued") && value != null && !value.equalsIgnoreCase("")) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    int existingbalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = "0";
                    String vaccineName = object.getString("title").replace("Stock Issued", "").trim();
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_Issued")) {
                                if (questions.has("value")) {
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    currentBalance = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));
                                }
                            }

                            if (questions.getString("key").equalsIgnoreCase("Vials_Wasted")) {
                                if (questions.has("value")) {
                                    if (!StringUtils.isBlank(questions.getString("value"))) {
                                        wastedvials = questions.getString("value");
                                    }
                                } else {
                                    wastedvials = "0";
                                }
                            }
                            if (questions.getString("key").equalsIgnoreCase("Vials_Issued")) {
                                if (questions.has("value")) {
                                    if (!StringUtils.isBlank(questions.getString("value"))) {
                                        vialsvalue = questions.getString("value");
                                    }
                                } else {
                                    kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                                }
                            }

                        }
                    }
                    if (!StringUtils.isBlank(vialsvalue)) {
                        newBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime()) - Integer.parseInt(vialsvalue) - Integer.parseInt(wastedvials);
                        kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    }

                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccineTypeRepository = KipApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalance % dosesPerVial == 0) {
                        vialsused = currentBalance / dosesPerVial;
                    } else if (currentBalance != 0) {
                        vialsused = (currentBalance / dosesPerVial) + 1;
                    }
                    if (currentBalance != 0) {
                        displaybalance = vialsused;
                        if (balancetextview != null) {
                            balancetextview.setErrorColor(Color.BLACK);
                            balancetextview.setError(currentBalance + " children vaccinated today.Assuming " + displaybalance + " vials used.");
//                                    writeValue("step1","labelHeaderImage","checkwritetolabel","","","");
                        }
                    } else {
                        balancetextview.setErrorColor(Color.BLACK);
                        balancetextview.setError("");

                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void stockVialsEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Issued")) {
                StockRepository str = KipApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Vials_Issued")) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int displaybalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = "0";
                    String vaccineName = object.getString("title").replace("Stock Issued", "").trim();
                    int existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_Issued")) {
                                if (questions.has("value")) {
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }
                                    existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                    currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));

                                }
                            }

                            if (questions.getString("key").equalsIgnoreCase("Vials_Wasted")) {

                                if (questions.has("value")) {
                                    if (!StringUtils.isBlank(questions.getString("value"))) {
                                        wastedvials = questions.getString("value");
                                    }
                                } else {
                                    wastedvials = "0";
                                }
                            }

                        }
                    }
                    kipJsonFormFragment.getLabelViewFromTag("Balance", "");

                    if (value != null && !StringUtils.isBlank(value)) {

                        newBalance = existingbalance - Integer.parseInt(value) - Integer.parseInt(wastedvials);
                        kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    } else {
                        kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                    }
                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccineTypeRepository = KipApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccineTypeRepository.getDosesPerVial(vaccineName);
                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsused = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsused = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }
                    if (currentBalanceVaccineUsed != 0) {
                        displaybalance = vialsused;
                        if (balancetextview != null) {
                            balancetextview.setErrorColor(Color.BLACK);
                            balancetextview.setError(currentBalanceVaccineUsed + " children vaccinated today.Assuming " + displaybalance + " vials used.");
//                                    writeValue("step1","labelHeaderImage","checkwritetolabel","","","");
                        }
                    } else {
                        balancetextview.setErrorColor(Color.BLACK);
                        balancetextview.setError("");

                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void stockWastedVialsEnteredinIssuedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Issued")) {
                StockRepository str = KipApplication.getInstance().stockRepository();
                if (key.equalsIgnoreCase("Vials_Wasted")) {
                    if (balancetextview == null) {
                        ArrayList<View> views = getFormDataViews();
                        for (int i = 0; i < views.size(); i++) {
                            if (views.get(i) instanceof MaterialEditText) {
                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Issued")) {
                                    balancetextview = (MaterialEditText) views.get(i);
                                }
                            }
                        }
                    }
                    String label = "";
                    int currentBalanceVaccineUsed = 0;
                    int displaybalance = 0;
                    int newBalance = 0;
                    Date encounterDate = new Date();
                    String vialsvalue = "";
                    String wastedvials = value;
                    String vaccineName = object.getString("title").replace("Stock Issued", "").trim();
                    int existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());

                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_Issued") && questions.has("value")) {
                                label = questions.getString("value");
                                if (label != null && StringUtils.isNotBlank(label)) {
                                    Date dateTime = JsonFormUtils.formatDate(label, false);
                                    if (dateTime != null) {
                                        encounterDate = dateTime;
                                    }
                                }

                                existingbalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                currentBalanceVaccineUsed = str.getVaccineUsedToday(encounterDate.getTime(), checkifmeasles(vaccineName.toLowerCase()));
                            }

                            if (questions.getString("key").equalsIgnoreCase("Vials_Issued")) {

                                if (questions.has("value")) {
                                    if (!StringUtils.isBlank(questions.getString("value"))) {
                                        vialsvalue = questions.getString("value");
                                    }
                                } else {
                                    vialsvalue = "0";
                                }
                            }
                        }
                    }
                    kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                    if (wastedvials == null || StringUtils.isBlank(wastedvials)) {
                        wastedvials = "0";
                    }
                    if (vialsvalue != null && !StringUtils.isBlank(vialsvalue)) {

                        newBalance = existingbalance - Integer.parseInt(vialsvalue) - Integer.parseInt(wastedvials);
                        kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + newBalance);
                    } else {
                        kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                    }
                    int DosesPerVial = 0;
                    int vialsused = 0;
                    VaccineTypeRepository vaccine_typesRepository = KipApplication.getInstance().vaccineTypeRepository();
                    int dosesPerVial = vaccine_typesRepository.getDosesPerVial(vaccineName);
                    if (currentBalanceVaccineUsed % dosesPerVial == 0) {
                        vialsused = currentBalanceVaccineUsed / dosesPerVial;
                    } else if (currentBalanceVaccineUsed != 0) {
                        vialsused = (currentBalanceVaccineUsed / dosesPerVial) + 1;
                    }
                    if (currentBalanceVaccineUsed != 0) {
                        displaybalance = vialsused;
                        if (balancetextview != null) {
                            balancetextview.setErrorColor(Color.BLACK);
                            balancetextview.setError(currentBalanceVaccineUsed + " children vaccinated today.Assuming " + displaybalance + " vials used.");
//                                    writeValue("step1","labelHeaderImage","checkwritetolabel","","","");
                        }
                    } else {
                        balancetextview.setErrorColor(Color.BLACK);
                        balancetextview.setError("");

                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void stockDateEnteredinReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Received")) {
                if (key.equalsIgnoreCase("Date_Stock_Received") && value != null && !value.equalsIgnoreCase("")) {
//                    if(balancetextview == null) {
//                        ArrayList<View> views = getFormDataViews();
//                        for (int i = 0; i < views.size(); i++) {
//                            if (views.get(i) instanceof MaterialEditText) {
//                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Received")) {
//                                    balancetextview = (MaterialEditText) views.get(i);
//                                }
//                            }
//                        }
//                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    String vialsvalue = "";
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_Received")) {
                                if (questions.has("value")) {
                                    Date encounterDate = new Date();
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString("title").replace("Stock Received", "").trim();
                                    StockRepository str = KipApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }
                            if (questions.getString("key").equalsIgnoreCase("Vials_Received")) {
                                if (questions.has("value")) {
                                    label = questions.getString("value");
                                    vialsvalue = label;
                                }
                            }
                            if (vialsvalue != null && !vialsvalue.equalsIgnoreCase("")) {
                                displaybalance = currentBalance + Integer.parseInt(vialsvalue);
//                                if (balancetextview != null) {
//                                    balancetextview.setErrorColor(getResources().getColor(R.color.dark_grey));
//                                    balancetextview.setError("New balance : " + displaybalance);
//                                }
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void stockVialsenteredinReceivedForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Received")) {
                if (key.equalsIgnoreCase("Vials_Received") && value != null && !value.equalsIgnoreCase("")) {
//                    if(balancetextview == null) {
//                        ArrayList<View> views = getFormDataViews();
//                        for (int i = 0; i < views.size(); i++) {
//                            if (views.get(i) instanceof MaterialEditText) {
//                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase(key)) {
//                                    balancetextview = (MaterialEditText) views.get(i);
//                                }
//                            }
//                        }
//                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_Received")) {
                                if (questions.has("value")) {
                                    Date encounterDate = new Date();
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString("title").replace("Stock Received", "").trim();
                                    StockRepository str = KipApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }

                            if (StringUtils.isNotBlank(value)) {
                                displaybalance = currentBalance + Integer.parseInt(value);
//                                if (balancetextview != null) {
//                                    balancetextview.setErrorColor(Color.BLACK);
//                                    balancetextview.setError("New balance : " + displaybalance);
//                                }
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                            }
                        }
                    }
                } else {
                    kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                }
            }
        } catch (
                JSONException e
                )

        {
            e.printStackTrace();
        }

    }

    private void stockDateEnteredinAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Loss/Adjustment")) {
                if (key.equalsIgnoreCase("Date_Stock_loss_adjustment") && value != null && !value.equalsIgnoreCase("")) {
//                    if(balancetextview == null) {
//                        ArrayList<View> views = getFormDataViews();
//                        for (int i = 0; i < views.size(); i++) {
//                            if (views.get(i) instanceof MaterialEditText) {
//                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase("Vials_Received")) {
//                                    balancetextview = (MaterialEditText) views.get(i);
//                                }
//                            }
//                        }
//                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    String vialsvalue = "";
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_loss_adjustment")) {
                                if (questions.has("value")) {
                                    Date encounterDate = new Date();
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString("title").replace("Stock Loss/Adjustment", "").trim();
                                    StockRepository str = KipApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }
                            if (questions.getString("key").equalsIgnoreCase("Vials_Adjustment")) {
                                if (questions.has("value")) {
                                    label = questions.getString("value");
                                    vialsvalue = label;
                                }
                            }
                            if (vialsvalue != null && !vialsvalue.equalsIgnoreCase("")) {
                                displaybalance = currentBalance + Integer.parseInt(vialsvalue);
//                                if (balancetextview != null) {
//                                    balancetextview.setErrorColor(getResources().getColor(R.color.dark_grey));
//                                    balancetextview.setError("New balance : " + displaybalance);
//                                }
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "");

                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void stockVialsenteredinAdjustmentForm(String key, String value) {
        JSONObject object = getStep("step1");
        try {
            if (object.getString("title").contains("Stock Loss/Adjustment")) {
                if (key.equalsIgnoreCase("Vials_Adjustment") && value != null && !value.equalsIgnoreCase("")) {
//                    if(balancetextview == null) {
//                        ArrayList<View> views = getFormDataViews();
//                        for (int i = 0; i < views.size(); i++) {
//                            if (views.get(i) instanceof MaterialEditText) {
//                                if (((String) views.get(i).getTag(com.vijay.jsonwizard.R.id.key)).equalsIgnoreCase(key)) {
//                                    balancetextview = (MaterialEditText) views.get(i);
//                                }
//                            }
//                        }
//                    }
                    String label = "";
                    int currentBalance = 0;
                    int displaybalance = 0;
                    JSONArray fields = object.getJSONArray("fields");
                    for (int i = 0; i < fields.length(); i++) {
                        JSONObject questions = fields.getJSONObject(i);
                        if (questions.has("key")) {
                            if (questions.getString("key").equalsIgnoreCase("Date_Stock_loss_adjustment")) {
                                if (questions.has("value")) {
                                    Date encounterDate = new Date();
                                    label = questions.getString("value");
                                    if (label != null && StringUtils.isNotBlank(label)) {
                                        Date dateTime = JsonFormUtils.formatDate(label, false);
                                        if (dateTime != null) {
                                            encounterDate = dateTime;
                                        }
                                    }

                                    String vaccineName = object.getString("title").replace("Stock Loss/Adjustment", "").trim();
                                    vaccineName = checkifmeasles(vaccineName);
                                    StockRepository str = KipApplication.getInstance().stockRepository();
                                    currentBalance = str.getBalanceFromNameAndDate(vaccineName, encounterDate.getTime());
                                }
                            }
                            if (StringUtils.isNotBlank(value) && !value.equalsIgnoreCase("-")) {
                                displaybalance = currentBalance + Integer.parseInt(value);
//                                if (balancetextview != null) {
//                                    balancetextview.setErrorColor(Color.BLACK);
//                                    balancetextview.setError("New balance : " + displaybalance);
//                                }
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "New balance: " + displaybalance);

                            } else {
                                kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                            }
                        }
                    }
                } else {
                    kipJsonFormFragment.getLabelViewFromTag("Balance", "");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String checkifmeasles(String vaccineName) {
        if (vaccineName.equalsIgnoreCase("M/MR")) {
            return "measles";
        }
        return vaccineName;
    }

    public boolean checkIfBalanceNegative() {
        boolean balancecheck = true;
        String balancestring = kipJsonFormFragment.getRelevantTextViewString("Balance");

        if (balancestring.contains("New balance")) {
            int balance = Integer.parseInt(balancestring.replace("New balance:", "").trim());
            if (balance < 0) {
                balancecheck = false;
            }
        }

        return balancecheck;
    }
}


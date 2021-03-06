package org.smartregister.kip.repository;

import android.database.Cursor;
import android.util.Log;

import net.sqlcipher.SQLException;
import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.kip.application.KipApplication;
import org.smartregister.kip.domain.DailyTally;
import org.smartregister.kip.domain.MohIndicator;
import org.smartregister.kip.domain.MonthlyTally;
import org.smartregister.repository.BaseRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Jason Rogena - jrogena@ona.io on 15/06/2017.
 */

public class MonthlyTalliesRepository extends BaseRepository {
    private static final String TAG = MonthlyTalliesRepository.class.getCanonicalName();
    public static final SimpleDateFormat DF_YYYYMM = new SimpleDateFormat("yyyy-MM");
    public static final SimpleDateFormat DF_DDMMYY = new SimpleDateFormat("dd/MM/yy");
    private static final String TABLE_NAME = "monthly_tallies";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_PROVIDER_ID = "provider_id";
    private static final String COLUMN_INDICATOR_ID = "indicator_id";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_MONTH = "month";
    private static final String COLUMN_EDITED = "edited";
    private static final String COLUMN_DATE_SENT = "date_sent";
    private static final String COLUMN_UPDATED_AT = "updated_at";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_INDICATOR_ID, COLUMN_PROVIDER_ID,
            COLUMN_VALUE, COLUMN_MONTH, COLUMN_EDITED, COLUMN_DATE_SENT, COLUMN_CREATED_AT, COLUMN_UPDATED_AT
    };

    private static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
            COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
            COLUMN_INDICATOR_ID + " INTEGER NOT NULL," +
            COLUMN_PROVIDER_ID + " VARCHAR NOT NULL," +
            COLUMN_VALUE + " VARCHAR NOT NULL," +
            COLUMN_MONTH + " VARCHAR NOT NULL," +
            COLUMN_EDITED + " INTEGER NOT NULL DEFAULT 0," +
            COLUMN_DATE_SENT + " DATETIME NULL," +
            COLUMN_CREATED_AT + " DATETIME NULL," +
            COLUMN_UPDATED_AT + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

    private static final String INDEX_PROVIDER_ID = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_PROVIDER_ID + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_PROVIDER_ID + " COLLATE NOCASE);";
    private static final String INDEX_INDICATOR_ID = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_INDICATOR_ID + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_INDICATOR_ID + " COLLATE NOCASE);";
    private static final String INDEX_UPDATED_AT = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_UPDATED_AT + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_UPDATED_AT + ");";
    private static final String INDEX_MONTH = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_MONTH + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_MONTH + ");";
    private static final String INDEX_EDITED = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_EDITED + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_EDITED + ");";
    private static final String INDEX_DATE_SENT = "CREATE INDEX " + TABLE_NAME + "_" + COLUMN_DATE_SENT + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_DATE_SENT + ");";

    public static final String INDEX_UNIQUE = "CREATE UNIQUE INDEX " + TABLE_NAME + "_" + COLUMN_INDICATOR_ID + "_" + COLUMN_MONTH + "_index" +
            " ON " + TABLE_NAME + "(" + COLUMN_INDICATOR_ID + "," + COLUMN_MONTH + ");";

    public MonthlyTalliesRepository(KipRepository kipRepository) {
        super(kipRepository);
    }

    protected static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_QUERY);
        database.execSQL(INDEX_PROVIDER_ID);
        database.execSQL(INDEX_INDICATOR_ID);
        database.execSQL(INDEX_UPDATED_AT);
        database.execSQL(INDEX_MONTH);
        database.execSQL(INDEX_EDITED);
        database.execSQL(INDEX_DATE_SENT);
    }

    /**
     * Returns a list of all months that have corresponding daily tallies by unsent monthly tallies
     *
     * @param startDate The earliest date for the draft reports' month. Set argument to null if you
     *                  don't want this enforced
     * @param endDate   The latest date for the draft reports' month. Set argument to null if you
     *                  don't want this enforced
     * @return List of months with unsent monthly tallies
     */
    public List<Date> findUneditedDraftMonths(Date startDate, Date endDate) {
        List<String> allTallyMonths = KipApplication.getInstance().dailyTalliesRepository()
                .findAllDistinctMonths(DF_YYYYMM, startDate, endDate);
        try {
            List<Date> unsentMonths = new ArrayList<>();
            if (allTallyMonths != null && !allTallyMonths.isEmpty()) {
                for (String curMonthString : allTallyMonths) {
                    Date curMonth = DF_YYYYMM.parse(curMonthString);
                    if ((startDate != null && curMonth.getTime() < startDate.getTime())
                            || (endDate != null && curMonth.getTime() > endDate.getTime())) {
                        continue;
                    }
                    unsentMonths.add(curMonth);
                }

                return unsentMonths;
            }
        } catch (SQLException | ParseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return new ArrayList<>();
    }

    /**
     * Returns a list of draft monthly tallies corresponding to the provided month
     *
     * @param month The month to get the draft tallies for
     * @return
     */
    public List<MonthlyTally> findDrafts(String month) {
        Cursor cursor = null;
        List<MonthlyTally> monthlyTallies = new ArrayList<>();
        try {

            Log.w(TAG, "Using daily tallies instead of monthly");
            Map<Long, List<DailyTally>> dailyTallies = KipApplication.getInstance().dailyTalliesRepository().findTalliesInMonth(DF_YYYYMM.parse(month));
            for (List<DailyTally> curList : dailyTallies.values()) {
                MonthlyTally curTally = addUpDailyTallies(curList);
                if (curTally != null) {
                    monthlyTallies.add(curTally);
                }
            }

        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (ParseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return monthlyTallies;
    }

    /**
     * Returns a list of draft monthly tallies corresponding to a custom date range
     *
     * @param startDate The start date of the custom range
     * @param endDate   The end date of the custom range
     * @return
     */
    public List<MonthlyTally> findDrafts(Date startDate, Date endDate) {
        List<MonthlyTally> monthlyTallies = new ArrayList<>();
        try {

            Log.w(TAG, "Using daily tallies instead of monthly");
            Map<Long, List<DailyTally>> dailyTallies = KipApplication.getInstance().dailyTalliesRepository().findTallies(startDate, endDate);
            for (List<DailyTally> curList : dailyTallies.values()) {
                MonthlyTally curTally = addUpDailyTallies(curList);
                if (curTally != null) {
                    monthlyTallies.add(curTally);
                }
            }

        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return monthlyTallies;
    }

    /**
     * Returns a list of all monthly tallies corresponding to the provided month
     *
     * @param month The month to get the draft tallies for
     * @return
     */
    public List<MonthlyTally> find(String month) {
        // Check if there exists any sent tally in the database for the month provided
        Cursor cursor = null;
        List<MonthlyTally> monthlyTallies = new ArrayList<>();
        try {
            cursor = getReadableDatabase().query(TABLE_NAME, TABLE_COLUMNS,
                    COLUMN_MONTH + " = '" + month + "'",
                    null, null, null, null, null);
            monthlyTallies = readAllDataElements(cursor);
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return monthlyTallies;
    }

    private MonthlyTally addUpDailyTallies(List<DailyTally> dailyTallies) {
        String userName = KipApplication.getInstance().context().allSharedPreferences().fetchRegisteredANM();
        MonthlyTally monthlyTally = null;
        double value = 0d;
        for (int i = 0; i < dailyTallies.size(); i++) {
            if (i == 0) {
                monthlyTally = new MonthlyTally();
                monthlyTally.setIndicator(dailyTallies.get(i).getIndicator());
            }
            try {
                value = value + Double.valueOf(dailyTallies.get(i).getValue());
            } catch (NumberFormatException e) {
                Log.w(TAG, Log.getStackTraceString(e));
            }
        }

        if (monthlyTally != null) {
            monthlyTally.setUpdatedAt(Calendar.getInstance().getTime());
            monthlyTally.setValue(String.valueOf(Math.round(value)));
            monthlyTally.setProviderId(userName);
        }

        return monthlyTally;
    }


    private List<MonthlyTally> readAllDataElements(Cursor cursor) {
        List<MonthlyTally> tallies = new ArrayList<>();
        HashMap<Long, MohIndicator> indicatorMap = KipApplication.getInstance().moh710IndicatorsRepository().findAll();

        try {
            if (cursor != null && cursor.getCount() > 0) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    MonthlyTally curTally = extractMonthlyTally(indicatorMap, cursor);
                    if (curTally != null) {
                        tallies.add(curTally);
                    }
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (ParseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return tallies;
    }

    private MonthlyTally extractMonthlyTally(HashMap<Long, MohIndicator> indicatorMap, Cursor cursor) throws ParseException {
        long indicatorId = cursor.getLong(cursor.getColumnIndex(COLUMN_INDICATOR_ID));
        if (indicatorMap.containsKey(indicatorId)) {
            MonthlyTally curTally = new MonthlyTally();
            curTally.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
            curTally.setProviderId(
                    cursor.getString(cursor.getColumnIndex(COLUMN_PROVIDER_ID)));
            curTally.setIndicator(indicatorMap.get(indicatorId));
            curTally.setValue(cursor.getString(cursor.getColumnIndex(COLUMN_VALUE)));
            curTally.setMonth(DF_YYYYMM.parse(cursor.getString(cursor.getColumnIndex(COLUMN_MONTH))));
            curTally.setEdited(
                    cursor.getInt(cursor.getColumnIndex(COLUMN_EDITED)) != 0
            );
            curTally.setDateSent(
                    cursor.getString(cursor.getColumnIndex(COLUMN_DATE_SENT)) == null ?
                            null : new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_DATE_SENT)))
            );
            curTally.setUpdatedAt(
                    new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_UPDATED_AT)))
            );

            return curTally;
        }

        return null;
    }

    /**
     * Returns a list of dates for monthly reports that have been edited, but not sent
     *
     * @param startDate The earliest date for the monthly reports. Set argument to null if you
     *                  don't want this enforced
     * @param endDate   The latest date for the monthly reports. Set argument to null if you
     *                  don't want this enforced
     * @return The list of monthly reports that have been edited, but not sent
     */
    public List<MonthlyTally> findEditedDraftMonths(Date startDate, Date endDate) {
        Cursor cursor = null;
        List<MonthlyTally> tallies = new ArrayList<>();

        try {
            cursor = getReadableDatabase().query(
                    TABLE_NAME, new String[]{COLUMN_MONTH, COLUMN_CREATED_AT},
                    COLUMN_DATE_SENT + " IS NULL AND " + COLUMN_EDITED + " = 1",
                    null, COLUMN_MONTH, null, null);

            if (cursor != null && cursor.getCount() > 0) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String curMonth = cursor.getString(cursor.getColumnIndex(COLUMN_MONTH));
                    Date month = DF_YYYYMM.parse(curMonth);

                    if ((startDate != null && month.getTime() < startDate.getTime())
                            || (endDate != null && month.getTime() > endDate.getTime())) {
                        continue;
                    }

                    Long dateStarted = cursor.getLong(cursor.getColumnIndex(COLUMN_CREATED_AT));
                    MonthlyTally tally = new MonthlyTally();
                    tally.setMonth(month);
                    tally.setCreatedAt(new Date(dateStarted));
                    tallies.add(tally);
                }
            }
        } catch (SQLException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (ParseException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return tallies;
    }

    private Long checkIfExists(long indicatorId, String month) {
        Cursor mCursor = null;
        try {
            String query = "SELECT " + COLUMN_ID + " FROM " + TABLE_NAME +
                    " WHERE " + COLUMN_INDICATOR_ID + " = " + String.valueOf(indicatorId) + " COLLATE NOCASE "
                    + " and " + COLUMN_MONTH + " = '" + month + "'";
            mCursor = getWritableDatabase().rawQuery(query, null);
            if (mCursor != null && mCursor.moveToFirst()) {
                return mCursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return null;
    }

}

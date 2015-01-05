package udacity.gdg.help2find.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import udacity.gdg.help2find.database.HelpFindContract;
import udacity.gdg.help2find.database.HelpFindContract.AnnouncementEntry;
import udacity.gdg.help2find.database.HelpFindContract.ImageEntry;
import udacity.gdg.help2find.entities.Announcement;
import udacity.gdg.help2find.entities.Image;
import udacity.gdg.help2find.utils.JsonUtils;

/**
 * Created by nnet on 30.12.14.
 */
public class FetchAllAnnouncementsTask extends AsyncTask<String, Void, Void> {

    private static final String ALL_ANNOUNCEMENTS_URL = "http://helpme2findit.herokuapp.com/api/announcements.json";
    private final String LOG_TAG = FetchAllAnnouncementsTask.class.getSimpleName();
    private final Context mContext;

    public FetchAllAnnouncementsTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr = null;
        try {
            Uri builtUri = Uri.parse(ALL_ANNOUNCEMENTS_URL).buildUpon()
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getDataFromJson(forecastJsonStr);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

    private void getDataFromJson(String forecastJsonStr) throws JSONException {
        List<Announcement> announcements = new ArrayList<Announcement>();
        JSONArray response = new JSONArray(forecastJsonStr);
        for (int i = 0; i< response.length(); i++) {
            try {
                JsonNode actualObj = JsonUtils.defaultMapper().readTree(String.valueOf(response.get(i)));
                announcements.add(JsonUtils.parseJson(actualObj, Announcement.class));
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Vector<ContentValues> announcementVector = new Vector<ContentValues>(announcements.size());
        Vector<ContentValues> imageContentValues = new Vector<ContentValues>();

        for(Announcement announcement : announcements) {
            ContentValues values = new ContentValues();
            values.put(AnnouncementEntry.ANNOUNCEMENT_ID, announcement.getId());

            values.put(AnnouncementEntry.ANNOUNCEMENT_PREVIEW_IMAGE, announcement.getPreviewImageUrl());
            values.put(AnnouncementEntry.ANNOUNCEMENT_CATEGORY, announcement.getCategory());
            values.put(AnnouncementEntry.ANNOUNCEMENT_IS_LOST, announcement.isLost());
            values.put(AnnouncementEntry.ANNOUNCEMENT_TITLE, announcement.getTitle());
            values.put(AnnouncementEntry.ANNOUNCEMENT_DESCRIPTION, announcement.getDescription());

            values.put(AnnouncementEntry.ANNOUNCEMENT_ADDRESS, announcement.getAddress());
            values.put(AnnouncementEntry.ANNOUNCEMENT_LATITUDE, announcement.getLatitude());
            values.put(AnnouncementEntry.ANNOUNCEMENT_LONGITUDE, announcement.getLatitude());

            values.put(AnnouncementEntry.ANNOUNCEMENT_CREATED_AT, HelpFindContract.getDbDateString(new Date(announcement.getCreatedAt() * 1000L)));
            values.put(AnnouncementEntry.ANNOUNCEMENT_UPDATED_AT, HelpFindContract.getDbDateString(new Date(announcement.getUpdatedAt() * 1000L)));
            values.put(AnnouncementEntry.ANNOUNCEMENT_DATE, HelpFindContract.getDbDateString(new Date(announcement.getDate() * 1000L)));


            for (Image image : announcement.getImages()) {
                imageContentValues.add(addImage(announcement.getId(), image));
            }

            announcementVector.add(values);
        }
        saveAnnouncements(announcementVector);
        saveImages(imageContentValues);
    }

    private void saveImages(Vector<ContentValues> imageContentValues) {
        if (imageContentValues.size() > 0) {
            ContentValues[] cvArray = new ContentValues[imageContentValues.size()];
            imageContentValues.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(ImageEntry.CONTENT_URI, cvArray);
        }
    }

    private void saveAnnouncements(Vector<ContentValues> announcementVector) {
        if (announcementVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[announcementVector.size()];
            announcementVector.toArray(cvArray);
            mContext.getContentResolver().bulkInsert(AnnouncementEntry.CONTENT_URI, cvArray);
        }
    }

    private ContentValues addImage(long announcementId, Image image) {
        ContentValues values = new ContentValues();

        values.put(ImageEntry.IMAGE_URL, image.getImageUrl());
        values.put(ImageEntry.IMAGE_ANNOUNCEMENT_ID, announcementId);

        return values;
    }

}

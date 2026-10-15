package com.asadmarket.oldmarket.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.asadmarket.oldmarket.model.AppItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class Catalog {

public static final String CATALOG_URL =
    "https://raw.githubusercontent.com/asasdbek-67671488/asadmarket-server/main/apps.json";

public static final String RAW_BASE =
    "https://raw.githubusercontent.com/asasdbek-67671488/asadmarket-erver/main/";

private static final String PREFS = "static_catalog";
private static final String CACHE_KEY = "apps_json";

public static String resolveUrl(String value) {
    if (value == null || value.trim().length() == 0) {
        return "";
    }

    value = value.trim();

    if (value.startsWith("https://") ||
        value.startsWith("http://")) {
        return value;
    }

    while (value.startsWith("/")) {
        value = value.substring(1);
    }

    return RAW_BASE + value;
}

private static String readUrl(String address) throws Exception {
    HttpURLConnection connection =
        (HttpURLConnection) new URL(address).openConnection();

    connection.setConnectTimeout(12000);
    connection.setReadTimeout(20000);
    connection.setRequestMethod("GET");
    connection.setInstanceFollowRedirects(true);

    try {
        int code = connection.getResponseCode();

        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code);
        }

        InputStream input = connection.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int count;

        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }

        input.close();

        return new String(output.toByteArray(), "UTF-8");
    } finally {
        connection.disconnect();
    }
}

public static ArrayList<AppItem> load(Context context)
        throws Exception {

    String json = null;

    try {
        json = readUrl(CATALOG_URL);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(CACHE_KEY, json)
            .apply();

    } catch (Exception networkError) {
        SharedPreferences preferences =
            context.getSharedPreferences(
                PREFS, Context.MODE_PRIVATE);

        json = preferences.getString(CACHE_KEY, null);

        if (json == null || json.length() == 0) {
            throw networkError;
        }
    }

    JSONArray array;

    JSONObject root = null;

    if (json.trim().startsWith("{")) {
        root = new JSONObject(json);
        array = root.optJSONArray("apps");

        if (array == null) {
            throw new Exception("apps.json: missing apps array");
        }
    } else {
        array = new JSONArray(json);
    }

    ArrayList<AppItem> result = new ArrayList<AppItem>();

    for (int i = 0; i < array.length(); i++) {
        JSONObject object = array.optJSONObject(i);
        if (object == null) continue;

        AppItem app = new AppItem();

        app.id = object.optInt("id", i + 1);
        app.name = object.optString("name", "");
        app.developer = object.optString(
            "developer", object.optString("author", ""));
        app.description = object.optString("description", "");
        app.icon = object.optString("icon", "");
        app.apkUrl = object.optString(
        "downloadUrl",
        object.optString("apk_url", object.optString("download_url", ""))
        );

        app.api = object.optInt(
            "api", object.optInt("min_api", 1));

        app.packageName = object.optString(
            "package_name", object.optString("package", ""));

        app.version = object.optString("version", "");
        app.versionCode = object.optInt("version_code", 1);
        app.isGame = object.optBoolean("is_game", false);

        app.categoryCode = object.optString(
            "category_code", object.optString("category", ""));

        app.categoryLabel = object.optString(
            "category_label", app.categoryCode);

        app.rating = (float) object.optDouble("rating", 0.0);
        app.downloads = object.optInt("downloads", 0);

        JSONArray screenshots = object.optJSONArray("screenshots");

        if (screenshots != null) {
            for (int j = 0; j < screenshots.length(); j++) {
                String image = screenshots.optString(j, "");

                if (image.length() > 0) {
                    app.screenshots.add(resolveUrl(image));
                }
            }
        }

        if (app.name.length() == 0) continue;

        if (app.icon.length() > 0) {
            app.icon = resolveUrl(app.icon);
        }

        result.add(app);
    }

    return result;
}

public static AppItem find(Context context, int id)
        throws Exception {

    ArrayList<AppItem> apps = load(context);

    for (int i = 0; i < apps.size(); i++) {
        if (apps.get(i).id == id) {
            return apps.get(i);
        }
    }

    return null;
}

public static boolean isCompatible(AppItem app) {
    return app != null && app.api <= Build.VERSION.SDK_INT;
}

}
package scs2682.exercise_10;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONException;
import org.json.JSONObject;
import scs2682.exercise_10.model.Game;

public class AppActivity extends AppCompatActivity {
	public static final String NAME = AppCompatActivity.class.getSimpleName();

	private static final String CACHED_GAME_TIME_KEY = "cachedGameTime";
	private static final String GAME_KEY = "game";

	private static final class DownloadJsonTask extends AsyncTask<String, Void, Game> {
		@NonNull
		private final WeakReference<AppActivity> appActivityWeakReference;

		private DownloadJsonTask(@NonNull AppActivity appActivity) {
			appActivityWeakReference = new WeakReference<>(appActivity);
		}

		@Override
		protected Game doInBackground(final String... urls) {
			// read the first url
			String urlString = urls != null && urls.length > 0 ? urls[0] : "";

			if (TextUtils.isEmpty(urlString)) {
				// supplied url is empty or null
				return null;
			}

			InputStream inputStream;
			Game game = null;

			try {
				URL url = new URL(urlString);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setReadTimeout(10000);
				connection.setConnectTimeout(15000);
				connection.setRequestMethod("GET");
				connection.connect();

				int statusCode = connection.getResponseCode();
				String statusMessage = connection.getResponseMessage();

				Log.w("DownloadWebsiteTask", "statusCode=" + statusCode + " and message=" + statusMessage);

				// get stream from the remote place
				inputStream = connection.getInputStream();

				// convert steram to text
				String jsonString = new Scanner(inputStream, "UTF-8")
						.useDelimiter("\\A")
						.next();

				// close the stream
				inputStream.close();

				// finally disconnect
				connection.disconnect();

				JSONObject json = new JSONObject(jsonString);

				String status = json.optString("status", "");
				int code = json.optInt("code", -1);
				JSONObject dataJson = json.optJSONObject("data");

				if ("success".equals(status) && code == 0 && dataJson != null) {
					game = new Game(dataJson.optJSONObject("game"));
				}

			}
			catch (IOException | JSONException e) {
				game = null;
			}

			return game;
		}

		@Override
		protected void onPostExecute(final Game game) {
			if (appActivityWeakReference.get() != null && appActivityWeakReference.get().isValid()) {
				// activity is fine, clal the UI update method
				appActivityWeakReference.get().updateCache(game);
			}
		}
	}

	private TextView label;

	private ImageView locationImage;
	private TextView locationText;
	private TextView type;

	private TextView visitingScore;
	private ImageView visitingImage;
	private TextView visitingName;
	private TextView visitingStats;

	private TextView homeScore;
	private ImageView homeImage;
	private TextView homeName;
	private TextView homeStats;

	private DownloadJsonTask downloadJsonTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.appactivity);

		label = (TextView) findViewById(R.id.label);

		locationImage = (ImageView) findViewById(R.id.locationImage);
		locationText = (TextView) findViewById(R.id.locationText);
		type = (TextView) findViewById(R.id.type);

		visitingScore = (TextView) findViewById(R.id.visitingScore);
		visitingImage = (ImageView) findViewById(R.id.visitingImage);
		visitingName = (TextView) findViewById(R.id.visitingName);
		visitingStats = (TextView) findViewById(R.id.visitingStats);

		homeScore = (TextView) findViewById(R.id.homeScore);
		homeImage = (ImageView) findViewById(R.id.homeImage);
		homeName = (TextView) findViewById(R.id.homeName);
		homeStats = (TextView) findViewById(R.id.homeStats);

		String dataJsonUrl = "http://mobileapp-elasticl-l0g6xu1571v2-1583716606.us-east-1.elb.amazonaws" +
				".com/api/v1/games?league=euro&id=1596947";
		int cacheExpiration = getResources().getInteger(R.integer.cache_expiration);

		SharedPreferences sharedPreferences = getSharedPreferences(NAME, MODE_PRIVATE);
		long now = System.currentTimeMillis();
		long cachedTime = sharedPreferences.getLong(CACHED_GAME_TIME_KEY, 0L);

		Game cachedGame;

		try {
			String gameString = sharedPreferences.getString(GAME_KEY, "{}");
			JSONObject gameJson = new JSONObject(gameString);
			cachedGame = new Game(gameJson);
		}
		catch(JSONException e) {
			// not interested
			e.printStackTrace();
			cachedGame = null;
		}

		if (cachedGame != null && !cachedGame.isEmpty
				&& now - cachedTime < cacheExpiration) {
			// we have a safe cache, let's use it
			updateUi(cachedGame, true);
		}
		else {
			downloadJsonTask = new DownloadJsonTask(this);
			downloadJsonTask.execute(dataJsonUrl);
		}
	}

	@Override
	protected void onDestroy() {
		if (downloadJsonTask != null) {
			downloadJsonTask.cancel(true);
			downloadJsonTask = null;
		}

		super.onDestroy();
	}

	/**
	 * Called from the second thread
	 *
	 * @param game game
	 */
	private void updateCache(@Nullable Game game) {
		if (game != null && !game.isEmpty) {
			// valid game - store it into shared preferences
			getSharedPreferences(NAME, MODE_PRIVATE)
					.edit()
					.putLong(CACHED_GAME_TIME_KEY, System.currentTimeMillis())
					.putString(GAME_KEY, game.json.toString())
					.apply();
		}
		else {
			// not a valid game - check do we have something old from the cache
			String gameString
					= getSharedPreferences(NAME, MODE_PRIVATE).getString(GAME_KEY, "{}");
			try {
				JSONObject gameJson = new JSONObject(gameString);
				game = new Game(gameJson);
			}
			catch(JSONException e) {
				e.printStackTrace();
			}
		}

		updateUi(game, false);
	}

	private void updateUi(@Nullable Game game, boolean isFromCache) {
		String labelValue = isFromCache ? "Loaded from cache" : "Loaded from network";
		label.setText(labelValue);

		if (game != null && !game.isEmpty) {
			// valid game, update UI
			locationText.setText(game.location);
			type.setText(game.type);
			Glide.with(this)
					.load(game.locationImageUrl)
					.diskCacheStrategy(DiskCacheStrategy.SOURCE)
					.into(locationImage);

			visitingScore.setText("" + game.visitingScore);
			visitingName.setText(game.visitingName);
			visitingStats.setText(game.visitingLeagueRank + " " + game.visitingConferenceName);
			Glide.with(this)
					.load(game.visitingImageUrl)
					.diskCacheStrategy(DiskCacheStrategy.SOURCE)
					.into(visitingImage);

			homeScore.setText("" + game.homeScore);
			homeName.setText(game.homeName);
			homeStats.setText(game.homeLeagueRank + " " + game.homeConferenceName);
			Glide.with(this)
					.load(game.homeImageUrl)
					.diskCacheStrategy(DiskCacheStrategy.SOURCE)
					.into(homeImage);
		}
		else {
			// bad game, show a toast and reset any UI values
			locationImage.setImageBitmap(null);
			locationText.setText("");
			type.setText("");

			visitingScore.setText("");
			visitingImage.setImageBitmap(null);
			visitingName.setText("");
			visitingStats.setText("");

			homeScore.setText("");
			homeImage.setImageBitmap(null);
			homeName.setText("");
			homeStats.setText("");
		}
	}

	private boolean isValid() {
		return !isDestroyed() && !isFinishing();
	}
}
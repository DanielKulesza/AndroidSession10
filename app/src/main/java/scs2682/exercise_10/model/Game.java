package scs2682.exercise_10.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.json.JSONObject;

public final class Game {
	@NonNull
	public final JSONObject json;

	public final String location;
	public final String locationImageUrl;
	public final String type;

	public final int visitingScore;
	public final String visitingName;
	public final String visitingImageUrl;
	public final String visitingConferenceName;
	public final int visitingLeagueRank;

	public final int homeScore;
	public final String homeName;
	public final String homeImageUrl;
	public final String homeConferenceName;
	public final int homeLeagueRank;

	public final boolean isEmpty;

	public Game(@Nullable JSONObject json) {
		this.json = json != null ? json : new JSONObject();

		JSONObject detailsJson = this.json.optJSONObject("details");
		JSONObject visitingJson = this.json.optJSONObject("visiting_team");
		JSONObject homeJson = this.json.optJSONObject("home_team");

		if (detailsJson != null) {
			// valid 'details'
			location = detailsJson.optString("location", "");
			locationImageUrl = detailsJson.optString("location_image_url");
			type = detailsJson.optString("type");
		}
		else {
			// no 'details'
			location = "";
			locationImageUrl = "";
			type = "";
		}

		if (visitingJson != null) {
			// valid 'visiting_team'
			visitingScore = visitingJson.optInt("score");
			visitingName = visitingJson.optString("name", "");
			visitingImageUrl = visitingJson.optString("image_url", "");
			visitingLeagueRank = visitingJson.optInt("league_rank");

			JSONObject conferenceJson = visitingJson.optJSONObject("conference");

			if (conferenceJson != null) {
				visitingConferenceName = conferenceJson.optString("name", "");
			}
			else {
				visitingConferenceName = "";
			}
		}
		else {
			// no 'visiting_team'
			visitingScore = 0;
			visitingName = "";
			visitingImageUrl = "";
			visitingConferenceName = "";
			visitingLeagueRank = 0;
		}

		if (homeJson != null) {
			// valid 'home_team'
			homeScore = homeJson.optInt("score");
			homeName = homeJson.optString("name", "");
			homeImageUrl = homeJson.optString("image_url", "");
			homeLeagueRank = homeJson.optInt("league_rank");

			JSONObject conferenceJson = homeJson.optJSONObject("conference");

			if (conferenceJson != null) {
				homeConferenceName = conferenceJson.optString("name", "");
			}
			else {
				homeConferenceName = "";
			}
		}
		else {
			// no 'home_team'
			homeScore = 0;
			homeName = "";
			homeImageUrl = "";
			homeConferenceName = "";
			homeLeagueRank = 0;
		}

		isEmpty = this.json.length() == 0;
	}
}
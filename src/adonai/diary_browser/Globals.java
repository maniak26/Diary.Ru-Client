package adonai.diary_browser;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

public class Globals 
{
	public static Drawable tempDrawable = null;
	public static SharedPreferences mSharedPrefs = null;
	public static UserData mUser = new UserData();
	public static DiaryHttpClient mDHCL = new DiaryHttpClient();
}
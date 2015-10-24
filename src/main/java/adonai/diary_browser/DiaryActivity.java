package adonai.diary_browser;

import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewFragment;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.util.IabHelper;
import com.android.vending.util.IabResult;
import com.android.vending.util.Inventory;
import com.android.vending.util.Purchase;

import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.preferences.PreferencePage;

/**
 * Родительская активность для всех остальных.
 * <br/>
 * Здесь хранятся:
 * <ul>
 *     <li>Обработка платёжек и MOTD</li>
 *     <li>Обработка старта {@link NetworkService}</li>
 *     <li>Обработка ошибок исполнения</li>
 *     <li>Обработка посылок сообщений в {@link NetworkService}</li>
 * </ul>
 * 
 * @author Адонай
 */
public class DiaryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    protected IabHelper mHelper;
    protected boolean mCanBuy = false;

    protected TextView mLogin;
    protected TextView mUmailNum;
    protected Toolbar toolbar;
    protected FloatingActionButton fab;
    protected DrawerLayout drawer;
    protected NavigationView navigationView;
    protected SharedPreferences mSharedPrefs;
    protected DiaryFragment diaryFragment;
    protected DiaryListFragment diaryListFragment;
    protected UmailListFragment umailListFragment;
    protected DatabaseHandler mDatabase;


    //костыли
    public BrowseHistory browserHistory;
    protected NetworkService mService;

    protected DiarySlidePane slider;
    protected MessageSenderFragment messagePane;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_new);
        mDatabase = new DatabaseHandler(this);
        mSharedPrefs = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        diaryFragment = new DiaryFragment();
        diaryListFragment = new DiaryListFragment();
        umailListFragment = new UmailListFragment();
        mLogin = (TextView) navigationView.findViewById(R.id.login_name);
        //mUmailNum = (TextView) navigationView.findViewById(R.id.nav_umail_counter);
        //mUmailNum.setOnClickListener(this);
        //mUmailNum.setVisibility(View.GONE);


        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjuleYDZj7oG7JeX8+bwJWQrf+DYgqGOSiIA6frTZJ+/C7Jt/+PMbWjd/rOelshuYy5HWqywFjvOPoK18zIRMavS1QtlxIMbA/eaVlk+QKEaqOY0EIuBUEIog9e2H7HMq9BVE7o1j8NFuG0skj2jDYfO2R0OfZS2xetqQcXtEtQLp0osS9GQK20oVfNM+LQyyG5ROcab3TmXXjiR0J43XdD8txhSLRB7gzFflMy9C1zYE7736i/R7NAHdmX6KRWmK+YsbI78Wnoy6xa63npdUTIcTUlUwV9zg6VWxQjSLsWnhkgqqJltmKGXk/d3DGYVlwZBu7XnwU0ufGvC1wBC09wIDAQAB";

        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess())
                    mCanBuy = true;
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        //slider = (DiarySlidePane) findViewById(R.id.slider);
        //slider.setPanelSlideListener(sliderListener);
        //slider.setSliderFadeColor(Color.WHITE);


        TypedValue color = new TypedValue();
        getTheme().resolveAttribute(R.attr.colorPrimary, color, true);
        //swipeList.setColorSchemeColors(color.data);
        //swipeBrowser.setColorSchemeColors(color.data);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(this);
                builder.setTitle(R.string.about);
                View aboutContent = LayoutInflater.from(this).inflate(R.layout.about_d, null);
                TextView author = (TextView) aboutContent.findViewById(R.id.author_info);
                author.setText(Html.fromHtml(getString(R.string.author_description)));
                author.setMovementMethod(LinkMovementMethod.getInstance());
                TextView app = (TextView) aboutContent.findViewById(R.id.app_info);

                String appWithVersion;
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    appWithVersion = String.format(getString(R.string.application_description), pInfo.versionName);
                } catch (PackageManager.NameNotFoundException nnfe) {
                    appWithVersion = String.format(getString(R.string.application_description), "unknown");
                }

                app.setText(Html.fromHtml(appWithVersion));
                app.setMovementMethod(LinkMovementMethod.getInstance());
                builder.setView(aboutContent);
                builder.create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.diary_list_a, menu);
        return true;
    }

    public DatabaseHandler getDatabase() {
        return mDatabase;
    }


    @Override
    protected void onDestroy() {
        mDatabase.close();
        if (mCanBuy)
            mHelper.dispose();
        super.onDestroy();
    }

    protected UserData getUser() {
        return UserData.getInstance();
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_favlist:
                //setCurrentTab(TAB_FAV_LIST, false);
                break;
            case R.id.nav_fav:
                //setCurrentTab(TAB_FAV_POSTS, false);
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_diary:
                //setCurrentTab(TAB_MY_DIARY, false);
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_discussions:
                //setCurrentTab(TAB_DISCUSSIONS, false);
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_quotes:
                //handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl() + "?quote", false));
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_umail:
                //TODO call umail fragment here
                //Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
                //startActivity(postIntent);
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_settings:
                startActivity(new Intent(this, PreferencePage.class));
                break;
            case R.id.nav_menu_close:
                break;
        }
        //DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
        public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    // костыли для вызова функций фрагментов

    // Часть кода относится к кнопке быстрой промотки
    void handleScroll(int direction) {
        diaryListFragment.handleScroll(direction);
    }
}

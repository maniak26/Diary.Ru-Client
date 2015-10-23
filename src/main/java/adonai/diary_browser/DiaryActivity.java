package adonai.diary_browser;

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
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
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
public class DiaryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Callback {
    private static final int HANDLE_APP_START = -100;
    private static final String SKU_DONATE = "small";

    protected IabHelper mHelper;
    protected boolean mCanBuy = false;

    protected SwipeRefreshLayout swipeList;
    protected SwipeRefreshLayout swipeBrowser;

    protected DiarySlidePane slider;
    protected DiaryFragment mainPane;
    protected DiaryWebView mPageBrowser;
    protected MessageSenderFragment messagePane;
    protected MaterialDialog pd;
     
    protected Handler mUiHandler;
    protected NetworkService mService;
    protected DiaryHttpClient mHttpClient;
    protected SharedPreferences mSharedPrefs;
    
    protected String pageToLoad;
    protected String textToWrite;
    protected Uri imageToUpload;
    
    protected DatabaseHandler mDatabase;

    protected TextView mLogin;
    protected TextView mUmailNum;
    protected Toolbar toolbar;
    protected FloatingActionButton fab;
    protected DrawerLayout drawer;
    protected NavigationView navigationView;


    SlidingPaneLayout.PanelSlideListener sliderListener = new SlidingPaneLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View view, float v) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelOpened(View view) {
            messagePane.setHasOptionsMenu(true);
            mainPane.setHasOptionsMenu(false);
        }

        @Override
        public void onPanelClosed(View view) {
            messagePane.setHasOptionsMenu(false);
            mainPane.setHasOptionsMenu(true);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Utils.setupTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_new);
        mSharedPrefs = getApplicationContext().getSharedPreferences(Utils.mPrefsFile, MODE_PRIVATE);
        mDatabase = new DatabaseHandler(this);
        mUiHandler = new Handler(this);

        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjuleYDZj7oG7JeX8+bwJWQrf+DYgqGOSiIA6frTZJ+/C7Jt/+PMbWjd/rOelshuYy5HWqywFjvOPoK18zIRMavS1QtlxIMbA/eaVlk+QKEaqOY0EIuBUEIog9e2H7HMq9BVE7o1j8NFuG0skj2jDYfO2R0OfZS2xetqQcXtEtQLp0osS9GQK20oVfNM+LQyyG5ROcab3TmXXjiR0J43XdD8txhSLRB7gzFflMy9C1zYE7736i/R7NAHdmX6KRWmK+YsbI78Wnoy6xa63npdUTIcTUlUwV9zg6VWxQjSLsWnhkgqqJltmKGXk/d3DGYVlwZBu7XnwU0ufGvC1wBC09wIDAQAB";

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

        mLogin = (TextView) navigationView.findViewById(R.id.login_name);

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
        slider = (DiarySlidePane) findViewById(R.id.slider);
        slider.setPanelSlideListener(sliderListener);
        slider.setSliderFadeColor(Color.WHITE);

        mUiHandler.sendEmptyMessage(HANDLE_APP_START); // ensure that service is running

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
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLE_APP_START:
                mService = NetworkService.getInstance(this);
                if (mService == null)
                    mUiHandler.sendEmptyMessageDelayed(HANDLE_APP_START, 50);
                else {
                    setRequestedOrientation(mService.mOrientation);
                    mHttpClient = mService.mNetworkClient;
                    mUiHandler.sendEmptyMessage(Utils.HANDLE_START); // выполняем стартовые действия для всех остальных

                    if (getPackageName().contains("pro"))
                        break;
                    
                    showChangesPage();
                }
                break;
            case Utils.HANDLE_SERVICE_ERROR:
                Toast.makeText(getApplicationContext(), getString(R.string.service_not_running), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_CONNECTIVITY_ERROR:
                Toast.makeText(getApplicationContext(), getString((Integer) msg.obj), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_NOTFOUND_ERROR:
                Toast.makeText(getApplicationContext(), getString(R.string.notfound_error), Toast.LENGTH_SHORT).show();
                break;
            case Utils.HANDLE_JUST_DO_GET:
                Toast.makeText(getApplicationContext(), getString(R.string.completed), Toast.LENGTH_SHORT).show();
                break;
        }
        
        if (pd != null) {
            pd.dismiss();
            pd = null;
        }

        return true;
    }

    private void showChangesPage() {
        // Показываем страничку изменений
        try {
            final String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            final String stored = mSharedPrefs.getString("stored.version", "");
            boolean show = mSharedPrefs.getBoolean("show.version", true);
            if (show && !current.equals(stored)) {
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing()) // бывает при неверной авторизации
                            return;

                        // TODO: move to XML
                        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(DiaryActivity.this);
                        TextView message = new TextView(DiaryActivity.this);
                        message.setMovementMethod(LinkMovementMethod.getInstance());
                        message.setGravity(Gravity.CENTER_HORIZONTAL);
                        message.setText(Html.fromHtml(getString(R.string.ad_text)));
                        TypedValue color = new TypedValue();
                        getTheme().resolveAttribute(R.attr.text_color_main, color, true);
                        message.setTextColor(color.data);
                        builder.setTitle(R.string.ad_title).setView(message);
                        builder.setPositiveButton(R.string.help, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                purchaseGift();
                            }
                        });
                        builder.setNegativeButton(R.string.later, null);
                        builder.create().show();
                    }
                }, 5000);

                mSharedPrefs.edit()
                    .putString("stored.version", current)
                    .apply();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // не сработало - и ладно
        }
    }

    protected void purchaseGift() {
        if (mCanBuy) {
            mHelper.launchPurchaseFlow(DiaryActivity.this, SKU_DONATE, 6666, new IabHelper.OnIabPurchaseFinishedListener() {
                @Override
                public void onIabPurchaseFinished(IabResult result, Purchase info) {
                    if (result.isSuccess()) {
                        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(DiaryActivity.this);
                        builder.setTitle(R.string.completed).setMessage(R.string.thanks);
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.create().show();
                    }

                    mHelper.queryInventoryAsync(false, new IabHelper.QueryInventoryFinishedListener() {
                        @Override
                        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                            if (result.isSuccess()) {
                                if (inv.getPurchase(SKU_DONATE) != null)
                                    mHelper.consumeAsync(inv.getPurchase(SKU_DONATE), null);
                            }
                        }
                    });
                }
            }, "NothingAndNowhere" + getUser().getUserName());
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

    public void handleBackground(int opCode, Object body) {
        if(pd != null && pd.isShowing()) {
            pd.setTitle(R.string.loading);
            pd.setContent(getString(R.string.loading_data));
        } else {
            pd = new MaterialDialog.Builder(this)
                    .title(R.string.loading)
                    .content(R.string.loading_data)
                    .progress(true, 0)
                    .build();
            pd.show();
        }

        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mHttpClient.abort();
            }
        });
        mService.handleRequest(opCode, body);
    }

    public void handleUi(int opCode, Object body) {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }

    public void handleFontChange(String currSize) {
        try {
            int realNum = Integer.parseInt(currSize);
            mPageBrowser.getSettings().setMinimumFontSize(realNum);
        } catch (NumberFormatException ex) {
            Toast.makeText(this, R.string.invalid_number, Toast.LENGTH_SHORT).show();
        }
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

    protected void onMessagePaneRemove(boolean reload) {
        slider.closePane();
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
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl() + "?quote", false));
                drawer.closeDrawer(navigationView);
                break;
            case R.id.nav_umail:
                Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
                startActivity(postIntent);
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
}

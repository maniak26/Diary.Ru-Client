package adonai.diary_browser;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Pair;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import adonai.diary_browser.preferences.PreferencePage;

public class DiaryAppDrawerActivity extends DiaryActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_new);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.diary_list_a, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onSearchRequested();
                return true;
            case R.id.menu_new_post:
                if (mService.mPreloadThemes)
                    handleBackground(Utils.HANDLE_PRELOAD_THEMES, null);
                else
                    //newPost("");
                return true;
            case R.id.menu_show_online_list:
                handleBackground(Utils.HANDLE_QUERY_ONLINE, null);
                return true;
            case R.id.menu_new_comment:
                //newComment("");
                return true;
            case R.id.menu_purchase:
                purchaseGift();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencePage.class));
                return true;
            case R.id.menu_share:
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_TITLE, getUser().getCurrentDiaryPage().getTitle());
                sendIntent.putExtra(Intent.EXTRA_TEXT, getUser().getCurrentDiaryPage().getPageURL());
                startActivity(Intent.createChooser(sendIntent, getString(R.string.menu_share)));
                return true;
            case R.id.copy_to_clipboard:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                Toast.makeText(getApplicationContext(), getString(R.string.copied) + " " + getUser().getCurrentDiaryPage().getPageURL(), Toast.LENGTH_SHORT).show();
                clipboard.setPrimaryClip(ClipData.newPlainText(getUser().getCurrentDiaryPage().getTitle(), getUser().getCurrentDiaryPage().getPageURL()));
                return true;
            case R.id.menu_subscr_list:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getSubscribersUrl(), false));
                return true;
            case R.id.menu_refresh:
                //reloadContent();
                return true;
            case R.id.menu_close_app:
                stopService(new Intent(this, NetworkService.class));
                finish();
                System.exit(0);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }


    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.nav_favlist:
                return true;
            case R.id.nav_fav:
                return true;
            case R.id.nav_diary:
                return true;
            case R.id.nav_discussions:
                return true;
            case R.id.nav_quotes:
                return true;
            case R.id.nav_umail:
                return true;
            case R.id.nav_settings:
                return true;
            case R.id.nav_menu_close:
                return true;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

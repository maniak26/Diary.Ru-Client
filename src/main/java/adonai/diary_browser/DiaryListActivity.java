package adonai.diary_browser;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryListArrayAdapter;
import adonai.diary_browser.entities.DiaryPage;
import adonai.diary_browser.entities.DiscListArrayAdapter;
import adonai.diary_browser.entities.DiscPage;
import adonai.diary_browser.entities.ListPage;
import adonai.diary_browser.entities.Post;
import adonai.diary_browser.preferences.PreferencesScreen;
import adonai.diary_browser.theming.HotLayoutInflater;
import adonai.diary_browser.theming.HotTheme;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class DiaryListActivity extends DiaryActivity implements OnClickListener, OnChildClickListener, OnGroupClickListener, OnItemLongClickListener, View.OnLongClickListener, PasteSelector.PasteAcceptor {
    // вкладки приложения
    public static final int TAB_FAVOURITES = 0;
    public static final int TAB_FAV_POSTS = 1;
    public static final int TAB_MY_DIARY = 2;
    public static final int TAB_DISCUSSIONS = 3;
    static final int PART_LIST = 0;
    static final int PART_WEB = 1;
    static final int PART_DISC_LIST = 2;
    public BrowseHistory browserHistory;
    int mCurrentTab = 0;
    // Адаптеры типов
    DiaryListArrayAdapter mFavouritesAdapter;
    DiscListArrayAdapter mDiscussionsAdapter;
    TextView mLogin;
    Button mDiscussNum;
    Button mCommentsNum;
    TextView mUmailNum;
    ListView mDiaryBrowser;
    ExpandableListView mDiscussionBrowser;
    ImageButton mExitButton;
    ImageButton mQuotesButton;
    ImageButton mUmailButton;
    ImageButton mScrollButton;
    LinearLayout mTabs;
    Handler mUiHandler;

    // Часть кода относится к кнопке быстрой промотки
    private Runnable fadeAnimation = new Runnable() {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(mScrollButton.getContext(), android.R.anim.fade_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mScrollButton.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mScrollButton.startAnimation(animation);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary_main);

        mainPane = (DiaryListFragment) getFragmentManager().findFragmentById(R.id.main_pane);
        messagePane = (MessageSenderFragment) getFragmentManager().findFragmentById(R.id.message_pane);

        // Оповещаем остальных, что мы создались
        // Если был простой приложения
        browserHistory = new BrowseHistory();

        mUiHandler = new Handler(this);
        CookieSyncManager.createInstance(this);

        initializeUI(mainPane.getView());
    }

    public void initializeUI(View main) {
        getActionBar().setHomeButtonEnabled(true);

        mPullToRefreshAttacher = (PullToRefreshLayout) main.findViewById(R.id.refresher_layout);
        mPageBrowser = (DiaryWebView) main.findViewById(R.id.page_browser);
        mPageBrowser.setDefaultSettings();
        mPageBrowser.setOnClickListener(this);
        registerForContextMenu(mPageBrowser);
        ActionBarPullToRefresh.from(this).allChildrenArePullable().listener(new OnRefreshListener() {
            @Override
            public void onRefreshStarted(View view) {
                if (view == mPageBrowser)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getCurrentDiaryPage().getPageURL(), true));
                if (view == mDiaryBrowser)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getCurrentDiaries().getURL(), true));
                if (view == mDiscussionBrowser)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getDiscussionsUrl(), true));
            }
        }).setup(mPullToRefreshAttacher);
        mPullToRefreshAttacher.getHeaderView().setTag(getString(R.string.tag_actionbar_style));
        HotTheme.manage(mPullToRefreshAttacher.getHeaderView());

        mLogin = (TextView) main.findViewById(R.id.login_name);

        mExitButton = (ImageButton) main.findViewById(R.id.exit_button);
        mExitButton.setOnClickListener(this);
        mQuotesButton = (ImageButton) main.findViewById(R.id.quotes_button);
        mQuotesButton.setOnClickListener(this);
        mUmailButton = (ImageButton) main.findViewById(R.id.umail_button);
        mUmailButton.setOnClickListener(this);
        mScrollButton = (ImageButton) main.findViewById(R.id.updown_button);
        mScrollButton.setOnClickListener(this);

        mDiaryBrowser = (ListView) main.findViewById(R.id.diary_browser);
        mDiaryBrowser.setOnItemLongClickListener(this);
        mDiaryBrowser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListPage diary = (ListPage) mDiaryBrowser.getAdapter().getItem(position);
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(diary.getURL(), false));
            }
        });
        mDiscussionBrowser = (ExpandableListView) main.findViewById(R.id.discussion_browser);

        mTabs = (LinearLayout) main.findViewById(R.id.tabs);
        for (int i = 0; i < mTabs.getChildCount(); i++) {
            Button current = (Button) mTabs.getChildAt(i);
            current.setOnClickListener(this);
            current.setOnLongClickListener(this);
        }

        mCommentsNum = (Button) main.findViewById(R.id.diary_button);
        mDiscussNum = (Button) main.findViewById(R.id.discussions_button);

        mUmailNum = (TextView) main.findViewById(R.id.umail_counter);
        mUmailNum.setOnClickListener(this);

        mDiscussionBrowser.setOnChildClickListener(this);
        mDiscussionBrowser.setOnGroupClickListener(this);
        mDiscussionBrowser.setOnItemLongClickListener(this);

        setCurrentVisibleComponent(0);
    }

    @Override
    protected void onDestroy() {
        mService.removeListener(this);

        super.onDestroy();

    }

    // старые телефоны тоже должны работать
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getGroupId() == DiaryListFragment.ITEM_PAGE_LINKS)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(((DiaryPage) getUser().getCurrentDiaryPage()).userLinks.get(item.getTitle()), false));


        switch (item.getItemId()) {
            case R.id.menu_new_post:
                if (mService.preload_themes)
                    handleBackground(Utils.HANDLE_PRELOAD_THEMES, null);
                else
                    newPostPost();
                return true;
            case R.id.menu_show_online_list:
                handleBackground(Utils.HANDLE_QUERY_ONLINE, null);
                return true;
            case R.id.menu_new_comment:
                newCommentPost();
                return true;
            case R.id.menu_purchase:
                purchaseGift();
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, PreferencesScreen.class));
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
            case R.id.menu_about:
                ContextThemeWrapper ctw = new ContextThemeWrapper(this, android.R.style.Theme_Black);
                AlertDialog.Builder builder = new AlertDialog.Builder(ctw);
                builder.setTitle(R.string.about);
                View aboutContent = HotLayoutInflater.from(ctw).inflate(R.layout.about_d, null);
                TextView author = (TextView) aboutContent.findViewById(R.id.author_info);
                author.setText(Html.fromHtml(getString(R.string.author_description)));
                author.setMovementMethod(LinkMovementMethod.getInstance());
                TextView app = (TextView) aboutContent.findViewById(R.id.app_info);
                app.setText(Html.fromHtml(getString(R.string.application_description)));
                app.setMovementMethod(LinkMovementMethod.getInstance());
                builder.setView(aboutContent);
                builder.create().show();
                return true;
            case R.id.menu_subscr_list:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getSubscribersUrl(), false));
                return true;
            case R.id.menu_refresh:
                reloadContent();
                return true;
            case R.id.menu_close_app:
                stopService(new Intent(this, NetworkService.class));
                finish();
                System.exit(0);
                return true;
            case android.R.id.home:
                return onSearchRequested();
            case R.id.menu_special_paste:
                DialogFragment newFragment = PasteSelector.newInstance();
                newFragment.show(getFragmentManager(), "selector");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (v.getId() == R.id.page_browser) {
            Message msg = Message.obtain(mUiHandler, Utils.HANDLE_IMAGE_CLICK);
            mPageBrowser.requestImageRef(msg);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().hasExtra("url")) {
            pageToLoad = getIntent().getStringExtra("url");
            getIntent().removeExtra("url");
        } else if (getIntent().getData() != null) {
            pageToLoad = getIntent().getDataString();
            getIntent().setData(null);
            if (mService != null) // if ativity is already running, just redirect to needed page
                mUiHandler.sendEmptyMessage(Utils.HANDLE_START);
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case Utils.HANDLE_START:
                mService.addListener(this);
                if (pageToLoad != null) {
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(pageToLoad, false));
                    pageToLoad = null;
                } else if (browserHistory.isEmpty()) // запускаем в первый раз
                    setCurrentTab(TAB_FAVOURITES, false);
                return true;
            case Utils.HANDLE_PROGRESS:
                if (pd != null)
                    pd.setMessage(getString(R.string.parsing_data));
                return true;
            case Utils.HANDLE_PROGRESS_2:
                if (pd != null)
                    pd.setMessage(getString(R.string.sorting_data));
                return true;
            case Utils.HANDLE_UPDATE_HEADERS:
                // обрабатываем обновление контента
                mLogin.setText(getUser().getUserName());
                if (getUser().getNewDiaryCommentsNum() != 0)
                    mCommentsNum.setText(getString(R.string.my_diary) + " - " + getUser().getNewDiaryCommentsNum().toString());
                else
                    mCommentsNum.setText(getString(R.string.my_diary));

                if (getUser().getNewDiscussNum() != 0)
                    mDiscussNum.setText(getString(R.string.discussions) + " - " + getUser().getNewDiscussNum());
                else
                    mDiscussNum.setText(getString(R.string.discussions));

                if (getUser().getNewUmailNum() != 0) {
                    mUmailNum.setText(getUser().getNewUmailNum().toString());
                    mUmailNum.setVisibility(View.VISIBLE);
                } else {
                    mUmailNum.setText("");
                    mUmailNum.setVisibility(View.GONE);
                }
                return true;
            case Utils.HANDLE_SET_HTTP_COOKIE: // успешно авторизовались
                pd.setMessage(getString(R.string.getting_user_info));
                return true;
            case Utils.HANDLE_GET_LIST_PAGE_DATA:
                setCurrentVisibleComponent(PART_LIST);
                mDiaryBrowser.setAdapter(null);
                mDiaryBrowser.removeFooterView(mDiaryBrowser.findViewWithTag("footer"));
                mFavouritesAdapter = new DiaryListArrayAdapter(DiaryListActivity.this, android.R.layout.simple_list_item_1, getUser().getCurrentDiaries());
                mDiaryBrowser.setAdapter(mFavouritesAdapter);
                if (getUser().getCurrentDiaries().getPageLinks() != null) {
                    LinearLayout LL = new LinearLayout(mDiaryBrowser.getContext());
                    LL.setTag("footer");
                    Spanned pageLinks = getUser().getCurrentDiaries().getPageLinks();
                    URLSpan[] URLs = pageLinks.getSpans(0, pageLinks.length(), URLSpan.class);
                    for (URLSpan url : URLs) {
                        Button click = new Button(LL.getContext());
                        click.setMaxLines(1);
                        click.setText(pageLinks.subSequence(pageLinks.getSpanStart(url), pageLinks.getSpanEnd(url)).toString());
                        click.setTag(getString(R.string.tag_button_style));
                        click.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        click.setTag(R.integer.button_url, url.getURL());
                        click.setOnClickListener(DiaryListActivity.this);
                        LL.addView(click);

                        LayoutParams LP = (LayoutParams) click.getLayoutParams();
                        LP.width = 0;
                        LP.height = LayoutParams.MATCH_PARENT;
                        LP.weight = 1.0f;
                    }
                    mDiaryBrowser.addFooterView(LL);
                    HotTheme.manage(LL);
                }
                browserHistory.add(getUser().getCurrentDiaries().getURL());
                handleTabChange(getUser().getCurrentDiaries().getURL());
                mPullToRefreshAttacher.setRefreshComplete();

                // На Андроиде > 2.3.3 нужно обновлять меню для верного отображения нужных для страниц кнопок
                invalidateOptionsMenu(); // PART_LIST
                break;
            case Utils.HANDLE_GET_WEB_PAGE_DATA: // the most important part!
                setCurrentVisibleComponent(PART_WEB);
                if (message.obj == null) { // это страница
                    mPageBrowser.loadDataWithBaseURL(getUser().getCurrentDiaryPage().getPageURL(), getUser().getCurrentDiaryPage().getContent(), null, "utf-8", getUser().getCurrentDiaryPage().getPageURL());

                    browserHistory.add(getUser().getCurrentDiaryPage().getPageURL());
                    handleTabChange(getUser().getCurrentDiaryPage().getPageURL());

                    setTitle(getUser().getCurrentDiaryPage().getTitle());
                    if (getUser().getCurrentDiaryPage().getClass() == DiaryPage.class)
                        mDatabase.addAutocompleteText(DatabaseHandler.AutocompleteType.URL, getUser().getCurrentDiaryPage().getPageURL(), getUser().getCurrentDiaryPage().getTitle());
                } else { // это картинка
                    String src = (String) message.obj;
                    mPageBrowser.loadUrl(src);
                    browserHistory.add(src);
                }
                mPullToRefreshAttacher.setRefreshComplete();

                invalidateOptionsMenu(); // PART_WEB
                break;
            case Utils.HANDLE_GET_DISCUSSIONS_DATA:
                setCurrentVisibleComponent(PART_DISC_LIST);
                mDiscussionsAdapter = new DiscListArrayAdapter(this, getUser().getDiscussions());
                mDiscussionBrowser.setAdapter(mDiscussionsAdapter);

                browserHistory.add(getUser().getDiscussions().getURL());
                handleTabChange(getUser().getDiscussions().getURL());

                invalidateOptionsMenu(); // PART_DISC_LIST
                break;
            case Utils.HANDLE_AUTHORIZATION_ERROR:
                mPullToRefreshAttacher.setRefreshComplete();
                Toast.makeText(getApplicationContext(), getString(R.string.not_authorized), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                finish();
                break;
            case Utils.HANDLE_GET_DISCUSSION_LIST_DATA:
                int pos = (Integer) message.obj;
                mDiscussionBrowser.expandGroup(pos);
                break;
            case Utils.HANDLE_NAME_CLICK: {
                String href = message.getData().getString("url");
                if (href != null && href.contains("#form") && slider.isDouble()) {
                    slider.openPane();
                    String name = message.getData().getString("title");
                    if (name != null)
                        messagePane.contentText.setText(messagePane.contentText.getText() + "[L]" + name + "[/L], ");
                }
                return true;
            }
            case Utils.HANDLE_IMAGE_CLICK: {
                final String src = message.getData().getString("url");
                if (src == null) // нет картинки!
                    return false;

                ArrayList<String> itemsBuilder = new ArrayList<>();
                itemsBuilder.add(getString(R.string.image_save));
                itemsBuilder.add(getString(R.string.image_copy_url));
                itemsBuilder.add(getString(R.string.image_open));

                final String[] items = itemsBuilder.toArray(new String[itemsBuilder.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
                builder.setTitle(R.string.image_action);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @SuppressWarnings("deprecation")
                    public void onClick(DialogInterface dialog, int item) {
                        switch (item) {
                            case DiaryWebView.IMAGE_SAVE: // save
                            {
                                Toast.makeText(DiaryListActivity.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
                                mService.handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(src, true));
                            }
                            break;
                            case DiaryWebView.IMAGE_COPY_URL: // copy
                            {
                                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                Toast.makeText(DiaryListActivity.this, getString(R.string.copied) + " " + src, Toast.LENGTH_SHORT).show();
                                clipboard.setText(src);
                            }
                            break;
                            case DiaryWebView.IMAGE_OPEN: // open Link
                            {
                                Toast.makeText(DiaryListActivity.this, getString(R.string.loading), Toast.LENGTH_SHORT).show();
                                mService.handleRequest(Utils.HANDLE_PICK_URL, new Pair<>(src, false));
                            }
                            break;
                        }
                    }
                });
                builder.create().show();
                break;
            }
            case Utils.HANDLE_EDIT_POST:
                Post sendPost = (Post) message.obj;
                editPost(sendPost);
                break;
            case Utils.HANDLE_EDIT_COMMENT:
                Comment sendComment = (Comment) message.obj;
                editComment(sendComment);
                break;
            case Utils.HANDLE_REPOST:
                Post repost = (Post) message.obj;
                newPostPost(repost);
                break;
            case Utils.HANDLE_PRELOAD_THEMES:
                Post newPost = (Post) message.obj;
                newPostPost(newPost);
                break;
            case Utils.HANDLE_QUERY_ONLINE:
                HashMap<Integer, Spanned> onliners = (HashMap<Integer, Spanned>) message.obj;
                if(onliners.isEmpty()) {
                    Toast.makeText(DiaryListActivity.this, getString(R.string.nobody_here), Toast.LENGTH_SHORT).show();
                    break;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.whos_online);
                View aboutContent = HotLayoutInflater.from(this).inflate(R.layout.whos_online_d, null);
                TextView favs = (TextView) aboutContent.findViewById(R.id.favs_online);
                favs.setMovementMethod(LinkMovementMethod.getInstance());
                if(onliners.containsKey(R.string.favourites_online)) {
                    favs.setText(onliners.get(R.string.favourites_online));
                }
                TextView subs = (TextView) aboutContent.findViewById(R.id.subs_online);
                favs.setMovementMethod(LinkMovementMethod.getInstance());
                if(onliners.containsKey(R.string.subscribers_online)) {
                    subs.setText(onliners.get(R.string.subscribers_online));
                }
                builder.setView(aboutContent);
                builder.create().show();
                break;
        }

        super.handleMessage(message);
        return true;
    }

    @Override
    protected void onMessagePaneRemove(boolean reload) {
        super.onMessagePaneRemove(reload);
        if (reload)
            reloadContent();
    }

    private void handleTabChange(String url) {
        // Обработка случая, когда URL страницы совпадает с URL одного из табов
        if (url.equals(getUser().getFavoritesUrl())) {
            setTitle(R.string.title_activity_diary_list);
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = 0;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        } else if (url.equals(getUser().getOwnDiaryUrl() + "?favorite")) {
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = 1;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        } else if (url.equals(getUser().getOwnDiaryUrl()) || getUser().getNewDiaryLink().startsWith(url)) {
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = 2;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        } else if (url.equals(getUser().getDiscussionsUrl()) || getUser().getNewDiscussLink().startsWith(url)) {
            mTabs.getChildAt(mCurrentTab).setSelected(false);
            mCurrentTab = 3;
            mTabs.getChildAt(mCurrentTab).setSelected(true);
        }
    }

    // Часть кода относится к кнопке быстрой промотки
    void handleScroll(int direction) {
        mScrollButton.setVisibility(View.VISIBLE);
        mScrollButton.removeCallbacks(fadeAnimation);
        mScrollButton.clearAnimation();
        mScrollButton.postDelayed(fadeAnimation, 2000);
        switch (direction) {
            case Utils.VIEW_SCROLL_DOWN:
                mScrollButton.setImageResource(R.drawable.overscroll_button_down);
                break;
            case Utils.VIEW_SCROLL_UP:
                mScrollButton.setImageResource(R.drawable.overscroll_button_up);
                break;
        }

    }

    public void onClick(View view) {
        if (view == mExitButton) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mPageBrowser.getContext());
            builder.setTitle(R.string.really_exit);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    Editor lysosome = mService.mPreferences.edit();
                    lysosome.remove(Utils.KEY_USERNAME);
                    lysosome.remove(Utils.KEY_PASSWORD);
                    lysosome.commit();

                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeSessionCookie();
                    CookieSyncManager.getInstance().sync();
                    mService.newSession();

                    //TODO: просмотр без логина тоже еще не введен
                    startActivity(new Intent(getApplicationContext(), AuthorizationForm.class));
                    finish();
                }
            });
            builder.setNegativeButton(android.R.string.no, null);
            builder.create().show();
        } else if (view == mQuotesButton) {
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl() + "?quote", false));
        } else if (view == mUmailButton) {
            Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
            startActivity(postIntent);
        } else if (view == mScrollButton) {
            // Офигительная штука, документации по которой нет.
            // Устанавливает начальную скорость скролла даже если в данный момент уже происходит скроллинг
            if (mPageBrowser.scrolling == Utils.VIEW_SCROLL_DOWN)
                mPageBrowser.flingScroll(0, 100000);
            else
                mPageBrowser.flingScroll(0, -100000);
        } else if (view == mUmailNum) {
            Intent postIntent = new Intent(getApplicationContext(), UmailListActivity.class);
            postIntent.putExtra("url", getUser().getNewUmailLink());
            startActivity(postIntent);
        } else if (view.getParent() == mTabs)   // Если это кнопка табов
        {
            setCurrentTab(mTabs.indexOfChild(view), false);
        } else if (view.getTag(R.integer.button_url) != null)  // нижние кнопки списков
        {
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>((String) view.getTag(R.integer.button_url), false));
        }
    }

    @Override
    public boolean onLongClick(View view) {
        // по долгому клику принудительно входим в дневник/дискуссии без читки новых сообщений
        if (view.getParent() == mTabs)
            setCurrentTab(mTabs.indexOfChild(view), true);

        return true;
    }

    // Загружаем дискуссии
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        String link = ((DiscPage.Discussion) parent.getExpandableListAdapter().getChild(groupPosition, childPosition)).URL;
        handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(link, false));
        return true;
    }

    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (parent.isGroupExpanded(groupPosition)) {
            parent.collapseGroup(groupPosition);
            return true;
        }

        if (((DiscPage) parent.getExpandableListAdapter().getGroup(groupPosition)).getDiscussions().isEmpty()) {
            ArrayList<Object> params = new ArrayList<>();
            params.add(groupPosition);
            params.add(mDiscussionBrowser.getExpandableListAdapter().getGroup(groupPosition));
            params.add(false);
            handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, params);
        } else
            parent.expandGroup(groupPosition);
        return true;
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDiscussionBrowser) {
            ExpandableListView elv = (ExpandableListView) parent;
            if (ExpandableListView.getPackedPositionType(id) != ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                return false;

            int groupPosition = ExpandableListView.getPackedPositionGroup(id);
            if (elv.isGroupExpanded(groupPosition)) {
                elv.collapseGroup(groupPosition);
                return true;
            }
            ArrayList<Object> params = new ArrayList<>();
            params.add(groupPosition);
            params.add(mDiscussionBrowser.getExpandableListAdapter().getGroup(groupPosition));
            params.add(true);
            handleBackground(Utils.HANDLE_GET_DISCUSSION_LIST_DATA, params);
            return true;
        }

        if (parent == mDiaryBrowser) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            ListPage diary = (ListPage) mDiaryBrowser.getAdapter().getItem(position);
            builder.setMessage(diary.getPageHint()).create().show();
        }

        return true;
    }

    /*            is.close();
     * (non-Javadoc) Sets the contents to current tab and hides everything other. In addition, refreshes content on
     * page, if needed.
     */
    private void setCurrentTab(int index, boolean force) {
        switch (index) {
            case TAB_FAVOURITES:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getFavoritesUrl(), false));
                break;
            case TAB_FAV_POSTS:
                handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl() + "?favorite", false));
                break;
            case TAB_MY_DIARY:
                if (getUser().getNewDiaryCommentsNum() != 0 && !force)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getNewDiaryLink(), true));
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getOwnDiaryUrl(), false));
                break;
            case TAB_DISCUSSIONS:
                if (getUser().getNewDiscussNum() != 0 && !force)
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getNewDiscussLink(), true));
                else
                    handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getDiscussionsUrl(), false));
                break;
            default:
                Utils.showDevelSorry(this);
                break;
        }
    }

    private void setCurrentVisibleComponent(int needed) {
        mDiaryBrowser.setVisibility(needed == PART_LIST ? View.VISIBLE : View.GONE);
        mPageBrowser.setVisibility(needed == PART_WEB ? View.VISIBLE : View.GONE);
        //mAuthorBrowser.setVisibility(needed == AUTHOR_PAGE ? View.VISIBLE : View.GONE);
        mDiscussionBrowser.setVisibility(needed == PART_DISC_LIST ? View.VISIBLE : View.GONE);

        mainPane.mCurrentComponent = needed;
    }

    private void reloadContent() {
        if (mainPane.mCurrentComponent == PART_WEB)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getCurrentDiaryPage().getPageURL(), true));
        else if (mainPane.mCurrentComponent == PART_LIST)
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(getUser().getFavoritesUrl(), true));
    }

    @Override
    public void onBackPressed() {
        if (slider.isOpen())
            slider.closePane();
        else if (browserHistory.hasPrevious()) {
            browserHistory.moveBack();
            handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(browserHistory.getUrl(), false));
        } else
            finish();
    }

    @Override
    public boolean onSearchRequested() {
        int visibility = mTabs.getVisibility();
        if (visibility == View.GONE) {
            mainPane.getView().findViewById(R.id.upperDeck).setVisibility(View.VISIBLE);
            mTabs.setVisibility(View.VISIBLE);
        } else {
            mainPane.getView().findViewById(R.id.upperDeck).setVisibility(View.GONE);
            mTabs.setVisibility(View.GONE);
        }

        return super.onSearchRequested();
    }

    public void newPostPost() {
        if (!(getUser().getCurrentDiaryPage() instanceof DiaryPage))
            return;

        if (((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryID().equals(""))
            return;

        Post post = new Post();
        post.diaryID = ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryID();

        messagePane.prepareFragment(getUser().getSignature(), ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryURL() + "diary.php", post);
        slider.openPane();
    }

    public void newPostPost(Post post) {
        if (!(getUser().getCurrentDiaryPage() instanceof DiaryPage))
            return;

        if (((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryID().equals(""))
            return;

        messagePane.prepareFragment(getUser().getSignature(), ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryURL() + "diary.php", post);
        slider.openPane();
    }

    public void newCommentPost() {
        if (!(getUser().getCurrentDiaryPage() instanceof CommentsPage))
            return;

        if (((CommentsPage) getUser().getCurrentDiaryPage()).getPostID().equals(""))
            return;

        Comment comment = new Comment();
        comment.postID = ((CommentsPage) getUser().getCurrentDiaryPage()).getPostID();

        messagePane.prepareFragment(getUser().getSignature(), ((CommentsPage) getUser().getCurrentDiaryPage()).getDiaryURL() + "diary.php", comment);
        slider.openPane();
    }

    public void editPost(Post post) {
        messagePane.prepareFragment(getUser().getSignature(), ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryURL() + "diary.php", post);
        slider.openPane();
    }

    public void editComment(Comment comment) {
        messagePane.prepareFragment(getUser().getSignature(), ((DiaryPage) getUser().getCurrentDiaryPage()).getDiaryURL() + "diary.php", comment);
        slider.openPane();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void acceptDialogClick(View view, boolean pasteClipboard) {
        messagePane.acceptDialogClick(view, pasteClipboard);
    }
}
package adonai.diary_browser;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import adonai.diary_browser.database.DatabaseHandler;
import adonai.diary_browser.entities.CommentsPage;
import adonai.diary_browser.entities.DiaryPage;

/**
 * Фрагмент основной активности дайри, отвечающий за обработку пунктов меню и кнопок {@link ActionBar}'a
 * <br/>
 * По историческим причинам логика всего контента, который присутствует в этом фрагменте, обрабатывается в активности.
 * 
 * @see DiaryListActivity
 * 
 * @author Адонай
 */
public class DiaryListFragment extends DiaryFragment {
    
    public final static int GROUP_PAGE_LINKS = 100;
    public final static int ITEM_PAGE_LINKS = 101;
    
    private final URLAutocompleteQueryListener mUrlListener = new URLAutocompleteQueryListener();
    private final URLAutocompleteSuggestionListener mUrlSuggestionListener = new URLAutocompleteSuggestionListener();
    private URLAutocompleteAdapter mUrlAdapter; // created on attach to activity

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mUrlAdapter = new URLAutocompleteAdapter(getActivity(), getDiaryActivity().mDatabase.getAutocompleteCursor(DatabaseHandler.AutocompleteType.URL, ""));

        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.fragment_diary_list, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.diary_list_a, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        UserData mUser = ((DiaryActivity) getActivity()).getUser();

        if (mUser == null) // not initialized, hide all
        {
            for (int i = 0; i < menu.size(); ++i)
                menu.getItem(i).setVisible(false);
            return;
        }

        // Только если это дневник
        if (mCurrentComponent == DiaryListActivity.PART_WEB && mUser.getCurrentDiaryPage().getClass().equals(DiaryPage.class)) {
            menu.findItem(R.id.menu_new_post).setVisible(true);
        } else {
            menu.findItem(R.id.menu_new_post).setVisible(false);
        }

        // Только если это пост
        if (mCurrentComponent == DiaryListActivity.PART_WEB && mUser.getCurrentDiaryPage().getClass().equals(CommentsPage.class))
            menu.findItem(R.id.menu_new_comment).setVisible(true);
        else
            menu.findItem(R.id.menu_new_comment).setVisible(false);


        // Для всех веб-страничек
        if (mCurrentComponent == DiaryListActivity.PART_WEB)
            menu.setGroupVisible(R.id.group_web, true);
        else
            menu.setGroupVisible(R.id.group_web, false);

        // Для всех списков
        if (mCurrentComponent == DiaryListActivity.PART_LIST) {
            menu.findItem(R.id.menu_share).setVisible(false);
            menu.findItem(R.id.menu_subscr_list).setVisible(true);
            menu.findItem(R.id.menu_manual_input).setVisible(true);
            final SearchView searchView = (SearchView) menu.findItem(R.id.menu_manual_input).getActionView();
            searchView.setQueryHint(getString(R.string.url_hint));
            searchView.setSuggestionsAdapter(mUrlAdapter);
            searchView.setOnQueryTextListener(mUrlListener);
            searchView.setOnSuggestionListener(mUrlSuggestionListener);


            //final ImageView v = (ImageView) searchView.findViewById(Resources.getSystem().getIdentifier("search_button", "id", "android"));
            final ImageView v = (ImageView) searchView.findViewById(R.id.search_button);
            v.setImageResource(android.R.drawable.ic_menu_edit);

            /*
            //final EditText text = (EditText) searchView.findViewById(Resources.getSystem().getIdentifier("search_src_text", "id", "android"));
            final EditText text = (EditText) searchView.findViewById(R.id.search_src_text);
            text.setCursorVisible(false);
            text.setHintTextColor(Color.LTGRAY);
            text.setTextColor(Color.WHITE);
            */
        } else {
            menu.findItem(R.id.menu_share).setVisible(true);
            menu.findItem(R.id.menu_subscr_list).setVisible(false);
            menu.findItem(R.id.menu_manual_input).setVisible(false);
        }

        menu.removeGroup(GROUP_PAGE_LINKS);
        // добавляем ссылки дневника, если они есть
        if (mUser.getCurrentDiaryPage() instanceof DiaryPage) {
            final SubMenu linksMenu = menu.addSubMenu(GROUP_PAGE_LINKS, 0, 0, R.string.diary_links);
            for (String linkName : ((DiaryPage) mUser.getCurrentDiaryPage()).userLinks.keySet())
                linksMenu.add(ITEM_PAGE_LINKS, 0, 0, linkName);
        }
    }

    private String convertToSearchQuery(String somethingLikeURL) {
        try {
            if (!somethingLikeURL.startsWith("http")) {
                if (somethingLikeURL.contains("diary.ru"))
                    somethingLikeURL = "http://" + somethingLikeURL;
                else
                    somethingLikeURL = "http://diary.ru/search/?q=" + URLEncoder.encode(somethingLikeURL, "windows-1251");
            }
        } catch (UnsupportedEncodingException ignored) {
        }
        return somethingLikeURL;
    }

    private class URLAutocompleteAdapter extends CursorAdapter {

        public URLAutocompleteAdapter(Context context, Cursor c) {
            super(context, c, true);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.panel_background, typedValue, true);
            view.setBackgroundColor(typedValue.data);
            final TextView urlText = (TextView) view.findViewById(android.R.id.text1);
            final TextView caption = (TextView) view.findViewById(android.R.id.text2);

            urlText.setText(cursor.getString(1));
            if (!cursor.isNull(2))
                caption.setText(cursor.getString(2));
        }
    }

    private class URLAutocompleteQueryListener implements SearchView.OnQueryTextListener {

        @Override
        public boolean onQueryTextSubmit(String query) {
            getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(convertToSearchQuery(query), false));
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            if (newText.length() > 0)
                mUrlAdapter.changeCursor(getDiaryActivity().mDatabase.getAutocompleteCursor(DatabaseHandler.AutocompleteType.URL, newText));
            return true;
        }
    }

    private class URLAutocompleteSuggestionListener implements SearchView.OnSuggestionListener {
        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            final Cursor cur = (Cursor) mUrlAdapter.getItem(position);
            final String url = cur.getString(1);
            getDiaryActivity().handleBackground(Utils.HANDLE_PICK_URL, new Pair<>(url, false));
            return true;
        }
    }
}

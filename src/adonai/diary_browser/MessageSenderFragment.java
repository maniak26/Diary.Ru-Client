package adonai.diary_browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import adonai.diary_browser.entities.Comment;
import adonai.diary_browser.entities.Post;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MessageSenderFragment extends Fragment implements OnClickListener, android.widget.CompoundButton.OnCheckedChangeListener, android.widget.RadioGroup.OnCheckedChangeListener
{

    private static final int HANDLE_DO_POST 		= 0;
    private static final int HANDLE_DO_COMMENT 		= 1;
    private static final int HANDLE_DO_UMAIL 		= 2;
    private static final int HANDLE_UMAIL_ACK 		= 3;
    private static final int HANDLE_UMAIL_REJ 		= 4;
    private static final int HANDLE_REQUEST_AVATARS = 5;
    private static final int HANDLE_SET_AVATAR      = 6;
    private static final int HANDLE_PROGRESS        = 8;

    ImageButton mLeftGradient;
    ImageButton mRightGradient;
    Button mSetGradient;

    EditText toText;
    EditText titleText;
    EditText contentText;
    EditText themesText;
    EditText musicText;
    EditText moodText;
    Button mPublish;
    CheckBox mShowOptionals;
    CheckBox mShowPoll;
    CheckBox mSubscribe;
    CheckBox mShowAndClose;
    CheckBox mGetReceipt;
    CheckBox mCopyMessage;
    CheckBox mCustomAvatar;
    TextView mTitle;
    TextView mCurrentPage;

    EditText mPollTitle;
    EditText mPollChoice1;
    EditText mPollChoice2;
    EditText mPollChoice3;
    EditText mPollChoice4;
    EditText mPollChoice5;
    EditText mPollChoice6;
    EditText mPollChoice7;
    EditText mPollChoice8;
    EditText mPollChoice9;
    EditText mPollChoice10;

    EditText mCloseAllowList;
    EditText mCloseDenyList;
    EditText mCloseText;

    RadioGroup mCloseOpts;

    Handler mHandler, mUiHandler;
    Looper mLooper;
    ProgressDialog pd;

    LinearLayout mAvatars;
    List<View> postElements = new ArrayList<View>();
    List<View> commentElements = new ArrayList<View>();
    List<View> umailElements = new ArrayList<View>();

    List<View> optionals = new ArrayList<View>();
    List<View> pollScheme = new ArrayList<View>();
    List<NameValuePair> postParams;

    String mSignature;
    String mId;
    String mTypeId;
    NetworkService mService;

    SparseArray<Object> avatarMap;

    DiaryHttpClient mDHCL;
    String mSendURL;
    Comment mPost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.Classic);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View sender = localInflater.inflate(R.layout.fragment_message_sender, container, false);
        mPost = new Post();
        postParams = new ArrayList<NameValuePair>();

        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, HttpCallback);
        mUiHandler = new Handler(UiCallback);

        toText = (EditText) sender.findViewById(R.id.message_to);
        mGetReceipt = (CheckBox) sender.findViewById(R.id.message_getreceipt);
        mCopyMessage = (CheckBox) sender.findViewById(R.id.message_copy);

        titleText = (EditText) sender.findViewById(R.id.message_title);
        contentText = (EditText) sender.findViewById(R.id.message_content);
        themesText = (EditText) sender.findViewById(R.id.message_themes);
        musicText = (EditText) sender.findViewById(R.id.message_music);
        moodText = (EditText) sender.findViewById(R.id.message_mood);
        mPublish = (Button) sender.findViewById(R.id.message_publish);
        mPublish.setOnClickListener(this);
        mTitle = (TextView) sender.findViewById(R.id.fragment_title);
        mCurrentPage = (TextView) sender.findViewById(R.id.fragment_page);

        mLeftGradient = (ImageButton) sender.findViewById(R.id.left_gradient);
        mLeftGradient.setOnClickListener(this);
        mRightGradient = (ImageButton) sender.findViewById(R.id.right_gradient);
        mRightGradient.setOnClickListener(this);
        mSetGradient = (Button) sender.findViewById(R.id.set_gradient);
        mSetGradient.setOnClickListener(this);

        mPollTitle = (EditText) sender.findViewById(R.id.message_poll_title);
        mPollChoice1 = (EditText) sender.findViewById(R.id.message_poll_1);
        mPollChoice2 = (EditText) sender.findViewById(R.id.message_poll_2);
        mPollChoice3 = (EditText) sender.findViewById(R.id.message_poll_3);
        mPollChoice4 = (EditText) sender.findViewById(R.id.message_poll_4);
        mPollChoice5 = (EditText) sender.findViewById(R.id.message_poll_5);
        mPollChoice6 = (EditText) sender.findViewById(R.id.message_poll_6);
        mPollChoice7 = (EditText) sender.findViewById(R.id.message_poll_7);
        mPollChoice8 = (EditText) sender.findViewById(R.id.message_poll_8);
        mPollChoice9 = (EditText) sender.findViewById(R.id.message_poll_9);
        mPollChoice10 = (EditText) sender.findViewById(R.id.message_poll_10);

        mCloseOpts = (RadioGroup) sender.findViewById(R.id.close_opts);
        mCloseOpts.setOnCheckedChangeListener(this);
        mCloseAllowList = (EditText) sender.findViewById(R.id.close_allowed_list);
        mCloseDenyList = (EditText) sender.findViewById(R.id.close_denied_list);
        mCloseText = (EditText) sender.findViewById(R.id.close_text);

        mCustomAvatar = (CheckBox) sender.findViewById(R.id.message_custom_avatar);
        mAvatars = (LinearLayout) sender.findViewById(R.id.message_avatars);
        mCustomAvatar.setOnCheckedChangeListener(this);

        mShowOptionals = (CheckBox) sender.findViewById(R.id.message_optional);
        mShowOptionals.setOnCheckedChangeListener(this);
        mShowPoll = (CheckBox) sender.findViewById(R.id.message_poll);
        mShowPoll.setOnCheckedChangeListener(this);
        mSubscribe = (CheckBox) sender.findViewById(R.id.message_subscribe);
        mShowAndClose = (CheckBox) sender.findViewById(R.id.message_close);
        mShowAndClose.setOnCheckedChangeListener(this);

        optionals.add(sender.findViewById(R.id.message_themes_hint));
        optionals.add(themesText);
        optionals.add(sender.findViewById(R.id.message_music_hint));
        optionals.add(musicText);
        optionals.add(sender.findViewById(R.id.message_mood_hint));
        optionals.add(moodText);

        pollScheme.add(mPollTitle);
        pollScheme.add(mPollChoice1);
        pollScheme.add(mPollChoice2);
        pollScheme.add(mPollChoice3);
        pollScheme.add(mPollChoice4);
        pollScheme.add(mPollChoice5);
        pollScheme.add(mPollChoice6);
        pollScheme.add(mPollChoice7);
        pollScheme.add(mPollChoice8);
        pollScheme.add(mPollChoice9);
        pollScheme.add(mPollChoice10);

        commentElements.add(sender.findViewById(R.id.message_content_hint));
        commentElements.add(sender.findViewById(R.id.message_specials));
        commentElements.add(contentText);
        commentElements.add(mSubscribe);

        postElements.add(sender.findViewById(R.id.message_title_hint));
        postElements.add(titleText);
        postElements.add(sender.findViewById(R.id.message_content_hint));
        postElements.add(sender.findViewById(R.id.message_specials));
        postElements.add(contentText);
        postElements.add(mShowOptionals);
        postElements.add(mShowAndClose);
        postElements.add(mShowPoll);

        umailElements.add(sender.findViewById(R.id.message_to_hint));
        umailElements.add(toText);
        umailElements.add(sender.findViewById(R.id.message_title_hint));
        umailElements.add(titleText);
        umailElements.add(sender.findViewById(R.id.message_content_hint));
        umailElements.add(sender.findViewById(R.id.message_specials));
        umailElements.add(contentText);
        umailElements.add(mGetReceipt);
        umailElements.add(mCopyMessage);

        return sender;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.message_sender_a, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroyView()
    {
        mLooper.quit();
        super.onDestroyView();
    }

    Handler.Callback HttpCallback = new Handler.Callback()
    {
        @SuppressWarnings("unchecked")
        public boolean handleMessage(Message message)
        {
            try
            {
                switch (message.what)
                {
                    case HANDLE_DO_POST:
                    case HANDLE_DO_COMMENT:
                    {
                        mDHCL.postPage(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        mUiHandler.sendEmptyMessage(message.what);
                        return true;
                    }
                    case HANDLE_DO_UMAIL:
                    {
                        HttpResponse page = mDHCL.postPage(mSendURL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        String result = EntityUtils.toString(page.getEntity());
                        if(result.contains("Письмо отправлено"))
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_ACK);
                        else
                            mUiHandler.sendEmptyMessage(HANDLE_UMAIL_REJ);
                        return true;
                    }
                    case HANDLE_REQUEST_AVATARS:
                    {
                        String URL = "http://www.diary.ru/options/member/?avatar";
                        HttpResponse page = mDHCL.postPage(URL, null);
                        if(page == null)
                            return false;

                        // собираем пары ID аватара - URL аватара
                        String dataPage = EntityUtils.toString(page.getEntity());
                        Elements avatardivs = Jsoup.parse(dataPage).select("div#avatarbit");
                        avatarMap = new SparseArray<Object>();
                        for(Element avatarbit : avatardivs)
                        {
                            Integer avId = Integer.valueOf(avatarbit.select("input[name=use_avatar_id]").val());
                            String url = avatarbit.child(0).attr("style");
                            url = url.substring(url.lastIndexOf('(') + 1, url.lastIndexOf(')'));
                            avatarMap.put(avId, url);
                        }

                        // распараллеливаем получение аватарок
                        // в массиве теперь будет храниться ID аватара - задача загрузки аватара
                        ExecutorService executor = Executors.newFixedThreadPool(avatarMap.size());
                        for(int i = 0; i < avatarMap.size(); i++)
                        {
                            final String url = (String) avatarMap.valueAt(i);
                            FutureTask<Drawable> future = new FutureTask<Drawable>(new Callable<Drawable>()
                            {
                                @Override
                                public Drawable call() throws Exception
                                {
                                    HttpResponse page = mDHCL.getPage(url);
                                    InputStream is = page.getEntity().getContent();
                                    return BitmapDrawable.createFromStream(is, url);
                                }

                            });
                            avatarMap.setValueAt(i, future);
                            executor.execute(future);
                        }

                        // по мере выполнения задач переписываем массив по кусочкам на результат задачи
                        while(true)
                        {
                            int remaining = 0;
                            for(int i = 0; i < avatarMap.size(); i++)
                            {
                                if(avatarMap.valueAt(i) instanceof FutureTask)
                                {
                                    FutureTask<Drawable> future = (FutureTask<Drawable>) avatarMap.valueAt(i);
                                if(future.isDone())
                                    avatarMap.setValueAt(i, future.get());
                                else
                                    remaining++;
                                }
                            }
                            if(remaining == 0)
                                break;
                        }

                        mUiHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                        return true;
                    }
                    case HANDLE_SET_AVATAR:
                    {
                        String URL = "http://www.diary.ru/options/member/?avatar";
                        mDHCL.postPage(URL, new UrlEncodedFormEntity(postParams, "WINDOWS-1251"));
                        Toast.makeText(getActivity(), R.string.avatar_selected, Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    case Utils.HANDLE_UPLOAD_FILE:
                    {
                        try
                        {
                            File file = new File((String) message.obj);
                            final long length = file.length();
                            MultipartEntity mpEntity = new MultipartEntity();
                            ContentBody cbFile = new DiaryHttpClient.CountingFileBody(file, "image/*", new DiaryHttpClient.ProgressListener()
                            {
                                long decade;

                                @Override
                                public void transferred(long transferredBytes)
                                {
                                    long percent = (transferredBytes * 100) / length;
                                    mUiHandler.sendMessage(mUiHandler.obtainMessage(HANDLE_PROGRESS, (int)percent));
                                }
                            });
                            mpEntity.addPart("module", new StringBody("photolib"));
                            mpEntity.addPart("signature", new StringBody(mSignature));
                            mpEntity.addPart("resulttype1", new StringBody(String.valueOf(message.arg1)));
                            mpEntity.addPart("attachment1", cbFile);

                            HttpResponse response = mDHCL.postPage(mSendURL.substring(0, mSendURL.lastIndexOf('/') + 1) + "diary.php?upload=1&js", mpEntity);
                            HttpEntity resEntity = response.getEntity();
                            if (resEntity != null)
                            {
                                String result = EntityUtils.toString(resEntity);
                                result = result.substring(result.indexOf("'") + 1, result.indexOf("';"));
                                if(result.length() > 0)
                                {
                                    mUiHandler.sendMessage(mUiHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, result));
                                    pd.dismiss();
                                }
                                else
                                    Toast.makeText(getActivity(), getString(R.string.message_send_error), Toast.LENGTH_LONG).show();
                                //resEntity.consumeContent();
                            }
                        } catch (Exception e)
                        {
                            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    default:
                        break;
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return false;
        }
    };

    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
                case HANDLE_DO_POST:
                case HANDLE_DO_COMMENT:
                {
                    // Пост опубликован, возвращаемся
                    pd.dismiss();

                    closeMe(true);
                    break;
                }
                case HANDLE_UMAIL_ACK:
                {
                    pd.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(android.R.string.ok).setCancelable(false).setMessage(R.string.message_send_ok);
                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            closeMe(false);
                        }
                    });
                    builder.create().show();
                    break;
                }
                case HANDLE_UMAIL_REJ:
                {
                    pd.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(android.R.string.no).setCancelable(false).setMessage(R.string.message_send_error);
                    builder.setPositiveButton(android.R.string.no, new DialogInterface.OnClickListener()
                    {

                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            closeMe(false);
                        }
                    });
                    builder.create().show();
                    break;
                }
                case HANDLE_REQUEST_AVATARS:
                {
                    for(int i = 0; i < avatarMap.size(); i++)
                    {
                        ImageButton current = new ImageButton(getActivity());
                        current.setImageDrawable((Drawable) avatarMap.valueAt(i));
                        current.setTag(avatarMap.keyAt(i));
                        current.setOnClickListener(MessageSenderFragment.this);
                        mAvatars.addView(current);
                    }
                    pd.dismiss();
                    break;
                }
                case Utils.HANDLE_UPLOAD_FILE:
                {
                    int cursorPos = contentText.getSelectionStart();
                    contentText.setText(contentText.getText().toString().substring(0, cursorPos) + message.obj + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                    contentText.setSelection(contentText.getText().toString().indexOf("/>", cursorPos));
                    break;
                }
                case HANDLE_PROGRESS:
                    pd.setProgress((int)message.obj);
                    break;
                default:
                    break;
            }

            return false;
        }
    };

    public void prepareFragment(String signature, String sendURL, String typeId, String id, String contents)
    {
        mService = NetworkService.getInstance(getActivity());
        assert(mService != null);
        mDHCL = mService.mDHCL;

        // обязательно
        mSignature = signature;
        mSendURL = sendURL;

        // одно из двух
        mTypeId = typeId;
        mId = id;

        clearPage();

        // Если это пост
        if(mTypeId.equals("DiaryId"))
        {
            mTitle.setText(R.string.new_post);
            mCurrentPage.setText(mService.mUser.currentDiaryPage.getContent().title());

            for(View v : umailElements)
                v.setVisibility(View.GONE);

            for(View v : commentElements)
                v.setVisibility(View.GONE);

            for(View v : postElements)
                v.setVisibility(View.VISIBLE);
        }
        else if (mTypeId.equals("PostId")) // если это комментарий
        {
            mTitle.setText(R.string.new_comment);
            mCurrentPage.setText(mService.mUser.currentDiaryPage.getContent().title());

            for(View v : umailElements)
                v.setVisibility(View.GONE);

            for(View v : postElements)
                v.setVisibility(View.GONE);


            for(View v : commentElements)
                v.setVisibility(View.VISIBLE);
        }
        else if(mTypeId.equals("umailTo")) // Если почта
        {
            mTitle.setText(R.string.new_umail);
            mCurrentPage.setVisibility(View.GONE);

            for(View v : commentElements)
                v.setVisibility(View.GONE);

            for(View v : postElements)
                v.setVisibility(View.GONE);

            for(View v : umailElements)
                v.setVisibility(View.VISIBLE);

            if(mId != null)
                toText.setText(mId);
            if(contents != null)
                titleText.setText(contents);
        }
        else if(mTypeId.equals("CommentEditId")) // Редактирование коммента
        {
            mTitle.setText(R.string.edit_comment);
            mCurrentPage.setText(mService.mUser.currentDiaryPage.getContent().title());

            for(View v : umailElements)
                v.setVisibility(View.GONE);

            for(View v : postElements)
                v.setVisibility(View.GONE);

            for(View v : commentElements)
                v.setVisibility(View.VISIBLE);

            mPost = new Comment();
            mPost.deserialize(contents);
            prepareUi(mPost);
        }
        else if(mTypeId.equals("PostEditId")) // Редактирование поста (самое зло)
        {
            mTitle.setText(R.string.edit_post);
            mCurrentPage.setText(mService.mUser.currentDiaryPage.getContent().title());

            for(View v : umailElements)
                v.setVisibility(View.GONE);

            for(View v : commentElements)
                v.setVisibility(View.GONE);

            for(View v : postElements)
                v.setVisibility(View.VISIBLE);

            mPost = new Post();
            mPost.deserialize(contents);
            prepareUi((Post) mPost);
        }
    }

    private void prepareUi(Comment comment)
    {
        contentText.setText(comment.content);
    }

    private void prepareUi(Post post)
    {
        titleText.setText(post.title);
        contentText.setText(post.content);

        if(!"".equals(post.music + post.mood + post.themes))
        {
            mShowOptionals.setChecked(true);
            musicText.setText(post.music);
            moodText.setText(post.mood);
            themesText.setText(post.themes);
        }

        if(!post.pollTitle.equals(""))
        {
            mShowPoll.setChecked(true);
            mPollTitle.setText(post.pollTitle);
            mPollChoice1.setText(post.pollAnswer1);
            mPollChoice2.setText(post.pollAnswer2);
            mPollChoice3.setText(post.pollAnswer3);
            mPollChoice4.setText(post.pollAnswer4);
            mPollChoice5.setText(post.pollAnswer5);
            mPollChoice6.setText(post.pollAnswer6);
            mPollChoice7.setText(post.pollAnswer7);
            mPollChoice8.setText(post.pollAnswer8);
            mPollChoice9.setText(post.pollAnswer9);
            mPollChoice10.setText(post.pollAnswer10);
        }

        if(!post.closeAccessMode.equals(""))
        {
            mShowAndClose.setChecked(true);
            if(post.closeAccessMode.equals("6"))
                mCloseOpts.check(R.id.close_only_reg);
            else if(post.closeAccessMode.equals("1"))
                mCloseOpts.check(R.id.close_only_fav);
            else if(post.closeAccessMode.equals("5"))
                mCloseOpts.check(R.id.close_only_sub);
            else if(post.closeAccessMode.equals("4"))
                mCloseOpts.check(R.id.close_only_white);
            else if(post.closeAccessMode.equals("3"))
                mCloseOpts.check(R.id.close_for_list);
            else if(post.closeAccessMode.equals("2"))
                mCloseOpts.check(R.id.close_only_list);
            else if(post.closeAccessMode.equals("7"))
                mCloseOpts.check(R.id.close_for_all);

            mCloseText.setText(post.closeText);
            mCloseAllowList.setText(post.closeAllowList);
            mCloseDenyList.setText(post.closeDenyList);
        }
    }

    public void onClick(View view) 
    {
        if(view instanceof ImageButton && view.getTag() != null)
        {
            postParams.clear();
            postParams.add(new BasicNameValuePair("use_avatar_id", view.getTag().toString()));
            postParams.add(new BasicNameValuePair("avatar_url", ""));
            postParams.add(new BasicNameValuePair("signature", mSignature));
            mHandler.sendEmptyMessage(HANDLE_SET_AVATAR);
        }

        switch(view.getId())
        {
            case R.id.message_publish:
            {
                // TODO: Сохранение в черновики
                postParams.clear();

                // Добавляем параметры из настроек
                postParams.add(new BasicNameValuePair("signature", mSignature));
                postParams.add(new BasicNameValuePair("action", "dosend"));
                pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.sending_data), true, false);

                // Если пост
                if(mTypeId.equals("DiaryId"))
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));

                    postParams.add(new BasicNameValuePair("act", "new_post_post"));
                    postParams.add(new BasicNameValuePair("post_id", ""));
                    postParams.add(new BasicNameValuePair("journal_id", mId));
                    postParams.add(new BasicNameValuePair("referer", mDHCL.currentURL));
                    postParams.add(new BasicNameValuePair("post_type", ""));

                    postParams.add(new BasicNameValuePair("title", titleText.getText().toString()));
                    if(mShowOptionals.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("themes", themesText.getText().toString() + mService.mPreferences.getString("post.tags", "")));
                        postParams.add(new BasicNameValuePair("current_music", musicText.getText().toString()));
                        postParams.add(new BasicNameValuePair("current_mood", moodText.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("themes", ""));
                        postParams.add(new BasicNameValuePair("current_music", ""));
                        postParams.add(new BasicNameValuePair("current_mood", ""));
                    }

                    postParams.add(new BasicNameValuePair("attachment", ""));

                    if(mShowPoll.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("poll_title", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_1", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_2", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_3", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_4", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_5", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_6", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_7", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_8", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_9", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_10", ""));
                    }

                    if(mShowAndClose.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("private_post", "1"));
                        if(!mCloseText.getText().toString().equals(""))
                        {
                            postParams.add(new BasicNameValuePair("check_close_text", "1"));
                            postParams.add(new BasicNameValuePair("close_text", mCloseText.getText().toString()));
                        }

                        switch(mCloseOpts.getCheckedRadioButtonId())
                        {
                            case R.id.close_only_reg:
                                postParams.add(new BasicNameValuePair("close_access_mode", "6"));
                            break;
                            case R.id.close_only_fav:
                                postParams.add(new BasicNameValuePair("close_access_mode", "1"));
                            break;
                            case R.id.close_only_sub:
                                postParams.add(new BasicNameValuePair("close_access_mode", "5"));
                            break;
                            case R.id.close_only_white:
                                postParams.add(new BasicNameValuePair("close_access_mode", "4"));
                            break;
                            case R.id.close_for_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "2"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseDenyList.getText().toString()));
                            break;
                            case R.id.close_only_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "3"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseAllowList.getText().toString()));
                            break;
                            case R.id.close_for_all:
                                postParams.add(new BasicNameValuePair("close_access_mode", "7"));
                            break;
                        }
                    }

                    postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
                    postParams.add(new BasicNameValuePair("save_type", "js2"));

                    mHandler.sendEmptyMessage(HANDLE_DO_POST);
                }
                else if(mTypeId.equals("PostEditId")) // Если редактируем пост
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString()));
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));

                    postParams.add(new BasicNameValuePair("act", "edit_post_post"));
                    postParams.add(new BasicNameValuePair("post_id", mPost.postID));
                    postParams.add(new BasicNameValuePair("journal_id", ((Post) mPost).diaryID));
                    postParams.add(new BasicNameValuePair("referer", mDHCL.currentURL));
                    postParams.add(new BasicNameValuePair("post_type", ""));

                    postParams.add(new BasicNameValuePair("title", ((Post) mPost).title));
                    if(mShowOptionals.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("themes", ((Post) mPost).themes));
                        postParams.add(new BasicNameValuePair("current_music", ((Post) mPost).music));
                        postParams.add(new BasicNameValuePair("current_mood", ((Post) mPost).mood));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("themes", ""));
                        postParams.add(new BasicNameValuePair("current_music", ""));
                        postParams.add(new BasicNameValuePair("current_mood", ""));
                    }

                    postParams.add(new BasicNameValuePair("attachment", ""));

                    if(mShowPoll.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("poll_title", mPollTitle.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_1", mPollChoice1.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_2", mPollChoice2.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_3", mPollChoice3.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_4", mPollChoice4.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_5", mPollChoice5.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_6", mPollChoice6.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_7", mPollChoice7.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_8", mPollChoice8.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_9", mPollChoice9.getText().toString()));
                        postParams.add(new BasicNameValuePair("poll_answer_10", mPollChoice10.getText().toString()));
                    }
                    else
                    {
                        postParams.add(new BasicNameValuePair("poll_title", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_1", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_2", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_3", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_4", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_5", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_6", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_7", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_8", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_9", ""));
                        postParams.add(new BasicNameValuePair("poll_answer_10", ""));
                    }

                    if(mShowAndClose.isChecked())
                    {
                        postParams.add(new BasicNameValuePair("private_post", "1"));
                        if(!mCloseText.getText().toString().equals(""))
                        {
                            postParams.add(new BasicNameValuePair("check_close_text", "1"));
                            postParams.add(new BasicNameValuePair("close_text", mCloseText.getText().toString()));
                        }

                        switch(mCloseOpts.getCheckedRadioButtonId())
                        {
                            case R.id.close_only_reg:
                                postParams.add(new BasicNameValuePair("close_access_mode", "6"));
                            break;
                            case R.id.close_only_fav:
                                postParams.add(new BasicNameValuePair("close_access_mode", "1"));
                            break;
                            case R.id.close_only_sub:
                                postParams.add(new BasicNameValuePair("close_access_mode", "5"));
                            break;
                            case R.id.close_only_white:
                                postParams.add(new BasicNameValuePair("close_access_mode", "4"));
                            break;
                            case R.id.close_for_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "2"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseDenyList.getText().toString()));
                            break;
                            case R.id.close_only_list:
                                postParams.add(new BasicNameValuePair("close_access_mode", "3"));
                                postParams.add(new BasicNameValuePair("access_list", mCloseAllowList.getText().toString()));
                            break;
                            case R.id.close_for_all:
                                postParams.add(new BasicNameValuePair("close_access_mode", "7"));
                            break;
                        }
                    }

                    postParams.add(new BasicNameValuePair("rewrite", "rewrite"));
                    postParams.add(new BasicNameValuePair("save_type", "js2"));

                    mHandler.sendEmptyMessage(HANDLE_DO_POST);
                }
                else if(mTypeId.equals("PostId"))  // если коммент
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));

                    postParams.add(new BasicNameValuePair("act", "new_comment_post"));
                    postParams.add(new BasicNameValuePair("post_id", mId));
                    postParams.add(new BasicNameValuePair("commentid", ""));
                    postParams.add(new BasicNameValuePair("referer", ""));
                    postParams.add(new BasicNameValuePair("page", "last"));
                    postParams.add(new BasicNameValuePair("open_uri", ""));

                    postParams.add(new BasicNameValuePair("write_from", "0"));
                    //postParams.add(new BasicNameValuePair("write_from_name", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_USERNAME, "")));
                    //postParams.add(new BasicNameValuePair("write_from_pass", Globals.mSharedPrefs.getString(AuthorizationForm.KEY_PASSWORD, "")));

                    postParams.add(new BasicNameValuePair("subscribe", mSubscribe.isChecked() ? "1/" : ""));
                    postParams.add(new BasicNameValuePair("attachment1", ""));

                    mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
                }
                else if(mTypeId.equals("CommentEditId"))  // если редактируем коммент
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString()));
                    postParams.add(new BasicNameValuePair("avatar", "1")); // Показываем аватарку
                    postParams.add(new BasicNameValuePair("module", "journal"));
                    postParams.add(new BasicNameValuePair("resulttype", "2"));

                    postParams.add(new BasicNameValuePair("act", "edit_comment_post"));
                    postParams.add(new BasicNameValuePair("post_id", mPost.postID));
                    postParams.add(new BasicNameValuePair("commentid", mPost.commentID));
                    postParams.add(new BasicNameValuePair("referer", ""));
                    postParams.add(new BasicNameValuePair("page", "last"));
                    postParams.add(new BasicNameValuePair("open_uri", ""));

                    postParams.add(new BasicNameValuePair("write_from", "0"));

                    postParams.add(new BasicNameValuePair("subscribe", mSubscribe.isChecked() ? "1/" : ""));
                    postParams.add(new BasicNameValuePair("attachment1", ""));

                    mHandler.sendEmptyMessage(HANDLE_DO_COMMENT);
                }
                else if(mTypeId.equals("umailTo"))  // если почта
                {
                    postParams.add(new BasicNameValuePair("message", contentText.getText().toString() + mService.mPreferences.getString("post.signature", "")));
                    postParams.add(new BasicNameValuePair("module", "umail"));
                    postParams.add(new BasicNameValuePair("act", "umail_send"));
                    postParams.add(new BasicNameValuePair("from_folder", ""));
                    postParams.add(new BasicNameValuePair("to_user", toText.getText().toString()));
                    postParams.add(new BasicNameValuePair("title", titleText.getText().toString()));
                    postParams.add(new BasicNameValuePair("save_copy", mCopyMessage.isChecked() ? "yes" : ""));
                    postParams.add(new BasicNameValuePair("need_receipt", mGetReceipt.isChecked() ? "yes" : ""));

                    mHandler.sendEmptyMessage(HANDLE_DO_UMAIL);
                }
            break;
            }
            case R.id.left_gradient:
            case R.id.right_gradient:
            {
                final ImageButton imgbutton = (ImageButton) view;
                int oldColor = getColorFromPicture(imgbutton);
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(getActivity(), oldColor, new AmbilWarnaDialog.OnAmbilWarnaListener()
                {
                    public void onOk(AmbilWarnaDialog dialog, int color)
                    {
                        ColorDrawable newColor = new ColorDrawable(color);
                        imgbutton.setImageDrawable(newColor);
                    }

                    public void onCancel(AmbilWarnaDialog dialog)
                    {
                    }
                });
                dialog.show();
            }
            break;
            case R.id.set_gradient:
            {
                int startColor = getColorFromPicture(mLeftGradient);
                int endColor = getColorFromPicture(mRightGradient);

                CharSequence text = contentText.getText();
                String newText = "";
                int length = text.length();
                for(int i = 0; i < text.length(); i++)
                {
                    char current = text.charAt(i);
                    if (current == ' ' || current == '\n')
                    {
                        newText += current;
                        continue;
                    }
                    int newRed = ((Color.red(startColor) - Color.red(startColor) * i / length) + (Color.red(endColor) - Color.red(endColor) * (length - i) / text.length()));
                    int newGreen = ((Color.green(startColor) - Color.green(startColor) * i / length) + (Color.green(endColor) - Color.green(endColor) * (length - i) / text.length()));
                    int newBlue = ((Color.blue(startColor) - Color.blue(startColor) * i / length) + (Color.blue(endColor) - Color.blue(endColor) * (length - i) / text.length()));
                    String red = String.format("%02X", newRed > 0xFF ? 0xFF : newRed);
                    String green = String.format("%02X", newGreen > 0xFF ? 0xFF : newGreen);
                    String blue = String.format("%02X", newBlue > 0xFF ? 0xFF : newBlue);

                    String addiction = "<span style=\"color: #" + red + green + blue + "\">" + current + "</span>";
                    newText += addiction;
                }
                contentText.setText(newText);
            }
            break;
        }
    }

    public int getColorFromPicture(ImageButton view)
    {
        Drawable old = view.getDrawable();
        Bitmap example = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas tCanvas = new Canvas(example);
        old.draw(tCanvas);
        return example.getPixel(0, 0);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        switch(buttonView.getId())
        {
            case R.id.message_optional:
                if(isChecked)
                    for(View view : optionals)
                        view.setVisibility(View.VISIBLE);
                else
                    for(View view : optionals)
                        view.setVisibility(View.GONE);
                break;
            case R.id.message_poll:
                if(isChecked)
                    for(View view : pollScheme)
                        view.setVisibility(View.VISIBLE);
                else
                    for(View view : pollScheme)
                        view.setVisibility(View.GONE);
                break;
            case R.id.message_close:
                if(isChecked)
                    mCloseOpts.setVisibility(View.VISIBLE);
                else
                    mCloseOpts.setVisibility(View.GONE);
                break;
            case R.id.message_custom_avatar:
                if(isChecked)
                {
                    mAvatars.setVisibility(View.VISIBLE);
                    if(avatarMap == null)
                    {
                        pd = ProgressDialog.show(getActivity(), getString(R.string.loading), getString(R.string.sending_data), true, true);
                        mHandler.sendEmptyMessage(HANDLE_REQUEST_AVATARS);
                    }
                }
                else
                    mAvatars.setVisibility(View.GONE);
            default:
                break;
        }
    }

    public void onCheckedChanged(RadioGroup group, int checkedId)
    {
        switch(checkedId)
        {
            case R.id.close_for_list:
                mCloseDenyList.setVisibility(View.VISIBLE);
                mCloseAllowList.setVisibility(View.GONE);
            break;
            case R.id.close_only_list:
                mCloseAllowList.setVisibility(View.VISIBLE);
                mCloseDenyList.setVisibility(View.GONE);
            break;
            default:
                mCloseDenyList.setVisibility(View.GONE);
                mCloseAllowList.setVisibility(View.GONE);
            break;
        }
    }

    @SuppressWarnings("deprecation")
    public void acceptDialogClick(View view, boolean pasteClipboard)
    {
        int cursorPos = contentText.getSelectionStart();
        if(cursorPos == -1)
            cursorPos = contentText.getText().length();

        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        CharSequence paste = clipboard.getText();
        if(paste == null || !pasteClipboard)
            paste = "";

        switch (view.getId())
        {
            case R.id.button_bold:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<b>" + paste.toString() + "</b>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</b>", cursorPos));
                break;
            }
            case R.id.button_italic:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<i>" + paste.toString() + "</i>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</i>", cursorPos));
                break;
            }
            case R.id.button_underlined:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<u>" + paste.toString() + "</u>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</u>", cursorPos));
                break;
            }
            case R.id.button_nick:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<L>" + paste.toString() + "</L>" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("</L>", cursorPos));
                break;
            }
            case R.id.button_link:
            {
                contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<a href=\"" + paste.toString() + "\" />" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                contentText.setSelection(contentText.getText().toString().indexOf("/>", cursorPos));
                break;
            }
            case R.id.button_image:
            {

                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if(pasteClipboard)
                {
                    contentText.setText(contentText.getText().toString().substring(0, cursorPos) + "<img src=\"" + paste.toString() + "\" />" + contentText.getText().toString().substring(cursorPos, contentText.getText().length()));
                    contentText.setSelection(contentText.getText().toString().indexOf("/>", cursorPos));
                }
                else
                    try
                    {
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), 0);
                    } catch (android.content.ActivityNotFoundException ex)
                    {
                        Toast.makeText(getActivity(), getString(R.string.no_file_manager_found), Toast.LENGTH_SHORT).show();
                    }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case 0:
                if (resultCode == Activity.RESULT_OK)
                {
                    Uri uri = data.getData();
                    File file = null;
                    if (ContentResolver.SCHEME_CONTENT.equalsIgnoreCase(uri.getScheme()))
                    {
                        String[] projection = { "_data" };
                        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
                        int column_index = cursor.getColumnIndex("_data");
                        if (cursor.moveToFirst())
                            file = new File(cursor.getString(column_index));

                    }
                    else if ("file".equalsIgnoreCase(uri.getScheme()))
                        file = new File(uri.getPath());

                    try
                    {
                        if (file != null)
                        {
                            final Message msg = mHandler.obtainMessage(Utils.HANDLE_UPLOAD_FILE, file.getCanonicalPath());
                            msg.arg1 = 3;
                            AlertDialog.Builder origOrMoreOrLink = new AlertDialog.Builder(getActivity());
                            DialogInterface.OnClickListener selector = new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    switch(which)
                                    {
                                        case DialogInterface.BUTTON_NEGATIVE:
                                            msg.arg1 = 1;
                                            break;
                                        case DialogInterface.BUTTON_NEUTRAL:
                                            msg.arg1 = 2;
                                            break;
                                        case DialogInterface.BUTTON_POSITIVE:
                                        default:
                                            msg.arg1 = 3;
                                            break;
                                    }

                                    pd = new ProgressDialog(getActivity());
                                    pd.setIndeterminate(false);
                                    pd.setTitle(R.string.loading);
                                    pd.setMessage(getString(R.string.sending_data));
                                    pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                    pd.show();
                                    mHandler.sendMessage(msg);
                                }
                            };
                            origOrMoreOrLink.setTitle(R.string.howto_send_img);
                            origOrMoreOrLink.setNegativeButton(R.string.pack_inoriginal, selector);
                            origOrMoreOrLink.setPositiveButton(R.string.pack_inmore, selector);
                            origOrMoreOrLink.setNeutralButton(R.string.pack_inlink, selector);
                            origOrMoreOrLink.create().show();
                        }
                        else
                            Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                    } catch (IOException e)
                    {
                        Toast.makeText(getActivity(), getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void closeMe(boolean reload)
    {
        if(getActivity() instanceof  DiaryActivity)
            ((DiaryActivity)getActivity()).onFragmentRemove(reload);
    }

    private void clearPage()
    {
        titleText.setText("");
        contentText.setText("");
        toText.setText("");

        mShowOptionals.setChecked(false);
        themesText.setText("");
        musicText.setText("");
        moodText.setText("");

        mShowPoll.setChecked(false);
        mPollTitle.setText("");
        mPollChoice1.setText("");
        mPollChoice2.setText("");
        mPollChoice3.setText("");
        mPollChoice4.setText("");
        mPollChoice5.setText("");
        mPollChoice6.setText("");
        mPollChoice7.setText("");
        mPollChoice8.setText("");
        mPollChoice9.setText("");
        mPollChoice10.setText("");

        mShowAndClose.setChecked(false);
        mCloseOpts.check(R.id.close_only_reg);
        mCloseText.setText("");
        mCloseAllowList.setText("");
        mCloseDenyList.setText("");

        mCustomAvatar.setChecked(false);
    }
}
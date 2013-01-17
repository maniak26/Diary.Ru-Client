package adonai.diary_browser.entities;

import org.jsoup.nodes.Element;

import android.graphics.drawable.Drawable;

public class Post
{
    /*
     * обработка
     */
    
    // Идентификатор поста
    private String _ID = "";
    // Автор поста
    private String _author = "";
    // ССылка на страничку автора
    private String _author_URL = "";
    //  название комьюнити поста
    private String _community = "";
    // ссылка на комьюнити поста
    private String _community_URL = "";
    // ссылка на пост
    private String _URL = "";
    // Содержимое поста
    private Element _content = null;
    // Дата размещения поста
    private String _date;
    // Заголовок поста
    private String _title = "";
    // Число комментариев к посту
    private String _comment_count = "";
    // Аватарка автора ^_^
    private Drawable _author_avatar = null;
    
    /*
     * постинг
     */
    
    // Настроение
    private String _mood = "";
    // Темы
    private String _themes = "";
    // Тэги
    private String _tags = "";
    // Музыка
    private String _music = "";
    
    // Является ли пост эпиграфом?
    private boolean epigraph = false;
    
    /**
     * @return the _author
     */
    public String get_author()
    {
        return _author;
    }
    
    /**
     * @param _author
     *            the _author to set
     */
    public void set_author(String _author)
    {
        this._author = _author;
    }
    
    /**
     * @return the _URL
     */
    public String get_URL()
    {
        return _URL;
    }
    
    /**
     * @param _URL
     *            the _URL to set
     */
    public void set_URL(String _URL)
    {
        this._URL = _URL;
    }
    
    /**
     * @return the _date
     */
    public String get_date()
    {
        return _date;
    }
    
    /**
     * @param _date
     *            the _date to set
     */
    public void set_date(String _date)
    {
        this._date = _date;
    }
    
    /**
     * @return the _author_avatar
     */
    public Drawable get_author_avatar()
    {
        return _author_avatar;
    }
    
    /**
     * @param _author_avatar
     *            the _author_avatar to set
     */
    public void set_author_avatar(Drawable _author_avatar)
    {
        this._author_avatar = _author_avatar;
    }
    
    public String get_title()
    {
        return _title;
    }
    
    public void set_title(String _title)
    {
        this._title = _title;
    }

    public String get_author_URL()
    {
        return _author_URL;
    }

    public void set_author_URL(String _author_URL)
    {
        this._author_URL = _author_URL;
    }

    /**
     * @return the _comment_count
     */
    final public String get_comment_count()
    {
        return _comment_count;
    }

    /**
     * @param _comment_count the _comment_count to set
     */
    final public void set_comment_count(String _comment_count)
    {
        this._comment_count = _comment_count;
    }

	public String get_community() 
	{
		return _community;
	}

	public void set_community(String _community) 
	{
		this._community = _community;
	}

	final public String get_community_URL() 
	{
		return _community_URL;
	}

	final public void set_community_URL(String _community_URL) 
	{
		this._community_URL = _community_URL;
	}

	public String get_mood() 
	{
		return _mood;
	}

	public void set_mood(String _mood) 
	{
		this._mood = _mood;
	}

	public String get_themes() 
	{
		return _themes;
	}

	public void set_themes(String _themes) 
	{
		this._themes = _themes;
	}

	public String get_tags() 
	{
		return _tags;
	}

	public void set_tags(String _tags) 
	{
		this._tags = _tags;
	}

	public String get_music() {
		return _music;
	}

	public void set_music(String _music) {
		this._music = _music;
	}

	public String get_ID() 
	{
		return _ID;
	}

	public void set_ID(String _ID) 
	{
		this._ID = _ID;
	}

	public void setIsEpigraph(boolean equals) 
	{
		epigraph = equals;
	}
	
	public boolean isEpigraph() 
	{
		return epigraph;
	}

    public Element get_content()
    {
        return _content;
    }

    public void set_content(Element _content)
    {
        this._content = _content;
    }
}